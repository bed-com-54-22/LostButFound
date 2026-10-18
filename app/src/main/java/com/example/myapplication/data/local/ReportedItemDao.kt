package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportedItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ReportedItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ReportedItemEntity)

    @Query("SELECT * FROM reported_items ORDER BY timestamp DESC")
    fun getAllItemsFlow(): Flow<List<ReportedItemEntity>>

    @Query("DELETE FROM reported_items WHERE id = :id")
    suspend fun deleteItemById(id: String)

    @Query("DELETE FROM reported_items")
    suspend fun clearAll()
}
