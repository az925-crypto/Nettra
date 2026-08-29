package com.zaaam.nettra.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.zaaam.nettra.inspector.HarExporter
import com.zaaam.nettra.inspector.HeaderMasking
import com.zaaam.nettra.inspector.JsonPretty
import com.zaaam.nettra.inspector.NetworkInspector
import com.zaaam.nettra.inspector.ReplayEngine
import com.zaaam.nettra.inspector.ReplayOverrides
import com.zaaam.nettra.inspector.model.ResourceType
import com.zaaam.nettra.privacy.CookiePolicy
import com.zaaam.nettra.privacy.PrivacyEngine
import com.zaaam.nettra.tabs.TabManager
import com.zaaam.nettra.webview.JsConsoleBridge
import com.zaaam.nettra.webview.NetTraWebViewClient
import com.zaaam.nettra.webview.ThrottlingProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Tokens sesuai mockup — 60 VoidInk / 30 Ledger / 10 Amber + TealLock/Slate/Line/Red
private val VoidInk = Color(0xFF0B0F14)
private val Ledger = Color(0xFFF2F4F7)
private val Amber = Color(0xFFFFC145)
private val Teal = Color(0xFF00C2A8)
private val Slate = Color(0xFF6B7A90)
private val Line = Color(0xFFD1D7E0)
private val Red = Color(0xFFE5484D)
private val VoidViolet = Color(0xFF130E1C)

private fun isUrlAllowed(url: String): Boolean {
    val t = url.trim(); if (t.isEmpty()) return false
    val l = t.lowercase()
    if (l == "about:blank") return true
    if (l.startsWith("http://") || l.startsWith("https://")) return true
    if (l.startsWith("javascript:") || l.startsWith("data:") || l.startsWith("file:") || l.startsWith("content:")) return false
    return false
}

// ---------- TabStrip (Chrome-native) ----------
@Composable
private fun NewTabStrip(tabManager: TabManager) {
    val tabs by tabManager.tabs.collectAsState()
    val selectedId by tabManager.selectedId.collectAsState()
    Row(
        modifier = Modifier.fillMaxWidth().background(VoidInk).horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { t ->
            val active = t.entity.id == selectedId
            val isPrivate = t.entity.isPrivate
            val bg = when { active && isPrivate -> Color(0xFF1A1426); active -> Ledger; isPrivate -> Color(0xFF1A1426); else -> Color.Transparent }
            val fg = when { active && !isPrivate -> VoidInk; isPrivate -> Color(0xFFE9E0FF); active -> VoidInk; else -> Slate }
            val border = if (isPrivate) Modifier.border(1.dp, Color(0xFF2A1F3D), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)) else Modifier
            Box(
                modifier = border.background(bg, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 0.dp))
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .border(width = if (active) 0.dp else 1.dp, color = if (active) Color.Transparent else Line.copy(alpha = 0.0f))
            ) {
                // bottom amber line for active
                Column {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).clickableWithoutRipple { tabManager.validateSelect(t.entity.id) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (isPrivate) Icon(Icons.Default.VisibilityOff, null, tint = fg, modifier = Modifier.size(14.dp)) else Icon(Icons.Default.Language, null, tint = fg, modifier = Modifier.size(14.dp))
                        Text((if (isPrivate) "" else "") + t.entity.title.take(18), color = fg, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        Icon(Icons.Default.Close, null, tint = fg.copy(alpha = 0.6f), modifier = Modifier.size(14.dp).clickableWithoutRipple { tabManager.closeTab(t.entity.id) })
                    }
                    if (active) Box(Modifier.fillMaxWidth().height(2.dp).background(Amber)) else Box(Modifier.height(2.dp))
                }
            }
        }
        // + Tab baru dashed
        AssistChip(
            onClick = { tabManager.createTab() },
            label = { Text("+ Tab baru", fontSize = 13.sp) },
            border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = Line),
            colors = AssistChipDefaults.assistChipColors(containerColor = Color.Transparent, labelColor = Slate)
        )
        AssistChip(
            onClick = { tabManager.createTab(isPrivate = true) },
            label = { Text("Private", fontSize = 13.sp) },
            colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF1A1426), labelColor = Color(0xFFE9E0FF)),
            border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = Color(0xFF2A1F3D))
        )
    }
}

// clickable tanpa ripple helper
private fun Modifier.clickableWithoutRipple(onClick: () -> Unit): Modifier = this.then(
    androidx.compose.foundation.clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = onClick)
)

