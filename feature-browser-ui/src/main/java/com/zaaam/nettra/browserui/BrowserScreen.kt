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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
        Box(modifier = Modifier.fillMaxSize().background(NettraColors.VoidBlack)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // TopAppBar — center wordmark, edge-to-edge, not web header
                Row(
                    modifier = Modifier.fillMaxWidth().background(NettraColors.VoidBlack).padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NettraColors.SignalLime))
                        Spacer(Modifier.width(8.dp))
                        Text("NETTRA", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontSpace, letterSpacing = 1.6.sp, color = NettraColors.GhostWhite, fontWeight = FontWeight.Bold))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("◉", color = if (activeTab.secure) NettraColors.SignalLime else NettraColors.GhostAsh, fontSize = 12.sp)
                        Text("⋮", color = NettraColors.GhostAsh, fontSize = 18.sp, modifier = Modifier.clickable { vm.toggleMenu(true) }.padding(4.dp))
                    }
                }
                // Search Pill — 56dp, 28dp radius, single hue lime focus
                BunkerSearchPill(
                    input = vm.addressInput,
                    onInputChange = vm::onAddressChange,
                    onSubmit = onSubmit,
                    activeTab = activeTab,
                    tabCount = tabCount,
                    onTabsClick = { vm.toggleTabSwitcher(true) }
                )
                // Tab chips — FilterChip style, not dossier stack
                BunkerTabRow(tabs = vm.tabs, activeId = vm.activeId, onSwitch = onSwitchTab, onClose = onCloseTab, onNewTab = onNewTab)
                // Viewport — true OLED, no paper
                Box(modifier = Modifier.weight(1f).fillMaxWidth().background(NettraColors.VoidBlack)) {
                    when (activeTab.type) {
                        "newtab" -> BunkerNewTab(blockedTotal = vm.blockedTotal, version = vm.trackerBlocker.version, onChip = { q -> vm.onAddressChange(q); vm.navigate(q) }, onDemo = vm::navigate)
                        "results" -> BunkerResults(query = activeTab.query, onOpen = vm::navigate)
                        "site" -> BunkerSite(tab = activeTab, onTrackerClick = { vm.togglePrivacy(true) })
                        "http" -> BunkerHttpWarning(url = activeTab.url, onUpgrade = vm::upgradeToHttps)
                        else -> BunkerNewTab(vm.blockedTotal, vm.trackerBlocker.version, { q -> vm.onAddressChange(q); vm.navigate(q) }, vm::navigate)
                    }
                }
                // Bunker Bottom Strip — 28dp top radius, 80dp height, Fire Vault centered above
                BunkerBottomBar(onBack = {}, onForward = {}, onFire = vm::requestFire, onTabs = { vm.toggleTabSwitcher(true) }, onMenu = { vm.toggleMenu(true) }, tabCount = tabCount)
            }
            if (vm.showPrivacy) {
                ModalBottomSheet(
                    onDismissRequest = { vm.togglePrivacy(false) },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = NettraColors.Bunker,
                    contentColor = NettraColors.GhostWhite,
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                ) {
                    BunkerPrivacySheet(tab = activeTab, version = vm.trackerBlocker.version) { vm.togglePrivacy(false) }
                }
            }
            if (vm.showFireDialog) {
                AlertDialog(
                    onDismissRequest = vm::dismissFire,
                    containerColor = NettraColors.Bunker,
                    titleContentColor = NettraColors.GhostWhite,
                    textContentColor = NettraColors.GhostAsh,
                    title = { Text("Bakar Sesi?", style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(fontFamily = NettraFontSpace, fontWeight = FontWeight.Bold, color = NettraColors.GhostWhite)) },
                    text = { Text("$tabCount tab → 1 • Cookie & cache dimusnahkan • Bookmark aman", style = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(fontFamily = NettraFontInstrument, color = NettraColors.GhostAsh)) },
                    confirmButton = { TextButton(onClick = vm::doFire) { Text("BAKAR", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.AlertEmber)) } },
                    dismissButton = { TextButton(onClick = vm::dismissFire) { Text("BATAL", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.GhostAsh)) } }
                )
            }
            if (vm.showTabSwitcher) {
                BunkerTabSwitcher(tabs = vm.tabs, activeId = vm.activeId, onSwitch = onSwitchTab, onClose = onCloseTab, onNewTab = { vm.newTab(); vm.toggleTabSwitcher(false) }, onPrivate = { vm.newTab(private = true); vm.toggleTabSwitcher(false) }, onDismiss = { vm.toggleTabSwitcher(false) })
            }
            if (vm.showMenu) {
                Box(modifier = Modifier.fillMaxSize().background(Color(0x99060A0C)).clickable { vm.toggleMenu(false) }, contentAlignment = Alignment.CenterEnd) {
                    Card(modifier = Modifier.width(280.dp).fillMaxSize().padding(12.dp).clickable(enabled = false) {}, colors = CardDefaults.cardColors(containerColor = NettraColors.Bunker), shape = RoundedCornerShape(24.dp)) {
                        LazyColumn(modifier = Modifier.padding(16.dp)) {
                            item { Text("NETTRA", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontSpace, letterSpacing = 1.6.sp, color = NettraColors.GhostWhite, fontWeight = FontWeight.Bold)); Text("com.zaaam.nettra", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, fontSize = 9.sp, color = NettraColors.GhostAsh)); Spacer(Modifier.height(12.dp)) }
                            item { MenuRow("Tab baru") { vm.newTab(); vm.toggleMenu(false) }; MenuRow("Private Tab") { vm.newTab(private = true); vm.toggleMenu(false) }; MenuRow("Laporan Forensik") { vm.toggleMenu(false); vm.togglePrivacy(true) }; MenuRow("Bakar Sesi", ember = true) { vm.toggleMenu(false); vm.requestFire() } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BunkerSearchPill(input: String, onInputChange: (String) -> Unit, onSubmit: () -> Unit, activeTab: TabState, tabCount: Int, onTabsClick: () -> Unit) {
    // 56dp pill, Bunker, lime focus ring
    OutlinedTextField(
        value = input,
        onValueChange = onInputChange,
        placeholder = { Text("Cari atau masukkan alamat", style = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(fontFamily = NettraFontInstrument, color = NettraColors.GhostAsh)) },
        leadingIcon = { Text(if (activeTab.secure) "◉" else "○", color = if (activeTab.secure) NettraColors.SignalLime else NettraColors.GhostAsh, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp)) },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(NettraColors.VoidBlack).border(1.dp, NettraColors.Border, RoundedCornerShape(12.dp)).clickable { onTabsClick() }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(String.format("%02d", tabCount), style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.GhostWhite, fontSize = 10.sp))
                }
                Text("  ⌕", color = NettraColors.GhostAsh, fontSize = 14.sp, modifier = Modifier.padding(end = 8.dp))
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = NettraColors.Bunker, unfocusedContainerColor = NettraColors.Bunker,
            focusedBorderColor = NettraColors.SignalLime, unfocusedBorderColor = NettraColors.Border,
            focusedTextColor = NettraColors.GhostWhite, unfocusedTextColor = NettraColors.GhostWhite,
            cursorColor = NettraColors.SignalLime
        ),
        textStyle = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(fontFamily = NettraFontMono, color = NettraColors.GhostWhite, fontWeight = FontWeight.Bold)
    )
}

