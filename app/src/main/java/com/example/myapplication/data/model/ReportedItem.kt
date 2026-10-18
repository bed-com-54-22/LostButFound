package com.example.myapplication.data.model

enum class ItemType { LOST, FOUND }

data class ReportedItem(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val location: String = "",
    val date: String = "",
    val type: ItemType = ItemType.LOST,
    val contactName: String = "",
    val contactPhone: String = "",
    val contactEmail: String = "",
    val reporterEmail: String = "",
    val imageUrl: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