// ---------- Address Pill ----------
@Composable
private fun AddressPill(urlInput: String, onUrlChange: (String) -> Unit, blockedCount: Int, isPrivate: Boolean) {
    val isHttps = urlInput.trim().lowercase().startsWith("https://")
    val isHttp = urlInput.trim().lowercase().startsWith("http://")
    val pillBg = if (isPrivate) Color(0xFF1A1426) else Color.White
    val pillBorder = if (isPrivate) Color(0xFF2A1F3D) else Line
    val urlColor = if (isPrivate) Color(0xFFE9E0FF) else VoidInk
    Row(
        modifier = Modifier.fillMaxWidth().background(pillBg, RoundedCornerShape(999.dp)).border(1.dp, pillBorder, RoundedCornerShape(999.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(if (isHttps) Icons.Default.Lock else if (isHttp) Icons.Default.LockOpen else Icons.Default.Warning, null, tint = if (isHttps) Teal else if (isHttp) Red else Slate, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            androidx.compose.foundation.text.BasicTextField(
                value = urlInput, onValueChange = onUrlChange, singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = urlColor, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                modifier = Modifier.fillMaxWidth()
            )
            if (urlInput.isEmpty()) Text("Ketik URL atau cari…", color = Slate, fontSize = 13.sp)
        }
        Spacer(Modifier.width(10.dp))
        if (blockedCount > 0) {
            Row(modifier = Modifier.background(Amber, RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Shield, null, tint = VoidInk, modifier = Modifier.size(14.dp))
                Text("$blockedCount", color = VoidInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Row(modifier = Modifier.background(Slate, RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, null, tint = Color.White, modifier = Modifier.size(14.dp))
                Text("0", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Default.MoreVert, null, tint = Slate, modifier = Modifier.size(18.dp))
    }
}

// ---------- Toolbar (Android) ----------
@Composable
private fun AndroidToolbar(
    onBack: () -> Unit, onForward: () -> Unit, onRefresh: () -> Unit,
    inspectorOn: Boolean, onToggleInspector: () -> Unit,
    onBookmark: () -> Unit, onHistory: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(VoidInk).padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Ledger) }
        IconButton(onClick = onForward) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Forward", tint = Ledger.copy(alpha = 0.5f)) }
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Ledger).clickableWithoutRipple(onClick = onRefresh), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Refresh, "Refresh", tint = VoidInk, modifier = Modifier.size(22.dp))
        }
        IconButton(onClick = onToggleInspector) { Icon(Icons.Default.BugReport, "Inspector", tint = if (inspectorOn) Amber else Ledger) }
        IconButton(onClick = onBookmark) { Icon(Icons.Default.BookmarkBorder, "Bookmark", tint = Ledger) }
        IconButton(onClick = onHistory) { Icon(Icons.Default.History, "History", tint = Ledger) }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(tabManager: TabManager, privacyEngine: PrivacyEngine, inspector: NetworkInspector) {
    val selectedId by tabManager.selectedId.collectAsState()
    val tabsCollect by tabManager.tabs.collectAsState()
    val current = tabsCollect.find { it.entity.id == selectedId }
    val isPrivate = current?.entity?.isPrivate == true

    var urlInput by remember { mutableStateOf(current?.entity?.url ?: "https://example.com") }
    var inspectorVisible by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(ResourceType.All) }
    var search by remember { mutableStateOf("") }
    var selectedRequestId by remember { mutableStateOf<String?>(null) }
    val blockedCountMap = remember { mutableStateMapOf<String, Int>() }
    val blockedCount by remember(selectedId) { derivedStateOf { selectedId?.let { blockedCountMap[it] } ?: 0 } }
    var customBlocklist by remember { mutableStateOf(setOf("tracker.pixel.gif","ads.example.net","analytics.nope.io")) }

    LaunchedEffect(selectedId) { urlInput = current?.entity?.url ?: "https://example.com"; selectedRequestId = null }

    val currentLog by remember(selectedId) {
        val sid = selectedId; if (sid == null) kotlinx.coroutines.flow.flowOf(emptyList<com.zaaam.nettra.inspector.model.CapturedRequest>()) else inspector.getLogFlow(sid)
    }.collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize().background(VoidInk)) {
        NewTabStrip(tabManager)

        // Address wrap + signal thread
        Column(modifier = Modifier.background(VoidInk).padding(horizontal = 16.dp, vertical = 8.dp)) {
            AddressPill(urlInput = urlInput, onUrlChange = { urlInput = it }, blockedCount = blockedCount, isPrivate = isPrivate)
        }
        AnimatedVisibility(visible = inspectorVisible, enter = expandVertically(), exit = shrinkVertically()) {
            Box(Modifier.fillMaxWidth().height(2.dp).background(Amber))
        }
        if (!inspectorVisible) Box(Modifier.fillMaxWidth().height(2.dp).background(Color.Transparent))

        // Top notice ala mockup
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth().background(Color(0xFFFFF8E1), RoundedCornerShape(12.dp)).border(1.dp, Color(0xFFFFE8A0), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text("🔒 HTTPS-first aktif • Tracker diblokir $blockedCount • Third-party cookie diblokir • Zero telemetry", color = Color(0xFF5A4A00), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }

        // Phase2 controls — Chip pill style
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp)).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Throttle:", fontSize = 11.sp, color = Slate, fontWeight = FontWeight.Medium)
            listOf(ThrottlingProfile.OFF, ThrottlingProfile.FAST_3G, ThrottlingProfile.SLOW_3G, ThrottlingProfile.OFFLINE).forEach { p ->
                val sel = remember(tabsCollect, selectedId) { derivedStateOf { tabsCollect.find { it.entity.id == selectedId }?.throttling == p.name } }.value
                val chipBg = if (sel) VoidInk else Color.White
                val chipFg = if (sel) Color.White else VoidInk
                Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(chipBg).border(1.dp, Line, RoundedCornerShape(999.dp)).clickableWithoutRipple { current?.let { tabManager.setThrottling(it.entity.id, p.name) } }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(p.label, color = chipFg, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Custom blocklist — compact
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).background(if (isPrivate) VoidViolet else Color.White, RoundedCornerShape(8.dp)).border(1.dp, if (isPrivate) Color(0xFF2A1F3D) else Line, RoundedCornerShape(8.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Custom blocklist ${customBlocklist.size}", color = Slate, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text(customBlocklist.joinToString(", ").take(40), color = VoidInk, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
        }

        // WebView container
        val appContext = LocalContext.current.applicationContext
        val webViewPool = remember { mutableMapOf<String, WebView>() }
        LaunchedEffect(tabsCollect) {
            val alive = tabsCollect.map { it.entity.id }.toSet()
            webViewPool.keys.filter { it !in alive }.forEach { id -> try { webViewPool[id]?.removeAllViews(); webViewPool[id]?.destroy() } catch(_: Exception){}; webViewPool.remove(id) }
        }
        DisposableEffect(Unit) { onDispose { webViewPool.values.forEach { try { it.removeAllViews(); it.destroy() } catch(_: Exception){} }; webViewPool.clear() } }

        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(12.dp)).background(if (isPrivate) VoidViolet else Color.White).border(1.dp, if (isPrivate) Color(0xFF2A1F3D) else Line, RoundedCornerShape(12.dp))) {
            if (current != null) {
                val tabId = current.entity.id
                val client = remember(tabId, customBlocklist) {
                    NetTraWebViewClient(tabId, tabManager, privacyEngine, inspector, customBlocklist, onBlockedCount = { blockedCountMap[tabId] = it })
                }
                val webView = remember(tabId) {
                    webViewPool.getOrPut(tabId) {
                        WebView(appContext).apply {
                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                            settings.javaScriptEnabled = true; settings.domStorageEnabled = true
                            settings.allowFileAccess = false; settings.allowContentAccess = false
                            settings.allowFileAccessFromFileURLs = false; settings.allowUniversalAccessFromFileURLs = false
                            settings.safeBrowsingEnabled = true; settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            settings.cacheMode = if (isPrivate) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
                            addJavascriptInterface(JsConsoleBridge(tabId, inspector), "NetTraConsole")
                            CookiePolicy.applyToWebView(this); webChromeClient = WebChromeClient(); webViewClient = client
                            val iu = current.entity.url.takeIf { it.isNotBlank() } ?: "https://example.com"
                            if (isUrlAllowed(iu)) loadUrl(iu)
                        }
                    }
                }
                LaunchedEffect(isPrivate) { webView.settings.cacheMode = if (isPrivate) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT }
                DisposableEffect(tabId) { webView.onResume(); onDispose { webView.onPause() } }
                AndroidView(factory = { webView }, update = { wv -> if (wv.webViewClient !== client) wv.webViewClient = client; val t = current.entity.url; if (t.isNotBlank() && isUrlAllowed(t) && wv.url != t) wv.loadUrl(t) }, modifier = Modifier.fillMaxSize())
            } else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No tab", color = Slate) }

            if (urlInput.trim().lowercase().startsWith("http://")) {
                Card(modifier = Modifier.align(Alignment.TopCenter).padding(8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                    Text("⚠️ Not Secure — HTTP", modifier = Modifier.padding(8.dp), color = Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        AndroidToolbar(
            onBack = {},
            onForward = {},
            onRefresh = { /* reload via webViewPool[selectedId]?.reload() */ },
            inspectorOn = inspectorVisible, onToggleInspector = { inspectorVisible = !inspectorVisible },
            onBookmark = {}, onHistory = {}
        )
    }

    if (inspectorVisible && current != null) {
        InspectorSheet(inspector, current.entity.id, filter, { filter = it }, search, { search = it }, currentLog, selectedRequestId, { selectedRequestId = it }, { inspectorVisible = false }, tabManager)
    }
}

@Composable
fun InspectorSheet(
    inspector: NetworkInspector, tabId: String, filter: ResourceType, onFilter: (ResourceType)->Unit, search: String, onSearch: (String)->Unit,
    log: List<com.zaaam.nettra.inspector.model.CapturedRequest>, selectedId: String?, onSelect: (String)->Unit, onDismiss: ()->Unit, tabManager: TabManager
) {
    val summary = remember(log) { inspector.summary(tabId) }
    val tabs by tabManager.tabs.collectAsState()
    val tabState = tabs.find { it.entity.id == tabId }
    val preserve = tabState?.preserveLog ?: false
    val consoleLogs by inspector.getConsoleFlow(tabId).collectAsState()
    var showReplay by remember { mutableStateOf(false) }
    var replayTarget by remember { mutableStateOf<com.zaaam.nettra.inspector.model.CapturedRequest?>(null) }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    Card(modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp).padding(horizontal = 8.dp).padding(bottom = 8.dp), shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
        Column {
            // drag handle
            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) { Box(Modifier.width(32.dp).height(4.dp).clip(RoundedCornerShape(999.dp)).background(Line)) }
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Network Inspector", fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Default, modifier = Modifier.weight(1f), color = VoidInk)
                Text("Preserve log", fontSize = 11.sp, color = Slate, modifier = Modifier.padding(end = 8.dp))
                Switch(checked = preserve, onCheckedChange = { tabManager.setPreserveLog(tabId, it) }, colors = SwitchDefaults.colors(checkedThumbColor = VoidInk, checkedTrackColor = Amber))
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = {
                    scope.launch {
                        try {
                            val har = withContext(Dispatchers.IO) { HarExporter.export(log) }
                            val dir = java.io.File(ctx.cacheDir, "har"); dir.mkdirs()
                            val f = java.io.File(dir, "nettra-${tabId.take(6)}.har")
                            withContext(Dispatchers.IO) { f.writeText(har) }
                            val uri = androidx.core.content.FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply { type = "application/json"; putExtra(android.content.Intent.EXTRA_STREAM, uri); addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                            ctx.startActivity(android.content.Intent.createChooser(intent, "Ekspor HAR"))
                        } catch (_: Exception) {}
                    }
                }) { Text("Ekspor HAR", fontSize = 12.sp) }
                TextButton(onClick = { inspector.clear(tabId); inspector.clearConsole(tabId) }) { Text("Hapus Log", fontSize = 12.sp) }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = Slate) }
            }
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Inspektor", fontSize = 13.sp, color = VoidInk, modifier = Modifier.weight(1f))
                Switch(checked = tabState?.inspectorEnabled ?: false, onCheckedChange = { tabManager.setInspectorEnabled(tabId, it) }, colors = SwitchDefaults.colors(checkedTrackColor = Amber))
                Text(tabState?.throttling ?: "OFF", fontSize = 11.sp, color = Slate, modifier = Modifier.padding(start = 8.dp))
            }
            // summary bar VoidInk
            Row(modifier = Modifier.fillMaxWidth().background(VoidInk).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${summary.totalRequests} requests • ${summary.transferred} bytes • ${summary.loadTimeMs} ms • ${summary.blocked} blocked • console ${consoleLogs.size}", color = Ledger, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
            OutlinedTextField(value = search, onValueChange = onSearch, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), placeholder = { Text("Saring URL atau header…", fontSize = 12.sp, fontFamily = FontFamily.Monospace) }, singleLine = true, shape = RoundedCornerShape(12.dp))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ResourceType.entries.forEach { rt ->
                    val sel = filter == rt
                    Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (sel) VoidInk else Color.White).border(1.dp, Line, RoundedCornerShape(999.dp)).clickableWithoutRipple { onFilter(rt) }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text(rt.name, color = if (sel) Color.White else VoidInk, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            // colhead
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF9FAFB)).border(1.dp, Line).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("NAME", Modifier.weight(1f), fontSize = 11.sp, color = Slate, letterSpacing = 0.5.sp, fontFamily = FontFamily.Monospace)
                Text("STATUS", Modifier.width(56.dp), fontSize = 11.sp, color = Slate, fontFamily = FontFamily.Monospace)
                Text("TYPE", Modifier.width(60.dp), fontSize = 11.sp, color = Slate, fontFamily = FontFamily.Monospace)
                Text("TIME", Modifier.width(80.dp), fontSize = 11.sp, color = Slate, fontFamily = FontFamily.Monospace)
            }
            val filtered = remember(log, filter, search) { log.filter { (filter == ResourceType.All || it.type == filter) && (search.isBlank() || it.url.contains(search, true) || (it.bodyPreview?.contains(search, true)==true)) } }
            LazyColumn(modifier = Modifier.height(180.dp)) {
                items(filtered, key = { it.id }) { req ->
                    val sel = req.id == selectedId
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), colors = CardDefaults.cardColors(containerColor = if (sel) Color(0xFFFFF8E1) else Color.White), shape = RoundedCornerShape(6.dp), border = if (sel) CardDefaults.outlinedCardBorder().copy(width = 1.dp) else null, onClick = { onSelect(req.id) }) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(req.url.takeLast(32), maxLines = 1, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = VoidInk)
                                Text(req.method, fontSize = 10.sp, color = Slate, modifier = Modifier.background(Color(0xFFE8EBEE), RoundedCornerShape(999.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                            Text(req.status?.toString() ?: if (req.blocked) "Blocked" else "—", Modifier.width(56.dp), fontSize = 11.sp, color = if (req.blocked) Red else VoidInk, fontFamily = FontFamily.Monospace)
                            Box(Modifier.width(60.dp).clip(RoundedCornerShape(999.dp)).background(Color(0xFFF2F4F7)).padding(4.dp), contentAlignment = Alignment.Center) { Text(req.type.name, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = VoidInk) }
                            Column(Modifier.width(80.dp), horizontalAlignment = Alignment.End) {
                                val w = (req.durationMs ?: 0L).coerceIn(0,600).toFloat()/600f
                                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)).background(Color(0xFFE8EBEE))) { Box(Modifier.fillMaxWidth(w).height(6.dp).background(Amber)) }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("${req.durationMs ?: 0} ms", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = VoidInk)
                                    TextButton(onClick = { replayTarget = req; showReplay = true }, contentPadding = PaddingValues(2.dp)) { Text("Replay", fontSize = 11.sp) }
                                }
                            }
                        }
                    }
                }
            }
            val selected = log.find { it.id == selectedId }
            if (selected != null) {
                var tabIdx by remember { mutableIntStateOf(0) }
                TabRow(selectedTabIndex = tabIdx, containerColor = Color.White, contentColor = VoidInk, indicator = { tabPositions -> TabRowDefaults.Indicator(Modifier.tabIndicatorOffset(tabPositions[tabIdx]), color = Amber, height = 2.dp) }) {
                    listOf("Headers","Preview","Response","Timing","Console").forEachIndexed { i, t -> Tab(selected = tabIdx==i, onClick = {tabIdx=i}, text = {Text(t + if(t=="Console") " ${consoleLogs.size}" else "", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)}) }
                }
                when (tabIdx) { 0 -> HeadersTab(selected); 1 -> PreviewTab(selected); 2 -> ResponseTab(selected); 3 -> TimingTab(selected); 4 -> ConsoleTab(inspector, tabId, consoleLogs) }
            } else {
                var tabIdx by remember { mutableIntStateOf(0) }
                TabRow(selectedTabIndex = tabIdx, containerColor = Color.White, contentColor = VoidInk, indicator = { tabPositions -> TabRowDefaults.Indicator(Modifier.tabIndicatorOffset(tabPositions[tabIdx]), color = Amber) }) {
                    listOf("Headers","Preview","Response","Timing","Console").forEachIndexed { i, t -> Tab(selected = tabIdx==i, onClick = {tabIdx=i}, text = {Text(t + if(t=="Console") " ${consoleLogs.size}" else "", fontSize = 13.sp)}) }
                }
                if (tabIdx == 4) ConsoleTab(inspector, tabId, consoleLogs) else Text("Ketuk baris untuk detail • Header sensitif ter-mask • HAR siap ekspor", fontSize = 11.sp, color = Slate, modifier = Modifier.padding(12.dp))
            }
            if (showReplay && replayTarget != null) ReplayDialog(replayTarget!!, { showReplay = false }, inspector, tabId)
        }
    }
}

