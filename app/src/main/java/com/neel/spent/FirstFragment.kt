package com.neel.spent

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.neel.spent.databinding.FragmentFirstBinding
import com.neel.spent.data.Transaction
import java.text.SimpleDateFormat
import java.util.*

class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, 
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().finish()
                }
            }
        )

        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_SMS) 
            == PackageManager.PERMISSION_GRANTED) {
            readSMS()
        }
    }

    private fun readSMS() {
        val cursor: Cursor? = requireActivity().contentResolver.query(
            Uri.parse("content://sms/inbox"),
            arrayOf("body", "address", "date"),
            "body LIKE ? OR body LIKE ? OR body LIKE ?",
            arrayOf("%debited%", "%Sent Rs.%", "%debited by%"),
            "date DESC"
        )

        val transactions = mutableListOf<Transaction>()
        val federalRegex = Regex("Rs\\s+(\\d+\\.?\\d*)\\s+debited")
        val sbiRegex = Regex("debited by (\\d+\\.?\\d*) on date (\\d{2}[A-Za-z]{3}\\d{2})")
        val kotakRegex = Regex("Sent Rs\\.?(\\d+\\.?\\d*)[\\s\\S]*on (\\d{2}-\\d{2}-\\d{2})")
        val federalDateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
        val sbiDateFormat = SimpleDateFormat("ddMMMyy", Locale.getDefault())
        val kotakDateFormat = SimpleDateFormat("dd-MM-yy", Locale.getDefault())

        var federalCount = 0
        var sbiCount = 0
        var kotakCount = 0

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

        android.util.Log.d("SMSRead", "Federal: $federalCount, SBI: $sbiCount, Kotak: $kotakCount")

        val now = Calendar.getInstance()
        val today = getTodaySpending(transactions)
        val thisWeek = getWeekSpending(transactions)
        val thisMonth = getMonthSpending(transactions)

        val summary = """
            Today
            ₹%.2f
            
            This Week
            ₹%.2f
            
            This Month
            ₹%.2f
        """.trimIndent().format(today, thisWeek, thisMonth)

        binding.textviewFirst.apply {
            textSize = 70f  // Increase text size for better readability
            setTextIsSelectable(true)
            text = summary
        }
    }

    private fun getTodaySpending(transactions: List<Transaction>): Double {
        val cal = Calendar.getInstance()
        return transactions.filter { transaction ->
            val txnCal = Calendar.getInstance().apply { time = transaction.date }
            cal.get(Calendar.YEAR) == txnCal.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == txnCal.get(Calendar.DAY_OF_YEAR)
        }.sumOf { it.amount }
    }

    private fun getWeekSpending(transactions: List<Transaction>): Double {
        val cal = Calendar.getInstance()
        return transactions.filter { transaction ->
            val txnCal = Calendar.getInstance().apply { time = transaction.date }
            cal.get(Calendar.YEAR) == txnCal.get(Calendar.YEAR) &&
            cal.get(Calendar.WEEK_OF_YEAR) == txnCal.get(Calendar.WEEK_OF_YEAR)
        }.sumOf { it.amount }
    }

    private fun getMonthSpending(transactions: List<Transaction>): Double {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1) // Set to first day of month
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val startOfMonth = cal.time

        return transactions.filter { transaction ->
            transaction.date >= startOfMonth
        }.sumOf { it.amount }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}