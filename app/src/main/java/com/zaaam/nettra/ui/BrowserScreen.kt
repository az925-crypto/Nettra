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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.blur
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

// 2026 Depth Browser tokens — Abyss 60 / Linen 30 / Moss 10
private val Abyss = Color(0xFF0D1B1E)
private val AbyssLift = Color(0xFF1C2E32)
private val Linen = Color(0xFFEDE8E1)
private val Stone = Color(0xFFC9C2B8)
private val Moss = Color(0xFFD4FF32)
private val MossPressed = Color(0xFFA8CC26)
private val Slate = Color(0xFF8A9BA8)
private val Line = Color(0xFFE6E0D6)
private val Red = Color(0xFFE5484D)

private fun Modifier.clickableWithoutRipple(onClick: () -> Unit): Modifier = clickable(
    interactionSource = MutableInteractionSource(), indication = null, onClick = onClick
)

private fun isUrlAllowed(url: String): Boolean {
    val t = url.trim(); if (t.isEmpty()) return false
    val l = t.lowercase()
    if (l == "about:blank") return true
    if (l.startsWith("http://") || l.startsWith("https://")) return true
    if (l.startsWith("javascript:") || l.startsWith("data:") || l.startsWith("file:") || l.startsWith("content:")) return false
    return false
}

@Composable
private fun PillAddressBar(urlInput: String, onUrlChange: (String) -> Unit, blockedCount: Int, isPrivate: Boolean, progress: Float) {
    val pillBg = Color.White
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).clip(RoundedCornerShape(999.dp)).background(pillBg).border(1.5f.dpToPx(), if (isPrivate) Color(0xFF2A1F3D) else Line, RoundedCornerShape(999.dp)).padding(0.dp)) {
    }
    // Use Card with shadow for depth
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(containerColor = if (isPrivate) Color(0xFF1A1426) else Color.White),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.5.dp, brush = androidx.compose.ui.graphics.SolidColor(if (isPrivate) Color(0xFF2A1F3D) else Line)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(22.dp).clip(RoundedCornerShape(7.dp)).background(Abyss).border(1.dp, Color(0xFF1A2A4A), RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Language, null, tint = Moss, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.width(8.dp))
                Icon(if (urlInput.trim().lowercase().startsWith("https://")) Icons.Default.Lock else Icons.Default.LockOpen, null, tint = if (urlInput.trim().lowercase().startsWith("https://")) Moss else Slate, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = urlInput, onValueChange = onUrlChange, singleLine = true,
                        textStyle = LocalTextStyle.current.copy(color = if (isPrivate) Color(0xFFE9E0FF) else Abyss, fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (urlInput.isEmpty()) Text("Telusuri atau ketik URL", color = Slate, fontSize = 13.sp)
                }
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.Mic, null, tint = Slate, modifier = Modifier.size(20.dp).clip(CircleShape).clickableWithoutRipple {})
                Icon(Icons.Default.ChromeReaderMode, null, tint = Slate, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Box(modifier = Modifier.background(Moss, RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Shield, null, tint = Abyss, modifier = Modifier.size(12.dp))
                        Text("$blockedCount", color = Abyss, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
            // Liquid ink progress inside pill
            if (progress > 0f) {
                Box(Modifier.fillMaxWidth().height(4.dp).background(Color(0xFFE6E0D6))) {
                    Box(Modifier.fillMaxWidth(progress).height(4.dp).background(Moss))
                }
            }
        }
    }
}

private fun Modifier.dpToPx(): androidx.compose.ui.graphics.Brush = androidx.compose.ui.graphics.SolidColor(Line)