@Composable fun HeadersTab(req: com.zaaam.nettra.inspector.model.CapturedRequest) {
    var reveals by remember { mutableStateOf(setOf<String>()) }
    Column(Modifier.padding(12.dp)) {
        Text("General: ${req.method} ${req.url} — ${req.status ?: "pending"}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Slate)
        Text("Request Headers", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp), color = VoidInk)
        req.requestHeaders.forEach { (k,v) ->
            val masked = HeaderMasking.shouldMask(k) && k !in reveals
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                Text(k, Modifier.width(120.dp), fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Slate)
                Text(if (masked) "••••••••" else v, Modifier.weight(1f), fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = VoidInk)
                if (HeaderMasking.shouldMask(k)) TextButton(onClick = { reveals = if (k in reveals) reveals - k else reveals + k }) { Text(if (k in reveals) "Sembunyikan" else "Tampilkan", fontSize = 11.sp) }
            }
        }
        req.responseHeaders?.let { rh ->
            Text("Response Headers", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp), color = VoidInk)
            rh.forEach { (k,v) ->
                val masked = HeaderMasking.shouldMask(k) && k !in reveals
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(k, Modifier.width(120.dp), fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Slate)
                    Text(if (masked) "••••••••" else v, Modifier.weight(1f), fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = VoidInk)
                    if (HeaderMasking.shouldMask(k)) TextButton(onClick = { reveals = if (k in reveals) reveals - k else reveals + k }) { Text(if (k in reveals) "Sembunyikan" else "Tampilkan", fontSize = 11.sp) }
                }
            }
        }
    }
}
@Composable fun PreviewTab(req: com.zaaam.nettra.inspector.model.CapturedRequest) {
    val body = req.bodyPreview; val isJson = body != null && (JsonPretty.isJsonBody(body) || req.type == ResourceType.FetchXHR)
    val display = if (isJson && body != null) try { JsonPretty.prettyPrint(body) } catch (_: Exception) { body } else body ?: "No preview — gambar/binary ditampilkan sebagai placeholder"
    Text(display, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.background(VoidInk, RoundedCornerShape(8.dp)).padding(12.dp).fillMaxWidth(), color = Color.White)
}
@Composable fun ResponseTab(req: com.zaaam.nettra.inspector.model.CapturedRequest) {
    val body = req.bodyPreview; val pretty = if (body != null && JsonPretty.isJsonBody(body)) JsonPretty.prettyPrint(body) else body
    Column(Modifier.padding(12.dp)) {
        if (body == null) Text("Belum ada body — re-fetch GET akan isi otomatis, POST via Replay", color = Slate, fontSize = 11.sp)
        else if (body == "too large") Text("Terlalu besar untuk ditampilkan ( >1 MB )", color = Red, fontSize = 11.sp)
        else { Text("Full body ${body.length} chars", fontSize = 11.sp, color = Slate); Text(pretty ?: "—", fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 6.dp).background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp)).padding(12.dp).fillMaxWidth(), color = VoidInk) }
    }
}
@Composable fun TimingTab(req: com.zaaam.nettra.inspector.model.CapturedRequest) {
    Column(Modifier.padding(12.dp)) {
        Text("Total ${req.durationMs ?: 0} ms ${if(req.durationMs!=null && req.durationMs!!>150) "• throttled" else ""}", fontSize = 11.sp, color = VoidInk, fontFamily = FontFamily.Monospace)
        Box(Modifier.fillMaxWidth().height(18.dp).clip(RoundedCornerShape(999.dp)).background(Color(0xFFE8EBEE)).padding(2.dp)) {
            Row(Modifier.fillMaxSize()) { Box(Modifier.weight(0.08f).fillMaxHeight().background(Slate)); Box(Modifier.weight(0.55f).fillMaxHeight().background(Amber)); Box(Modifier.weight(0.22f).fillMaxHeight().background(Teal)) }
        }
        Text("Granular via proxy/OkHttp re-fetch — offline akan 503", fontSize = 11.sp, color = Slate)
    }
}
@Composable fun ConsoleTab(inspector: NetworkInspector, tabId: String, logs: List<com.zaaam.nettra.inspector.model.ConsoleEntry>) {
    var input by remember { mutableStateOf("") }; var output by remember { mutableStateOf<String?>(null) }
    Column(Modifier.padding(12.dp)) {
        LazyColumn(modifier = Modifier.height(140.dp).clip(RoundedCornerShape(8.dp)).background(Color.White).border(1.dp, Line, RoundedCornerShape(8.dp)).padding(8.dp)) {
            items(logs.size, key = { idx -> logs[idx].hashCode()+idx }) { idx ->
                val e = logs[idx]; Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    val badge = when(e.level) { "WARN"->Amber; "ERROR"-> Red; else->Color(0xFFE8EBEE) }
                    Text(e.level, modifier = Modifier.background(badge, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 1.dp), fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = VoidInk)
                    Spacer(Modifier.width(6.dp)); Text(e.message, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f), color = VoidInk)
                }
            }
        }
        Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("console: document.title", fontSize = 12.sp, fontFamily = FontFamily.Monospace) }, singleLine = true, shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.width(8.dp))
            Button(onClick = { inspector.addConsoleLog(tabId, com.zaaam.nettra.inspector.model.ConsoleEntry("LOG", "> $input", tabId = tabId)); output = "Executed: $input"; input = "" }, shape = RoundedCornerShape(999.dp), colors = ButtonDefaults.buttonColors(containerColor = VoidInk, contentColor = Ledger)) { Text("Jalankan", fontSize = 12.sp) }
            TextButton(onClick = { inspector.clearConsole(tabId) }) { Text("Bersihkan", fontSize = 12.sp) }
        }
        output?.let { Text(it, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 6.dp).background(VoidInk, RoundedCornerShape(8.dp)).padding(12.dp).fillMaxWidth(), color = Color.White) }
    }
}
@Composable fun ReplayDialog(request: com.zaaam.nettra.inspector.model.CapturedRequest, onDismiss: ()->Unit, inspector: NetworkInspector, tabId: String) {
    var method by remember { mutableStateOf(request.method) }; var url by remember { mutableStateOf(request.url) }; var body by remember { mutableStateOf(request.bodyPreview ?: "") }
    var headersText by remember { mutableStateOf(request.requestHeaders.entries.joinToString("\n") { "${it.key}: ${it.value}" }) }
    var result by remember { mutableStateOf<String?>(null) }; val scope = rememberCoroutineScope()
    fun parseHeaders(input: String): Map<String,String>? {
        val m = input.split("\n").mapNotNull { l -> val i=l.indexOf(":"); if(i==-1) null else { val k=l.substring(0,i).trim(); val v=l.substring(i+1).trim(); if(k.isEmpty()) null else k to v } }.toMap()
        return if(m.isEmpty()) null else m
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Kirim Ulang Permintaan", fontWeight = FontWeight.Bold, color = VoidInk) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = method, onValueChange = { method = it }, label = { Text("Method") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL") }, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = headersText, onValueChange = { headersText = it }, label = { Text("Headers (k: v per baris)") }, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace), minLines = 2, shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Body (JSON)") }, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace), shape = RoundedCornerShape(12.dp))
            result?.let { Text(it, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp)).padding(12.dp)) }
        }
    }, confirmButton = {
        Button(onClick = { scope.launch { try { val h=parseHeaders(headersText); val r=ReplayEngine.replay(request, ReplayOverrides(method=method, url=url, body=body.ifBlank{null}, headers=h)); result="Status ${r.status} • ${r.durationMs}ms\n${r.body?.take(500) ?: "-"}" } catch(e: Exception){ result="Error: ${e.message}" } } }, colors = ButtonDefaults.buttonColors(containerColor = VoidInk, contentColor = Ledger), shape = RoundedCornerShape(999.dp)) { Text("Kirim Ulang Sekarang") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } })
}
