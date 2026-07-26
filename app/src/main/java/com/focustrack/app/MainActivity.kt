package com.focustrack.app

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focustrack.app.data.CategoryKind
import com.focustrack.app.data.themedColor
import com.focustrack.app.notify.DailySummaryWorker
import com.focustrack.app.ui.theme.FocusTrackTheme
import com.focustrack.app.usage.AppUsage
import com.focustrack.app.usage.DailySummary
import com.focustrack.app.usage.DayStat
import com.focustrack.app.usage.UsagePermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(applicationContext)
    }

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        DailySummaryWorker.schedule(applicationContext)
        maybeRequestNotificationPermission()
        setContent {
            FocusTrackTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val state by viewModel.state.collectAsStateWithLifecycle()
                    when (val s = state) {
                        DataState.Loading -> LoadingScreen()
                        DataState.NoAccess -> PermissionScreen(onGrant = {
                            startActivity(UsagePermission.settingsIntent())
                        })
                        is DataState.Ready -> MainScaffold(
                            summary = s.summary,
                            recentDays = s.week,
                            onSetCategory = viewModel::setCategory,
                            onClearCategory = viewModel::clearCategory,
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun PermissionScreen(onGrant: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "FocusTrack",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "To measure your app usage and focus score, enable " +
                "\"Usage access\" for FocusTrack.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onGrant) { Text("Enable usage access") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(
    summary: DailySummary,
    recentDays: List<DayStat>,
    onSetCategory: (String, CategoryKind) -> Unit,
    onClearCategory: (String) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()
    var showScoreInfo by remember { mutableStateOf(false) }
    val titles = listOf("Today", "Trends", "Discover", "Settings")
    val icons = listOf(
        Icons.Rounded.Today,
        Icons.Rounded.BarChart,
        Icons.Rounded.Lightbulb,
        Icons.Rounded.Settings,
    )
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                titles.forEachIndexed { i, title ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == i,
                        onClick = { scope.launch { pagerState.animateScrollToPage(i) } },
                        icon = { Icon(icons[i], contentDescription = title) },
                        label = { Text(title) },
                    )
                }
            }
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (page) {
                0 -> TodayScreen(
                    summary = summary,
                    contentPadding = padding,
                    onScoreInfo = { showScoreInfo = true },
                    onSetCategory = onSetCategory,
                    onClearCategory = onClearCategory,
                )
                1 -> TrendsScreen(recentDays, padding)
                2 -> DiscoverScreen(padding)
                else -> SettingsScreen(padding, onScoreInfo = { showScoreInfo = true })
            }
        }
    }

    if (showScoreInfo) {
        ScoreInfoSheet(onDismiss = { showScoreInfo = false })
    }
}

