package com.focustrack.app.data

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * Mirrors the DeepTrack extension's category kinds. Each kind carries a
 * light/dark color pair so the palette adapts to the active theme and keeps
 * adequate contrast; use [themedColor] to resolve the current one.
 */
enum class CategoryKind(val label: String, val light: Color, val dark: Color) {
    RISKY("Risky", Color(0xFFD32F2F), Color(0xFFEF9A9A)),
    PRODUCTIVE("Productive", Color(0xFF2E7D32), Color(0xFF81C784)),
    NEUTRAL("Neutral", Color(0xFF0277BD), Color(0xFF4FC3F7)),
    UNKNOWN("Uncategorized", Color(0xFF616161), Color(0xFFBDBDBD)),
}

/** Resolves the category color for the current (light/dark) theme. */
@Composable
@ReadOnlyComposable
fun CategoryKind.themedColor(): Color = if (isSystemInDarkTheme()) dark else light

data class AppCategory(
    val name: String,
    val kind: CategoryKind,
    val packages: List<String>,
)

/**
 * Package-name -> category map. The Android equivalent of the extension's
 * domain patterns. Browsers are tracked as whole apps (Tier 1).
 */
object AppCategories {
    // Sensible out-of-the-box defaults so the score works immediately.
    // Users can re-categorize any app; overrides always win over these.
    val DEFAULTS: List<AppCategory> = listOf(
        // ---------------- Risky (distracting) ----------------
        AppCategory(
            "Short-form video", CategoryKind.RISKY,
            listOf(
                "com.instagram.android",
                "com.instagram.barcelona", // Threads
                "com.zhiliaoapp.musically", // TikTok
                "com.ss.android.ugc.trill", // TikTok (some regions)
                "com.google.android.youtube",
                "com.snapchat.android",
                "video.like", // Likee
                "co.triller.droid", // Triller
            ),
        ),
        AppCategory(
            "Social feeds", CategoryKind.RISKY,
            listOf(
                "com.twitter.android", // X / Twitter
                "com.facebook.katana",
                "com.facebook.lite",
                "com.reddit.frontpage",
                "com.pinterest",
                "com.tumblr",
                "com.ninegag.android.app", // 9GAG
                "com.bereal.ft", // BeReal
                "org.joinmastodon.android",
            ),
        ),
        AppCategory(
            "Streaming & video", CategoryKind.RISKY,
            listOf(
                "com.netflix.mediaclient",
                "com.disney.disneyplus",
                "com.amazon.avod.thirdpartyclient", // Prime Video
                "com.hulu.plus",
                "tv.twitch.android.app", // Twitch
                "in.startv.hotstar", // Hotstar
                "com.wbd.stream", // Max / HBO
                "com.peacocktv.peacockandroid",
            ),
        ),
        AppCategory(
            "Games", CategoryKind.RISKY,
            listOf(
                "com.king.candycrushsaga",
                "com.kiloo.subwaysurf", // Subway Surfers
                "com.supercell.clashofclans",
                "com.supercell.clashroyale",
                "com.supercell.brawlstars",
                "com.roblox.client",
                "com.tencent.ig", // PUBG Mobile
                "com.dts.freefireth", // Free Fire
                "com.miHoYo.GenshinImpact",
                "com.innersloth.spacemafia", // Among Us
                "com.activision.callofduty.shooter", // CoD Mobile
                "com.mojang.minecraftpe",
                "com.nianticlabs.pokemongo",
                "com.ludo.king",
            ),
        ),
        AppCategory(
            "Dating", CategoryKind.RISKY,
            listOf(
                "com.tinder",
                "com.bumble.app",
                "co.match.android.matchhinge", // Hinge
                "com.okcupid.okcupid",
            ),
        ),

        // ---------------- Productive ----------------
        AppCategory(
            "Work & code", CategoryKind.PRODUCTIVE,
            listOf(
                "com.github.android",
                "com.termux",
                "com.figma.mirror",
                "com.atlassian.android.jira.core", // Jira
            ),
        ),
        AppCategory(
            "Docs & office", CategoryKind.PRODUCTIVE,
            listOf(
                "com.google.android.apps.docs.editors.docs",
                "com.google.android.apps.docs.editors.sheets",
                "com.google.android.apps.docs.editors.slides",
                "com.google.android.apps.docs", // Drive
                "com.microsoft.office.word",
                "com.microsoft.office.excel",
                "com.microsoft.office.powerpoint",
                "com.microsoft.office.onenote",
                "com.microsoft.office.officehubrow", // Microsoft 365
                "com.adobe.reader",
            ),
        ),
        AppCategory(
            "Notes & planning", CategoryKind.PRODUCTIVE,
            listOf(
                "notion.id",
                "com.evernote",
                "com.todoist",
                "com.trello",
                "com.asana.app",
                "com.microsoft.todos",
                "com.google.android.keep",
                "md.obsidian",
                "com.anydo",
                "com.google.android.calendar",
            ),
        ),
        AppCategory(
            "Learning", CategoryKind.PRODUCTIVE,
            listOf(
                "com.duolingo",
                "org.khanacademy.android",
                "org.coursera.android",
                "com.udemy.android",
                "org.edx.mobile",
                "org.brilliant.android",
                "com.ichi2.anki", // AnkiDroid
                "com.microblink.photomath",
                "com.sololearn",
                "org.wikipedia",
            ),
        ),
        AppCategory(
            "Reading", CategoryKind.PRODUCTIVE,
            listOf(
                "com.amazon.kindle",
                "com.google.android.apps.books", // Play Books
                "com.audible.application",
                "com.ideashower.readitlater.pro", // Pocket
            ),
        ),
        AppCategory(
            "Creative", CategoryKind.PRODUCTIVE,
            listOf(
                "com.canva.editor",
            ),
        ),

        // ---------------- Neutral (utility / comms) ----------------
        AppCategory(
            "Email & chat", CategoryKind.NEUTRAL,
            listOf(
                "com.google.android.gm", // Gmail
                "com.microsoft.office.outlook",
                "com.whatsapp",
                "com.whatsapp.w4b", // WhatsApp Business
                "com.facebook.orca", // Messenger
                "org.telegram.messenger",
                "org.thoughtcrime.securesms", // Signal
                "com.discord",
                "com.google.android.apps.messaging",
                "com.tencent.mm", // WeChat
                "jp.naver.line.android", // Line
                "com.viber.voip",
            ),
        ),
        AppCategory(
            "Meetings & work chat", CategoryKind.NEUTRAL,
            listOf(
                "com.Slack",
                "com.microsoft.teams",
                "us.zoom.videomeetings",
                "com.google.android.apps.meetings", // Google Meet
                "com.google.android.apps.tachyon", // Meet / Duo
                "com.cisco.webex.meetings",
            ),
        ),
        AppCategory(
            "Networking", CategoryKind.NEUTRAL,
            listOf(
                "com.linkedin.android",
            ),
        ),
        AppCategory(
            "Browsers", CategoryKind.NEUTRAL,
            listOf(
                "com.android.chrome",
                "com.microsoft.emmx", // Edge
                "org.mozilla.firefox",
                "com.brave.browser",
                "com.opera.browser",
                "com.sec.android.app.sbrowser", // Samsung Internet
                "com.duckduckgo.mobile.android",
                "com.UCMobile.intl", // UC Browser
            ),
        ),
        AppCategory(
            "Maps & travel", CategoryKind.NEUTRAL,
            listOf(
                "com.google.android.apps.maps",
                "com.waze",
                "com.ubercab", // Uber
                "me.lyft.android",
                "com.olacabs.customer",
                "com.airbnb.android",
                "com.booking",
            ),
        ),
        AppCategory(
            "Music & podcasts", CategoryKind.NEUTRAL,
            listOf(
                "com.spotify.music",
                "com.google.android.apps.youtube.music",
                "com.apple.android.music",
                "com.soundcloud.android",
                "com.pandora.android",
                "au.com.shiftyjelly.pocketcasts",
                "com.gaana",
                "com.bsbportal.music", // Wynk
            ),
        ),
        AppCategory(
            "Shopping", CategoryKind.NEUTRAL,
            listOf(
                "com.amazon.mShop.android.shopping",
                "com.flipkart.android",
                "com.ebay.mobile",
                "com.alibaba.aliexpresshd",
                "com.etsy.android",
                "com.walmart.android",
                "com.myntra.android",
            ),
        ),
        AppCategory(
            "Finance", CategoryKind.NEUTRAL,
            listOf(
                "com.google.android.apps.nbu.paisa.user", // Google Pay
                "com.paypal.android.p2pmobile",
                "com.phonepe.app",
                "net.one97.paytm",
                "com.venmo",
                "com.squareup.cash", // Cash App
                "com.coinbase.android",
            ),
        ),
        AppCategory(
            "Food delivery", CategoryKind.NEUTRAL,
            listOf(
                "com.dd.doordash",
                "com.ubercab.eats",
                "in.swiggy.android",
                "com.application.zomato",
                "com.grubhub.android",
            ),
        ),
        AppCategory(
            "Health & fitness", CategoryKind.NEUTRAL,
            listOf(
                "com.google.android.apps.fitness",
                "com.sec.android.app.shealth",
                "com.strava",
                "com.myfitnesspal.android",
                "com.getsomeheadspace.android",
                "com.calm.android",
            ),
        ),
        AppCategory(
            "System & utilities", CategoryKind.NEUTRAL,
            listOf(
                "com.android.settings",
                "com.android.vending", // Play Store
                "com.google.android.googlequicksearchbox", // Google
                "com.google.android.dialer",
                "com.google.android.contacts",
                "com.google.android.apps.nbu.files",
                "com.google.android.documentsui",
                "com.google.android.apps.photos",
                "com.google.android.GoogleCamera",
                "com.google.android.calculator",
                "com.google.android.deskclock",
                "com.google.android.apps.nexuslauncher",
            ),
        ),
    )

    private val packageToCategory: Map<String, AppCategory> =
        DEFAULTS.flatMap { cat -> cat.packages.map { it to cat } }.toMap()

    fun categoryFor(packageName: String): AppCategory? = packageToCategory[packageName]

    fun kindFor(packageName: String): CategoryKind =
        packageToCategory[packageName]?.kind ?: CategoryKind.UNKNOWN
}