@Composable
private fun BunkerTabRow(tabs: List<TabState>, activeId: Long, onSwitch: (Long) -> Unit, onClose: (Long) -> Unit, onNewTab: () -> Unit) {
    LazyRow(modifier = Modifier.fillMaxWidth().background(NettraColors.VoidBlack).padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(tabs, key = { it.id }) { t ->
            val isActive = t.id == activeId
            Row(
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (isActive) NettraColors.SignalLime else NettraColors.BunkerRaised).border(1.dp, if (isActive) NettraColors.SignalLime else NettraColors.Border, RoundedCornerShape(12.dp)).clickable { onSwitch(t.id) }.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (isActive) NettraColors.VoidBlack else NettraColors.GhostAsh))
                Spacer(Modifier.width(6.dp))
                Text(t.title.take(12), style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontSpace, color = if (isActive) NettraColors.VoidBlack else NettraColors.GhostWhite, fontSize = 11.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("  ✕", modifier = Modifier.clickable { onClose(t.id) }.padding(start = 4.dp), color = if (isActive) NettraColors.VoidBlack else NettraColors.GhostAsh, fontSize = 10.sp)
            }
        }
        item(key = "new_tab") {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).border(1.dp, NettraColors.Border, CircleShape).background(Color.Transparent).clickable { onNewTab() }, contentAlignment = Alignment.Center) { Text("+", color = NettraColors.GhostAsh, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun BunkerNewTab(blockedTotal: Int, version: String, onChip: (String) -> Unit, onDemo: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Sinyal aman.", style = androidx.compose.material3.MaterialTheme.typography.displayLarge.copy(fontFamily = NettraFontSpace, color = NettraColors.GhostWhite))
            Text("Tidak ada yang mengikuti.", style = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(fontFamily = NettraFontInstrument, color = NettraColors.GhostAsh))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("berita hari ini", "resep nasi goreng", "jadwal sholat").forEach { q ->
                    Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(NettraColors.BunkerRaised).border(1.dp, NettraColors.Border, RoundedCornerShape(12.dp)).clickable { onChip(q) }.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(q, style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontInstrument, color = NettraColors.GhostAsh, fontSize = 11.sp))
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = NettraColors.Bunker), shape = RoundedCornerShape(24.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(String.format("%02d", blockedTotal), style = androidx.compose.material3.MaterialTheme.typography.displayLarge.copy(fontFamily = NettraFontMono, color = NettraColors.SignalLime, fontSize = 36.sp))
                        Text("PELACAK DIBLOKIR", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.GhostAsh, fontSize = 9.sp))
                        Box(modifier = Modifier.padding(top = 8.dp).height(3.dp).fillMaxWidth().clip(RoundedCornerShape(99.dp)).background(NettraColors.SignalLime.copy(alpha = 0.3f))) { Box(modifier = Modifier.fillMaxWidth(0.7f).height(3.dp).background(NettraColors.SignalLime)) }
                    }
                }
                Card(modifier = Modifier.weight(0.9f), colors = CardDefaults.cardColors(containerColor = NettraColors.BunkerRaised), shape = RoundedCornerShape(24.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("DOSSIER", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.GhostAsh, fontSize = 9.sp))
                        Text(version, style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(fontFamily = NettraFontMono, color = NettraColors.GhostWhite, fontSize = 16.sp))
                        Text("CC BY-NC-SA 4.0", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.GhostAsh, fontSize = 9.sp))
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("BUKA NEWS", modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(NettraColors.GhostWhite).clickable { onDemo("https://contoh-berita.id/artikel/privasi") }.padding(horizontal = 14.dp, vertical = 10.dp), style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.VoidBlack))
                Text("SHOP 09", modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(NettraColors.SignalLime).clickable { onDemo("https://toko-contoh.com/promo") }.padding(horizontal = 14.dp, vertical = 10.dp), style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.VoidBlack))
            }
        }
    }
}

