package com.zaaam.nettra.browserui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.draw.scale
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Whatshot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(vm: BrowserViewModel = viewModel()) {
    val activeTab by remember { derivedStateOf { vm.activeTab } }
    val tabCount by remember { derivedStateOf { vm.tabs.size } }

    NettraTheme {
        Scaffold(
            containerColor = NettraColors.Bg,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NettraColors.Lime))
                            Spacer(Modifier.width(8.dp))
                            Text("NETTRA", style = MaterialTheme.typography.labelMedium.copy(color = NettraColors.Text, letterSpacing = 1.2.sp))
                            Spacer(Modifier.width(8.dp))
                            Text("• ${vm.trackerBlocker.version}", style = MaterialTheme.typography.labelSmall.copy(color = NettraColors.Muted, fontSize = 9.sp))
                        }
                    },
                    actions = {
                        // Shield + menu — thumb zone is bottom, so top is minimal
                        Icon(
                            Icons.Filled.Shield,
                            contentDescription = "Privacy",
                            tint = if (activeTab.secure) NettraColors.Lime else NettraColors.Muted,
                            modifier = Modifier.size(22.dp).clip(CircleShape).clickable { vm.togglePrivacy(true) }.padding(2.dp)
                        )
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).clickable { vm.toggleMenu(true) }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = NettraColors.Muted)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = NettraColors.Bg, titleContentColor = NettraColors.Text),
                    windowInsets = WindowInsets.statusBars
                )
            },
            bottomBar = {
                // 2026 bottom nav — 4 items + Fire, thumb zone, Fire as lime-filled FAB (not ring)
                Box(
                    modifier = Modifier
                        .background(NettraColors.Surface)
                        .border(1.dp, NettraColors.Border, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        NavigationBarItem(
                            selected = false,
                            onClick = {},
                            icon = {
                                Box(
                                    modifier = Modifier.size(32.dp).clip(CircleShape).background(NettraColors.Surface2).border(1.dp, NettraColors.Border, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = NettraColors.Text, modifier = Modifier.size(16.dp)) }
                            },
                            label = { Text("Kembali", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = NettraColors.Muted, fontWeight = FontWeight.Bold)) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
                        )
                        NavigationBarItem(
                            selected = false,
                            onClick = { vm.toggleTabSwitcher(true) },
                            icon = {
                                // Tabs pill — count + icon together, not badge on top
                                Row(
                                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(NettraColors.Surface2).border(1.dp, NettraColors.Border, RoundedCornerShape(16.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Stacked tabs icon — two overlapping rounded rects
                                    Box(modifier = Modifier.size(14.dp)) {
                                        Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(NettraColors.Muted).align(Alignment.BottomStart).border(1.dp, NettraColors.Surface2, RoundedCornerShape(2.dp)))
                                        Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(NettraColors.Text).align(Alignment.TopEnd).border(1.dp, NettraColors.Surface2, RoundedCornerShape(2.dp)))
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Text(String.format("%02d", tabCount), style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, color = NettraColors.Text, fontWeight = FontWeight.Bold))
                                }
                            },
                            label = { Text("Tab", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = NettraColors.Muted)) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
                        )
                        NavigationBarItem(
                            selected = false,
                            onClick = vm::requestFire,
                            icon = {
                                val interaction = remember { MutableInteractionSource() }
                                val pressed by interaction.collectIsPressedAsState()
                                val scale by animateFloatAsState(if (pressed) 0.92f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                                Box(
                                    modifier = Modifier.size(52.dp).clip(CircleShape).background(NettraColors.Lime).border(1.dp, NettraColors.Bg, CircleShape).scale(scale),
                                    contentAlignment = Alignment.Center
                                ) { Icon(Icons.Filled.Whatshot, contentDescription = "Fire", tint = NettraColors.Bg, modifier = Modifier.size(24.dp)) }
                            },
                            label = { Text("Bakar", style = MaterialTheme.typography.labelSmall.copy(color = NettraColors.Lime, fontSize = 9.sp, fontWeight = FontWeight.Bold)) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
                        )
                        NavigationBarItem(
                            selected = false,
                            onClick = { vm.toggleMenu(true) },
                            icon = { Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = NettraColors.Muted, modifier = Modifier.size(20.dp)) },
                            label = { Text("Menu", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = NettraColors.Muted)) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NettraColors.Bg)
                    .padding(innerPadding)
            ) {
                // Search Pill — now BELOW top bar, but still top third? 2026 says bottom is better, but for browser, address bar is traditionally top — we keep it below TopAppBar but with large pill, thumb reachable via bottom sheet on tap
                // For true 2026 bottom address, tap pill opens bottom sheet — pill itself is at top but sheet is bottom
                OutlinedTextField(
                    value = vm.addressInput,
                    onValueChange = vm::onAddressChange,
                    placeholder = { Text("Cari atau masukkan alamat", style = MaterialTheme.typography.bodySmall.copy(color = NettraColors.Muted)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = NettraColors.Muted, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (activeTab.blocked > 0) {
                                Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(NettraColors.Lime).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text(String.format("%02d", activeTab.blocked), style = MaterialTheme.typography.labelSmall.copy(color = NettraColors.Bg, fontSize = 10.sp))
                                }
                                Spacer(Modifier.width(8.dp))
                            }
                            Icon(Icons.Filled.Shield, contentDescription = null, tint = if (activeTab.secure) NettraColors.Lime else NettraColors.Muted, modifier = Modifier.size(16.dp))
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { vm.navigate(vm.addressInput) }),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = NettraColors.Surface, unfocusedContainerColor = NettraColors.Surface,
                        focusedBorderColor = NettraColors.Lime, unfocusedBorderColor = NettraColors.Border,
                        focusedTextColor = NettraColors.Text, unfocusedTextColor = NettraColors.Text,
                        cursorColor = NettraColors.Lime
                    ),
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = NettraColors.Text, fontWeight = FontWeight.Medium)
                )
                // Tab chips — Android FilterChip style, scrollable
                LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(vm.tabs, key = { it.id }) { t ->
                        val isActive = t.id == vm.activeId
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (isActive) NettraColors.Lime else NettraColors.Surface2).border(1.dp, if (isActive) NettraColors.Lime else NettraColors.Border, RoundedCornerShape(12.dp)).clickable { vm.switchTab(t.id) }.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (isActive) NettraColors.Bg else NettraColors.Muted))
                            Spacer(Modifier.width(6.dp))
                            Text(t.title.take(14), style = MaterialTheme.typography.labelMedium.copy(color = if (isActive) NettraColors.Bg else NettraColors.Text, fontSize = 11.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("  ✕", modifier = Modifier.clickable { vm.closeTab(t.id) }.padding(start = 4.dp), color = if (isActive) NettraColors.Bg else NettraColors.Muted, fontSize = 10.sp)
                        }
                    }
                    item(key = "new_tab") {
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).border(1.dp, NettraColors.Border, CircleShape).clickable { vm.newTab() }, contentAlignment = Alignment.Center) { Text("+", color = NettraColors.Muted, fontWeight = FontWeight.Bold) }
                    }
                }
                // Content — AnimatedContent biar tidak kaku (spring)
                Box(modifier = Modifier.weight(1f).fillMaxWidth().background(NettraColors.Bg)) {
                    AnimatedContent(
                        targetState = activeTab,
                        transitionSpec = {
                            (slideInVertically(tween(220, delayMillis = 20)) + fadeIn(tween(220))) togetherWith (slideOutVertically(tween(180)) + fadeOut(tween(180)))
                        },
                        label = "tabContent"
                    ) { tab ->
                        when (tab.type) {
                            "newtab" -> AndroidNewTab(blockedTotal = vm.blockedTotal, version = vm.trackerBlocker.version, onChip = { q -> vm.onAddressChange(q); vm.navigate(q) }, onDemo = vm::navigate)
                            "results" -> AndroidResults(query = tab.query, onOpen = vm::navigate)
                            "site" -> AndroidSite(tab = tab, onTrackerClick = { vm.togglePrivacy(true) })
                            "http" -> AndroidHttpWarning(url = tab.url, onUpgrade = vm::upgradeToHttps)
                            else -> AndroidNewTab(vm.blockedTotal, vm.trackerBlocker.version, { q -> vm.onAddressChange(q); vm.navigate(q) }, vm::navigate)
                        }
                    }
                }
            }
            if (vm.showPrivacy) {
                ModalBottomSheet(
                    onDismissRequest = { vm.togglePrivacy(false) },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = NettraColors.Surface,
                    contentColor = NettraColors.Text,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    AndroidPrivacySheet(tab = activeTab, version = vm.trackerBlocker.version) { vm.togglePrivacy(false) }
                }
            }
            if (vm.showFireDialog) {
                AlertDialog(
                    onDismissRequest = vm::dismissFire,
                    containerColor = NettraColors.Surface,
                    titleContentColor = NettraColors.Text,
                    textContentColor = NettraColors.Muted,
                    title = { Text("Bakar Sesi?", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                    text = { Text("$tabCount tab → 1 • Cookie & cache dimusnahkan • Bookmark aman", style = MaterialTheme.typography.bodySmall) },
                    confirmButton = { TextButton(onClick = vm::doFire) { Text("BAKAR", color = NettraColors.Burn, fontWeight = FontWeight.Bold) } },
                    dismissButton = { TextButton(onClick = vm::dismissFire) { Text("BATAL", color = NettraColors.Muted) } }
                )
            }
            if (vm.showTabSwitcher) {
                ModalBottomSheet(onDismissRequest = { vm.toggleTabSwitcher(false) }, containerColor = NettraColors.Surface, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
                    AndroidTabSwitcher(tabs = vm.tabs, activeId = vm.activeId, onSwitch = { vm.switchTab(it); vm.toggleTabSwitcher(false) }, onClose = vm::closeTab, onNewTab = { vm.newTab(); vm.toggleTabSwitcher(false) }, onPrivate = { vm.newTab(private = true); vm.toggleTabSwitcher(false) })
                }
            }
            if (vm.showMenu) {
                ModalBottomSheet(onDismissRequest = { vm.toggleMenu(false) }, containerColor = NettraColors.Surface, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
                    LazyColumn(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
                                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(NettraColors.Lime), contentAlignment = Alignment.Center) { Text("N", style = MaterialTheme.typography.titleLarge.copy(color = NettraColors.Bg, fontWeight = FontWeight.Bold)) }
                                Spacer(Modifier.width(12.dp))
                                Column { Text("NETTRA", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)); Text("com.zaaam.nettra • v1.0.0", style = MaterialTheme.typography.labelSmall.copy(color = NettraColors.Muted, fontSize = 10.sp)) }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        item {
                            Text("BROWSING", style = MaterialTheme.typography.labelSmall.copy(color = NettraColors.Muted, fontSize = 10.sp, letterSpacing = 1.sp), modifier = Modifier.padding(vertical = 8.dp))
                            MenuRowAndroid(icon = Icons.Filled.Tab, title = "Tab baru", subtitle = "Buka tab kosong", onClick = { vm.newTab(); vm.toggleMenu(false) })
                            Spacer(Modifier.height(8.dp))
                            MenuRowAndroid(icon = Icons.Filled.Close, title = "Tab samaran", subtitle = "Private • tidak simpan history", onClick = { vm.newTab(private = true); vm.toggleMenu(false) })
                            Spacer(Modifier.height(12.dp))
                        }
                        item {
                            Text("PRIVASI", style = MaterialTheme.typography.labelSmall.copy(color = NettraColors.Muted, fontSize = 10.sp, letterSpacing = 1.sp), modifier = Modifier.padding(vertical = 8.dp))
                            MenuRowAndroid(icon = Icons.Filled.Shield, title = "Laporan Forensik", subtitle = "${vm.activeTab.blocked} tracker • ${if (vm.activeTab.secure) "HTTPS" else "HTTP"}", onClick = { vm.toggleMenu(false); vm.togglePrivacy(true) })
                            Spacer(Modifier.height(8.dp))
                            MenuRowAndroid(icon = Icons.Filled.Whatshot, title = "Bakar Sesi", subtitle = "Hapus cookie & cache • bookmark aman", ember = true, onClick = { vm.toggleMenu(false); vm.requestFire() })
                            Spacer(Modifier.height(12.dp))
                        }
                        item {
                            Text("TENTANG", style = MaterialTheme.typography.labelSmall.copy(color = NettraColors.Muted, fontSize = 10.sp, letterSpacing = 1.sp), modifier = Modifier.padding(vertical = 8.dp))
                            MenuRowAndroid(icon = Icons.Filled.Home, title = "Riwayat & Bookmark", subtitle = "Lihat yang tersimpan", onClick = { vm.toggleMenu(false) })
                            Spacer(Modifier.height(8.dp))
                            MenuRowAndroid(icon = Icons.Filled.Menu, title = "Pengaturan", subtitle = "DuckDuckGo default • HTTPS-First", onClick = { vm.toggleMenu(false) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgedIcon(count: Int) {
    Box(contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.Tab, contentDescription = "Tabs", tint = NettraColors.Muted, modifier = Modifier.size(22.dp))
        Box(modifier = Modifier.align(Alignment.TopEnd).clip(RoundedCornerShape(8.dp)).background(NettraColors.Lime).padding(horizontal = 4.dp, vertical = 1.dp)) {
            Text(String.format("%02d", count), style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = NettraColors.Bg))
        }
    }
}

@Composable
private fun AndroidNewTab(blockedTotal: Int, version: String, onChip: (String) -> Unit, onDemo: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Sinyal aman.", style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.ExtraBold))
            Text("Tidak ada yang mengikuti.", style = MaterialTheme.typography.bodySmall.copy(color = NettraColors.Muted))
            Spacer(Modifier.height(4.dp))
            Text("DUCKDUCKGO • HTTPS-FIRST • $version", style = MaterialTheme.typography.labelSmall.copy(color = NettraColors.Muted, fontSize = 9.sp))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("berita hari ini", "resep nasi goreng", "jadwal sholat").forEach { q ->
                    Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(NettraColors.Surface2).border(1.dp, NettraColors.Border, RoundedCornerShape(12.dp)).clickable { onChip(q) }.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(q, style = MaterialTheme.typography.labelMedium.copy(color = NettraColors.Muted, fontSize = 11.sp))
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = NettraColors.Surface), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(String.format("%02d", blockedTotal), style = MaterialTheme.typography.displayLarge.copy(color = NettraColors.Lime, fontSize = 32.sp))
                        Text("PELACAK DIBLOKIR", style = MaterialTheme.typography.labelSmall.copy(color = NettraColors.Muted, fontSize = 9.sp))
                    }
                }
                Card(modifier = Modifier.weight(0.9f), colors = CardDefaults.cardColors(containerColor = NettraColors.Surface2), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("DOSSIER", style = MaterialTheme.typography.labelSmall.copy(color = NettraColors.Muted, fontSize = 9.sp))
                        Text(version, style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp))
                        Text("CC BY-NC-SA 4.0", style = MaterialTheme.typography.labelSmall.copy(color = NettraColors.Muted, fontSize = 9.sp))
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(onClick = { onDemo("https://contoh-berita.id/artikel/privasi") }, colors = CardDefaults.cardColors(containerColor = NettraColors.Surface2), shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(14.dp)) { Icon(Icons.Filled.Home, contentDescription = null, tint = NettraColors.Muted, modifier = Modifier.size(24.dp)); Spacer(Modifier.height(8.dp)); Text("Berita — tanpa tracker", style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp)); Text("contoh-berita.id • BERSIH", style = MaterialTheme.typography.labelSmall.copy(color = NettraColors.Lime, fontSize = 9.sp)) }
                }
                Card(onClick = { onDemo("https://toko-contoh.com/promo") }, colors = CardDefaults.cardColors(containerColor = NettraColors.Surface2), shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(14.dp)) { Icon(Icons.Filled.Tab, contentDescription = null, tint = NettraColors.Muted, modifier = Modifier.size(24.dp)); Spacer(Modifier.height(8.dp)); Text("Toko — 09 tracker", style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp)); Text("toko-contoh.com • 09", style = MaterialTheme.typography.labelSmall.copy(color = NettraColors.Lime, fontSize = 9.sp)) }
                }
            }
        }
    }
}

