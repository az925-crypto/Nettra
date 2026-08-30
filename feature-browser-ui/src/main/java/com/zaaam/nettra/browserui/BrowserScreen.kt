package com.zaaam.nettra.browserui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.input.ImeAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(vm: BrowserViewModel = viewModel()) {
    val activeTab by remember { derivedStateOf { vm.activeTab } }
    val tabCount by remember { derivedStateOf { vm.tabs.size } }
    val onSwitchTab = remember(vm) { { id: Long -> vm.switchTab(id) } }
    val onCloseTab = remember(vm) { { id: Long -> vm.closeTab(id) } }
    val onNewTab = remember(vm) { { vm.newTab() } }
    val onSubmit = remember(vm) { { vm.navigate(vm.addressInput) } }

    NettraTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NettraColors.VoidInk)
        ) {
            // Subtle grid + noise via overlay (hard brutalist, not blur)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0x0AFFFFFF))
                        )
                    )
            )
            Column(modifier = Modifier.fillMaxSize()) {
                // Top wordmark
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "N E T T R A",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                            fontFamily = NettraFontMono,
                            letterSpacing = 2.4.sp,
                            color = NettraColors.PaperBone
                        )
                    )
                    Text(
                        "DOSSIER • ${vm.trackerBlocker.version} • FORENSIK",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                            fontFamily = NettraFontMono,
                            fontSize = 9.sp,
                            color = NettraColors.Soot
                        )
                    )
                }
                // Redacted Bar — 4dp, PaperBone, redacted blocks
                RedactedBar(
                    input = vm.addressInput,
                    onInputChange = vm::onAddressChange,
                    onSubmit = onSubmit,
                    activeTab = activeTab,
                    tabCount = tabCount,
                    onPrivacyClick = { vm.togglePrivacy(true) },
                    onTabsClick = { vm.toggleTabSwitcher(true) },
                    onMenuClick = { vm.toggleMenu(true) }
                )
                // Dossier Stack — tab strip with overlap & hard shadow
                DossierStack(
                    tabs = vm.tabs,
                    activeId = vm.activeId,
                    onSwitch = onSwitchTab,
                    onClose = onCloseTab,
                    onNewTab = onNewTab
                )
                // Viewport — VoidInk with grid lines
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(NettraColors.VoidInk)
                ) {
                    when (activeTab.type) {
                        "newtab" -> BurnNewTab(
                            blockedTotal = vm.blockedTotal,
                            version = vm.trackerBlocker.version,
                            onChip = { q -> vm.onAddressChange(q); vm.navigate(q) },
                            onDemo = vm::navigate
                        )
                        "results" -> BurnResults(query = activeTab.query, onOpen = vm::navigate)
                        "site" -> BurnSite(tab = activeTab, onTrackerClick = { vm.togglePrivacy(true) })
                        "http" -> BurnHttpWarning(url = activeTab.url, onUpgrade = vm::upgradeToHttps)
                        else -> BurnNewTab(vm.blockedTotal, vm.trackerBlocker.version, { q -> vm.onAddressChange(q); vm.navigate(q) }, vm::navigate)
                    }
                }
                // Bottom concrete bar with hard border
                BurnBottomBar(
                    onBack = {},
                    onForward = {},
                    onFire = vm::requestFire,
                    onTabs = { vm.toggleTabSwitcher(true) },
                    onMenu = { vm.toggleMenu(true) }
                )
            }

            if (vm.showPrivacy) {
                ModalBottomSheet(
                    onDismissRequest = { vm.togglePrivacy(false) },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = NettraColors.PaperBone,
                    contentColor = NettraColors.VoidInk,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    BurnPrivacySheet(tab = activeTab, version = vm.trackerBlocker.version) { vm.togglePrivacy(false) }
                }
            }
            if (vm.showFireDialog) {
                AlertDialog(
                    onDismissRequest = vm::dismissFire,
                    containerColor = NettraColors.PaperBone,
                    titleContentColor = NettraColors.VoidInk,
                    textContentColor = NettraColors.Concrete,
                    title = {
                        Text(
                            "Bakar Jejak?",
                            style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
                                fontFamily = NettraFontInstrument, color = NettraColors.VoidInk
                            )
                        )
                    },
                    text = {
                        Text(
                            "Semua tab ($tabCount → 1), cookie & cache dimusnahkan. Bookmark tetap. Tindakan tidak bisa dibatalkan.",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(
                                fontFamily = NettraFontSpace, color = NettraColors.Concrete
                            )
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = vm::doFire) {
                            Text(
                                "◉ BAKAR",
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = NettraFontMono, color = NettraColors.BurnVermillion
                                )
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = vm::dismissFire) {
                            Text(
                                "BATAL",
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = NettraFontMono, color = NettraColors.Soot
                                )
                            )
                        }
                    }
                )
            }
            if (vm.showTabSwitcher) {
                BurnTabSwitcher(
                    tabs = vm.tabs,
                    activeId = vm.activeId,
                    onSwitch = onSwitchTab,
                    onClose = onCloseTab,
                    onNewTab = { vm.newTab(); vm.toggleTabSwitcher(false) },
                    onPrivate = { vm.newTab(private = true); vm.toggleTabSwitcher(false) },
                    onDismiss = { vm.toggleTabSwitcher(false) }
                )
            }
            if (vm.showMenu) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x990B0F12))
                        .clickable { vm.toggleMenu(false) },
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Card(
                        modifier = Modifier
                            .width(280.dp)
                            .fillMaxSize()
                            .padding(12.dp)
                            .clickable(enabled = false) {},
                        colors = CardDefaults.cardColors(containerColor = NettraColors.PaperBone),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                            Text(
                                "NETTRA",
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = NettraFontMono, letterSpacing = 2.sp, color = NettraColors.VoidInk
                                )
                            )
                            Text("DOSSIER • com.zaaam.nettra", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, fontSize = 9.sp, color = NettraColors.Soot))
                            Spacer(Modifier.height(12.dp))
                            listOf("Tab baru", "Private Tab", "Bookmark (3)", "History", "Laporan Forensik", "HTTPS-First ON", "◉ BAKAR JEJAK").forEach { label ->
                                Text(
                                    label,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable {
                                            when (label) {
                                                "Tab baru" -> { vm.newTab(); vm.toggleMenu(false) }
                                                "Private Tab" -> { vm.newTab(private = true); vm.toggleMenu(false) }
                                                "Laporan Forensik" -> { vm.toggleMenu(false); vm.togglePrivacy(true) }
                                                "◉ BAKAR JEJAK" -> { vm.toggleMenu(false); vm.requestFire() }
                                            }
                                        }
                                        .padding(10.dp),
                                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = NettraFontSpace, color = NettraColors.VoidInk, fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Text(
                                "Tracker: DuckDuckGo Tracker Radar • CC BY-NC-SA 4.0 • v2026.08.21",
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = NettraFontMono, fontSize = 8.sp, color = NettraColors.Soot
                                ),
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RedactedBar(
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
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(NettraColors.PaperBone)
            .border(1.5.dp, NettraColors.BorderStrong, RoundedCornerShape(4.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Privacy stamp
        val gradeColor = when (activeTab.grade) { "A" -> NettraColors.BurnVermillion; "B" -> Color(0xFFFFB020); else -> Color(0xFF8C877F) }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (activeTab.grade == "A") NettraColors.BurnVermillion else Color.Black)
                .clickable { onPrivacyClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                when (activeTab.grade) { "A" -> "✓"; "B" -> "!"; else -> "✕" },
                color = NettraColors.PaperBone, fontSize = 12.sp, fontWeight = FontWeight.Bold
            )
        }
        // Redacted blocks for trackers
        if (activeTab.blocked > 0) {
            Row(modifier = Modifier.padding(start = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(minOf(activeTab.blocked, 3)) {
                    Box(modifier = Modifier.size(width = 28.dp, height = 12.dp).clip(RoundedCornerShape(2.dp)).background(Color.Black))
                }
                if (activeTab.blocked > 3) {
                    Text(
                        "+${activeTab.blocked - 3}",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                            fontFamily = NettraFontMono, fontSize = 9.sp, color = Color.Black
                        ),
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }
        }
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            placeholder = {
                Text(
                    "Cari atau masukkan alamat —",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(
                        fontFamily = NettraFontSpace, color = NettraColors.Soot
                    )
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                focusedTextColor = NettraColors.VoidInk, unfocusedTextColor = NettraColors.VoidInk,
                cursorColor = NettraColors.BurnVermillion
            ),
            textStyle = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(
                fontFamily = NettraFontMono, color = NettraColors.VoidInk, fontWeight = FontWeight.Bold
            )
        )
        // Tab count mono 02
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(NettraColors.VoidInk)
                .clickable { onTabsClick() }
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                String.format("%02d", tabCount),
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                    fontFamily = NettraFontMono, color = NettraColors.PaperBone, fontSize = 11.sp
                )
            )
        }
        Text(
            "☰",
            modifier = Modifier.clickable { onMenuClick() }.padding(start = 8.dp),
            color = NettraColors.VoidInk, fontSize = 14.sp, fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DossierStack(tabs: List<TabState>, activeId: Long, onSwitch: (Long) -> Unit, onClose: (Long) -> Unit, onNewTab: () -> Unit) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(NettraColors.VoidInk)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy((-8).dp)
    ) {
        items(tabs, key = { it.id }) { t ->
            val isActive = t.id == activeId
            Box(
                modifier = Modifier
                    .shadow(elevation = if (isActive) 4.dp else 0.dp, shape = RoundedCornerShape(4.dp), clip = false)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isActive) NettraColors.PaperBone else NettraColors.Concrete)
                    .border(1.5.dp, if (isActive) NettraColors.BurnVermillion else NettraColors.Border, RoundedCornerShape(4.dp))
                    .clickable { onSwitch(t.id) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (t.isPrivate) Text("◉ ", color = NettraColors.BurnVermillion, fontSize = 8.sp)
                    Text(
                        t.title.take(14),
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                            fontFamily = if (isActive) NettraFontMono else NettraFontSpace,
                            color = if (isActive) NettraColors.VoidInk else NettraColors.Soot,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "  ✕",
                        modifier = Modifier.clickable { onClose(t.id) }.padding(start = 4.dp),
                        color = if (isActive) NettraColors.VoidInk else NettraColors.Soot,
                        fontSize = 10.sp
                    )
                }
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(NettraColors.BurnVermillion)
                    )
                }
            }
        }
        item(key = "new_tab_btn") {
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(32.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.5.dp, NettraColors.Border, RoundedCornerShape(4.dp))
                    .background(Color.Transparent)
                    .clickable { onNewTab() },
                contentAlignment = Alignment.Center
            ) { Text("+", color = NettraColors.Soot, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun BurnNewTab(blockedTotal: Int, version: String, onChip: (String) -> Unit, onDemo: (String) -> Unit) {
    val fireBrush = remember { androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(NettraColors.BurnVermillion, Color(0xFF8C877F))) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Headline Instrument Serif 42sp — the unforgettable
        Text(
            "Tak seorang pun\nmengikutimu.",
            style = androidx.compose.material3.MaterialTheme.typography.displayLarge.copy(
                fontFamily = NettraFontInstrument, color = NettraColors.PaperBone
            )
        )
        Text(
            "Pencarian DuckDuckGo • HTTPS-First aktif • $version",
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                fontFamily = NettraFontMono, color = NettraColors.Soot, fontSize = 10.sp
            )
        )
        // Search dossier field — Concrete, 4dp, redacted cursor
        Card(
            colors = CardDefaults.cardColors(containerColor = NettraColors.Concrete),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.fillMaxWidth().border(1.5.dp, NettraColors.Border, RoundedCornerShape(4.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(NettraColors.VoidInk)
                        .border(1.dp, NettraColors.Border, RoundedCornerShape(4.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⌕  ", color = NettraColors.Soot, fontSize = 14.sp)
                    Text(
                        "Cari atau masukkan alamat —",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(
                            fontFamily = NettraFontSpace, color = NettraColors.Soot
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Box(modifier = Modifier.size(width = 12.dp, height = 16.dp).background(NettraColors.BurnVermillion))
                }
                Text(
                    "CEPAT",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                        fontFamily = NettraFontMono, color = NettraColors.Soot, fontSize = 9.sp
                    ),
                    modifier = Modifier.padding(top = 10.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    listOf("berita hari ini", "resep nasi goreng", "jadwal sholat").forEach { q ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NettraColors.ConcreteElevated)
                                .border(1.dp, NettraColors.Border, RoundedCornerShape(4.dp))
                                .clickable { onChip(q) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                q,
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = NettraFontMono, color = NettraColors.Soot, fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
            }
        }
        // Two evidence cards — asymmetric
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(
                modifier = Modifier.weight(1.2f),
                colors = CardDefaults.cardColors(containerColor = NettraColors.PaperBone),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "DIMUSNAHKAN",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                            fontFamily = NettraFontMono, color = NettraColors.Soot, fontSize = 9.sp
                        )
                    )
                    Text(
                        String.format("%02d", blockedTotal),
                        style = androidx.compose.material3.MaterialTheme.typography.displayMedium.copy(
                            fontFamily = NettraFontInstrument, color = NettraColors.VoidInk, fontSize = 36.sp
                        )
                    )
                    Text(
                        "pelacak sesi ini",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(
                            fontFamily = NettraFontSpace, color = NettraColors.Concrete
                        )
                    )
                    Box(modifier = Modifier.padding(top = 8.dp).height(2.dp).fillMaxWidth().background(fireBrush))
                }
            }
            Card(
                modifier = Modifier.weight(0.8f),
                colors = CardDefaults.cardColors(containerColor = NettraColors.ConcreteElevated),
                shape = RoundedCornerShape(4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("DOSSIER", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.Soot, fontSize = 9.sp))
                    Text(version, style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(fontFamily = NettraFontMono, color = NettraColors.PaperBone, fontSize = 16.sp))
                    Text("CC BY-NC-SA 4.0", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.Soot, fontSize = 9.sp))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "BUKA DOSSIER NEWS",
                modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(NettraColors.PaperBone).clickable { onDemo("https://contoh-berita.id/artikel/privasi") }.padding(horizontal = 14.dp, vertical = 10.dp),
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.VoidInk)
            )
            Text(
                "SHOP 09 TRACKER",
                modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color.Black).border(1.dp, NettraColors.BurnVermillion, RoundedCornerShape(4.dp)).clickable { onDemo("https://toko-contoh.com/promo") }.padding(horizontal = 14.dp, vertical = 10.dp),
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.BurnVermillion)
            )
        }
    }
}

