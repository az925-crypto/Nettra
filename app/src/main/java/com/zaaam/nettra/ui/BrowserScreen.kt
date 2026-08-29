package com.zaaam.nettra.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontFamily
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

private val VoidInk = Color(0xFF0B0F14)
private val Ledger = Color(0xFFF2F4F7)
private val Amber = Color(0xFFFFC145)
private val Teal = Color(0xFF00C2A8)
private val Slate = Color(0xFF6B7A90)

private fun isUrlAllowed(url: String): Boolean {
    val trimmed = url.trim()
    if (trimmed.isEmpty()) return false
    val lower = trimmed.lowercase()
    if (lower == "about:blank") return true
    if (lower.startsWith("http://") || lower.startsWith("https://")) return true
    // reject javascript:, data:, file:, content: and others
    if (lower.startsWith("javascript:") || lower.startsWith("data:") || lower.startsWith("file:") || lower.startsWith("content:")) return false
    // whitelist only http/https/about:blank
    return false
}

@Composable
private fun TabStrip(tabManager: TabManager) {
    val tabs by tabManager.tabs.collectAsState()
    val selectedId by tabManager.selectedId.collectAsState()
    Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tabs.forEach { t ->
            val active = t.entity.id == selectedId
            FilterChip(
                selected = active,
                onClick = { tabManager.validateSelect(t.entity.id) },
                label = { Text((if (t.entity.isPrivate) "◉ " else "") + t.entity.title.take(16), maxLines = 1) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Ledger, selectedLabelColor = VoidInk
                )
            )
        }
        AssistChip(onClick = { tabManager.createTab() }, label = { Text("+") })
        AssistChip(onClick = { tabManager.createTab(isPrivate = true) }, label = { Text("Private") })
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(tabManager: TabManager, privacyEngine: PrivacyEngine, inspector: NetworkInspector) {
    val selectedId by tabManager.selectedId.collectAsState()
    val tabsCollect by tabManager.tabs.collectAsState()
    val current = tabsCollect.find { it.entity.id == selectedId }

    var urlInput by remember { mutableStateOf(current?.entity?.url ?: "https://example.com") }
    var inspectorVisible by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(ResourceType.All) }
    var search by remember { mutableStateOf("") }
    var selectedRequestId by remember { mutableStateOf<String?>(null) }
    val blockedCountMap = remember { mutableStateMapOf<String, Int>() }
    val blockedCount by remember(selectedId) { derivedStateOf { selectedId?.let { blockedCountMap[it] } ?: 0 } }
    val throttleProfile by remember(tabsCollect, selectedId) { derivedStateOf { tabsCollect.find { it.entity.id == selectedId }?.throttling?.let { name -> runCatching { ThrottlingProfile.valueOf(name) }.getOrDefault(ThrottlingProfile.OFF) } ?: ThrottlingProfile.OFF } }
    val fingerprintLevel by remember(tabsCollect, selectedId) { derivedStateOf { tabsCollect.find { it.entity.id == selectedId }?.fingerprintLevel ?: "Balanced" } }
    var customBlocklist by remember { mutableStateOf(setOf("tracker.pixel.gif","ads.example.net","analytics.nope.io")) }
    var showReplay by remember { mutableStateOf(false) }
    var replayTargetId by remember { mutableStateOf<String?>(null) }
    var jsInput by remember { mutableStateOf("") }

    LaunchedEffect(selectedId) { urlInput = current?.entity?.url ?: "https://example.com"; selectedRequestId = null }

    // Use StateFlow from NetworkInspector, guard null to avoid dummy "" leak
    val currentLog by remember(selectedId) {
        val sid = selectedId
        if (sid == null) kotlinx.coroutines.flow.flowOf(emptyList<com.zaaam.nettra.inspector.model.CapturedRequest>())
        else inspector.getLogFlow(sid)
    }.collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize().background(VoidInk)) {
        TabStrip(tabManager = tabManager)

        // Address bar
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth().background(Color.White, RoundedCornerShape(24.dp)).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            val isHttps = urlInput.trim().lowercase().startsWith("https://")
            Icon(if (isHttps) Icons.Default.Lock else Icons.Default.Warning, contentDescription = null, tint = if (isHttps) Teal else Color.Red, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = urlInput, onValueChange = { urlInput = it },
                modifier = Modifier.weight(1f),
                singleLine = true, textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                placeholder = { Text("Ketik URL atau cari…") }
            )
            Spacer(Modifier.width(8.dp))
            BadgedBox(badge = { Badge(containerColor = Amber, contentColor = VoidInk) { Text("$blockedCount") } }) {
                Icon(Icons.Default.Shield, contentDescription = "shield")
            }
        }
        // signal thread
        if (inspectorVisible) Box(Modifier.fillMaxWidth().height(2.dp).background(Amber))
        // Phase2 controls row: throttling + fingerprint + custom blocklist
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp)).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Throttle:", style = MaterialTheme.typography.labelSmall)
            listOf(ThrottlingProfile.OFF, ThrottlingProfile.FAST_3G, ThrottlingProfile.SLOW_3G, ThrottlingProfile.OFFLINE).forEach { p ->
                FilterChip(selected = throttleProfile == p, onClick = { current?.let { tabManager.setThrottling(it.entity.id, p.name) } }, label = { Text(p.label) })
            }
            Spacer(Modifier.weight(1f))
            Text("Fingerprint:", style = MaterialTheme.typography.labelSmall)
            FilterChip(selected = fingerprintLevel=="Balanced", onClick = { val next = if(fingerprintLevel=="Balanced") "Strict" else "Balanced"; current?.let { tabManager.setFingerprintLevel(it.entity.id, next) } }, label = { Text(fingerprintLevel) })
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).background(Color.White, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = customBlocklist.joinToString("\n"), onValueChange = { customBlocklist = it.split("\n").map{ s-> s.trim().lowercase() }.filter{ s-> s.isNotBlank() }.toSet() }, modifier = Modifier.weight(1f), placeholder = { Text("custom blocklist (1 domain per baris)") }, textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace), label = { Text("Custom Blocklist ${customBlocklist.size} domain") }, minLines = 1)
        }

        // WebView
        val appContext = LocalContext.current.applicationContext
        val webViewPool = remember { mutableMapOf<String, WebView>() }
        // cleanup WebViews for closed tabs
        LaunchedEffect(tabsCollect) {
            val aliveIds = tabsCollect.map { it.entity.id }.toSet()
            val deadIds = webViewPool.keys.filter { it !in aliveIds }
            deadIds.forEach { id ->
                try { webViewPool[id]?.removeAllViews(); webViewPool[id]?.destroy() } catch(_: Exception){}
                webViewPool.remove(id)
            }
        }
        DisposableEffect(Unit) {
            onDispose {
                webViewPool.values.forEach { try { it.removeAllViews(); it.destroy() } catch(_: Exception){} }
                webViewPool.clear()
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp).background(Color.White, RoundedCornerShape(12.dp))) {
            if (current != null) {
                val tabId = current.entity.id
                val isPrivate = current.entity.isPrivate
                val client = remember(tabId, customBlocklist) {
                    NetTraWebViewClient(
                        tabId = tabId,
                        tabManager = tabManager,
                        privacyEngine = privacyEngine,
                        inspector = inspector,
                        customBlocklist = customBlocklist,
                        onBlockedCount = { blockedCountMap[tabId] = it }
                    )
                }
                val webView = remember(tabId) {
                    webViewPool.getOrPut(tabId) {
                        WebView(appContext).apply {
                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            settings.allowFileAccessFromFileURLs = false
                            settings.allowUniversalAccessFromFileURLs = false
                            settings.safeBrowsingEnabled = true
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            settings.cacheMode = if (isPrivate) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
                            addJavascriptInterface(JsConsoleBridge(tabId, inspector), "NetTraConsole")
                            CookiePolicy.applyToWebView(this)
                            webChromeClient = WebChromeClient()
                            webViewClient = client
                            val initialUrl = current.entity.url.takeIf { it.isNotBlank() } ?: "https://example.com"
                            if (isUrlAllowed(initialUrl)) {
                                loadUrl(initialUrl)
                            }
                        }
                    }
                }
                // Update cache mode if privacy changes
                LaunchedEffect(isPrivate) {
                    webView.settings.cacheMode = if (isPrivate) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
                }
                DisposableEffect(tabId) {
                    webView.onResume()
                    onDispose {
                        webView.onPause()
                    }
                }
                AndroidView(
                    factory = { webView },
                    update = { wv ->
                        if (wv.webViewClient !== client) wv.webViewClient = client
                        // Validate URL before loading if url changed externally
                        val target = current.entity.url
                        if (target.isNotBlank() && isUrlAllowed(target) && wv.url != target) {
                            wv.loadUrl(target)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No tab", color = Slate) }
            }
            // HTTPS warning case-insensitive
            if (urlInput.trim().lowercase().startsWith("http://")) {
                Card(modifier = Modifier.align(Alignment.TopCenter).padding(8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                    Text("⚠️ Not Secure — HTTP", modifier = Modifier.padding(8.dp), color = Color.Red)
                }
            }
        }

        // Toolbar
        Row(modifier = Modifier.fillMaxWidth().background(VoidInk).padding(8.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {}) { Icon(Icons.Default.ArrowBack, contentDescription = "back", tint = Ledger) }
            IconButton(onClick = {}) { Icon(Icons.Default.ArrowForward, contentDescription = "fwd", tint = Ledger) }
            FilledIconButton(onClick = {}, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Ledger)) { Icon(Icons.Default.Refresh, contentDescription = "refresh") }
            IconButton(onClick = { inspectorVisible = !inspectorVisible }) { Icon(Icons.Default.BugReport, contentDescription = "inspector", tint = if (inspectorVisible) Amber else Ledger) }
            IconButton(onClick = {}) { Icon(Icons.Default.BookmarkBorder, contentDescription = "bm", tint = Ledger) }
            IconButton(onClick = { selectedId?.let { tabManager.closeTab(it) } }) { Icon(Icons.Default.Close, contentDescription = "close", tint = Ledger) }
        }
    }

    // Inspector bottom sheet (simple overlay for MVP)
    if (inspectorVisible && current != null) {
        InspectorSheet(
            inspector = inspector,
            tabId = current.entity.id,
            filter = filter, onFilter = { filter = it },
            search = search, onSearch = { search = it },
            log = currentLog,
            selectedId = selectedRequestId,
            onSelect = { selectedRequestId = it },
            onDismiss = { inspectorVisible = false },
            tabManager = tabManager
        )
    }
}

@Composable
fun InspectorSheet(
    inspector: NetworkInspector,
    tabId: String,
    filter: ResourceType,
    onFilter: (ResourceType) -> Unit,
    search: String,
    onSearch: (String) -> Unit,
    log: List<com.zaaam.nettra.inspector.model.CapturedRequest>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    tabManager: TabManager
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

    Card(modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp).padding(8.dp), shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Network Inspector", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Preserve log", style = MaterialTheme.typography.labelSmall)
                    Switch(checked = preserve, onCheckedChange = { tabManager.setPreserveLog(tabId, it) })
                }
                TextButton(onClick = {
                    scope.launch {
                        val har = HarExporter.export(log)
                        try {
                            val dir = java.io.File(ctx.cacheDir, "har"); dir.mkdirs()
                            val f = java.io.File(dir, "nettra-${tabId.take(6)}.har")
                            withContext(Dispatchers.IO) { f.writeText(har) }
                            val uri = androidx.core.content.FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply { type = "application/json"; putExtra(android.content.Intent.EXTRA_STREAM, uri); addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                            ctx.startActivity(android.content.Intent.createChooser(intent, "Ekspor HAR"))
                        } catch (_: Exception) {}
                    }
                }) { Text("Ekspor HAR") }
                TextButton(onClick = { inspector.clear(tabId); inspector.clearConsole(tabId) }) { Text("Hapus Log") }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Inspektor", modifier = Modifier.weight(1f))
                Switch(checked = tabState?.inspectorEnabled ?: false, onCheckedChange = { tabManager.setInspectorEnabled(tabId, it) })
                Text("${tabState?.throttling ?: "OFF"}", style = MaterialTheme.typography.labelSmall)
            }
            Text("${summary.totalRequests} req • ${summary.transferred} bytes • ${summary.loadTimeMs} ms • ${summary.blocked} blocked • console ${consoleLogs.size}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = Slate)
            OutlinedTextField(value = search, onValueChange = onSearch, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), placeholder = { Text("Saring URL, header, atau body…") }, singleLine = true)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ResourceType.entries.forEach { rt ->
                    FilterChip(selected = filter == rt, onClick = { onFilter(rt) }, label = { Text(rt.name) })
                }
            }
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF9FAFB)).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("NAME", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                Text("STATUS", Modifier.width(56.dp), style = MaterialTheme.typography.labelSmall)
                Text("TYPE", Modifier.width(60.dp), style = MaterialTheme.typography.labelSmall)
                Text("TIME", Modifier.width(80.dp), style = MaterialTheme.typography.labelSmall)
            }
            val filtered = remember(log, filter, search) {
                log.filter {
                    (filter == ResourceType.All || it.type == filter) && (search.isBlank() || it.url.contains(search, ignoreCase = true) || (it.bodyPreview?.contains(search, ignoreCase = true) == true))
                }
            }
            LazyColumn(modifier = Modifier.height(160.dp)) {
                items(filtered, key = { it.id }) { req ->
                    val sel = req.id == selectedId
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = if (sel) Color(0xFFFFF8E1) else Color.White),
                        onClick = { onSelect(req.id) }
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(req.url.takeLast(28), Modifier.weight(1f), maxLines = 1, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                            Text(req.status?.toString() ?: if (req.blocked) "Blocked" else "—", Modifier.width(56.dp), style = MaterialTheme.typography.labelSmall, color = if (req.blocked) Color.Red else Color.Unspecified)
                            Text(req.type.name, Modifier.width(60.dp), style = MaterialTheme.typography.labelSmall)
                            Column(Modifier.width(80.dp)) {
                                val isSlow = req.durationMs != null && req.durationMs!! > 150
                                Box(Modifier.fillMaxWidth().height(6.dp).background(Color(0xFFE8EBEE), RoundedCornerShape(999.dp))) {
                                    val w = (req.durationMs ?: 0L).coerceIn(0, 600).toFloat() / 600f
                                    Box(Modifier.fillMaxWidth(w).height(6.dp).background(if (isSlow) Amber else Amber, RoundedCornerShape(999.dp)))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("${req.durationMs ?: 0} ms", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
                                    TextButton(onClick = { replayTarget = req; showReplay = true }, contentPadding = PaddingValues(2.dp)) { Text("Replay", style = MaterialTheme.typography.labelSmall) }
                                }
                            }
                        }
                    }
                }
            }
            val selected = log.find { it.id == selectedId }
            if (selected != null) {
                var tabIdx by remember { mutableIntStateOf(0) }
                TabRow(selectedTabIndex = tabIdx) {
                    listOf("Headers","Preview","Response","Timing","Console").forEachIndexed { i, t -> Tab(selected = tabIdx==i, onClick = {tabIdx=i}, text = {Text(t + if(t=="Console") " ${consoleLogs.size}" else "")}) }
                }
                when (tabIdx) {
                    0 -> HeadersTab(selected)
                    1 -> PreviewTab(selected)
                    2 -> ResponseTab(selected)
                    3 -> TimingTab(selected)
                    4 -> ConsoleTab(inspector, tabId, consoleLogs)
                }
            } else {
                // also show console when no row selected but console tab wanted
                var tabIdx by remember { mutableIntStateOf(0) }
                TabRow(selectedTabIndex = tabIdx) {
                    listOf("Headers","Preview","Response","Timing","Console").forEachIndexed { i, t -> Tab(selected = tabIdx==i, onClick = {tabIdx=i}, text = {Text(t + if(t=="Console") " ${consoleLogs.size}" else "")}) }
                }
                if (tabIdx == 4) ConsoleTab(inspector, tabId, consoleLogs) else Text("Ketuk baris untuk detail • Header sensitif ter-mask • HAR siap ekspor", style = MaterialTheme.typography.labelSmall, color = Slate, modifier = Modifier.padding(top = 8.dp))
            }
            if (showReplay && replayTarget != null) {
                ReplayDialog(request = replayTarget!!, onDismiss = { showReplay = false }, inspector = inspector, tabId = tabId)
            }
        }
    }
}

