package com.neel.spent.utils

import com.neel.spent.data.Transaction
import java.util.*

object SpendingCalculator {
    fun getTodaySpending(transactions: List<Transaction>): Double {
        val cal = Calendar.getInstance()
        return transactions.filter { transaction ->
            val txnCal = Calendar.getInstance().apply { time = transaction.date }
            cal.get(Calendar.YEAR) == txnCal.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == txnCal.get(Calendar.DAY_OF_YEAR)
        }.sumOf { it.amount }
    }

    fun getWeekSpending(transactions: List<Transaction>): Double {
        val cal = Calendar.getInstance()
        return transactions.filter { transaction ->
            val txnCal = Calendar.getInstance().apply { time = transaction.date }
            cal.get(Calendar.YEAR) == txnCal.get(Calendar.YEAR) &&
            cal.get(Calendar.WEEK_OF_YEAR) == txnCal.get(Calendar.WEEK_OF_YEAR)
        }.sumOf { it.amount }
    }

    fun getMonthSpending(transactions: List<Transaction>): Double {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val startOfMonth = cal.time

        return transactions.filter { transaction ->
            transaction.date >= startOfMonth
        }.sumOf { it.amount }
    }
}