@Composable
private fun FloatingDock(
    inspectorOn: Boolean, onToggleInspector: () -> Unit,
    onBack: () -> Unit, onHome: () -> Unit, onTabs: () -> Unit, tabCount: Int
) {
    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.wrapContentWidth(),
            shape = RoundedCornerShape(999.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF20D1B1E)),
            border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1C2E32))),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Slate) }
                IconButton(onClick = onHome, modifier = Modifier.size(44.dp).clip(CircleShape).background(Moss)) { Icon(Icons.Default.Home, "Home", tint = Abyss) }
                Box(contentAlignment = Alignment.Center) {
                    IconButton(onClick = onTabs, modifier = Modifier.size(44.dp)) { Icon(Icons.Default.Tab, "Tabs", tint = Linen) }
                    Box(modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-2).dp).background(Moss, CircleShape).padding(horizontal = 5.dp, vertical = 2.dp)) {
                        Text("$tabCount", color = Abyss, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                IconButton(onClick = onToggleInspector, modifier = Modifier.size(44.dp)) { Icon(Icons.Default.BugReport, "Inspector", tint = if (inspectorOn) Moss else Slate) }
            }
        }
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
    val progress by remember { derivedStateOf { if (currentLog.isNotEmpty()) 0.62f else 0f } }

    LaunchedEffect(selectedId) { urlInput = current?.entity?.url ?: "https://example.com"; selectedRequestId = null }

    val currentLog by remember(selectedId) {
        val sid = selectedId; if (sid == null) kotlinx.coroutines.flow.flowOf(emptyList<com.zaaam.nettra.inspector.model.CapturedRequest>()) else inspector.getLogFlow(sid)
    }.collectAsState(initial = emptyList())

    Box(modifier = Modifier.fillMaxSize().background(Abyss)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar Abyss
            Row(modifier = Modifier.fillMaxWidth().background(Abyss).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("NetTra", color = Linen, fontSize = 19.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                Spacer(Modifier.weight(1f))
                Box(modifier = Modifier.background(AbyssLift, RoundedCornerShape(12.dp)).border(1.dp, Color(0xFF2A3343), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Tab, null, tint = Linen, modifier = Modifier.size(14.dp))
                        Text("${tabsCollect.size}", color = Linen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.MoreVert, null, tint = Linen, modifier = Modifier.size(22.dp))
            }

            PillAddressBar(urlInput = urlInput, onUrlChange = { urlInput = it }, blockedCount = blockedCount, isPrivate = isPrivate, progress = progress)

            // WebView with depth card
            val appContext = LocalContext.current.applicationContext
            val webViewPool = remember { mutableMapOf<String, WebView>() }
            LaunchedEffect(tabsCollect) {
                val alive = tabsCollect.map { it.entity.id }.toSet()
                webViewPool.keys.filter { it !in alive }.forEach { id -> try { webViewPool[id]?.removeAllViews(); webViewPool[id]?.destroy() } catch(_: Exception){}; webViewPool.remove(id) }
            }
            DisposableEffect(Unit) { onDispose { webViewPool.values.forEach { try { it.removeAllViews(); it.destroy() } catch(_: Exception){} }; webViewPool.clear() } }

            Card(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = if (isPrivate) Color(0xFF1A1426) else Color.White),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.5.dp, brush = androidx.compose.ui.graphics.SolidColor(if (isPrivate) Color(0xFF2A1F3D) else Line)),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
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
                    } else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Tidak ada tab", color = Slate) }

                    if (urlInput.trim().lowercase().startsWith("http://")) {
                        Card(modifier = Modifier.align(Alignment.TopCenter).padding(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFFE8A0)))) {
                            Text("⚠ Not Secure — HTTP", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Floating dock hide-on-scroll handled via AnimatedVisibility for inspector
            AnimatedVisibility(visible = true, enter = expandVertically(), exit = shrinkVertically()) {
                FloatingDock(inspectorOn = inspectorVisible, onToggleInspector = { inspectorVisible = !inspectorVisible }, onBack = {}, onHome = {}, onTabs = {}, tabCount = tabsCollect.size)
            }
        }

        if (inspectorVisible && current != null) {
            Card(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(Line))
            ) {
                InspectorSheetContent(inspector, current.entity.id, filter, { filter = it }, search, { search = it }, currentLog, selectedRequestId, { selectedRequestId = it }, { inspectorVisible = false }, tabManager)
            }
        }
    }
}

@Composable
private fun InspectorSheetContent(
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

    Column {
        Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) { Box(Modifier.width(32.dp).height(4.dp).clip(RoundedCornerShape(999.dp)).background(Stone)) }
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Network Inspector", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), color = Abyss)
            Text("Preserve", fontSize = 11.sp, color = Slate, modifier = Modifier.padding(end = 8.dp))
            Switch(checked = preserve, onCheckedChange = { tabManager.setPreserveLog(tabId, it) }, colors = SwitchDefaults.colors(checkedThumbColor = Abyss, checkedTrackColor = Moss))
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
            }) { Text("Ekspor HAR", fontSize = 12.sp, color = Abyss) }
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = Slate) }
        }
        Row(modifier = Modifier.fillMaxWidth().background(Abyss).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${summary.totalRequests} req • ${summary.transferred} bytes • ${summary.loadTimeMs} ms • ${summary.blocked} blocked", color = Linen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
        OutlinedTextField(value = search, onValueChange = onSearch, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), placeholder = { Text("Saring URL atau header…", fontSize = 12.sp, fontFamily = FontFamily.Monospace) }, singleLine = true, shape = RoundedCornerShape(999.dp))
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ResourceType.entries.forEach { rt ->
                val sel = filter == rt
                Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (sel) Abyss else Color.White).border(1.dp, Line, RoundedCornerShape(999.dp)).clickableWithoutRipple { onFilter(rt) }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(rt.name, color = if (sel) Linen else Abyss, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        val filtered = remember(log, filter, search) { log.filter { (filter == ResourceType.All || it.type == filter) && (search.isBlank() || it.url.contains(search, true) || (it.bodyPreview?.contains(search, true)==true)) } }
        LazyColumn(modifier = Modifier.height(180.dp).padding(horizontal = 8.dp)) {
            items(filtered, key = { it.id }) { req ->
                val sel = req.id == selectedId
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), colors = CardDefaults.cardColors(containerColor = if (sel) Color(0xFFFFF8E1) else Color.White), shape = RoundedCornerShape(12.dp), border = CardDefaults.outlinedCardBorder().copy(width = if (sel) 1.5.dp else 1.dp, brush = androidx.compose.ui.graphics.SolidColor(if (sel) Moss else Line)), onClick = { onSelect(req.id) }) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(req.url.takeLast(28), maxLines = 1, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Abyss)
                            Text(req.method, fontSize = 10.sp, color = Slate, modifier = Modifier.background(Color(0xFFEDE8E1), RoundedCornerShape(999.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Text(req.status?.toString() ?: if (req.blocked) "Blocked" else "—", Modifier.width(56.dp), fontSize = 11.sp, color = if (req.blocked) Red else Abyss, fontFamily = FontFamily.Monospace)
                        Column(Modifier.width(80.dp), horizontalAlignment = Alignment.End) {
                            val w = (req.durationMs ?: 0L).coerceIn(0,600).toFloat()/600f
                            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)).background(Color(0xFFE6E0D6))) { Box(Modifier.fillMaxWidth(w).height(6.dp).background(Moss)) }
                            Text("${req.durationMs ?: 0} ms", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Abyss)
                        }
                    }
                }
            }
        }
    }
}