@Composable
private fun BunkerResults(query: String, onOpen: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item { Text("DUCKDUCKGO • https://duckduckgo.com/?q=$query", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.GhostAsh, fontSize = 9.sp)); Text("Hasil untuk “$query”", style = androidx.compose.material3.MaterialTheme.typography.displayMedium.copy(fontFamily = NettraFontSpace, color = NettraColors.GhostWhite), modifier = Modifier.padding(vertical = 12.dp)) }
        item {
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onOpen("https://contoh-berita.id") }, colors = CardDefaults.cardColors(containerColor = NettraColors.Bunker), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(14.dp)) { Text("contoh-berita.id — BERSIH", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.SignalLime, fontSize = 9.sp)); Text("Berita hari ini — tanpa tracker", style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(fontFamily = NettraFontSpace, color = NettraColors.GhostWhite)) }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth().clickable { onOpen("https://toko-contoh.com/promo") }, colors = CardDefaults.cardColors(containerColor = NettraColors.Bunker), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(14.dp)) { Text("toko-contoh.com — 09 DIBLOKIR", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.SignalLime, fontSize = 9.sp)); Text("Toko — sembilan tracker dimusnahkan", style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(fontFamily = NettraFontSpace, color = NettraColors.GhostWhite)) }
            }
        }
    }
}