@Composable
private fun AndroidResults(query: String, onOpen: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item { Text("DUCKDUCKGO • https://duckduckgo.com/?q=$query", style = MaterialTheme.typography.labelSmall.copy(color = NettraColors.Muted, fontSize = 9.sp)); Text("Hasil untuk “$query”", style = MaterialTheme.typography.displayLarge.copy(fontSize = 20.sp), modifier = Modifier.padding(vertical = 12.dp)) }
        item {
            Card(onClick = { onOpen("https://contoh-berita.id") }, colors = CardDefaults.cardColors(containerColor = NettraColors.Surface2), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) { Text("contoh-berita.id — BERSIH", style = MaterialTheme.typography.labelSmall.copy(color = NettraColors.Lime, fontSize = 9.sp)); Text("Berita hari ini", style = MaterialTheme.typography.titleMedium) }
            }
            Spacer(Modifier.height(8.dp))
            Card(onClick = { onOpen("https://toko-contoh.com/promo") }, colors = CardDefaults.cardColors(containerColor = NettraColors.Surface2), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) { Text("toko-contoh.com — 09 DIBLOKIR", style = MaterialTheme.typography.labelSmall.copy(color = NettraColors.Lime, fontSize = 9.sp)); Text("Toko — sembilan tracker", style = MaterialTheme.typography.titleMedium) }
            }
        }
    }
}

