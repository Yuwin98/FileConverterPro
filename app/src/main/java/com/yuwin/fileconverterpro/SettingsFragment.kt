package com.yuwin.fileconverterpro

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.SeekBar
import android.widget.Toast
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.yuwin.fileconverterpro.Util.Companion.observeOnce
import com.yuwin.fileconverterpro.databinding.FragmentSettingsBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding


    private var mainViewModel: MainViewModel? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity?.let {
            val moveCopy = it.findViewById<MotionLayout>(R.id.moveCopyLayout)
            moveCopy.visibility = View.GONE
        }

        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        _binding?.lifecycleOwner = viewLifecycleOwner

        mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainViewModel?.readIsPremium?.observe(viewLifecycleOwner, { isPremium ->
            if(isPremium == 1) {
                binding?.goPremiumCardView?.visibility = View.GONE
                binding?.goPremiumTextView?.visibility = View.GONE
            }else {
                binding?.goPremiumCardView?.visibility = View.VISIBLE
                binding?.goPremiumTextView?.visibility = View.VISIBLE
            }
        })

        mainViewModel?.readQuality?.observeOnce(viewLifecycleOwner, { progress ->
            binding?.qualityProgressTextView?.text = progress.toString()
            binding?.qualitySeekBar?.progress = progress
        })
        binding?.qualitySeekBar?.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                mainViewModel?.setQuality(progress)
                binding?.qualityProgressTextView?.text = progress.toString()

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })

        mainViewModel?.readDefaultFormat?.observeOnce(viewLifecycleOwner, {
            binding?.defaultFormatSpinner?.setSelection(it)
        })
        binding?.defaultFormatSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                mainViewModel?.setFormatType(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

        }

        binding?.rateUsTextView?.setOnClickListener {
            openReviewUrl()
        }

        mainViewModel?.readCurrentStorage?.observe(viewLifecycleOwner) { path ->
            binding?.currentStoragePath?.text = path
        }

        binding?.goPremiumTextView?.setOnClickListener {
            (activity as MainActivity).subscribe()
        }

        binding?.sendBugReportTextView?.setOnClickListener {
            sendBugReport()
        }

    }

    private fun sendBugReport() {
        if(hasInternetConnection()) {
            lifecycleScope.launch {
                Toast.makeText(requireContext(),"Sending bug report", Toast.LENGTH_SHORT).show()
                delay(3000)
                Toast.makeText(requireContext(),"Bug report sent", Toast.LENGTH_SHORT).show()
            }
        }else {
            Toast.makeText(requireContext(), "Not connected to the internet", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openReviewUrl() {
        val uri: Uri = Uri.parse("market://details?id=${requireContext().applicationContext.packageName}")
        val goToMarket = Intent(Intent.ACTION_VIEW, uri)
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        try {
            startActivity(goToMarket)
        } catch (e: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=${requireContext().applicationContext.packageName}")
                )
            )
        }
    }

    private fun hasInternetConnection(): Boolean {
        val connectivityManager = requireActivity().application.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.defaultFormatSpinner?.adapter = null
        _binding = null
        mainViewModel = null

        (0..21).random()
    }


}