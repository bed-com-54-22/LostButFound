package com.example.myapplication

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.ItemType
import com.example.myapplication.data.model.ReportedItem
import com.example.myapplication.data.repository.ItemRepository
import com.example.myapplication.data.util.ItemMatcher
import com.example.myapplication.ui.ItemDetailDialog
import com.example.myapplication.ui.ReportItemDialog
import com.example.myapplication.ui.base64ToBitmap

/* ---------- Colors ---------- */
private val HomeBlack = Color.Black
private val HomeBackground = Color(0xFFF2F5F9)
private val HomeBorder = Color(0xFFC5CEDA)
private val HomeHint = Color(0xFF8A94A0)
private val HomeSubtext = Color(0xFF555555)
private val LostRed = Color(0xFFB3261E)
private val LostRedBg = Color(0xFFFDECEA)
private val FoundGreen = Color(0xFF1B7F3B)
private val FoundGreenBg = Color(0xFFE6F4EA)

private val categories = listOf(
    "All", "Electronics", "Books", "Clothing", "IDs & Cards", "Keys", "Bags", "Other"
)

@Composable
private fun HomeText(
    text: String,
    fontSize: TextUnit,
    color: Color,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Bold
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontFamily = FontFamily.SansSerif,
        fontWeight = weight
    )
}

@Composable
fun HomeScreen(
    userName: String = "User",
    userEmail: String = "",
    unseenNotificationsCount: Int = 0,
    targetItemIdToOpen: String? = null,
    onClearTargetItemId: () -> Unit = {},
    itemRepository: ItemRepository,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedTab by remember { mutableStateOf("All") } // All / Lost / Found
    var selectedNav by remember { mutableStateOf(0) }

    // Dialog States
    var showReportDialog by remember { mutableStateOf(false) }
    var reportDialogInitialType by remember { mutableStateOf(ItemType.LOST) }
    var selectedItemForDetail by remember { mutableStateOf<ReportedItem?>(null) }

    // Real-time items flow from ItemRepository (backed by Firebase Firestore + Room)
    val realTimeItems by itemRepository.getItemsFlow().collectAsState(initial = emptyList())

    // Auto-open item when navigated from a notification
    LaunchedEffect(targetItemIdToOpen, realTimeItems) {
        if (!targetItemIdToOpen.isNullOrBlank()) {
            val matched = realTimeItems.find { it.id == targetItemIdToOpen }
            if (matched != null) {
                selectedItemForDetail = matched
            }
        }
    }

    val filteredItems = remember(realTimeItems, query, selectedCategory, selectedTab) {
        realTimeItems.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.name.contains(query.trim(), ignoreCase = true) ||
                    item.location.contains(query.trim(), ignoreCase = true)
            val matchesCategory = selectedCategory == "All" || item.category == selectedCategory
            val matchesTab = selectedTab == "All" ||
                    (selectedTab == "Lost" && item.type == ItemType.LOST) ||
                    (selectedTab == "Found" && item.type == ItemType.FOUND)
            matchesQuery && matchesCategory && matchesTab
        }
    }

    if (showReportDialog) {
        ReportItemDialog(
            initialType = reportDialogInitialType,
            currentUserEmail = userEmail,
            itemRepository = itemRepository,
            onDismiss = { showReportDialog = false },
            onSuccess = { showReportDialog = false }
        )
    }

    if (selectedItemForDetail != null) {
        ItemDetailDialog(
            item = selectedItemForDetail!!,
            allItems = realTimeItems,
            currentUserEmail = userEmail,
            onSelectMatch = { selectedItemForDetail = it },
            onRemoveItem = { itemToRemove ->
                itemRepository.deleteItem(
                    itemId = itemToRemove.id,
                    onSuccess = {
                        Toast.makeText(context, "Item removed / marked as collected!", Toast.LENGTH_SHORT).show()
                        selectedItemForDetail = null
                        onClearTargetItemId()
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onDismiss = {
                selectedItemForDetail = null
                onClearTargetItemId()
            }
        )
    }

    Scaffold(
        containerColor = HomeBackground,
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                val navItems = listOf(
                    "Home" to Icons.Default.Home,
                    "Report" to Icons.Default.Add,
                    "Profile" to Icons.Default.Person,
                    "About" to Icons.Default.Info
                )
                navItems.forEachIndexed { index, (label, icon) ->
                    NavigationBarItem(
                        selected = selectedNav == index,
                        onClick = {
                            selectedNav = index
                            when (label) {
                                "Report" -> {
                                    reportDialogInitialType = ItemType.LOST
                                    showReportDialog = true
                                }
                                "Profile" -> onNavigateToProfile()
                                "About" -> onNavigateToAbout()
                            }
                        },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { HomeText(label, 11.sp, HomeBlack) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            unselectedIconColor = HomeHint,
                            indicatorColor = HomeBlack
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ---------- Header ----------
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        HomeText("Hello, $userName", 24.sp, Color.Black)
                        HomeText("Lost something? Found something?", 14.sp, HomeSubtext)
                    }
                    Box {
                        IconButton(onClick = onNavigateToNotifications) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = HomeBlack
                            )
                        }
                        if (unseenNotificationsCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .clip(CircleShape)
                                    .background(LostRed)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unseenNotificationsCount > 99) "99+" else "$unseenNotificationsCount",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    TextButton(onClick = onLogout) {
                        HomeText("Log out", 13.sp, HomeBlack)
                    }
                }
            }

            // ---------- Search input ----------
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { HomeText("Search items or locations", 14.sp, HomeHint) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = HomeBlack)
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HomeBlack,
                        unfocusedBorderColor = HomeBorder,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        cursorColor = HomeBlack
                    )
                )
            }

            // ---------- Report actions ----------
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionCard(
                        title = "Report Lost",
                        subtitle = "I lost an item",
                        icon = Icons.Default.Search,
                        containerColor = HomeBlack,
                        contentColor = Color.White,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            reportDialogInitialType = ItemType.LOST
                            showReportDialog = true
                        }
                    )
                    ActionCard(
                        title = "Report Found",
                        subtitle = "I found an item",
                        icon = Icons.Default.Add,
                        containerColor = Color.White,
                        contentColor = HomeBlack,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            reportDialogInitialType = ItemType.FOUND
                            showReportDialog = true
                        }
                    )
                }
            }

            // ---------- Categories ----------
            item {
                HomeText("Categories", 18.sp, Color.Black)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { category ->
                        Pill(
                            text = category,
                            selected = category == selectedCategory,
                            onClick = { selectedCategory = category }
                        )
                    }
                }
            }

            // ---------- Recent reports ----------
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HomeText("Recent Reports", 18.sp, Color.Black, Modifier.weight(1f))
                    HomeText("${filteredItems.size} items", 12.sp, HomeHint)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Lost", "Found").forEach { tab ->
                        Pill(
                            text = tab,
                            selected = tab == selectedTab,
                            onClick = { selectedTab = tab }
                        )
                    }
                }
            }

            if (filteredItems.isEmpty()) {
                item { EmptyState() }
            } else {
                items(filteredItems, key = { it.id }) { item ->
                    ItemCard(item = item, allItems = realTimeItems, onClick = { selectedItemForDetail = item })
                }
            }
        }
    }
}

