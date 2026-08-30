package com.zaaam.nettra.browserui

import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.zaaam.nettra.webview.NettraWebViewClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(vm: BrowserViewModel = remember { BrowserViewModel() }) {
    val context = LocalContext.current
    NettraTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NettraBg)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Status bar spacer
                Spacer(modifier = Modifier.height(24.dp).fillMaxWidth().background(Color.Black))
                // Address bar
                AddressBar(
                    input = vm.addressInput,
                    onInputChange = vm::onAddressChange,
                    onSubmit = { vm.navigate(vm.addressInput) },
                    activeTab = vm.activeTab,
                    tabCount = vm.tabs.size,
                    onPrivacyClick = { vm.togglePrivacy(true) },
                    onTabsClick = { vm.toggleTabSwitcher(true) },
                    onMenuClick = { vm.toggleMenu(true) }
                )
                // Tab strip FR-1
                TabStrip(
                    tabs = vm.tabs,
                    activeId = vm.activeId,
                    onSwitch = vm::switchTab,
                    onClose = vm::closeTab,
                    onNewTab = { vm.newTab() }
                )
                // Main viewport
                Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xFF0E141E))) {
                    when (vm.activeTab.type) {
                        "newtab" -> NewTabContent(
                            blockedTotal = vm.blockedTotal,
                            version = vm.trackerBlocker.version,
                            onChip = { q -> vm.onAddressChange(q); vm.navigate(q) },
                            onDemo = vm::navigate,
                            onSearch = { q -> vm.navigate(q) }
                        )
                        "results" -> ResultsContent(query = vm.activeTab.query, onOpen = vm::navigate)
                        "site" -> SiteContent(tab = vm.activeTab, onTrackerClick = { vm.togglePrivacy(true) })
                        "http" -> HttpWarningContent(url = vm.activeTab.url, onUpgrade = vm::upgradeToHttps, onContinue = { /* keep http */ })
                        else -> NewTabContent(vm.blockedTotal, vm.trackerBlocker.version, { q -> vm.onAddressChange(q); vm.navigate(q) }, vm::navigate) { q -> vm.navigate(q) }
                    }
                    // Optional real WebView overlay for actual loading (behind mock content for now, but functional)
                    // Uncomment to load real URL:
                    // if (vm.activeTab.type == "site" || vm.activeTab.type == "results") {
                    //   AndroidView(factory = { ctx ->
                    //     WebView(ctx).apply {
                    //       webViewClient = NettraWebViewClient(vm.trackerBlocker, onTrackerBlocked = { vm.onTrackerBlocked() })
                    //       settings.javaScriptEnabled = true
                    //       loadUrl(vm.activeTab.url)
                    //     }
                    //   }, modifier = Modifier.fillMaxSize())
                    // }
                }
                // Bottom bar FR-6 Fire center
                BottomBar(
                    onBack = {},
                    onForward = {},
                    onFire = vm::requestFire,
                    onTabs = { vm.toggleTabSwitcher(true) },
                    onMenu = { vm.toggleMenu(true) }
                )
            }

            // Privacy Report sheet FR-7
            if (vm.showPrivacy) {
                ModalBottomSheet(
                    onDismissRequest = { vm.togglePrivacy(false) },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = NettraSurface,
                    contentColor = NettraText
                ) {
                    PrivacyReportSheet(tab = vm.activeTab, version = vm.trackerBlocker.version) { vm.togglePrivacy(false) }
                }
            }

            // Fire dialog FR-6
            if (vm.showFireDialog) {
                FireDialog(
                    tabCount = vm.tabs.size,
                    onDismiss = vm::dismissFire,
                    onConfirm = vm::doFire
                )
            }

            // Tab switcher FR-1
            if (vm.showTabSwitcher) {
                TabSwitcherSheet(
                    tabs = vm.tabs,
                    activeId = vm.activeId,
                    onSwitch = vm::switchTab,
                    onClose = vm::closeTab,
                    onNewTab = { vm.newTab() },
                    onPrivateTab = { vm.newTab(private = true) },
                    onDismiss = { vm.toggleTabSwitcher(false) }
                )
            }

            // Menu drawer simple
            if (vm.showMenu) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x99050A14))
                        .clickable { vm.toggleMenu(false) },
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Card(
                        modifier = Modifier
                            .width(280.dp)
                            .fillMaxSize()
                            .padding(12.dp)
                            .clickable(enabled = false) {},
                        colors = CardDefaults.cardColors(containerColor = NettraSurface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                            Text("Nettra", fontWeight = FontWeight.ExtraBold, color = NettraText)
                            Text("com.zaaam.nettra · v1", fontSize = 11.sp, color = NettraMuted)
                            Spacer(Modifier.height(12.dp))
                            MenuItem("Tab baru") { vm.newTab(); vm.toggleMenu(false) }
                            MenuItem("Private Tab") { vm.newTab(private = true); vm.toggleMenu(false) }
                            MenuItem("Bookmark (3)") {}
                            MenuItem("History (Room)") {}
                            MenuItem("Privacy Report") { vm.toggleMenu(false); vm.togglePrivacy(true) }
                            MenuItem("HTTPS-First ON") {}
                            MenuItem("🔥 Fire Button") { vm.toggleMenu(false); vm.requestFire() }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Atribusi: Tracker data © DuckDuckGo CC BY-NC-SA 4.0 · v2026.08.21 bundled.",
                                fontSize = 10.sp, color = NettraMuted, lineHeight = 14.sp
                            )
                            TextButton(onClick = { vm.toggleMenu(false) }) { Text("Tutup", color = NettraMuted) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddressBar(
    input: String,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    activeTab: TabState,
    tabCount: Int,
    onPrivacyClick: () -> Unit,
    onTabsClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(NettraSurface2)
            .border(1.dp, NettraBorder, RoundedCornerShape(999.dp))
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Privacy chip A/B/C
        val chipColor = when (activeTab.grade) { "A" -> NettraGreen; "B" -> NettraYellow; else -> NettraRed }
        val chipBg = when (activeTab.grade) { "A" -> Color(0x1F1DD75B); "B" -> Color(0x1FFFB020); else -> Color(0x1FFF3B3B) }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(chipBg)
                .border(1.dp, chipColor.copy(alpha = 0.3f), CircleShape)
                .clickable { onPrivacyClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(if (activeTab.grade == "A") "✓" else "!", color = chipColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            if (activeTab.blocked > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(NettraPurple)
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${activeTab.blocked}", fontSize = 7.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        androidx.compose.material3.OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            placeholder = { Text("Cari atau masukkan alamat", color = NettraDim, fontSize = 14.sp) },
            singleLine = true,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                focusedTextColor = NettraText, unfocusedTextColor = NettraText, cursorColor = NettraText
            )
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(NettraSurface3)
                .border(1.dp, NettraBorder, RoundedCornerShape(999.dp))
                .clickable { onTabsClick() }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) { Text("$tabCount", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NettraMuted) }
        Text("  ☰", modifier = Modifier.clickable { onMenuClick() }.padding(6.dp), color = NettraMuted, fontSize = 14.sp)
    }
    // Hidden submit via IME — for now extra button
    // The parent handles onSubmit via keyboard; simplified here
}

@Composable
private fun TabStrip(tabs: List<TabState>, activeId: Long, onSwitch: (Long) -> Unit, onClose: (Long) -> Unit, onNewTab: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tabs.forEach { t ->
            val isActive = t.id == activeId
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isActive) NettraSurface3 else Color(0x0FFFFFFF))
                    .border(1.dp, if (isActive) NettraBorder else Color.Transparent, RoundedCornerShape(999.dp))
                    .clickable { onSwitch(t.id) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (t.isPrivate) Text("🕶️ ", fontSize = 10.sp)
                Text(
                    t.title.take(18),
                    fontSize = 12.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    color = if (isActive) NettraText else NettraMuted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 90.dp)
                )
                Text(" ✕", modifier = Modifier.clickable { onClose(t.id) }.padding(start = 6.dp), color = NettraMuted, fontSize = 10.sp)
            }
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .border(1.dp, NettraBorder, CircleShape)
                .clickable { onNewTab() },
            contentAlignment = Alignment.Center
        ) { Text("+", color = NettraMuted) }
    }
}