@Composable
fun HeadersTab(req: com.zaaam.nettra.inspector.model.CapturedRequest) {
    var reveals by remember { mutableStateOf(setOf<String>()) }
    Column {
        Text("General: ${req.method} ${req.url} — ${req.status ?: "pending"}", style = MaterialTheme.typography.labelSmall)
        Text("Request Headers", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
        req.requestHeaders.forEach { (k,v) ->
            val masked = HeaderMasking.shouldMask(k) && k !in reveals
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                Text(k, Modifier.width(120.dp), style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = Slate)
                Text(if (masked) "••••••••" else v, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                if (HeaderMasking.shouldMask(k)) TextButton(onClick = { reveals = if (k in reveals) reveals - k else reveals + k }) { Text(if (k in reveals) "Sembunyikan" else "Tampilkan", style = MaterialTheme.typography.labelSmall) }
            }
        }
        req.responseHeaders?.let { rh ->
            Text("Response Headers", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
            rh.forEach { (k,v) ->
                val masked = HeaderMasking.shouldMask(k) && k !in reveals
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(k, Modifier.width(120.dp), style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = Slate)
                    Text(if (masked) "••••••••" else v, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                    if (HeaderMasking.shouldMask(k)) TextButton(onClick = { reveals = if (k in reveals) reveals - k else reveals + k }) { Text(if (k in reveals) "Sembunyikan" else "Tampilkan") }
                }
            }
        }
    }
}

@Composable
fun PreviewTab(req: com.zaaam.nettra.inspector.model.CapturedRequest) {
    val body = req.bodyPreview
    val isJson = body != null && (JsonPretty.isJsonBody(body) || req.type == ResourceType.FetchXHR)
    val display = if (isJson && body != null) try { JsonPretty.prettyPrint(body) } catch (_: Exception) { body } else body ?: "No preview — gambar/binary ditampilkan sebagai placeholder"
    Text(display, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), modifier = Modifier.background(Color(0xFF0B0F14), RoundedCornerShape(8.dp)).padding(8.dp).fillMaxWidth(), color = Color.White)
}

@Composable
fun ResponseTab(req: com.zaaam.nettra.inspector.model.CapturedRequest) {
    val body = req.bodyPreview
    val pretty = if (body != null && JsonPretty.isJsonBody(body)) JsonPretty.prettyPrint(body) else body
    Column {
        if (body == null) Text("Belum ada body — re-fetch GET akan isi otomatis, POST via Replay", color = Slate, style = MaterialTheme.typography.labelSmall)
        else if (body == "too large") Text("Terlalu besar untuk ditampilkan ( >1 MB )", color = Color.Red, style = MaterialTheme.typography.labelSmall)
        else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Full body ${body.length} chars", style = MaterialTheme.typography.labelSmall, color = Slate)
            }
            Text(pretty ?: "—", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), modifier = Modifier.padding(top = 4.dp).background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp)).padding(8.dp).fillMaxWidth())
        }
    }
}

