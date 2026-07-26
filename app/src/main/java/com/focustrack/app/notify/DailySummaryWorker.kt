package com.focustrack.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.focustrack.app.MainActivity
import com.focustrack.app.R
import com.focustrack.app.data.CategoryKind
import com.focustrack.app.usage.DailySummary
import com.focustrack.app.usage.UsagePermission
import com.focustrack.app.usage.UsageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Computes today's summary once a day and posts a notification. */
class DailySummaryWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (!UsagePermission.hasAccess(ctx)) return Result.success()
        val summary = withContext(Dispatchers.IO) { UsageRepository(ctx).getTodaySummary() }
        if (summary.hasUsage) postNotification(ctx, summary)
        return Result.success()
    }

    private fun postNotification(ctx: Context, summary: DailySummary) {
        ensureChannel(ctx)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val title = if (summary.hasScore) {
            "Focus score ${summary.focusScore}"
        } else {
            "Today's usage"
        }
        val topRisky = summary.apps.firstOrNull { it.kind == CategoryKind.RISKY }
        val body = buildString {
            append("Screen time ${formatDuration(summary.totalMs)}")
            if (topRisky != null) {
                append("\nTop distraction: ${topRisky.label} (${formatDuration(topRisky.totalMs)})")
            }
        }

        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            ctx, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setContentTitle(title)
            .setContentText("Screen time ${formatDuration(summary.totalMs)}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(ctx).notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(ctx: Context) {
        val manager = ctx.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Daily summary",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "A daily recap of your focus score and screen time." }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "daily_summary"
        private const val NOTIFICATION_ID = 1001
        private const val WORK_NAME = "daily_summary_work"
        private const val SUMMARY_HOUR = 20 // 8 PM local time

        /** Schedules the daily summary for the next [SUMMARY_HOUR], repeating every 24h. */
        fun schedule(context: Context) {
            val now = Calendar.getInstance()
            val target = (now.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, SUMMARY_HOUR)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (!target.after(now)) target.add(Calendar.DAY_OF_MONTH, 1)
            val initialDelay = target.timeInMillis - now.timeInMillis

            val request = PeriodicWorkRequestBuilder<DailySummaryWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalMinutes = ms / 60000
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
