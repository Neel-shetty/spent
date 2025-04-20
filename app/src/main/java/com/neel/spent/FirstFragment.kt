package com.neel.spent

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.neel.spent.databinding.FragmentFirstBinding
import com.neel.spent.utils.SpendingCalculator
import com.neel.spent.utils.SmsReader

class FirstFragment : Fragment() {
    private var _binding: FragmentFirstBinding? = null
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
            updateSpending()
        }
    }

    private fun updateSpending() {
        val transactions = SmsReader.readTransactions(requireContext())
        
        val today = SpendingCalculator.getTodaySpending(transactions)
        val thisWeek = SpendingCalculator.getWeekSpending(transactions)
        val thisMonth = SpendingCalculator.getMonthSpending(transactions)

        val summary = """
            Today
            ₹%.2f
            
            This Week
            ₹%.2f
            
            This Month
            ₹%.2f
        """.trimIndent().format(today, thisWeek, thisMonth)

        binding.textviewFirst.apply {
            textSize = 70f
            setTextIsSelectable(true)
            text = summary
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}