@Composable
private fun AndroidSite(tab: TabState, onTrackerClick: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(modifier = Modifier.fillMaxWidth().background(NettraColors.Surface).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(tab.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(NettraColors.Lime).clickable { onTrackerClick() }.padding(horizontal = 10.dp, vertical = 6.dp)) { Text(String.format("%02d", tab.blocked), style = MaterialTheme.typography.labelSmall.copy(color = NettraColors.Bg)) }
            }
        }
        item {
            if (tab.blocked > 0) Box(modifier = Modifier.fillMaxWidth().background(NettraColors.Lime.copy(alpha = 0.12f)).padding(10.dp)) { Text("${tab.blocked} ███ DIMUSNAHKAN", style = MaterialTheme.typography.labelSmall.copy(color = NettraColors.Lime, fontSize = 10.sp)) }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(if (tab.url.contains("shop")) "Sembilan pelacak mengintai" else "Sinyal terenkripsi", style = MaterialTheme.typography.displayMedium)
                Text("14:02 • HTTPS aktif • ${tab.blocked} dimusnahkan", style = MaterialTheme.typography.labelSmall.copy(color = NettraColors.Muted, fontSize = 9.sp), modifier = Modifier.padding(vertical = 6.dp))
                Text("Nettra memusnahkan di shouldInterceptRequest — request tidak pernah keluar.", style = MaterialTheme.typography.bodySmall, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
private fun AndroidHttpWarning(url: String, onUpgrade: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = NettraColors.Surface2), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(14.dp)) { Text("HTTP TIDAK AMAN", style = MaterialTheme.typography.labelSmall.copy(color = NettraColors.Burn)); Text("Koneksi tidak aman — upgrade ke HTTPS", style = MaterialTheme.typography.titleLarge); Text(url, style = MaterialTheme.typography.labelSmall.copy(color = NettraColors.Muted, fontSize = 10.sp), maxLines = 2, overflow = TextOverflow.Ellipsis) }
            }
        }
        item {
            Card(onClick = onUpgrade, colors = CardDefaults.cardColors(containerColor = NettraColors.Lime), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.padding(14.dp).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("UPGRADE KE HTTPS", style = MaterialTheme.typography.labelMedium.copy(color = NettraColors.Bg)) }
            }
        }
    }
}

