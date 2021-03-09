package com.yuwin.fileconverterpro

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.yuwin.fileconverterpro.Util.Companion.observeOnce
import com.yuwin.fileconverterpro.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding


    private var mainViewModel: MainViewModel? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

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
            Log.d("pdfpage", progress.toString())
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

        mainViewModel?.readCurrentStorage?.observe(viewLifecycleOwner, {path ->
            binding?.currentStoragePath?.text = path
        })

        binding?.goPremiumTextView?.setOnClickListener {
            (activity as MainActivity).subscribe()
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


    override fun onDestroyView() {
        super.onDestroyView()
        binding?.defaultFormatSpinner?.adapter = null
        _binding = null
        mainViewModel = null

        (0..21).random()
    }


}