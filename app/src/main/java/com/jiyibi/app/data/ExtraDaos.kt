package com.jiyibi.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE month = :month")
    fun observeByMonth(month: String): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE month = :month")
    suspend fun getByMonthOnce(month: String): List<Budget>

    @Query("SELECT * FROM budgets")
    suspend fun getAllOnce(): List<Budget>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: Budget)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(budgets: List<Budget>)

    @Query("DELETE FROM budgets WHERE month = :month AND categoryKey = :categoryKey")
    suspend fun deleteByMonthAndCategory(month: String, categoryKey: String)

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()
}

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SavingsGoal>>

    @Query("SELECT * FROM savings_goals ORDER BY createdAt DESC")
    suspend fun getAllOnce(): List<SavingsGoal>

    @Insert
    suspend fun insert(goal: SavingsGoal): Long

    @Insert
    suspend fun insertAll(goals: List<SavingsGoal>)

    @Update
    suspend fun update(goal: SavingsGoal)

    @Delete
    suspend fun delete(goal: SavingsGoal)

    @Query("DELETE FROM savings_goals")
    suspend fun deleteAll()
}

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring ORDER BY nextDue ASC")
    fun observeAll(): Flow<List<RecurringTransaction>>

    @Query("SELECT * FROM recurring ORDER BY nextDue ASC")
    suspend fun getAllOnce(): List<RecurringTransaction>

    @Query("SELECT * FROM recurring WHERE active = 1 AND nextDue <= :now")
    suspend fun dueList(now: Long): List<RecurringTransaction>

    @Insert
    suspend fun insert(item: RecurringTransaction): Long

    @Insert
    suspend fun insertAll(items: List<RecurringTransaction>)

    @Update
    suspend fun update(item: RecurringTransaction)

    @Delete
    suspend fun delete(item: RecurringTransaction)

    @Query("DELETE FROM recurring")
    suspend fun deleteAll()
}

@Dao
interface CustomCategoryDao {
    @Query("SELECT * FROM custom_categories")
    fun observeAll(): Flow<List<CustomCategory>>

    @Query("SELECT * FROM custom_categories")
    suspend fun getAllOnce(): List<CustomCategory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: CustomCategory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(categories: List<CustomCategory>)

    @Delete
    suspend fun delete(category: CustomCategory)

    @Query("DELETE FROM custom_categories")
    suspend fun deleteAll()
}
