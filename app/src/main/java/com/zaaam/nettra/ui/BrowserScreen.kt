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
import com.zaaam.nettra.inspector.HeaderMasking
import com.zaaam.nettra.inspector.NetworkInspector
import com.zaaam.nettra.inspector.model.ResourceType
import com.zaaam.nettra.privacy.CookiePolicy
import com.zaaam.nettra.privacy.PrivacyEngine
import com.zaaam.nettra.tabs.TabManager
import com.zaaam.nettra.webview.NetTraWebViewClient

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
                onClick = { tabManager.selectTab(t.entity.id) },
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
    var blockedCount by remember { mutableStateOf(0) }

    LaunchedEffect(current?.entity?.url) { urlInput = current?.entity?.url ?: "" }
    LaunchedEffect(selectedId) { selectedRequestId = null }

    // Use StateFlow from NetworkInspector, collect only when visible or for display
    val currentLog by inspector.getLogFlow(selectedId ?: "").collectAsState()

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

        // WebView
        val appContext = LocalContext.current.applicationContext
        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp).background(Color.White, RoundedCornerShape(12.dp))) {
            if (current != null) {
                val tabId = current.entity.id
                val isPrivate = current.entity.isPrivate
                val webViewClient = remember(tabId) {
                    NetTraWebViewClient(
                        tabId = tabId,
                        tabManager = tabManager,
                        privacyEngine = privacyEngine,
                        inspector = inspector,
                        onBlockedCount = { blockedCount = it }
                    )
                }
                val webView = remember(tabId) {
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
                        CookiePolicy.applyToWebView(this)
                        webChromeClient = WebChromeClient()
                        webViewClient = webViewClient
                        val initialUrl = current.entity.url.takeIf { it.isNotBlank() } ?: "https://example.com"
                        if (isUrlAllowed(initialUrl)) {
                            loadUrl(initialUrl)
                        }
                    }
                }
                // Update cache mode if privacy changes
                LaunchedEffect(isPrivate) {
                    webView.settings.cacheMode = if (isPrivate) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
                }
                DisposableEffect(tabId) {
                    onDispose {
                        webView.removeAllViews()
                        webView.destroy()
                    }
                }
                AndroidView(
                    factory = { webView },
                    update = { wv ->
                        // Do not recreate client each update; only update if needed
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
    val summary = inspector.summary(tabId)
    val tabs by tabManager.tabs.collectAsState()
    val tabState = tabs.find { it.entity.id == tabId }
    val preserve = tabState?.preserveLog ?: false

    Card(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).padding(8.dp), shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Network Inspector", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Preserve log", style = MaterialTheme.typography.labelSmall)
                    Switch(checked = preserve, onCheckedChange = { tabManager.setPreserveLog(tabId, it) })
                }
                TextButton(onClick = { inspector.clear(tabId) }) { Text("Hapus Log") }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
            }
            // inspector toggle per tab
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Inspektor", modifier = Modifier.weight(1f))
                Switch(checked = tabState?.inspectorEnabled ?: false, onCheckedChange = { tabManager.setInspectorEnabled(tabId, it) })
            }
            Text("${summary.totalRequests} requests • ${summary.transferred} bytes • ${summary.loadTimeMs} ms • ${summary.blocked} blocked", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = Slate)
            OutlinedTextField(value = search, onValueChange = onSearch, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), placeholder = { Text("Saring URL atau header…") }, singleLine = true)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ResourceType.entries.forEach { rt ->
                    FilterChip(selected = filter == rt, onClick = { onFilter(rt) }, label = { Text(rt.name) })
                }
            }
            // column header
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF9FAFB)).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("NAME", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                Text("STATUS", Modifier.width(56.dp), style = MaterialTheme.typography.labelSmall)
                Text("TYPE", Modifier.width(60.dp), style = MaterialTheme.typography.labelSmall)
                Text("TIME", Modifier.width(80.dp), style = MaterialTheme.typography.labelSmall)
            }
            val filtered = remember(log, filter, search) {
                log.filter {
                    (filter == ResourceType.All || it.type == filter) && (search.isBlank() || it.url.contains(search, ignoreCase = true))
                }
            }
            LazyColumn(modifier = Modifier.height(180.dp)) {
                items(filtered, key = { it.id }) { req ->
                    val sel = req.id == selectedId
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = if (sel) Color(0xFFFFF8E1) else Color.White),
                        onClick = { onSelect(req.id) }
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(req.url.takeLast(32), Modifier.weight(1f), maxLines = 1, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                            Text(req.status?.toString() ?: if (req.blocked) "Blocked" else "—", Modifier.width(56.dp), style = MaterialTheme.typography.labelSmall, color = if (req.blocked) Color.Red else Color.Unspecified)
                            Text(req.type.name, Modifier.width(60.dp), style = MaterialTheme.typography.labelSmall)
                            Column(Modifier.width(80.dp)) {
                                Box(Modifier.fillMaxWidth().height(6.dp).background(Color(0xFFE8EBEE), RoundedCornerShape(999.dp))) {
                                    val w = (req.durationMs ?: 0L).coerceIn(0, 400).toFloat() / 400f
                                    Box(Modifier.fillMaxWidth(w).height(6.dp).background(Amber, RoundedCornerShape(999.dp)))
                                }
                                Text("${req.durationMs ?: 0} ms", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
                            }
                        }
                    }
                }
            }
            // detail tabs
            val selected = log.find { it.id == selectedId }
            if (selected != null) {
                var tabIdx by remember { mutableIntStateOf(0) }
                TabRow(selectedTabIndex = tabIdx) {
                    listOf("Headers","Preview","Response","Timing").forEachIndexed { i, t -> Tab(selected = tabIdx==i, onClick = {tabIdx=i}, text = {Text(t)}) }
                }
                when (tabIdx) {
                    0 -> HeadersTab(selected)
                    1 -> PreviewTab(selected)
                    2 -> ResponseTab(selected)
                    3 -> TimingTab(selected)
                }
            } else {
                Text("Ketuk baris untuk detail • Header sensitif ter-mask", style = MaterialTheme.typography.labelSmall, color = Slate, modifier = Modifier.padding(top = 8.dp))
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
    val txt = req.bodyPreview ?: "No preview — Opsi A MVP: body penuh di Phase 2 proxy"
    Text(txt, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), modifier = Modifier.background(Color(0xFF0B0F14), RoundedCornerShape(8.dp)).padding(8.dp).fillMaxWidth(), color = Color.White)
}

@Composable
fun ResponseTab(req: com.zaaam.nettra.inspector.model.CapturedRequest) {
    Column {
        Text("⚠️ Opsi A: body penuh tidak tersedia di MVP", color = Color.Red, style = MaterialTheme.typography.labelSmall)
        Text(req.bodyPreview ?: "—", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun TimingTab(req: com.zaaam.nettra.inspector.model.CapturedRequest) {
    Column {
        Text("Total ${req.durationMs ?: 0} ms (breakdown granular di Phase 2 Opsi B)", style = MaterialTheme.typography.labelSmall)
        Box(Modifier.fillMaxWidth().height(18.dp).background(Color(0xFFE8EBEE), RoundedCornerShape(999.dp)).padding(2.dp)) {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(0.08f).fillMaxHeight().background(Slate))
                Box(Modifier.weight(0.55f).fillMaxHeight().background(Amber))
                Box(Modifier.weight(0.22f).fillMaxHeight().background(Teal))
            }
        }
        Text("Opsi A hanya total akurat — segmen lain placeholder", style = MaterialTheme.typography.labelSmall, color = Slate)
    }
}