@Composable
private fun BurnResults(query: String, onOpen: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(
            "DUCKDUCKGO — https://duckduckgo.com/?q=$query",
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.Soot, fontSize = 9.sp)
        )
        Text(
            "Hasil untuk “$query” — tanpa profiling",
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(fontFamily = NettraFontInstrument, color = NettraColors.PaperBone, fontSize = 18.sp),
            modifier = Modifier.padding(vertical = 12.dp)
        )
        listOf("contoh-berita.id" to "Berita hari ini — ringkasan tanpa tracker", "toko-contoh.com" to "Toko online — 09 tracker dimusnahkan").forEach { (host, title) ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onOpen("https://$host") },
                colors = CardDefaults.cardColors(containerColor = NettraColors.ConcreteElevated),
                shape = RoundedCornerShape(4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("https://$host — BERSIH", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.BurnVermillion, fontSize = 9.sp))
                    Text(title, style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(fontFamily = NettraFontInstrument, color = NettraColors.PaperBone, fontSize = 16.sp))
                }
            }
        }
    }
}

@Composable
private fun BurnSite(tab: TabState, onTrackerClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().background(NettraColors.PaperBone).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                tab.title,
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(fontFamily = NettraFontInstrument, color = NettraColors.VoidInk, fontSize = 16.sp),
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
            )
            Text(
                String.format("%02d DIBLOKIR", tab.blocked),
                modifier = Modifier.clickable { onTrackerClick() }.background(NettraColors.BurnVermillion, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.PaperBone, fontSize = 9.sp)
            )
        }
        if (tab.blocked > 0) {
            Box(modifier = Modifier.fillMaxWidth().background(Color.Black).padding(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    repeat(minOf(tab.blocked, 4)) { Box(modifier = Modifier.size(width = 32.dp, height = 10.dp).background(Color.White)) }
                    Text("— ${tab.blocked} ███ DIMUSNAHKAN", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = Color.White, fontSize = 9.sp))
                }
            }
        }
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                if (tab.url.contains("shop")) "Promo besar — sembilan pelacak mengintai" else "Kenapa privasi itu pembakaran",
                style = androidx.compose.material3.MaterialTheme.typography.displayMedium.copy(fontFamily = NettraFontInstrument, color = NettraColors.PaperBone)
            )
            Text(
                "14:02:11 • HTTPS-First aktif • ${tab.blocked} dimusnahkan",
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.Soot, fontSize = 9.sp),
                modifier = Modifier.padding(vertical = 6.dp)
            )
            Text(
                "Nettra memusnahkan di level shouldInterceptRequest — request tidak pernah keluar, bukan disembunyikan. First-party tetap utuh.",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(fontFamily = NettraFontSpace, color = NettraColors.PaperBone),
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun BurnHttpWarning(url: String, onUpgrade: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = NettraColors.PaperBone), shape = RoundedCornerShape(4.dp)) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("KONEKSI TIDAK AMAN", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = Color.Black, fontSize = 9.sp))
                Text("HTTP terdeteksi — akan di-upgrade ke HTTPS", style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(fontFamily = NettraFontInstrument, color = Color.Black))
                Text(url, style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.Concrete, fontSize = 10.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "◉ UPGRADE KE HTTPS",
                modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(NettraColors.BurnVermillion).clickable { onUpgrade() }.padding(horizontal = 16.dp, vertical = 12.dp),
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.PaperBone)
            )
        }
    }
}

