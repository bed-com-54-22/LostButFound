package com.example.myapplication

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.local.UserDao
import com.example.myapplication.data.local.UserEntity
import com.example.myapplication.data.model.ItemType
import com.example.myapplication.data.model.ReportedItem
import com.example.myapplication.data.repository.ItemRepository
import com.example.myapplication.ui.base64ToBitmap
import com.example.myapplication.ui.theme.AppThemeColors
import com.example.myapplication.ui.theme.getThemeColors
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

private val LostRed = Color(0xFFB3261E)
private val LostRedBg = Color(0xFFFDECEA)
private val FoundGreen = Color(0xFF1B7F3B)
private val FoundGreenBg = Color(0xFFE6F4EA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userEmail: String,
    userDao: UserDao,
    itemRepository: ItemRepository,
    isDarkMode: Boolean = false,
    onBackClick: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val colors = getThemeColors(isDarkMode)

    var userProfile by remember { mutableStateOf<UserEntity?>(null) }
    var showPhotoOptionsDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    var editFullName by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var editLocation by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(userEmail) {
        if (userEmail.isNotBlank()) {
            val localUser = userDao.getUserByEmail(userEmail)
            if (localUser != null && localUser.fullName.isNotBlank()) {
                userProfile = localUser
            } else {
                userProfile = localUser
                try {
                    val db = FirebaseFirestore.getInstance()
                    db.collection("users").document(userEmail.lowercase()).get()
                        .addOnSuccessListener { doc ->
                            val fn = doc.getString("fullName") ?: ""
                            val ph = doc.getString("phone") ?: ""
                            val loc = doc.getString("location") ?: ""
                            if (localUser != null) {
                                val updated = localUser.copy(
                                    fullName = fn.ifBlank { localUser.fullName },
                                    phone = ph.ifBlank { localUser.phone },
                                    location = loc.ifBlank { localUser.location }
                                )
                                userProfile = updated
                                scope.launch(Dispatchers.IO) { userDao.insertUser(updated) }
                            } else if (fn.isNotBlank()) {
                                val newUser = UserEntity(email = userEmail.lowercase(), password = "", fullName = fn, phone = ph, location = loc)
                                userProfile = newUser
                                scope.launch(Dispatchers.IO) { userDao.insertUser(newUser) }
                            }
                        }
                } catch (_: Exception) {}
            }
        }
    }

    fun saveProfilePicture(bitmap: Bitmap) {
        try {
            val scaled = Bitmap.createScaledBitmap(
                bitmap,
                400,
                (bitmap.height * (400.0 / bitmap.width)).toInt().coerceAtLeast(1),
                true
            )
            val outputStream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val bytes = outputStream.toByteArray()
            val imageBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

            scope.launch(Dispatchers.IO) {
                userDao.updateProfilePicture(userEmail, imageBase64)
                val updated = userDao.getUserByEmail(userEmail)
                withContext(Dispatchers.Main) {
                    userProfile = updated
                    Toast.makeText(context, "Profile picture updated!", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (_: Exception) {
            Toast.makeText(context, "Failed to save profile picture", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveProfileDetails() {
        val updatedUser = (userProfile ?: UserEntity(userEmail, "", "", "")).copy(
            fullName = editFullName.trim(),
            phone = editPhone.trim(),
            location = editLocation.trim()
        )
        userProfile = updatedUser

        scope.launch(Dispatchers.IO) {
            userDao.insertUser(updatedUser)
            try {
                val db = FirebaseFirestore.getInstance()
                db.collection("users").document(userEmail.lowercase()).update(
                    mapOf(
                        "fullName" to editFullName.trim(),
                        "phone" to editPhone.trim(),
                        "location" to editLocation.trim()
                    )
                )
            } catch (_: Exception) {}
            withContext(Dispatchers.Main) {
                showEditProfileDialog = false
                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Camera Capture Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            saveProfilePicture(bitmap)
        }
    }

    // Gallery Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    saveProfilePicture(bitmap)
                }
            } catch (_: Exception) {
                Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val allItems by itemRepository.getItemsFlow().collectAsState(initial = emptyList())

    // My Reported Items / Activity History
    val myItems = remember(allItems, userEmail) {
        allItems.filter { item ->
            userEmail.isNotBlank() && (
                    item.reporterEmail.equals(userEmail, ignoreCase = true) ||
                    item.contactEmail.equals(userEmail, ignoreCase = true)
            )
        }
    }

    val totalReported = myItems.size
    val lostCount = myItems.count { it.type == ItemType.LOST }
    val foundCount = myItems.count { it.type == ItemType.FOUND }

    val formattedEmailName = if (userEmail.isNotBlank()) userEmail.substringBefore("@").replaceFirstChar { it.uppercase() } else "User Profile"
    val displayName = userProfile?.fullName?.ifBlank { null } ?: formattedEmailName

    val profileBitmap = remember(userProfile?.profilePictureUrl) {
        base64ToBitmap(userProfile?.profilePictureUrl ?: "")
    }

    if (showPhotoOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoOptionsDialog = false },
            title = { Text("Profile Photo Options", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
            text = { Text("Choose a photo source for your profile picture:", color = colors.textSecondary) },
            containerColor = colors.cardBackground,
            confirmButton = {
                Button(
                    onClick = {
                        showPhotoOptionsDialog = false
                        cameraLauncher.launch(null)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primaryButtonBg, contentColor = colors.primaryButtonText)
                ) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Camera")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showPhotoOptionsDialog = false
                        galleryLauncher.launch("image/*")
                    }
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gallery", color = colors.textPrimary)
                }
            }
        )
    }

    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profile Details", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
            containerColor = colors.cardBackground,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Full Name", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    OutlinedTextField(
                        value = editFullName,
                        onValueChange = { editFullName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Your full name") }
                    )

                    Text("Phone Number", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("+265 ...") }
                    )

                    Text("Location / Address", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    OutlinedTextField(
                        value = editLocation,
                        onValueChange = { editLocation = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("e.g. Main Campus, Block A") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { saveProfileDetails() },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primaryButtonBg, contentColor = colors.primaryButtonText)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = colors.textPrimary)
                }
            }
        )
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Profile & Activity", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Log out", tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Profile Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar Container Centered
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clickable { showPhotoOptionsDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape)
                                    .background(colors.textPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                if (profileBitmap != null) {
                                    Image(
                                        bitmap = profileBitmap.asImageBitmap(),
                                        contentDescription = "Profile Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = displayName.take(1).uppercase(),
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.surface
                                    )
                                }
                            }

                            // Plus Sign (+) Overlay Badge
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(colors.surface)
                                    .padding(2.dp)
                                    .align(Alignment.BottomEnd)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(colors.textPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add/Change Photo",
                                        tint = colors.surface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = displayName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = userEmail.ifEmpty { "No email set" }, fontSize = 13.sp, color = colors.textSecondary)
                        }

                        if (!userProfile?.phone.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = userProfile?.phone ?: "", fontSize = 13.sp, color = colors.textSecondary)
                            }
                        }

                        if (!userProfile?.location.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = userProfile?.location ?: "", fontSize = 13.sp, color = colors.textSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedButton(
                            onClick = {
                                editFullName = userProfile?.fullName ?: ""
                                editPhone = userProfile?.phone ?: ""
                                editLocation = userProfile?.location ?: ""
                                showEditProfileDialog = true
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Profile Details", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Usage Statistics Summary
            item {
                Text(text = "Application Usage Summary", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard("Total", "$totalReported", colors.textPrimary, colors, Modifier.weight(1f))
                    StatCard("Lost", "$lostCount", LostRed, colors, Modifier.weight(1f))
                    StatCard("Found", "$foundCount", FoundGreen, colors, Modifier.weight(1f))
                }
            }

            // User's Activity History Header
            item {
                Text(text = "My Reported Items (${myItems.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            }

            if (myItems.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No items reported by you yet", fontSize = 14.sp, color = colors.textSecondary)
                        }
                    }
                }
            } else {
                items(myItems, key = { it.id }) { item ->
                    MyItemCard(item = item, colors = colors)
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primaryButtonBg, contentColor = colors.primaryButtonText)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = colors.primaryButtonText)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log Out", color = colors.primaryButtonText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, color: Color, colors: AppThemeColors, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = title, fontSize = 12.sp, color = colors.textSecondary)
        }
    }
}

@Composable
private fun MyItemCard(item: ReportedItem, colors: AppThemeColors) {
    val isLost = item.type == ItemType.LOST

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.category.take(1).uppercase(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = item.location, fontSize = 12.sp, color = colors.textSecondary)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = item.date, fontSize = 11.sp, color = colors.textSecondary)
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isLost) LostRedBg else FoundGreenBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isLost) "Lost" else "Found",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLost) LostRed else FoundGreen
                )
            }
        }
    }
}
