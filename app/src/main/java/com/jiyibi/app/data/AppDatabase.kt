package com.jiyibi.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Transaction::class,
        Budget::class,
        SavingsGoal::class,
        RecurringTransaction::class,
        CustomCategory::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun recurringDao(): RecurringDao
    abstract fun customCategoryDao(): CustomCategoryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // transactions 表新增字段（老数据给空字符串默认值）
                db.execSQL("ALTER TABLE transactions ADD COLUMN photoPath TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE transactions ADD COLUMN transferFrom TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE transactions ADD COLUMN transferTo TEXT NOT NULL DEFAULT ''")
                // 新表
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS budgets (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "month TEXT NOT NULL, categoryKey TEXT NOT NULL, amount REAL NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS savings_goals (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, targetAmount REAL NOT NULL, " +
                        "savedAmount REAL NOT NULL, deadline INTEGER NOT NULL, createdAt INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS recurring (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "amount REAL NOT NULL, type TEXT NOT NULL, categoryKey TEXT NOT NULL, " +
                        "note TEXT NOT NULL, intervalDays INTEGER NOT NULL, " +
                        "nextDue INTEGER NOT NULL, active INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS custom_categories (" +
                        "`key` TEXT PRIMARY KEY NOT NULL, label TEXT NOT NULL, " +
                        "iconName TEXT NOT NULL, colorArgb INTEGER NOT NULL, type TEXT NOT NULL)"
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jiyibi.db"
                ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
            }
    }
}
