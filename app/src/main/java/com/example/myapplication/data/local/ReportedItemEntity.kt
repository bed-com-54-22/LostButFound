package com.example.myapplication.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reported_items")
data class ReportedItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val location: String,
    val date: String,
    val type: String, // "LOST" or "FOUND"
    val contactName: String,
    val contactPhone: String,
    val contactEmail: String,
    val reporterEmail: String,
    val imageUrl: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
