package com.neel.spent.utils

import android.content.Context
import android.database.Cursor
import com.neel.spent.data.Transaction
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.net.toUri

object SmsReader {
    fun readTransactions(context: Context): List<Transaction> {
        val cursor: Cursor? = context.contentResolver.query(
            "content://sms/inbox".toUri(),
            arrayOf("body", "address", "date"),
            "body LIKE ? OR body LIKE ? OR body LIKE ? OR body LIKE ? OR body LIKE ?",
            arrayOf("%debited%", "%Sent Rs.%", "%debited by%", "%paid thru%", "%sent from%"),
            "date DESC"
        )

        val transactions = mutableListOf<Transaction>()
        var federalCount = 0
        var sbiCount = 0
        var kotakCount = 0
        var karnatakaBankCount = 0
        var canaraBankCount = 0
        var sliceCount = 0

        val federalRegex = Regex("Rs\\s+(\\d+\\.?\\d*)\\s+debited")
        val sbiRegex = Regex("debited by (\\d+\\.?\\d*) on date (\\d{2}[A-Za-z]{3}\\d{2})")
        val kotakRegex = Regex("Sent Rs\\.?(\\d+\\.?\\d*)[\\s\\S]*on (\\d{2}-\\d{2}-\\d{2})")
        val karnatakaBankRegex = Regex("debited for Rs\\.(\\d+\\.\\d{2}) on (\\d{2}-\\d{2}-\\d{2})")
        val canaraBankRegex = Regex("Rs\\.\\s*(\\d+\\.?\\d*)\\s+paid thru.*on (\\d{1,2}-\\d{1,2}-\\d{2})")
        val sliceRegex = Regex("Rs\\.\\s*(\\d+\\.?\\d*)\\s+sent from.*on (\\d{2}-[A-Za-z]{3}-\\d{2})")

        val federalDateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
        val sbiDateFormat = SimpleDateFormat("ddMMMyy", Locale.getDefault())
        val kotakDateFormat = SimpleDateFormat("dd-MM-yy", Locale.getDefault())
        val karnatakaBankDateFormat = SimpleDateFormat("dd-MM-yy", Locale.getDefault())
        val canaraBankDateFormat = SimpleDateFormat("dd-M-yy", Locale.getDefault())
        val sliceDateFormat = SimpleDateFormat("dd-MMM-yy", Locale.getDefault())


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
                        } ?: run {
                            // Karnataka Bank format
                            karnatakaBankRegex.find(body)?.let { match ->
                                karnatakaBankCount++
                                val amount = match.groupValues[1].toDouble()
                                val dateStr = match.groupValues[2]
                                val date = try {
                                    karnatakaBankDateFormat.parse(dateStr)
                                } catch (e: Exception) {
                                    null
                                } ?: Date()
                                transactions.add(Transaction(amount, date))
                            } ?: run {
                                // Canara Bank format
                                canaraBankRegex.find(body)?.let { match ->
                                    canaraBankCount++
                                    val amount = match.groupValues[1].toDouble()
                                    val dateStr = match.groupValues[2]
                                    val date = try {
                                        canaraBankDateFormat.parse(dateStr)
                                    } catch (e: Exception) {
                                        null
                                    } ?: Date()
                                    transactions.add(Transaction(amount, date))

                                } ?: run {
                                    // Slice format
                                    sliceRegex.find(body)?.let { match ->
                                        sliceCount++
                                        val amount = match.groupValues[1].toDouble()
                                        val dateStr = match.groupValues[2]
                                        val date = try {
                                            sliceDateFormat.parse(dateStr)
                                        } catch (e: Exception) {
                                            null
                                        } ?: Date()
                                        transactions.add(Transaction(amount, date))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        println("SMS Count - Federal Bank: $federalCount, SBI: $sbiCount, Kotak: $kotakCount, Canara Bank: $canaraBankCount, Slice: $sliceCount")
        return transactions
    }
}