@Composable
private fun AndroidPrivacySheet(tab: TabState, version: String, onDismiss: () -> Unit) {
    LazyColumn(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
        item { Text("LAPORAN FORENSIK", style = MaterialTheme.typography.labelSmall.copy(color = NettraColors.Muted, fontSize = 9.sp)); Text("Forensik", style = MaterialTheme.typography.displayLarge); Spacer(Modifier.height(12.dp)); Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(NettraColors.Bg).padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Text(String.format("%02d", tab.blocked), style = MaterialTheme.typography.displayLarge.copy(color = NettraColors.Lime, fontSize = 36.sp), modifier = Modifier.padding(end = 12.dp)); Column { Text("PELACAK DIMUSNAHKAN", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)); Text(if (tab.secure) "HTTPS AKTIF • $version" else "HTTP", style = MaterialTheme.typography.labelSmall.copy(color = NettraColors.Muted, fontSize = 9.sp)) } }; Spacer(Modifier.height(10.dp)); Text("Situs: ${tab.url.ifEmpty { "—" }}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)); Text("Angka dari shouldInterceptRequest, bukan statis.", style = MaterialTheme.typography.labelSmall.copy(color = NettraColors.Muted, fontSize = 9.sp), modifier = Modifier.padding(top = 4.dp)); TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 8.dp)) { Text("TUTUP") } }
    }
}