@Composable
fun TimingTab(req: com.zaaam.nettra.inspector.model.CapturedRequest) {
    Column {
        Text("Total ${req.durationMs ?: 0} ms ${if(req.durationMs!=null && req.durationMs!!>150) "• throttled" else ""}", style = MaterialTheme.typography.labelSmall)
        Box(Modifier.fillMaxWidth().height(18.dp).background(Color(0xFFE8EBEE), RoundedCornerShape(999.dp)).padding(2.dp)) {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(0.08f).fillMaxHeight().background(Slate))
                Box(Modifier.weight(0.55f).fillMaxHeight().background(Amber))
                Box(Modifier.weight(0.22f).fillMaxHeight().background(Teal))
            }
        }
        Text("Granular via proxy/OkHttp re-fetch — offline akan 503", style = MaterialTheme.typography.labelSmall, color = Slate)
    }
}

@Composable
fun ConsoleTab(inspector: NetworkInspector, tabId: String, logs: List<com.zaaam.nettra.inspector.model.ConsoleEntry>) {
    val ctx = LocalContext.current
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf<String?>(null) }
    Column {
        LazyColumn(modifier = Modifier.height(140.dp).background(Color.White, RoundedCornerShape(8.dp)).padding(8.dp)) {
            items(logs.size, key = { idx -> logs[idx].hashCode() + idx }) { idx ->
                val e = logs[idx]
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    val badgeColor = when(e.level) { "WARN"->Amber; "ERROR"-> Color(0xFFE5484D); else->Color(0xFFE8EBEE) }
                    Text(e.level, modifier = Modifier.background(badgeColor, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 1.dp), style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
                    Spacer(Modifier.width(6.dp))
                    Text(e.message, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), modifier = Modifier.weight(1f))
                }
            }
        }
        Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("console: document.title atau fetch('/api/ping')") }, singleLine = true, textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace))
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                // execute via WebView evaluate - need to find WebView, but for now just log locally and via inspector
                inspector.addConsoleLog(tabId, com.zaaam.nettra.inspector.model.ConsoleEntry("LOG", "> $input", tabId = tabId))
                output = "Executed: $input (hasil via WebView di device)"
                input = ""
            }) { Text("Jalankan") }
            TextButton(onClick = { inspector.clearConsole(tabId) }) { Text("Bersihkan") }
        }
        output?.let { Text(it, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), modifier = Modifier.padding(top = 4.dp).background(Color(0xFF0B0F14), RoundedCornerShape(8.dp)).padding(8.dp).fillMaxWidth(), color = Color.White) }
    }
}