/* ---------- Reusable components ---------- */

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(12.dp))
            HomeText(title, 16.sp, contentColor)
            HomeText(subtitle, 12.sp, contentColor.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun Pill(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) HomeBlack else Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        HomeText(text, 13.sp, if (selected) Color.White else HomeBlack)
    }
}

@Composable
private fun ItemCard(item: ReportedItem, allItems: List<ReportedItem> = emptyList(), onClick: () -> Unit) {
    val isLost = item.type == ItemType.LOST
    val bitmap = remember(item.imageUrl) { base64ToBitmap(item.imageUrl) }
    val matches = remember(item, allItems) { ItemMatcher.findMatches(item, allItems) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(HomeBackground),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Item Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    HomeText(
                        text = item.category.take(1).ifEmpty { "I" },
                        fontSize = 20.sp,
                        color = HomeBlack
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HomeText(item.name, 16.sp, Color.Black)
                    if (matches.isNotEmpty()) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFFF3CD))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            HomeText("⚡ Match", 10.sp, Color(0xFF856404))
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        tint = HomeHint,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    HomeText(item.location, 12.sp, HomeSubtext, weight = FontWeight.Normal)
                }
                Spacer(Modifier.height(2.dp))
                HomeText(
                    text = if (item.contactName.isNotBlank()) "Contact: ${item.contactName} (${item.date})" else item.date,
                    fontSize = 11.sp,
                    color = HomeHint,
                    weight = FontWeight.Normal
                )
            }

            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isLost) LostRedBg else FoundGreenBg)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                HomeText(
                    if (isLost) "Lost" else "Found",
                    12.sp,
                    if (isLost) LostRed else FoundGreen
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = HomeHint, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(8.dp))
        HomeText("No reported items found", 16.sp, HomeBlack)
        HomeText("Be the first to report a lost or found item!", 13.sp, HomeHint, weight = FontWeight.Normal)
    }
}
