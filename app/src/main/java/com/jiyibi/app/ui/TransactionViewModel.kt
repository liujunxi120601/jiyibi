package com.jiyibi.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jiyibi.app.data.AppDatabase
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
import java.util.Calendar

data class MonthSummary(val income: Double, val expense: Double) {
    val balance: Double get() = income - expense
}

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = TransactionRepository(AppDatabase.get(app).transactionDao())

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

    /** 当前选中月份的交易记录 */
    val monthTransactions: StateFlow<List<Transaction>> =
        _monthOffset.flatMapLatest { offset ->
            val (start, end) = monthRange(offset)
            repository.observeRange(start, end)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 当前选中月份的收支汇总 */
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
        note: String
    ) {
        viewModelScope.launch {
            repository.add(
                Transaction(
                    amount = amount,
                    type = type.name,
                    categoryKey = categoryKey,
                    timestamp = timestamp,
                    note = note
                )
            )
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch { repository.remove(transaction) }
    }
}
