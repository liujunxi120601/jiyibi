package com.jiyibi.app.data

import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val dao: TransactionDao) {
    fun observeAll(): Flow<List<Transaction>> = dao.observeAll()
    fun observeRange(from: Long, to: Long): Flow<List<Transaction>> = dao.observeRange(from, to)
    suspend fun add(transaction: Transaction) = dao.insert(transaction)
    suspend fun remove(transaction: Transaction) = dao.delete(transaction)
}
