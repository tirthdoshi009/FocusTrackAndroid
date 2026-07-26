package com.focustrack.app.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.focustrack.app.data.AppCategories
import com.focustrack.app.data.CategoryKind
import com.focustrack.app.data.CategoryOverrides
import com.focustrack.app.data.db.DailyStatEntity
import com.focustrack.app.data.db.FocusTrackDatabase
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

data class AppUsage(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
    val kind: CategoryKind,
    val categoryName: String,
    val totalMs: Long,
    val isOverridden: Boolean,
)

/** Lightweight per-day aggregate for the Trends screen (no per-app icons). */
data class DayStat(
    val date: LocalDate,
    val totalMs: Long,
    val categorizedMs: Long,
    val msByKind: Map<CategoryKind, Long>,
    val focusScore: Int,
) {
    val hasUsage: Boolean get() = totalMs > 0L
    val hasScore: Boolean get() = categorizedMs > 0L
}

data class DailySummary(
    val apps: List<AppUsage>,
    val msByKind: Map<CategoryKind, Long>,
    val totalMs: Long,
    // Time spent in classified categories (excludes Uncategorized).
    val categorizedMs: Long,
    val focusScore: Int,
) {
    val hasUsage: Boolean get() = totalMs > 0L
    val hasScore: Boolean get() = categorizedMs > 0L

    companion object {
        val EMPTY = DailySummary(emptyList(), emptyMap(), 0L, 0L, 0)
    }
}

/**
 * Reads per-app foreground durations for "today" from UsageStatsManager,
 * using foreground/background events for accuracy, and categorizes them.
 */
class UsageRepository(private val context: Context) {

    private val overrides = CategoryOverrides(context)
    private val dao = FocusTrackDatabase.get(context).dailyStatDao()
    private val ownPackage = context.packageName
    private val trackableCache = HashMap<String, Boolean>()

    /**
     * Whether a package represents meaningful user-facing usage. Excludes this
     * app itself and non-launchable packages (system UI, launchers, keyboards,
     * background services) which shouldn't count toward "your app usage".
     */
    private fun isTrackable(pm: android.content.pm.PackageManager, pkg: String): Boolean {
        if (pkg == ownPackage) return false
        return trackableCache.getOrPut(pkg) {
            pm.getLaunchIntentForPackage(pkg) != null
        }
    }

    fun getTodaySummary(): DailySummary {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val pm = context.packageManager
        val start = startOfToday()
        val end = System.currentTimeMillis()

        val durations = aggregateForegroundMs(usm, start, end)
        if (durations.isEmpty()) return DailySummary.EMPTY

        val apps = durations
            .filter { it.value > 0L && isTrackable(pm, it.key) }
            .map { (pkg, ms) ->
                val cat = AppCategories.categoryFor(pkg)
                val override = overrides.kindFor(pkg)
                val kind = override ?: cat?.kind ?: CategoryKind.UNKNOWN
                val (label, icon) = appInfoFor(pm, pkg)
                AppUsage(
                    packageName = pkg,
                    label = label,
                    icon = icon,
                    kind = kind,
                    // Hide the default descriptive name once the user overrides
                    // the category, to avoid mismatches like "Productive · Short-form video".
                    categoryName = if (override != null) "" else (cat?.name ?: ""),
                    totalMs = ms,
                    isOverridden = override != null,
                )
            }
            .sortedByDescending { it.totalMs }

        val msByKind = apps.groupBy { it.kind }
            .mapValues { entry -> entry.value.sumOf { it.totalMs } }
        val total = apps.sumOf { it.totalMs }
        val categorized = total - (msByKind[CategoryKind.UNKNOWN] ?: 0L)

        return DailySummary(
            apps = apps,
            msByKind = msByKind,
            totalMs = total,
            categorizedMs = categorized,
            focusScore = focusScore(msByKind, categorized),
        )
    }

    /**
     * Computes today's full summary and the last [days]-day series in a single
     * pass. Today is derived from the same computation used for the Today screen
     * (no drift between tabs). Past days are frozen in Room the first time
     * they're captured, so history stays stable and survives beyond the
     * ~7-day UsageStatsManager event window.
     */
    suspend fun loadAll(days: Int = 7): Pair<DailySummary, List<DayStat>> {
        val summary = getTodaySummary()
        val today = LocalDate.now()
        val todayStat = DayStat(
            date = today,
            totalMs = summary.totalMs,
            categorizedMs = summary.categorizedMs,
            msByKind = summary.msByKind,
            focusScore = summary.focusScore,
        )

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val startDate = today.minusDays((days - 1).toLong())

        val existing = dao.since(startDate.toString()).associateBy { it.date }
        val toStore = ArrayList<DailyStatEntity>()
        // Always refresh today; capture past days once (don't retroactively rewrite).
        toStore.add(todayStat.toEntity())
        for (i in days - 1 downTo 1) {
            val date = today.minusDays(i.toLong())
            if (!existing.containsKey(date.toString())) {
                val stat = dayStatFrom(usm, date, zone, now)
                if (stat.hasUsage) toStore.add(stat.toEntity())
            }
        }
        dao.upsertAll(toStore)

        val stored = dao.since(startDate.toString()).associateBy { it.date }
        val week = (0 until days).map { offset ->
            val date = startDate.plusDays(offset.toLong())
            if (date == today) todayStat
            else stored[date.toString()]?.toDayStat() ?: emptyDay(date)
        }
        return summary to week
    }