@Composable
private fun TodayScreen(
    summary: DailySummary,
    contentPadding: PaddingValues,
    onScoreInfo: () -> Unit,
    onSetCategory: (String, CategoryKind) -> Unit,
    onClearCategory: (String) -> Unit,
) {
    var editing by remember { mutableStateOf<AppUsage?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                "Today",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
        item {
            ScoreCard(
                summary,
                onInfo = onScoreInfo,
                onCategorize = { scope.launch { listState.animateScrollToItem(3) } },
            )
        }
        val topRisky = summary.apps.firstOrNull { it.kind == CategoryKind.RISKY }
        if (topRisky != null) {
            item { InsightCard(topRisky, summary.msByKind[CategoryKind.RISKY] ?: 0L) }
        }
        item { KindLegend(summary) }
        item {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    "By app",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    "Tap an app to change its category",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (summary.apps.isEmpty()) {
            item {
                Text(
                    "No tracked usage yet today.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        } else {
            items(summary.apps, key = { it.packageName }) { app ->
                AppRow(app, onEdit = { editing = app })
            }
        }
    }

    editing?.let { app ->
        CategoryEditorSheet(
            app = app,
            onPick = { kind ->
                onSetCategory(app.packageName, kind)
                editing = null
            },
            onReset = {
                onClearCategory(app.packageName)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun ScoreCard(summary: DailySummary, onInfo: () -> Unit, onCategorize: () -> Unit) {
    val onContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val coverage = if (summary.totalMs > 0L) {
        summary.categorizedMs.toFloat() / summary.totalMs
    } else {
        0f
    }
    val status = when {
        !summary.hasUsage -> "No usage data"
        !summary.hasScore -> "Not scored yet"
        coverage < 0.5f -> "Low confidence"
        else -> scoreLabel(summary.focusScore)
    }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = onContainer,
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScoreRing(summary.focusScore, hasScore = summary.hasScore)
                    Spacer(Modifier.size(16.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Focus score", style = MaterialTheme.typography.labelLarge)
                            IconButton(onClick = onInfo) {
                                Icon(
                                    Icons.Outlined.Info,
                                    contentDescription = "How your focus score works",
                                    tint = onContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        Text(
                            status,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Screen time", style = MaterialTheme.typography.labelLarge)
                    Text(
                        formatDuration(summary.totalMs),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            ProportionBar(summary)
            CoverageNote(summary, onCategorize)
        }
    }
}

@Composable
private fun CoverageNote(summary: DailySummary, onCategorize: () -> Unit) {
    when {
        !summary.hasUsage -> Unit
        !summary.hasScore -> {
            Spacer(Modifier.height(6.dp))
            TextButton(
                onClick = onCategorize,
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
            ) {
                Text(
                    "Categorize your apps to get a score →",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        summary.categorizedMs < summary.totalMs -> {
            val pct = (100f * summary.categorizedMs / summary.totalMs).roundToInt()
            Spacer(Modifier.height(10.dp))
            Text(
                "Based on $pct% of tracked time that's categorized",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun ScoreRing(score: Int, hasScore: Boolean) {
    val progress = if (hasScore) (score / 100f).coerceIn(0f, 1f) else 0f
    val ring = MaterialTheme.colorScheme.onPrimaryContainer
    val track = ring.copy(alpha = 0.18f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(84.dp).semantics {
            contentDescription = if (hasScore) "Focus score $score out of 100"
            else "Focus score not available yet"
        },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeW = 9.dp.toPx()
            val inset = strokeW / 2
            val arcSize = Size(size.width - strokeW, size.height - strokeW)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round),
            )
            if (hasScore) {
                drawArc(
                    color = ring,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round),
                )
            }
        }
        Text(
            if (hasScore) "$score" else "–",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = ring,
        )
    }
}

@Composable
private fun ProportionBar(summary: DailySummary) {
    val total = summary.totalMs
    val description = if (total <= 0L) {
        "No category breakdown yet"
    } else {
        "Category breakdown: " + CategoryKind.entries.mapNotNull { kind ->
            val ms = summary.msByKind[kind] ?: 0L
            if (ms > 0L) "${kind.label} ${(100f * ms / total).roundToInt()} percent" else null
        }.joinToString(", ")
    }
    Row(
        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))
            .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (total <= 0L) {
            Box(
                modifier = Modifier.weight(1f).fillMaxSize()
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
            )
        } else {
            for (kind in CategoryKind.entries) {
                val ms = summary.msByKind[kind] ?: 0L
                if (ms > 0L) {
                    Box(
                        modifier = Modifier.weight(ms.toFloat()).fillMaxSize()
                            .background(kind.themedColor())
                    )
                }
            }
        }
    }
}

private fun scoreLabel(score: Int): String = when {
    score >= 80 -> "Excellent"
    score >= 60 -> "Good"
    score >= 40 -> "Fair"
    else -> "Needs focus"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScoreInfoSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(
                "How your focus score works",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "The share of your categorized time spent productively:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            ScoreRuleRow(CategoryKind.PRODUCTIVE, "counts fully")
            ScoreRuleRow(CategoryKind.NEUTRAL, "counts half")
            ScoreRuleRow(CategoryKind.RISKY, "counts as zero")
            ScoreRuleRow(CategoryKind.UNKNOWN, "not counted")
            Spacer(Modifier.height(24.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Got it")
            }
        }
    }
}

@Composable
private fun ScoreRuleRow(kind: CategoryKind, rule: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(kind.themedColor()))
        Spacer(Modifier.size(10.dp))
        Text(
            kind.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.size(6.dp))
        Text(
            rule,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InsightCard(topRisky: AppUsage, riskyTotalMs: Long) {
    val pct = if (riskyTotalMs > 0L) (100f * topRisky.totalMs / riskyTotalMs).roundToInt() else 0
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(topRisky)
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Top distraction today",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    topRisky.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${formatDuration(topRisky.totalMs)} \u00B7 $pct% of distracting time",
                    style = MaterialTheme.typography.bodySmall,
                    color = CategoryKind.RISKY.themedColor(),
                )
            }
        }
    }
}

@Composable
private fun KindLegend(summary: DailySummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val kinds = CategoryKind.entries
            kinds.chunked(2).forEach { rowKinds ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowKinds.forEach { kind ->
                        LegendCell(kind, summary, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendCell(kind: CategoryKind, summary: DailySummary, modifier: Modifier) {
    val ms = summary.msByKind[kind] ?: 0L
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Dot(kind.themedColor())
        Spacer(Modifier.size(10.dp))
        Column {
            Text(
                kind.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                formatDuration(ms),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun AppRow(app: AppUsage, onEdit: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(app)
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    app.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryChip(app.kind)
                    if (app.categoryName.isNotEmpty()) {
                        Spacer(Modifier.size(8.dp))
                        Text(
                            app.categoryName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(Modifier.size(10.dp))
            Text(
                formatDuration(app.totalMs),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.size(6.dp))
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = "Change category",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun AppIcon(app: AppUsage) {
    val shape = RoundedCornerShape(12.dp)
    val bmp = app.icon
    if (bmp != null) {
        Image(
            bitmap = bmp,
            contentDescription = null,
            modifier = Modifier.size(44.dp).clip(shape),
        )
    } else {
        val kindColor = app.kind.themedColor()
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(44.dp).clip(shape)
                .background(kindColor.copy(alpha = 0.18f)),
        ) {
            Text(
                app.label.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = kindColor,
            )
        }
    }
}

@Composable
private fun CategoryChip(kind: CategoryKind) {
    val color = kind.themedColor()
    Row(
        modifier = Modifier.clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(7.dp).clip(CircleShape).background(color)
        )
        Spacer(Modifier.size(5.dp))
        Text(
            kind.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryEditorSheet(
    app: AppUsage,
    onPick: (CategoryKind) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(app)
                Spacer(Modifier.size(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        app.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Category",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            for (kind in CategoryKind.entries) {
                CategoryOptionRow(
                    kind = kind,
                    selected = app.kind == kind,
                    onClick = { onPick(kind) },
                )
                Spacer(Modifier.height(6.dp))
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onReset,
                enabled = app.isOverridden,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Rounded.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(if (app.isOverridden) "Reset to default" else "Using default category")
            }
        }
    }
}

@Composable
private fun CategoryOptionRow(
    kind: CategoryKind,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(kind.themedColor()))
        Spacer(Modifier.size(14.dp))
        Text(
            kind.label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun TrendsScreen(days: List<DayStat>, contentPadding: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    "Trends",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    "Last 7 days",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (days.none { it.hasUsage }) {
            item { EmptyTrends() }
        } else {
            item { WeeklySummaryCard(days) }
            item { CategoryPieCard(days) }
            item { DailyBreakdownCard(days) }
        }
    }
}

@Composable
private fun WeeklySummaryCard(days: List<DayStat>) {
    val totalMs = days.sumOf { it.totalMs }
    val activeDays = days.count { it.hasUsage }.coerceAtLeast(1)
    val dailyAvg = totalMs / activeDays
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Total screen time", style = MaterialTheme.typography.labelLarge)
                Text(
                    formatDuration(totalMs),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Daily average", style = MaterialTheme.typography.labelLarge)
                Text(
                    formatDuration(dailyAvg),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun CategoryPieCard(days: List<DayStat>) {
    val totals = CategoryKind.entries.associateWith { k -> days.sumOf { it.msByKind[k] ?: 0L } }
    val total = totals.values.sum().coerceAtLeast(1L)
    // Resolve theme colors here (Canvas draw scope isn't @Composable).
    val slices = CategoryKind.entries.mapNotNull { k ->
        val ms = totals[k] ?: 0L
        if (ms > 0L) SliceData(k, ms, k.themedColor()) else null
    }
    val description = "Category breakdown, last 7 days: " + slices.joinToString(", ") {
        "${it.kind.label} ${(100f * it.ms / total).roundToInt()} percent"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                "Category breakdown",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "Last 7 days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DonutChart(
                    slices = slices,
                    total = total,
                    modifier = Modifier.size(120.dp)
                        .clearAndSetSemantics { contentDescription = description },
                )
                Spacer(Modifier.size(20.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    slices.forEach { slice ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(12.dp).clip(CircleShape)
                                    .background(slice.color)
                            )
                            Spacer(Modifier.size(10.dp))
                            Text(
                                slice.kind.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "${(100f * slice.ms / total).roundToInt()}%",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class SliceData(val kind: CategoryKind, val ms: Long, val color: Color)

@Composable
private fun DonutChart(slices: List<SliceData>, total: Long, modifier: Modifier) {
    Canvas(modifier = modifier) {
        val strokeW = 26.dp.toPx()
        val inset = strokeW / 2
        val arcSize = Size(size.width - strokeW, size.height - strokeW)
        val topLeft = Offset(inset, inset)
        var start = -90f
        slices.forEach { slice ->
            val sweep = 360f * slice.ms / total
            drawArc(
                color = slice.color,
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Butt),
            )
            start += sweep
        }
    }
}

@Composable
private fun DailyBreakdownCard(days: List<DayStat>) {
    val maxMs = days.maxOf { it.totalMs }.coerceAtLeast(1L)
    val chartHeight = 130.dp
    val todayIndex = days.lastIndex
    val emptyMarker = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    val colors = CategoryKind.entries.associateWith { it.themedColor() }
    val description = "Daily screen time, last 7 days: " + days.joinToString(", ") {
        "${it.date.dayLabel()} ${if (it.hasUsage) formatDuration(it.totalMs) else "no usage"}"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                "Daily breakdown",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "Screen time by category",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(chartHeight)
                    .clearAndSetSemantics { contentDescription = description },
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                days.forEach { day ->
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        if (!day.hasUsage) {
                            Box(
                                modifier = Modifier.padding(bottom = 3.dp).size(6.dp)
                                    .clip(CircleShape).background(emptyMarker)
                            )
                        } else {
                            val barFrac = (day.totalMs.toFloat() / maxMs).coerceAtLeast(0.04f)
                            Column(
                                modifier = Modifier.fillMaxWidth().fillMaxHeight(barFrac)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)),
                            ) {
                                CategoryKind.entries.forEach { kind ->
                                    val ms = day.msByKind[kind] ?: 0L
                                    if (ms > 0L) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth()
                                                .weight(ms.toFloat())
                                                .background(colors.getValue(kind))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                days.forEachIndexed { i, day ->
                    Text(
                        day.date.dayLabel(),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (i == todayIndex) FontWeight.Bold else FontWeight.Normal,
                        color = if (i == todayIndex) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTrends() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "No usage data yet this week",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Come back after using your phone for a bit.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SettingsScreen(
    contentPadding: PaddingValues,
    onScoreInfo: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        "FocusTrack",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Version 1.0.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "See where your time goes and how it splits between " +
                            "productive and distracting apps.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            SettingsRow(
                icon = Icons.Outlined.Info,
                title = "How your focus score works",
                onClick = onScoreInfo,
            )
        }
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.size(14.dp))
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun java.time.LocalDate.dayLabel(): String =
    dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())

private data class RecommendedApp(
    val name: String,
    val packageName: String,
    val description: String,
    val group: String,
)

// Curated, popular productivity apps to suggest to users.
private val RECOMMENDED_APPS = listOf(
    RecommendedApp("Notion", "notion.id", "All-in-one notes, docs & wikis", "Notes & docs"),
    RecommendedApp("Google Keep", "com.google.android.keep", "Quick notes & checklists", "Notes & docs"),
    RecommendedApp("Obsidian", "md.obsidian", "Markdown knowledge base", "Notes & docs"),
    RecommendedApp("Todoist", "com.todoist", "Simple, powerful task manager", "Tasks & projects"),
    RecommendedApp("Trello", "com.trello", "Organize projects on boards", "Tasks & projects"),
    RecommendedApp("Forest", "cc.forestapp", "Stay focused, plant a tree", "Focus & habits"),
    RecommendedApp("Loop Habits", "org.isoron.uhabits", "Build & track daily habits", "Focus & habits"),
    RecommendedApp("Duolingo", "com.duolingo", "Learn a language in minutes a day", "Learning"),
    RecommendedApp("AnkiDroid", "com.ichi2.anki", "Flashcards with spaced repetition", "Learning"),
    RecommendedApp("Pocket", "com.ideashower.readitlater.pro", "Save articles to read later", "Read later"),
)

@Composable
private fun DiscoverScreen(contentPadding: PaddingValues) {
    val context = LocalContext.current
    // Compute the not-installed apps off the main thread, grouped by category.
    val grouped by produceState<Map<String, List<RecommendedApp>>?>(null, context) {
        value = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            RECOMMENDED_APPS
                .filterNot { isInstalled(pm, it.packageName) }
                .groupBy { it.group }
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    "Discover",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    "Productivity apps people love \u2014 tap to view on the Play Store",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val sections = grouped
        if (sections != null && sections.isEmpty()) {
            item {
                Text(
                    "You already have these covered \u2014 nice! \uD83C\uDF89",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        } else if (sections != null) {
            sections.forEach { (group, apps) ->
                item(key = "header-$group") {
                    Text(
                        group,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                    )
                }
                items(apps, key = { it.packageName }) { app ->
                    RecommendedRow(app, onClick = { openInPlayStore(context, app.packageName) })
                }
            }
        }
    }
}

/** True if [pkg] is installed (needs manifest <queries> visibility on API 30+). */
private fun isInstalled(pm: android.content.pm.PackageManager, pkg: String): Boolean =
    try {
        pm.getPackageInfo(pkg, 0)
        true
    } catch (_: android.content.pm.PackageManager.NameNotFoundException) {
        false
    }

@Composable
private fun RecommendedRow(app: RecommendedApp, onClick: () -> Unit) {
    val accent = CategoryKind.PRODUCTIVE.themedColor()
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.18f)),
        ) {
            Text(
                app.name.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
        }
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                app.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                app.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.size(10.dp))
        Icon(
            Icons.AutoMirrored.Rounded.OpenInNew,
            contentDescription = "View on Play Store",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Opens an app's Play Store page, falling back to the web listing. */
private fun openInPlayStore(context: android.content.Context, packageName: String) {
    val market = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("market://details?id=$packageName"),
    )
    try {
        context.startActivity(market)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
            )
        )
    }
}

@Composable
private fun Dot(color: Color) {
    Box(modifier = Modifier.size(14.dp).clip(CircleShape)) {
        Surface(color = color, modifier = Modifier.fillMaxSize()) {}
    }
}

private fun formatDuration(ms: Long): String {
    val totalMinutes = ms / 60000
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