@Composable
private fun NewTabContent(blockedTotal: Int, version: String, onChip: (String) -> Unit, onDemo: (String) -> Unit, onSearch: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(NettraFire, Color(0xFFFF8A1A), NettraYellow))),
                contentAlignment = Alignment.Center
            ) { Text("🔥", fontSize = 20.sp) }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text("Nettra — private", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = NettraText)
                Text("Cari tanpa profiling. Tracker diblokir otomatis.", fontSize = 12.sp, color = NettraMuted)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = NettraSurface), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().border(1.dp, NettraBorder, RoundedCornerShape(18.dp))) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(999.dp)).background(NettraSurface2).border(1.dp, NettraBorder, RoundedCornerShape(999.dp)).padding(10.dp)) {
                    Text("⌕ ", color = NettraMuted)
                    Text("Cari atau masukkan alamat", color = NettraDim, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text("DuckDuckGo", color = Color(0xFFDE5833), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(Color(0x1FDE5833), RoundedCornerShape(999.dp)).padding(horizontal = 7.dp, vertical = 4.dp))
                }
                Text("CEPAT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NettraMuted, modifier = Modifier.padding(top = 10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    listOf("berita hari ini", "resep nasi goreng", "jadwal sholat").forEach { q ->
                        Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(NettraSurface2).border(1.dp, NettraBorder, RoundedCornerShape(999.dp)).clickable { onChip(q) }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(q, fontSize = 11.sp, color = NettraMuted)
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = NettraSurface), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("PROTEKSI AKTIF", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NettraDim)
                    Text("$blockedTotal", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = NettraText)
                    Text("tracker diblokir (sesi)", fontSize = 11.sp, color = NettraMuted)
                }
            }
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = NettraSurface), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("BLOCKLIST DDG", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NettraDim)
                    Text(version, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NettraText)
                    Text("CC BY-NC-SA 4.0", fontSize = 11.sp, color = NettraMuted)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Buka situs demo (news)", modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(NettraSurface2).border(1.dp, NettraBorder, RoundedCornerShape(999.dp)).clickable { onDemo("https://contoh-berita.id/artikel/privasi") }.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 12.sp, color = NettraText)
            Text("Shop (9 tracker)", modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(NettraSurface2).border(1.dp, NettraBorder, RoundedCornerShape(999.dp)).clickable { onDemo("https://toko-contoh.com/promo") }.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 12.sp, color = NettraText)
        }
    }
}

