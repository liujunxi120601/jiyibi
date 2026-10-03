package com.jiyibi.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jiyibi.app.data.AppDatabase
import com.jiyibi.app.data.Budget
import com.jiyibi.app.data.CustomCategory
import com.jiyibi.app.data.RecurringTransaction
import com.jiyibi.app.data.SavingsGoal
import com.jiyibi.app.data.Transaction
import com.jiyibi.app.data.TransactionRepository
import com.jiyibi.app.data.TransactionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class MonthSummary(val income: Double, val expense: Double) {
    val balance: Double get() = income - expense
}

data class SearchFilter(
    val keyword: String = "",
    val type: String = "", // ""=全部 / EXPENSE / INCOME / TRANSFER
    val categoryKey: String = "",
    val from: Long = 0L,
    val to: Long = Long.MAX_VALUE
)

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = TransactionRepository(AppDatabase.get(app))

    /** 月份偏移：0 = 本月，-1 = 上月，依此类推 */
    private val _monthOffset = MutableStateFlow(0)
    val monthOffset: StateFlow<Int> = _monthOffset.asStateFlow()

    private fun monthRange(offset: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, offset)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        return start to cal.timeInMillis
    }

    /** "yyyy-MM" 格式的月份 key（预算用） */
    fun monthKey(offset: Int = _monthOffset.value): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, offset)
        return SimpleDateFormat("yyyy-MM", Locale.CHINA).format(cal.time)
    }

    /** 当前选中月份的交易记录 */
    val monthTransactions: StateFlow<List<Transaction>> =
        _monthOffset.flatMapLatest { offset ->
            val (start, end) = monthRange(offset)
            repository.observeRange(start, end)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 当前选中月份的收支汇总（转账不计入） */
    val monthSummary: StateFlow<MonthSummary> =
        monthTransactions.map { list ->
            MonthSummary(
                income = list.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount },
                expense = list.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthSummary(0.0, 0.0))

    /** 全部交易记录（用于统计页近 6 个月柱状图） */
    val allTransactions: StateFlow<List<Transaction>> =
        repository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 当前月份的预算 */
    val budgets: StateFlow<List<Budget>> =
        _monthOffset.flatMapLatest { offset ->
            repository.observeBudgets(monthKey(offset))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 储蓄目标 */
    val savingsGoals: StateFlow<List<SavingsGoal>> =
        repository.observeGoals()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 周期性账目 */
    val recurringList: StateFlow<List<RecurringTransaction>> =
        repository.observeRecurring()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 自定义分类 */
    val customCategories: StateFlow<List<CustomCategory>> =
        repository.observeCustomCategories()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 搜索 */
    private val _searchFilter = MutableStateFlow(SearchFilter())
    val searchFilter: StateFlow<SearchFilter> = _searchFilter.asStateFlow()
    val searchResults: StateFlow<List<Transaction>> =
        _searchFilter.flatMapLatest { f ->
            repository.search(f.type, f.categoryKey, f.from, f.to, f.keyword)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchFilter(filter: SearchFilter) { _searchFilter.value = filter }

    fun prevMonth() { _monthOffset.value -= 1 }

    fun nextMonth() { if (_monthOffset.value < 0) _monthOffset.value += 1 }

    fun monthLabel(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, _monthOffset.value)
        return "${cal.get(Calendar.YEAR)}年${cal.get(Calendar.MONTH) + 1}月"
    }

    fun addTransaction(
        type: TransactionType,
        amount: Double,
        categoryKey: String,
        timestamp: Long,
        note: String,
        photoPath: String = "",
        transferFrom: String = "",
        transferTo: String = ""
    ) {
        viewModelScope.launch {
            repository.add(
                Transaction(
                    amount = amount,
                    type = type.name,
                    categoryKey = categoryKey,
                    timestamp = timestamp,
                    note = note,
                    photoPath = photoPath,
                    transferFrom = transferFrom,
                    transferTo = transferTo
                )
            )
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch { repository.remove(transaction) }
    }

    // ---- 预算 ----
    fun upsertBudget(categoryKey: String, amount: Double) {
        viewModelScope.launch {
            repository.upsertBudget(Budget(month = monthKey(), categoryKey = categoryKey, amount = amount))
        }
    }

    fun deleteBudget(categoryKey: String) {
        viewModelScope.launch { repository.deleteBudget(monthKey(), categoryKey) }
    }

    // ---- 储蓄目标 ----
    fun addSavingsGoal(name: String, targetAmount: Double, deadline: Long = 0L) {
        viewModelScope.launch {
            repository.addGoal(SavingsGoal(name = name, targetAmount = targetAmount, deadline = deadline))
        }
    }

    fun contributeGoal(goal: SavingsGoal, amount: Double) {
        viewModelScope.launch {
            repository.updateGoal(goal.copy(savedAmount = (goal.savedAmount + amount).coerceAtLeast(0.0)))
        }
    }

    fun deleteGoal(goal: SavingsGoal) {
        viewModelScope.launch { repository.deleteGoal(goal) }
    }

    // ---- 周期性账目 ----
    fun addRecurring(
        amount: Double,
        type: TransactionType,
        categoryKey: String,
        note: String,
        intervalDays: Int,
        firstDue: Long
    ) {
        viewModelScope.launch {
            repository.addRecurring(
                RecurringTransaction(
                    amount = amount,
                    type = type.name,
                    categoryKey = categoryKey,
                    note = note,
                    intervalDays = intervalDays,
                    nextDue = firstDue
                )
            )
            repository.generateDueRecurring()
        }
    }

    fun setRecurringActive(item: RecurringTransaction, active: Boolean) {
        viewModelScope.launch { repository.updateRecurring(item.copy(active = active)) }
    }

    fun deleteRecurring(item: RecurringTransaction) {
        viewModelScope.launch { repository.deleteRecurring(item) }
    }

    fun generateRecurring() {
        viewModelScope.launch { repository.generateDueRecurring() }
    }

    // ---- 自定义分类 ----
    fun addCustomCategory(label: String, type: TransactionType, iconName: String, colorArgb: Int) {
        viewModelScope.launch {
            repository.upsertCustomCategory(
                CustomCategory(
                    key = "custom_${System.currentTimeMillis()}",
                    label = label,
                    iconName = iconName,
                    colorArgb = colorArgb,
                    type = type.name
                )
            )
        }
    }

    fun deleteCustomCategory(category: CustomCategory) {
        viewModelScope.launch { repository.deleteCustomCategory(category) }
    }

    // ---- 备份 / 恢复 / 导出 ----
    suspend fun exportBackupJson(): String = repository.exportBackupJsonFull()
    suspend fun restoreBackupJson(json: String) = repository.restoreBackupJson(json)
    suspend fun exportCsv(): String = repository.exportCsv()
}