@Composable
private fun BurnBottomBar(onBack: () -> Unit, onForward: () -> Unit, onFire: () -> Unit, onTabs: () -> Unit, onMenu: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp).background(NettraColors.Concrete).border(1.5.dp, NettraColors.Border, RoundedCornerShape(0.dp)).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row { Text("←", modifier = Modifier.size(42.dp).clip(RoundedCornerShape(4.dp)).clickable { onBack() }.padding(12.dp), color = NettraColors.Soot, fontWeight = FontWeight.Bold); Text("→", modifier = Modifier.size(42.dp).clip(RoundedCornerShape(4.dp)).clickable { onForward() }.padding(12.dp), color = NettraColors.Soot) }
        // Burn Stamp — the unforgettable 56dp
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(NettraColors.BurnVermillion)
                .border(3.dp, NettraColors.PaperBone, CircleShape)
                .border(1.dp, Color.Black, CircleShape)
                .clickable { onFire() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("◉", color = NettraColors.PaperBone, fontSize = 10.sp, lineHeight = 10.sp)
                Text("BURN", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.PaperBone, fontSize = 8.sp, letterSpacing = 1.sp))
            }
        }
        Row { Text("▦", modifier = Modifier.size(42.dp).clip(RoundedCornerShape(4.dp)).clickable { onTabs() }.padding(12.dp), color = NettraColors.Soot); Text("☰", modifier = Modifier.size(42.dp).clip(RoundedCornerShape(4.dp)).clickable { onMenu() }.padding(12.dp), color = NettraColors.Soot) }
    }
}

