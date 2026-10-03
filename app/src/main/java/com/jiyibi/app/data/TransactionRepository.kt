package com.jiyibi.app.data

import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class TransactionRepository(private val db: AppDatabase) {
    private val dao = db.transactionDao()
    private val budgetDao = db.budgetDao()
    private val goalDao = db.savingsGoalDao()
    private val recurringDao = db.recurringDao()
    private val customCategoryDao = db.customCategoryDao()

    // ---- 记账 ----
    fun observeAll(): Flow<List<Transaction>> = dao.observeAll()
    fun observeRange(from: Long, to: Long): Flow<List<Transaction>> = dao.observeRange(from, to)
    fun search(type: String, categoryKey: String, from: Long, to: Long, keyword: String): Flow<List<Transaction>> =
        dao.search(type, categoryKey, from, to, keyword)
    suspend fun add(transaction: Transaction) = dao.insert(transaction)
    suspend fun remove(transaction: Transaction) = dao.delete(transaction)

    // ---- 预算 ----
    fun observeBudgets(month: String): Flow<List<Budget>> = budgetDao.observeByMonth(month)
    suspend fun upsertBudget(budget: Budget) = budgetDao.upsert(budget)
    suspend fun deleteBudget(month: String, categoryKey: String) =
        budgetDao.deleteByMonthAndCategory(month, categoryKey)

    // ---- 储蓄目标 ----
    fun observeGoals(): Flow<List<SavingsGoal>> = goalDao.observeAll()
    suspend fun addGoal(goal: SavingsGoal) = goalDao.insert(goal)
    suspend fun updateGoal(goal: SavingsGoal) = goalDao.update(goal)
    suspend fun deleteGoal(goal: SavingsGoal) = goalDao.delete(goal)

    // ---- 周期性账目 ----
    fun observeRecurring(): Flow<List<RecurringTransaction>> = recurringDao.observeAll()
    suspend fun addRecurring(item: RecurringTransaction) = recurringDao.insert(item)
    suspend fun updateRecurring(item: RecurringTransaction) = recurringDao.update(item)
    suspend fun deleteRecurring(item: RecurringTransaction) = recurringDao.delete(item)

    /** 生成所有已到期的周期性账目（App 启动时调用一次） */
    suspend fun generateDueRecurring(now: Long = System.currentTimeMillis()) {
        val dayMs = 24L * 60 * 60 * 1000
        for (r in recurringDao.dueList(now)) {
            var next = r.nextDue
            var guard = 0
            while (next <= now && guard < 366) {
                dao.insert(
                    Transaction(
                        amount = r.amount,
                        type = r.type,
                        categoryKey = r.categoryKey,
                        timestamp = next,
                        note = r.note
                    )
                )
                next += r.intervalDays * dayMs
                guard++
            }
            recurringDao.update(r.copy(nextDue = next))
        }
    }

    // ---- 自定义分类 ----
    fun observeCustomCategories(): Flow<List<CustomCategory>> = customCategoryDao.observeAll()
    suspend fun upsertCustomCategory(category: CustomCategory) = customCategoryDao.upsert(category)
    suspend fun deleteCustomCategory(category: CustomCategory) = customCategoryDao.delete(category)

    // ---- 备份 / 恢复 / 导出 ----
    suspend fun exportBackupJsonFull(): String {
        val root = JSONObject()
        root.put("version", 2)
        suspend fun txJson() = JSONArray().apply {
            dao.getAllOnce().forEach {
                put(JSONObject().apply {
                    put("amount", it.amount); put("type", it.type)
                    put("categoryKey", it.categoryKey); put("timestamp", it.timestamp)
                    put("note", it.note); put("photoPath", it.photoPath)
                    put("transferFrom", it.transferFrom); put("transferTo", it.transferTo)
                })
            }
        }
        root.put("transactions", txJson())
        root.put("budgets", JSONArray().apply {
            budgetDao.getAllOnce().forEach {
                put(JSONObject().apply {
                    put("month", it.month); put("categoryKey", it.categoryKey)
                    put("amount", it.amount)
                })
            }
        })
        root.put("savings_goals", JSONArray().apply {
            goalDao.getAllOnce().forEach {
                put(JSONObject().apply {
                    put("name", it.name); put("targetAmount", it.targetAmount)
                    put("savedAmount", it.savedAmount); put("deadline", it.deadline)
                    put("createdAt", it.createdAt)
                })
            }
        })
        root.put("recurring", JSONArray().apply {
            recurringDao.getAllOnce().forEach {
                put(JSONObject().apply {
                    put("amount", it.amount); put("type", it.type)
                    put("categoryKey", it.categoryKey); put("note", it.note)
                    put("intervalDays", it.intervalDays); put("nextDue", it.nextDue)
                    put("active", it.active)
                })
            }
        })
        root.put("custom_categories", JSONArray().apply {
            customCategoryDao.getAllOnce().forEach {
                put(JSONObject().apply {
                    put("key", it.key); put("label", it.label)
                    put("iconName", it.iconName); put("colorArgb", it.colorArgb)
                    put("type", it.type)
                })
            }
        })
        return root.toString()
    }

    /** 恢复备份（覆盖当前全部数据） */
    suspend fun restoreBackupJson(json: String) {
        val root = JSONObject(json)
        dao.deleteAll(); goalDao.deleteAll(); recurringDao.deleteAll()
        customCategoryDao.deleteAll(); budgetDao.deleteAll()
        val budgetArr = root.optJSONArray("budgets") ?: JSONArray()
        val budgetList = mutableListOf<Budget>()
        for (i in 0 until budgetArr.length()) {
            val o = budgetArr.getJSONObject(i)
            budgetList.add(
                Budget(
                    month = o.optString("month"),
                    categoryKey = o.optString("categoryKey"),
                    amount = o.optDouble("amount")
                )
            )
        }
        budgetDao.upsertAll(budgetList)
        val txArr = root.optJSONArray("transactions") ?: JSONArray()
        val txList = mutableListOf<Transaction>()
        for (i in 0 until txArr.length()) {
            val o = txArr.getJSONObject(i)
            txList.add(
                Transaction(
                    amount = o.optDouble("amount"),
                    type = o.optString("type", "EXPENSE"),
                    categoryKey = o.optString("categoryKey", "other_expense"),
                    timestamp = o.optLong("timestamp"),
                    note = o.optString("note", ""),
                    photoPath = o.optString("photoPath", ""),
                    transferFrom = o.optString("transferFrom", ""),
                    transferTo = o.optString("transferTo", "")
                )
            )
        }
        dao.insertAll(txList)
        val goalArr = root.optJSONArray("savings_goals") ?: JSONArray()
        val goals = mutableListOf<SavingsGoal>()
        for (i in 0 until goalArr.length()) {
            val o = goalArr.getJSONObject(i)
            goals.add(
                SavingsGoal(
                    name = o.optString("name"),
                    targetAmount = o.optDouble("targetAmount"),
                    savedAmount = o.optDouble("savedAmount"),
                    deadline = o.optLong("deadline"),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }
        goalDao.insertAll(goals)
        val recArr = root.optJSONArray("recurring") ?: JSONArray()
        val recs = mutableListOf<RecurringTransaction>()
        for (i in 0 until recArr.length()) {
            val o = recArr.getJSONObject(i)
            recs.add(
                RecurringTransaction(
                    amount = o.optDouble("amount"),
                    type = o.optString("type", "EXPENSE"),
                    categoryKey = o.optString("categoryKey", "other_expense"),
                    note = o.optString("note", ""),
                    intervalDays = o.optInt("intervalDays", 30),
                    nextDue = o.optLong("nextDue"),
                    active = o.optBoolean("active", true)
                )
            )
        }
        recurringDao.insertAll(recs)
        val catArr = root.optJSONArray("custom_categories") ?: JSONArray()
        val cats = mutableListOf<CustomCategory>()
        for (i in 0 until catArr.length()) {
            val o = catArr.getJSONObject(i)
            cats.add(
                CustomCategory(
                    key = o.optString("key"),
                    label = o.optString("label"),
                    iconName = o.optString("iconName", "more"),
                    colorArgb = o.optInt("colorArgb", -7829368),
                    type = o.optString("type", "EXPENSE")
                )
            )
        }
        customCategoryDao.upsertAll(cats)
    }

    /** 导出 CSV（Excel 可直接打开） */
    suspend fun exportCsv(): String {
        val sb = StringBuilder()
        sb.append("日期,类型,分类,金额,备注,转出方,转入方\n")
        val typeName = mapOf("EXPENSE" to "支出", "INCOME" to "收入", "TRANSFER" to "转账")
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.CHINA)
        dao.getAllOnce().forEach {
            fun esc(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
            sb.append(fmt.format(java.util.Date(it.timestamp))).append(",")
                .append(typeName[it.type] ?: it.type).append(",")
                .append(esc(findCategory(it.categoryKey).label)).append(",")
                .append(it.amount).append(",")
                .append(esc(it.note)).append(",")
                .append(esc(it.transferFrom)).append(",")
                .append(esc(it.transferTo)).append("\n")
        }
        return sb.toString()
    }
}