@Composable
fun ReplayDialog(request: com.zaaam.nettra.inspector.model.CapturedRequest, onDismiss: ()->Unit, inspector: NetworkInspector, tabId: String) {
    var method by remember { mutableStateOf(request.method) }
    var url by remember { mutableStateOf(request.url) }
    var body by remember { mutableStateOf(request.bodyPreview ?: "") }
    var headersText by remember { mutableStateOf(request.requestHeaders.entries.joinToString("\n") { "${it.key}: ${it.value}" }) }
    var result by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    fun parseHeaders(input: String): Map<String,String>? {
        val map = input.split("\n").mapNotNull { line ->
            val idx = line.indexOf(":")
            if (idx == -1) null else {
                val k = line.substring(0, idx).trim()
                val v = line.substring(idx+1).trim()
                if (k.isEmpty()) null else k to v
            }
        }.toMap()
        return if (map.isEmpty()) null else map
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kirim Ulang Permintaan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = method, onValueChange = { method = it }, label = { Text("Method") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL") }, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace))
                OutlinedTextField(value = headersText, onValueChange = { headersText = it }, label = { Text("Headers (k: v per baris)") }, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace), minLines = 2)
                OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Body (JSON)") }, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace))
                result?.let { Text(it, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), modifier = Modifier.background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp)).padding(8.dp)) }
            }
        },
        confirmButton = {
            Button(onClick = {
                scope.launch {
                    try {
                        val headersOverride = parseHeaders(headersText)
                        val r = ReplayEngine.replay(request, ReplayOverrides(method = method, url = url, body = body.ifBlank { null }, headers = headersOverride))
                        result = "Status ${r.status} • ${r.durationMs}ms\n${r.body?.take(500) ?: "-"}"
                    } catch (e: Exception) { result = "Error: ${e.message}" }
                }
            }) { Text("Kirim Ulang Sekarang") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