@Composable
private fun BurnPrivacySheet(tab: TabState, version: String, onDismiss: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
        Text("LAPORAN FORENSIK", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.Soot, fontSize = 9.sp))
        Text("Bukti Pelacakan", style = androidx.compose.material3.MaterialTheme.typography.displayMedium.copy(fontFamily = NettraFontInstrument, color = NettraColors.VoidInk, fontSize = 28.sp))
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(NettraColors.VoidInk).padding(14.dp)) {
            Text(
                String.format("%02d", tab.blocked),
                style = androidx.compose.material3.MaterialTheme.typography.displayLarge.copy(fontFamily = NettraFontInstrument, color = NettraColors.BurnVermillion, fontSize = 48.sp),
                modifier = Modifier.padding(end = 12.dp)
            )
            Column {
                Text("PELACAK DIMUSNAHKAN", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.PaperBone, fontSize = 9.sp))
                Text(if (tab.secure) "HTTPS AKTIF • $version" else "HTTP TIDAK AMAN", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.Soot, fontSize = 9.sp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Situs: ${tab.url.ifEmpty { "—" }}", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.Concrete, fontSize = 10.sp))
        Text("Angka dari shouldInterceptRequest FR-4, bukan statis.", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.Soot, fontSize = 9.sp), modifier = Modifier.padding(top = 4.dp))
        TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 8.dp)) { Text("TUTUP", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.VoidInk)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BurnTabSwitcher(tabs: List<TabState>, activeId: Long, onSwitch: (Long) -> Unit, onClose: (Long) -> Unit, onNewTab: () -> Unit, onPrivate: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = NettraColors.PaperBone, contentColor = NettraColors.VoidInk, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("DOSSIER — ${String.format("%02d", tabs.size)}", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.VoidInk))
                Text("TUTUP", modifier = Modifier.clickable { onDismiss() }.padding(8.dp), style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.Soot))
            }
            tabs.forEach { t ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onSwitch(t.id) },
                    colors = CardDefaults.cardColors(containerColor = if (t.id == activeId) NettraColors.VoidInk else Color.White),
                    shape = RoundedCornerShape(4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (t.id == activeId) 4.dp else 0.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${if (t.isPrivate) "◉ " else ""}${t.title}", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = if (t.id == activeId) NettraColors.PaperBone else NettraColors.VoidInk), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(String.format("%02d • %s %s", t.blocked, t.grade, if (t.secure) "HTTPS" else "HTTP"), style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, fontSize = 9.sp, color = NettraColors.Soot))
                        }
                        Text("✕", modifier = Modifier.clickable { onClose(t.id) }.padding(8.dp), style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.Soot))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                Text("◉ TAB BARU", modifier = Modifier.weight(1f).clip(RoundedCornerShape(4.dp)).background(NettraColors.VoidInk).clickable { onNewTab() }.padding(12.dp), style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.PaperBone))
                Text("◉ PRIVATE", modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color.Black).border(1.dp, NettraColors.Soot, RoundedCornerShape(4.dp)).clickable { onPrivate() }.padding(12.dp), style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.Soot))
            }
        }
    }
}
