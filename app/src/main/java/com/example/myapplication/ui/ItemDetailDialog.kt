package com.example.myapplication.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.ui.window.Dialog
import com.example.myapplication.data.model.ItemType
import com.example.myapplication.data.model.ReportedItem
import com.example.myapplication.data.util.ItemMatcher

private val Black = Color.Black
private val LostRed = Color(0xFFB3261E)
private val LostRedBg = Color(0xFFFDECEA)
private val FoundGreen = Color(0xFF1B7F3B)
private val FoundGreenBg = Color(0xFFE6F4EA)

@Composable
fun ItemDetailDialog(
    item: ReportedItem,
    allItems: List<ReportedItem> = emptyList(),
    currentUserEmail: String = "",
    onSelectMatch: (ReportedItem) -> Unit = {},
    onRemoveItem: (ReportedItem) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isLost = item.type == ItemType.LOST
    val isReporter = currentUserEmail.isNotBlank() && (
            item.reporterEmail.equals(currentUserEmail, ignoreCase = true) ||
            item.contactEmail.equals(currentUserEmail, ignoreCase = true)
    )

    // Find similar items of opposite type
    val potentialMatches = remember(item, allItems) {
        ItemMatcher.findMatches(item, allItems)
    }

    fun dialPhone() {
        if (item.contactPhone.isNotBlank()) {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.contactPhone}"))
            context.startActivity(intent)
        }
    }

    fun sendEmail() {
        if (item.contactEmail.isNotBlank()) {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${item.contactEmail}"))
            context.startActivity(intent)
        }
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
            Column(modifier = Modifier.padding(20.dp)) {

                val bitmap = remember(item.imageUrl) { base64ToBitmap(item.imageUrl) }

                if (bitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF2F5F9))
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Item Photo",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF2F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.category.take(1).uppercase(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Black
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Black
                        )
                        Text(
                            text = item.category,
                            fontSize = 13.sp,
                            color = Color(0xFF555555)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isLost) LostRedBg else FoundGreenBg)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isLost) "LOST" else "FOUND",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLost) LostRed else FoundGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Location & Date
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = Color(0xFF8A94A0),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Location: ${item.location}",
                        fontSize = 14.sp,
                        color = Black
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Reported: ${item.date}",
                    fontSize = 13.sp,
                    color = Color(0xFF8A94A0),
                    modifier = Modifier.padding(start = 22.dp)
                )

                // Potential Matches Banner Section
                if (potentialMatches.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "⚡ Potential Match Found! (${potentialMatches.size})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB78103)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            potentialMatches.take(2).forEach { matchedItem ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .clickable { onSelectMatch(matchedItem) }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(matchedItem.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Black)
                                        Text("${matchedItem.type.name} at ${matchedItem.location}", fontSize = 11.sp, color = Color(0xFF555555))
                                    }
                                    Text("View >", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Black)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Contact Details Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Contact Details",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Black
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (item.contactName.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(item.contactName, fontSize = 13.sp, color = Black)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        if (item.contactPhone.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Call, contentDescription = null, tint = Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(item.contactPhone, fontSize = 13.sp, color = Black, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        if (item.contactEmail.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Email, contentDescription = null, tint = Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(item.contactEmail, fontSize = 13.sp, color = Black)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (item.contactPhone.isNotBlank()) {
                        Button(
                            onClick = { dialPhone() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Black)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Call", color = Color.White, fontSize = 13.sp)
                        }
                    }

                    if (item.contactEmail.isNotBlank()) {
                        OutlinedButton(
                            onClick = { sendEmail() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Email", color = Black, fontSize = 13.sp)
                        }
                    }
                }

                if (isReporter) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { onRemoveItem(item) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LostRed)
                    ) {
                        Text("Mark as Collected / Remove Report", color = LostRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Close", color = Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
