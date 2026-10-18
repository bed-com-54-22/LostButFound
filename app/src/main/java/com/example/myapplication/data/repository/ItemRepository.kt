package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.data.local.ReportedItemDao
import com.example.myapplication.data.local.ReportedItemEntity
import com.example.myapplication.data.model.ItemType
import com.example.myapplication.data.model.ReportedItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ItemRepository(private val itemDao: ReportedItemDao) {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    init {
        startRealtimeListener()
    }

    /**
     * Real-time listener: Attaches a Firestore snapshot listener to the "items" collection.
     * Whenever an item is reported or updated in Firebase (by any user on any device),
     * Firestore fires a callback in real time. We save the latest list into Room,
     * which automatically triggers Room's Flow to emit fresh data to the UI.
     */
    fun startRealtimeListener() {
        firestore.collection("items")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ItemRepository", "Realtime listener notice: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val entities = snapshot.documents.mapNotNull { doc ->
                        val name = doc.getString("name") ?: return@mapNotNull null
                        val category = doc.getString("category") ?: "Other"
                        val location = doc.getString("location") ?: ""
                        val date = doc.getString("date") ?: ""
                        val typeStr = doc.getString("type") ?: "LOST"
                        val contactName = doc.getString("contactName") ?: ""
                        val contactPhone = doc.getString("contactPhone") ?: ""
                        val contactEmail = doc.getString("contactEmail") ?: ""
                        val reporterEmail = doc.getString("reporterEmail") ?: ""
                        val imageUrl = doc.getString("imageUrl") ?: ""
                        val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                        ReportedItemEntity(
                            id = doc.id,
                            name = name,
                            category = category,
                            location = location,
                            date = date,
                            type = typeStr,
                            contactName = contactName,
                            contactPhone = contactPhone,
                            contactEmail = contactEmail,
                            reporterEmail = reporterEmail,
                            imageUrl = imageUrl,
                            timestamp = timestamp
                        )
                    }

                    CoroutineScope(Dispatchers.IO).launch {
                        itemDao.insertItems(entities)
                    }
                }
            }
    }

    fun getItemsFlow(): Flow<List<ReportedItem>> {
        return itemDao.getAllItemsFlow().map { list ->
            list.map { entity ->
                ReportedItem(
                    id = entity.id,
                    name = entity.name,
                    category = entity.category,
                    location = entity.location,
                    date = entity.date,
                    type = if (entity.type == "FOUND") ItemType.FOUND else ItemType.LOST,
                    contactName = entity.contactName,
                    contactPhone = entity.contactPhone,
                    contactEmail = entity.contactEmail,
                    reporterEmail = entity.reporterEmail,
                    imageUrl = entity.imageUrl,
                    timestamp = entity.timestamp
                )
            }
        }
    }

    fun addItem(
        item: ReportedItem,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val docRef = if (item.id.isBlank()) firestore.collection("items").document() else firestore.collection("items").document(item.id)
        val id = docRef.id

        val itemData = mapOf(
            "id" to id,
            "name" to item.name,
            "category" to item.category,
            "location" to item.location,
            "date" to item.date,
            "type" to item.type.name,
            "contactName" to item.contactName,
            "contactPhone" to item.contactPhone,
            "contactEmail" to item.contactEmail,
            "reporterEmail" to item.reporterEmail,
            "imageUrl" to item.imageUrl,
            "timestamp" to item.timestamp
        )

        // Save locally first for instant feedback
        val entity = ReportedItemEntity(
            id = id,
            name = item.name,
            category = item.category,
            location = item.location,
            date = item.date,
            type = item.type.name,
            contactName = item.contactName,
            contactPhone = item.contactPhone,
            contactEmail = item.contactEmail,
            reporterEmail = item.reporterEmail,
            imageUrl = item.imageUrl,
            timestamp = item.timestamp
        )

        CoroutineScope(Dispatchers.IO).launch {
            itemDao.insertItem(entity)
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        }

        // Push to Firebase Firestore in background
        docRef.set(itemData)
            .addOnFailureListener { e ->
                Log.e("ItemRepository", "Background set error: ${e.message}")
            }
    }

    fun deleteItem(
        itemId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (itemId.isBlank()) return

        // Delete from local Room DB immediately
        CoroutineScope(Dispatchers.IO).launch {
            itemDao.deleteItemById(itemId)
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        }

        // Delete from Firebase Firestore in background
        firestore.collection("items").document(itemId).delete()
            .addOnFailureListener { e ->
                Log.e("ItemRepository", "Background delete error: ${e.message}")
            }
    }
}
