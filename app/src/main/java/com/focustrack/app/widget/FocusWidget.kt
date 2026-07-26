package com.focustrack.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.focustrack.app.MainActivity
import com.focustrack.app.data.CategoryKind
import com.focustrack.app.usage.DayStat
import com.focustrack.app.usage.UsagePermission
import com.focustrack.app.usage.UsageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Home-screen widget showing today's focus score, screen time, and category mix. */
class FocusWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val granted = UsagePermission.hasAccess(context)
        val stat = if (granted) {
            withContext(Dispatchers.IO) { UsageRepository(context).getTodayStat() }
        } else {
            null
        }
        provideContent {
            GlanceTheme {
                WidgetContent(granted, stat)
            }
        }
    }
}

@Composable
private fun WidgetContent(granted: Boolean, stat: DayStat?) {
    val colors = GlanceTheme.colors
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier.fillMaxSize()
            .background(colors.primaryContainer)
            .cornerRadius(16.dp)
            .padding(16.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
    ) {
        Text(
            "FocusTrack",
            style = TextStyle(color = colors.onPrimaryContainer, fontSize = 12.sp),
        )
        if (!granted || stat == null) {
            Spacer(GlanceModifier.height(8.dp))
            Text(
                "Open the app to enable usage access",
                style = TextStyle(color = colors.onPrimaryContainer, fontSize = 14.sp),
            )
            return@Column
        }
        Spacer(GlanceModifier.height(6.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "Focus score",
                    style = TextStyle(color = colors.onPrimaryContainer, fontSize = 12.sp),
                )
                Text(
                    if (stat.categorizedMs > 0L) "${stat.focusScore}" else "\u2013",
                    style = TextStyle(
                        color = colors.onPrimaryContainer,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
            Spacer(GlanceModifier.defaultWeight())
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Screen time",
                    style = TextStyle(color = colors.onPrimaryContainer, fontSize = 12.sp),
                )
                Text(
                    formatDuration(stat.totalMs),
                    style = TextStyle(
                        color = colors.onPrimaryContainer,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
        Spacer(GlanceModifier.height(10.dp))
        CategoryLegend(stat)
    }
}

@Composable
private fun CategoryLegend(stat: DayStat) {
    val total = stat.totalMs.coerceAtLeast(1L)
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryKind.entries.forEach { kind ->
            val ms = stat.msByKind[kind] ?: 0L
            if (ms > 0L) {
                Row(
                    modifier = GlanceModifier.padding(end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = GlanceModifier.size(8.dp).cornerRadius(4.dp)
                            .background(ColorProvider(kind.light)),
                    ) {}
                    Spacer(GlanceModifier.width(4.dp))
                    Text(
                        "${(100f * ms / total).roundToInt()}%",
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimaryContainer,
                            fontSize = 12.sp,
                        ),
                    )
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalMinutes = ms / 60000
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