    private fun emptyDay(date: LocalDate) =
        DayStat(date = date, totalMs = 0L, categorizedMs = 0L, msByKind = emptyMap(), focusScore = 0)

    private fun DayStat.toEntity() = DailyStatEntity(
        date = date.toString(),
        totalMs = totalMs,
        categorizedMs = categorizedMs,
        riskyMs = msByKind[CategoryKind.RISKY] ?: 0L,
        productiveMs = msByKind[CategoryKind.PRODUCTIVE] ?: 0L,
        neutralMs = msByKind[CategoryKind.NEUTRAL] ?: 0L,
        uncategorizedMs = msByKind[CategoryKind.UNKNOWN] ?: 0L,
        focusScore = focusScore,
    )

    private fun DailyStatEntity.toDayStat() = DayStat(
        date = LocalDate.parse(date),
        totalMs = totalMs,
        categorizedMs = categorizedMs,
        msByKind = buildMap {
            if (riskyMs > 0L) put(CategoryKind.RISKY, riskyMs)
            if (productiveMs > 0L) put(CategoryKind.PRODUCTIVE, productiveMs)
            if (neutralMs > 0L) put(CategoryKind.NEUTRAL, neutralMs)
            if (uncategorizedMs > 0L) put(CategoryKind.UNKNOWN, uncategorizedMs)
        },
        focusScore = focusScore,
    )

    /** Lightweight today aggregate (no app icons/labels) for the widget. */
    fun getTodayStat(): DayStat {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        return dayStatFrom(usm, LocalDate.now(), ZoneId.systemDefault(), System.currentTimeMillis())
    }

    /** Aggregates a single day's per-kind usage from events. */
    private fun dayStatFrom(
        usm: UsageStatsManager,
        date: LocalDate,
        zone: ZoneId,
        now: Long,
    ): DayStat {
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = minOf(date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(), now)
        val durations = aggregateForegroundMs(usm, start, end)
        val pm = context.packageManager

        val msByKind = HashMap<CategoryKind, Long>()
        var total = 0L
        for ((pkg, ms) in durations) {
            if (ms <= 0L || !isTrackable(pm, pkg)) continue
            val kind = overrides.kindFor(pkg) ?: AppCategories.kindFor(pkg)
            msByKind[kind] = (msByKind[kind] ?: 0L) + ms
            total += ms
        }
        val categorized = total - (msByKind[CategoryKind.UNKNOWN] ?: 0L)
        return DayStat(
            date = date,
            totalMs = total,
            categorizedMs = categorized,
            msByKind = msByKind,
            focusScore = focusScore(msByKind, categorized),
        )
    }

    private fun aggregateForegroundMs(
        usm: UsageStatsManager,
        start: Long,
        end: Long,
    ): Map<String, Long> {
        val totals = HashMap<String, Long>()
        val lastForeground = HashMap<String, Long>()
        val events = usm.queryEvents(start, end)
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND,
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    lastForeground[pkg] = event.timeStamp
                }

                UsageEvents.Event.MOVE_TO_BACKGROUND,
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val startedAt = lastForeground.remove(pkg) ?: continue
                    val delta = event.timeStamp - startedAt
                    if (delta in 1..MAX_SESSION_MS) {
                        totals[pkg] = (totals[pkg] ?: 0L) + delta
                    }
                }
            }
        }
        // Apps still in foreground at query end.
        for ((pkg, startedAt) in lastForeground) {
            val delta = end - startedAt
            if (delta in 1..MAX_SESSION_MS) {
                totals[pkg] = (totals[pkg] ?: 0L) + delta
            }
        }
        return totals
    }

    /**
     * Focus score (0-100) over *categorized* time only: productive counts
     * fully, neutral half, risky zero. Uncategorized time is excluded from
     * the denominator so unclassified apps don't unfairly tank the score.
     */
    private fun focusScore(msByKind: Map<CategoryKind, Long>, categorizedMs: Long): Int {
        if (categorizedMs <= 0L) return 0
        val productive = msByKind[CategoryKind.PRODUCTIVE] ?: 0L
        val neutral = msByKind[CategoryKind.NEUTRAL] ?: 0L
        val score = 100.0 * (productive + neutral * 0.5) / categorizedMs
        return score.toInt().coerceIn(0, 100)
    }

    /** Resolves a display label + launcher icon for a package, with fallbacks. */
    private fun appInfoFor(pm: PackageManager, pkg: String): Pair<String, ImageBitmap?> =
        try {
            val info = pm.getApplicationInfo(pkg, 0)
            val label = pm.getApplicationLabel(info).toString()
            val icon = runCatching {
                pm.getApplicationIcon(info).toBitmap(ICON_PX, ICON_PX).asImageBitmap()
            }.getOrNull()
            label to icon
        } catch (_: PackageManager.NameNotFoundException) {
            pkg.substringAfterLast('.') to null
        }

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private companion object {
        // Ignore implausibly long single sessions (e.g. >12h) from missed events.
        const val MAX_SESSION_MS = 12L * 60 * 60 * 1000
        const val ICON_PX = 96
    }
}
