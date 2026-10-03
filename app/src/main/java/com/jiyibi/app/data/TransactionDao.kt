package com.jiyibi.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE timestamp >= :from AND timestamp < :to ORDER BY timestamp DESC")
    fun observeRange(from: Long, to: Long): Flow<List<Transaction>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE (:type = '' OR type = :type)
          AND (:categoryKey = '' OR categoryKey = :categoryKey)
          AND timestamp >= :from AND timestamp < :to
          AND (:keyword = '' OR note LIKE '%' || :keyword || '%' OR CAST(amount AS TEXT) LIKE '%' || :keyword || '%')
        ORDER BY timestamp DESC
        """
    )
    fun search(
        type: String,
        categoryKey: String,
        from: Long,
        to: Long,
        keyword: String
    ): Flow<List<Transaction>>

    @Insert
    suspend fun insert(transaction: Transaction): Long

    @Insert
    suspend fun insertAll(transactions: List<Transaction>)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