@Composable
private fun ResultsContent(query: String, onOpen: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {
        Text("DuckDuckGo → https://duckduckgo.com/?q=$query", fontSize = 11.sp, color = NettraMuted)
        Text("Hasil untuk \"$query\" — tanpa profiling (FR-2)", fontSize = 12.sp, color = NettraMuted, modifier = Modifier.padding(vertical = 8.dp))
        listOf("contoh-berita.id" to "Berita hari ini — ringkasan tanpa tracker", "toko-contoh.com" to "Toko online — 9 tracker dicegat").forEach { (host, title) ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onOpen("https://$host") }, colors = CardDefaults.cardColors(containerColor = NettraSurface)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("https://$host · 🔒", fontSize = 11.sp, color = NettraGreen)
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF7CC8FF))
                }
            }
        }
    }
}

@Composable
private fun SiteContent(tab: TabState, onTrackerClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(modifier = Modifier.fillMaxWidth().background(NettraSurface).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(tab.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NettraText, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text("${tab.blocked} diblokir", fontSize = 11.sp, color = NettraPurple, modifier = Modifier.clickable { onTrackerClick() })
        }
        if (tab.blocked > 0) {
            Box(modifier = Modifier.fillMaxWidth().background(Color(0x1F8B5CF6)).padding(10.dp)) {
                Text("🛡️ ${tab.blocked} tracker diblokir — duckduckgo/tracker-blocklists", fontSize = 12.sp, color = Color(0xFFC4B5FD))
            }
        }
        Column(modifier = Modifier.padding(14.dp)) {
            Text(if (tab.url.contains("shop")) "Promo besar — 9 tracker mengintai" else "Kenapa privasi browsing itu penting", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = NettraText)
            Text("🔒 HTTPS-First aktif · 🛡️ ${tab.blocked} diblokir", fontSize = 11.sp, color = NettraMuted, modifier = Modifier.padding(vertical = 6.dp))
            Text("Nettra memblokir di level WebViewClient.shouldInterceptRequest — request tidak pernah keluar, bukan cuma disembunyikan. First-party tetap jalan.", fontSize = 14.sp, color = Color(0xFFC9D4E3), lineHeight = 20.sp)
        }
    }
}

@Composable
private fun HttpWarningContent(url: String, onUpgrade: () -> Unit, onContinue: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0x1FFFB020)), shape = RoundedCornerShape(14.dp)) {
            Text("⚠️ Koneksi tidak aman — HTTPS-First\n$url akan di-upgrade ke HTTPS otomatis (FR-5).", modifier = Modifier.padding(12.dp), fontSize = 13.sp, color = NettraText)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🔒 Buka HTTPS", modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(NettraText).clickable { onUpgrade() }.padding(horizontal = 14.dp, vertical = 10.dp), color = NettraBg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("Lanjut HTTP", modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(NettraSurface2).border(1.dp, NettraBorder, RoundedCornerShape(999.dp)).clickable { onContinue() }.padding(horizontal = 14.dp, vertical = 10.dp), color = NettraText, fontSize = 13.sp)
        }
    }
}

