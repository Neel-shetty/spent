package com.neel.spent

import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.neel.spent.databinding.FragmentFirstBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
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

        binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }

        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_SMS) 
            == PackageManager.PERMISSION_GRANTED) {
            readSMS()
        }
    }

    private fun readSMS() {
        val cursor: Cursor? = requireActivity().contentResolver.query(
            Uri.parse("content://sms/inbox"),
            arrayOf("body", "address", "date"),
            "body LIKE ?",
            arrayOf("%debited%"),
            "date DESC"
        )

        val messages = StringBuilder()
        cursor?.use { 
            if (it.count == 0) {
                messages.append("No messages containing 'debited' found.")
            } else {
                while (it.moveToNext()) {
                    val body = it.getString(0)
                    val address = it.getString(1)
                    messages.append("From: $address\n$body\n\n")
                }
            }
        }

        binding.textviewFirst.text = messages.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}