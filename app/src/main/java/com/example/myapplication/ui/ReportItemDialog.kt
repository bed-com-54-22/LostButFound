package com.example.myapplication.ui

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myapplication.data.model.ItemType
import com.example.myapplication.data.model.ReportedItem
import com.example.myapplication.data.repository.ItemRepository
import java.io.ByteArrayOutputStream

private val Black = Color.Black
private val FieldBorder = Color(0xFFC5CEDA)
private val HintGray = Color(0xFF8A94A0)
private val LostRed = Color(0xFFB3261E)
private val FoundGreen = Color(0xFF1B7F3B)

fun autoCategorizeItem(title: String, location: String = ""): String {
    val text = "$title $location".lowercase()
    return when {
        text.containsAny("phone", "samsung", "iphone", "laptop", "macbook", "ipad", "tablet", "charger", "headphones", "earbuds", "airpods", "watch", "smartwatch", "camera", "powerbank", "cable", "usb", "mouse", "keyboard", "calculator", "speaker", "electronic") -> "Electronics"
        text.containsAny("book", "textbook", "notebook", "novel", "binder", "folder", "paper", "journal", "dictionary", "syllabus") -> "Books"
        text.containsAny("shirt", "jacket", "hoodie", "sweater", "pants", "jeans", "coat", "hat", "cap", "shoes", "shoe", "sneakers", "scarf", "gloves", "belt", "dress", "skirt", "umbrella", "clothing", "clothes") -> "Clothing"
        text.containsAny("id", "card", "license", "passport", "wallet", "purse", "credit", "debit", "bank", "student id", "badge") -> "IDs & Cards"
        text.containsAny("key", "keys", "keychain", "car key", "fob", "house key", "room key") -> "Keys"
        text.containsAny("bag", "backpack", "pouch", "tote", "handbag", "suitcase", "duffel", "satchel", "case") -> "Bags"
        else -> "Other"
    }
}

private fun String.containsAny(vararg keywords: String): Boolean {
    return keywords.any { this.contains(it, ignoreCase = true) }
}

fun base64ToBitmap(base64Str: String): Bitmap? {
    return try {
        if (base64Str.isBlank()) return null
        val decodedBytes = Base64.decode(base64Str, Base64.NO_WRAP)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (_: Exception) {
        null
    }
}

@Composable
fun ReportItemDialog(
    initialType: ItemType = ItemType.LOST,
    currentUserEmail: String = "",
    itemRepository: ItemRepository,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var selectedType by remember { mutableStateOf(initialType) }
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("Today") }
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf(currentUserEmail) }
    var imageBase64 by remember { mutableStateOf("") }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val context = LocalContext.current

    fun processBitmap(bitmap: Bitmap) {
        val scaled = Bitmap.createScaledBitmap(
            bitmap,
            500,
            (bitmap.height * (500.0 / bitmap.width)).toInt().coerceAtLeast(1),
            true
        )
        val outputStream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val bytes = outputStream.toByteArray()
        imageBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        selectedBitmap = scaled
    }

    // Camera Capture Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            processBitmap(bitmap)
        }
    }

    // Gallery Image Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    processBitmap(bitmap)
                }
            } catch (_: Exception) {
                Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Black,
        unfocusedBorderColor = FieldBorder,
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        focusedPlaceholderColor = HintGray,
        unfocusedPlaceholderColor = HintGray,
        cursorColor = Black
    )

    fun submitReport() {
        if (name.isBlank() || location.isBlank() || contactPhone.isBlank()) {
            Toast.makeText(context, "Please fill in item name, location, and contact phone", Toast.LENGTH_SHORT).show()
            return
        }

        isSubmitting = true
        val computedCategory = autoCategorizeItem(name, location)

        val newItem = ReportedItem(
            name = name.trim(),
            category = computedCategory,
            location = location.trim(),
            date = date.trim().ifEmpty { "Today" },
            type = selectedType,
            contactName = contactName.trim().ifEmpty { "Contact Person" },
            contactPhone = contactPhone.trim(),
            contactEmail = contactEmail.trim(),
            reporterEmail = currentUserEmail,
            imageUrl = imageBase64,
            timestamp = System.currentTimeMillis()
        )

        itemRepository.addItem(
            item = newItem,
            onSuccess = {
                isSubmitting = false
                Toast.makeText(context, "Item reported successfully!", Toast.LENGTH_SHORT).show()
                onSuccess()
            },
            onError = { err ->
                isSubmitting = false
                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (selectedType == ItemType.LOST) "Report Lost Item" else "Report Found Item",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Toggle Lost / Found
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF2F5F9))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedType == ItemType.LOST) LostRed else Color.Transparent)
                            .clickable { selectedType = ItemType.LOST }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Lost Item",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (selectedType == ItemType.LOST) Color.White else Black
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedType == ItemType.FOUND) FoundGreen else Color.Transparent)
                            .clickable { selectedType = ItemType.FOUND }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Found Item",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (selectedType == ItemType.FOUND) Color.White else Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Photo Picker Section (Gallery only for Lost, Camera & Gallery for Found)
                Text("Item Photo", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Black)
                Spacer(modifier = Modifier.height(6.dp))

                if (selectedBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF2F5F9))
                    ) {
                        Image(
                            bitmap = selectedBitmap!!.asImageBitmap(),
                            contentDescription = "Selected Item Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Row(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (selectedType == ItemType.FOUND) {
                                TextButton(onClick = { cameraLauncher.launch(null) }) {
                                    Text("Take Photo", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            TextButton(onClick = { galleryLauncher.launch("image/*") }) {
                                Text("From Gallery", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    if (selectedType == ItemType.FOUND) {
                        // FOUND item -> Both Camera and Gallery
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { cameraLauncher.launch(null) },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Black, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Camera", color = Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Black, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Gallery", color = Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // LOST item -> Gallery option ONLY
                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Select Photo from Gallery", color = Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Item Title / Description", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Black)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("e.g. Black Samsung Phone", fontSize = 13.sp, color = HintGray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Location Lost/Found", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Black)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    placeholder = { Text("e.g. Library 2nd Floor", fontSize = 13.sp, color = HintGray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Date / Time", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Black)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    placeholder = { Text("e.g. Today 2:30 PM", fontSize = 13.sp, color = HintGray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text("Contact Details", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Black)
                Spacer(modifier = Modifier.height(6.dp))

                Text("Contact Name", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Black)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    placeholder = { Text("Your Name or Finder's Name", fontSize = 13.sp, color = HintGray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Phone Number", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Black)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = contactPhone,
                    onValueChange = { contactPhone = it },
                    placeholder = { Text("+265 ...", fontSize = 13.sp, color = HintGray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Email Address", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Black)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = contactEmail,
                    onValueChange = { contactEmail = it },
                    placeholder = { Text("contact@email.com", fontSize = 13.sp, color = HintGray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Black, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { submitReport() },
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Black)
                    ) {
                        Text(
                            text = if (isSubmitting) "Submitting..." else "Submit Report",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
