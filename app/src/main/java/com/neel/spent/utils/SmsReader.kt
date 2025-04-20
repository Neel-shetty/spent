package com.neel.spent.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.neel.spent.data.Transaction
import java.text.SimpleDateFormat
import java.util.*

object SmsReader {
    fun readTransactions(context: Context): List<Transaction> {
        val cursor: Cursor? = context.contentResolver.query(
            Uri.parse("content://sms/inbox"),
            arrayOf("body", "address", "date"),
            "body LIKE ? OR body LIKE ? OR body LIKE ?",
            arrayOf("%debited%", "%Sent Rs.%", "%debited by%"),
            "date DESC"
        )

        val transactions = mutableListOf<Transaction>()
        var federalCount = 0
        var sbiCount = 0
        var kotakCount = 0

        val federalRegex = Regex("Rs\\s+(\\d+\\.?\\d*)\\s+debited")
        val sbiRegex = Regex("debited by (\\d+\\.?\\d*) on date (\\d{2}[A-Za-z]{3}\\d{2})")
        val kotakRegex = Regex("Sent Rs\\.?(\\d+\\.?\\d*)[\\s\\S]*on (\\d{2}-\\d{2}-\\d{2})")
        val federalDateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
        val sbiDateFormat = SimpleDateFormat("ddMMMyy", Locale.getDefault())
        val kotakDateFormat = SimpleDateFormat("dd-MM-yy", Locale.getDefault())

        cursor?.use {
            while (it.moveToNext()) {
                val body = it.getString(0)
                // Federal Bank format
                federalRegex.find(body)?.let { match ->
                    federalCount++
                    val amount = match.groupValues[1].toDouble()
                    val dateStr = body.substringAfter("on ").substringBefore(" to")
                    val date = try { federalDateFormat.parse(dateStr) } catch (e: Exception) { null } ?: Date()
                    transactions.add(Transaction(amount, date))
                } ?: run {
                    // SBI format
                    sbiRegex.find(body)?.let { match ->
                        sbiCount++
                        val amount = match.groupValues[1].toDouble()
                        val dateStr = match.groupValues[2]
                        val date = try { sbiDateFormat.parse(dateStr) } catch (e: Exception) { null } ?: Date()
                        transactions.add(Transaction(amount, date))
                    } ?: run {
                        // Kotak format
                        kotakRegex.find(body)?.let { match ->
                            kotakCount++
                            val amount = match.groupValues[1].toDouble()
                            val dateStr = match.groupValues[2]
                            val date = try { kotakDateFormat.parse(dateStr) } catch (e: Exception) { null } ?: Date()
                            transactions.add(Transaction(amount, date))
                        }
                    }
                }
            }
        }
        println("SMS Count - Federal Bank: $federalCount, SBI: $sbiCount, Kotak: $kotakCount")
        return transactions
    }
}