@Composable
private fun BottomBar(onBack: () -> Unit, onForward: () -> Unit, onFire: () -> Unit, onTabs: () -> Unit, onMenu: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp).background(Color(0xE80A0E14)).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row { Text("←", modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).clickable { onBack() }.padding(12.dp), color = NettraMuted); Text("→", modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).clickable { onForward() }.padding(12.dp), color = NettraMuted) }
        Box(
            modifier = Modifier.size(56.dp).clip(CircleShape).background(Brush.verticalGradient(listOf(Color(0xFFFF6A1A), NettraFire))).border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape).clickable { onFire() },
            contentAlignment = Alignment.Center
        ) { Text("🔥", fontSize = 22.sp) }
        Row { Text("▦", modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).clickable { onTabs() }.padding(12.dp), color = NettraMuted); Text("☰", modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).clickable { onMenu() }.padding(12.dp), color = NettraMuted) }
    }
}

@Composable
private fun MenuItem(label: String, onClick: () -> Unit = {}) {
    Text(label, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onClick() }.padding(10.dp), color = NettraText, fontSize = 13.sp)
}

@Composable
private fun PrivacyReportSheet(tab: TabState, version: String, onDismiss: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
        Text("Privacy Report", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = NettraText)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(NettraSurface2).border(1.dp, NettraBorder, RoundedCornerShape(14.dp)).padding(12.dp)) {
            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(when (tab.grade) { "A" -> Color(0x1F1DD75B); "B" -> Color(0x1FFFB020); else -> Color(0x1FFF3B3B) }).border(1.dp, when (tab.grade) { "A" -> NettraGreen; "B" -> NettraYellow; else -> NettraRed }, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Text(tab.grade, fontWeight = FontWeight.ExtraBold, color = when (tab.grade) { "A" -> NettraGreen; "B" -> NettraYellow; else -> NettraRed })
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text("Privacy Grade ${tab.grade}", fontWeight = FontWeight.Bold, color = NettraText)
                Text("${if (tab.secure) "🔒 HTTPS aktif" else "⚠️ HTTP"} · ${tab.blocked} tracker diblokir", fontSize = 12.sp, color = NettraMuted)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Situs: ${tab.url.ifEmpty { "Tab baru" }}", fontSize = 11.sp, color = NettraMuted)
        Text("Blocklist: $version (bundled)", fontSize = 11.sp, color = NettraMuted)
        Text("Angka = request yang benar-benar diblokir FR-4, bukan statis.", fontSize = 10.sp, color = NettraDim, modifier = Modifier.padding(top = 8.dp))
        TextButton(onClick = onDismiss) { Text("Tutup", color = NettraText) }
    }
}

@Composable
private fun FireDialog(tabCount: Int, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hapus semua sesi?") },
        text = {
            Column {
                Text("Semua tab ($tabCount → 1), cookie & cache dihapus total (FR-6). Bookmark aman.", fontSize = 13.sp, color = NettraMuted)
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("🔥 Fire", color = NettraFire, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal", color = NettraMuted) } },
        containerColor = NettraSurface, titleContentColor = NettraText, textContentColor = NettraMuted
    )
}

@Composable
private fun TabSwitcherSheet(tabs: List<TabState>, activeId: Long, onSwitch: (Long) -> Unit, onClose: (Long) -> Unit, onNewTab: () -> Unit, onPrivateTab: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = NettraSurface, contentColor = NettraText) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Tab — ${tabs.size}", fontWeight = FontWeight.ExtraBold, color = NettraText)
                Text("Tutup", modifier = Modifier.clickable { onDismiss() }.padding(8.dp), color = NettraMuted, fontSize = 13.sp)
            }
            tabs.forEach { t ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onSwitch(t.id) },
                    colors = CardDefaults.cardColors(containerColor = if (t.id == activeId) NettraSurface3 else NettraSurface2),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${if (t.isPrivate) "🕶️ " else ""}${t.title}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NettraText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${t.blocked} blocked · ${t.grade} ${if (t.secure) "🔒" else "⚠️"}", fontSize = 11.sp, color = NettraMuted)
                        }
                        Text("✕", modifier = Modifier.clickable { onClose(t.id) }.padding(8.dp), color = NettraMuted)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                Text("+ Tab baru", modifier = Modifier.weight(1f).clip(RoundedCornerShape(999.dp)).background(NettraText).clickable { onNewTab() }.padding(12.dp), color = NettraBg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("🕶️ Private", modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(NettraSurface2).border(1.dp, NettraBorder, RoundedCornerShape(999.dp)).clickable { onPrivateTab() }.padding(12.dp), color = NettraText, fontSize = 13.sp)
            }
            Text("FR-1: isolasi per-tab, min 1 tab.", fontSize = 10.sp, color = NettraDim, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