@Composable
private fun AndroidTabSwitcher(tabs: List<TabState>, activeId: Long, onSwitch: (Long) -> Unit, onClose: (Long) -> Unit, onNewTab: () -> Unit, onPrivate: () -> Unit) {
    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("DOSSIER — ${String.format("%02d", tabs.size)}", style = MaterialTheme.typography.labelMedium); Text("${tabs.size} tab", style = MaterialTheme.typography.labelSmall.copy(color = NettraColors.Muted)) }; Spacer(Modifier.height(8.dp)) }
        items(tabs, key = { it.id }) { t ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onSwitch(t.id) }, colors = CardDefaults.cardColors(containerColor = if (t.id == activeId) NettraColors.Surface2 else NettraColors.Bg), shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) { Text("${if (t.isPrivate) "◉ " else ""}${t.title}", style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(String.format("%02d • %s %s", t.blocked, t.grade, if (t.secure) "HTTPS" else "HTTP"), style = MaterialTheme.typography.labelSmall.copy(color = NettraColors.Muted, fontSize = 9.sp)) }
                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(NettraColors.Surface).border(1.dp, NettraColors.Border, CircleShape).clickable { onClose(t.id) }, contentAlignment = Alignment.Center) { Text("✕", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = NettraColors.Muted)) }
                }
            }
        }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) { Card(onClick = onNewTab, colors = CardDefaults.cardColors(containerColor = NettraColors.Lime), shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) { Box(modifier = Modifier.padding(12.dp).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("TAB BARU", style = MaterialTheme.typography.labelMedium.copy(color = NettraColors.Bg)) } }; Card(onClick = onPrivate, colors = CardDefaults.cardColors(containerColor = NettraColors.Bg), shape = RoundedCornerShape(12.dp), modifier = Modifier.border(1.dp, NettraColors.Border, RoundedCornerShape(12.dp))) { Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) { Text("PRIVATE", style = MaterialTheme.typography.labelMedium.copy(color = NettraColors.Muted)) } } } }
    }
}

@Composable
private fun MenuRowAndroid(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, ember: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (ember) NettraColors.Burn.copy(alpha = 0.08f) else NettraColors.Bg).border(1.dp, if (ember) NettraColors.Burn.copy(alpha = 0.3f) else NettraColors.Border, RoundedCornerShape(12.dp)).clickable { onClick() }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(if (ember) NettraColors.Burn.copy(alpha = 0.15f) else NettraColors.Surface2), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = if (ember) NettraColors.Burn else NettraColors.Muted, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (ember) NettraColors.Burn else NettraColors.Text))
            Text(subtitle, style = MaterialTheme.typography.labelSmall.copy(color = NettraColors.Muted, fontSize = 11.sp))
        }
        Text("›", style = MaterialTheme.typography.bodyLarge.copy(color = NettraColors.Muted, fontSize = 18.sp))
    }
}
@Composable
private fun MenuRow(label: String, ember: Boolean = false, onClick: () -> Unit) {
    MenuRowAndroid(icon = if (ember) Icons.Filled.Whatshot else Icons.Filled.Home, title = label, subtitle = if (ember) "Hapus sesi" else "", onClick = onClick)
}