@Composable
private fun BunkerSite(tab: TabState, onTrackerClick: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(modifier = Modifier.fillMaxWidth().background(NettraColors.Bunker).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(tab.title, style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(fontFamily = NettraFontSpace, color = NettraColors.GhostWhite), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(String.format("%02d", tab.blocked), modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(NettraColors.SignalLime).clickable { onTrackerClick() }.padding(horizontal = 10.dp, vertical = 6.dp), style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.VoidBlack))
            }
        }
        item {
            if (tab.blocked > 0) Box(modifier = Modifier.fillMaxWidth().background(NettraColors.SignalLime.copy(alpha = 0.12f)).padding(10.dp)) { Text("${tab.blocked} ███ DIMUSNAHKAN — shouldInterceptRequest", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.SignalLime, fontSize = 10.sp)) }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(if (tab.url.contains("shop")) "Sembilan pelacak mengintai" else "Sinyal terenkripsi", style = androidx.compose.material3.MaterialTheme.typography.displayMedium.copy(fontFamily = NettraFontSpace, color = NettraColors.GhostWhite))
                Text("14:02:11 • HTTPS aktif • ${tab.blocked} dimusnahkan", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.GhostAsh, fontSize = 9.sp), modifier = Modifier.padding(vertical = 6.dp))
                Text("Nettra memusnahkan di shouldInterceptRequest — request tidak pernah keluar.", style = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(fontFamily = NettraFontInstrument, color = NettraColors.GhostWhite), lineHeight = 20.sp)
            }
        }
    }
}

@Composable
private fun BunkerHttpWarning(url: String, onUpgrade: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = NettraColors.Bunker), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(14.dp)) { Text("HTTP TIDAK AMAN", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.AlertEmber)); Text("Koneksi tidak aman — upgrade ke HTTPS", style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(fontFamily = NettraFontSpace, color = NettraColors.GhostWhite)); Text(url, style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.GhostAsh, fontSize = 10.sp), maxLines = 2, overflow = TextOverflow.Ellipsis) }
            }
        }
        item { Text("◉ UPGRADE KE HTTPS", modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(NettraColors.SignalLime).clickable { onUpgrade() }.padding(horizontal = 16.dp, vertical = 12.dp), style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.VoidBlack)) }
    }
}

@Composable
private fun BunkerBottomBar(onBack: () -> Unit, onForward: () -> Unit, onFire: () -> Unit, onTabs: () -> Unit, onMenu: () -> Unit, tabCount: Int) {
    Box(modifier = Modifier.fillMaxWidth().background(NettraColors.Bunker).padding(horizontal = 10.dp, vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(NettraColors.VoidBlack).border(1.dp, NettraColors.Border, RoundedCornerShape(28.dp)).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row { Text("←", modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).clickable { onBack() }.padding(12.dp), color = NettraColors.GhostAsh, fontWeight = FontWeight.Bold); Text("→", modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).clickable { onForward() }.padding(12.dp), color = NettraColors.GhostAsh) }
            // Fire Vault — 64dp, Vault Black + Lime ring, floating
            Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(NettraColors.VoidBlack).border(2.dp, NettraColors.SignalLime, CircleShape).clickable { onFire() }, contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("◉", color = NettraColors.SignalLime, fontSize = 14.sp); Text("FIRE", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.GhostWhite, fontSize = 8.sp, letterSpacing = 1.sp)) }
            }
            Row { Text(String.format("%02d", tabCount), modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(NettraColors.BunkerRaised).border(1.dp, NettraColors.Border, RoundedCornerShape(12.dp)).clickable { onTabs() }.padding(horizontal = 10.dp, vertical = 8.dp), style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.GhostWhite)); Text("☰", modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).clickable { onMenu() }.padding(12.dp), color = NettraColors.GhostAsh) }
        }
    }
}

@Composable
private fun BunkerPrivacySheet(tab: TabState, version: String, onDismiss: () -> Unit) {
    LazyColumn(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
        item { Text("LAPORAN FORENSIK", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.GhostAsh, fontSize = 9.sp)); Text("Forensik", style = androidx.compose.material3.MaterialTheme.typography.displayLarge.copy(fontFamily = NettraFontSpace, color = NettraColors.GhostWhite)); Spacer(Modifier.height(12.dp)); Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(NettraColors.VoidBlack).padding(14.dp)) { Text(String.format("%02d", tab.blocked), style = androidx.compose.material3.MaterialTheme.typography.displayLarge.copy(fontFamily = NettraFontMono, color = NettraColors.SignalLime, fontSize = 48.sp), modifier = Modifier.padding(end = 12.dp)); Column { Text("PELACAK DIMUSNAHKAN", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.GhostWhite, fontSize = 9.sp)); Text(if (tab.secure) "HTTPS AKTIF • $version" else "HTTP", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.GhostAsh, fontSize = 9.sp)) } }; Spacer(Modifier.height(10.dp)); Text("Situs: ${tab.url.ifEmpty { "—" }}", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.GhostWhite, fontSize = 10.sp)); Text("Angka dari shouldInterceptRequest, bukan statis.", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.GhostAsh, fontSize = 9.sp), modifier = Modifier.padding(top = 4.dp)); TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 8.dp)) { Text("TUTUP", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.GhostWhite)) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BunkerTabSwitcher(tabs: List<TabState>, activeId: Long, onSwitch: (Long) -> Unit, onClose: (Long) -> Unit, onNewTab: () -> Unit, onPrivate: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = NettraColors.Bunker, contentColor = NettraColors.GhostWhite, shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)) {
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            item { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("DOSSIER — ${String.format("%02d", tabs.size)}", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.GhostWhite)); Text("TUTUP", modifier = Modifier.clickable { onDismiss() }.padding(8.dp), style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.GhostAsh)) }; Spacer(Modifier.height(8.dp)) }
            items(tabs, key = { it.id }) { t ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onSwitch(t.id) }, colors = CardDefaults.cardColors(containerColor = if (t.id == activeId) NettraColors.VoidBlack else NettraColors.BunkerRaised), shape = RoundedCornerShape(14.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) { Text("${if (t.isPrivate) "◉ " else ""}${t.title}", style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontSpace, color = if (t.id == activeId) NettraColors.GhostWhite else NettraColors.GhostAsh), maxLines = 1, overflow = TextOverflow.Ellipsis); Text(String.format("%02d • %s %s", t.blocked, t.grade, if (t.secure) "HTTPS" else "HTTP"), style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, fontSize = 9.sp, color = NettraColors.GhostAsh)) }
                        Text("✕", modifier = Modifier.clickable { onClose(t.id) }.padding(8.dp), style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.GhostAsh))
                    }
                }
            }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) { Text("◉ TAB BARU", modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(NettraColors.SignalLime).clickable { onNewTab() }.padding(12.dp), style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.VoidBlack)); Text("◉ PRIVATE", modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(NettraColors.VoidBlack).border(1.dp, NettraColors.Border, RoundedCornerShape(14.dp)).clickable { onPrivate() }.padding(12.dp), style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontMono, color = NettraColors.GhostAsh)) } }
        }
    }
}

@Composable
private fun MenuRow(label: String, ember: Boolean = false, onClick: () -> Unit) {
    Text(label, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (ember) NettraColors.AlertEmber.copy(alpha = 0.12f) else Color.Transparent).border(1.dp, if (ember) NettraColors.AlertEmber else Color.Transparent, RoundedCornerShape(12.dp)).clickable { onClick() }.padding(12.dp), style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = NettraFontSpace, color = if (ember) NettraColors.AlertEmber else NettraColors.GhostWhite))
}
