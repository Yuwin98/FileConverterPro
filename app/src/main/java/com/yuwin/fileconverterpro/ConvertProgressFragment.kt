package com.yuwin.fileconverterpro

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import com.yuwin.fileconverterpro.converter.ConvertImage
import com.yuwin.fileconverterpro.converter.SimpleConverter
import com.yuwin.fileconverterpro.databinding.FragmentConvertBinding
import com.yuwin.fileconverterpro.databinding.FragmentConvertProgressBinding
import kotlinx.coroutines.launch


class ConvertProgressFragment : Fragment() {

    private val args by navArgs<ConvertProgressFragmentArgs>()

    private val binding by lazy {FragmentConvertProgressBinding.inflate(layoutInflater)}
    private val viewModel: ConvertProgressViewModel by viewModels {
        ConvertProgressViewModelFactory(context?.applicationContext as Application, args.data.items)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(requireActivity())

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mAdView: AdView = view.findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)


        binding.circularProgressBar.apply {
                progressMax = 100f
                setProgressWithAnimation(100f, 10000)
                progressBarColor = ContextCompat.getColor(requireActivity(), R.color.colorPrimary)
                backgroundProgressBarColor = ContextCompat.getColor(requireActivity(), R.color.cardBackground)
                progressBarWidth = 10f
                backgroundProgressBarWidth = 12f
                roundBorder = true
        }
        binding.circularProgressBar.onProgressChangeListener = { progress ->
            binding.progressBarTextView.text = "${progress.toInt()}%"
            if(progress == 100f){
                binding.circularProgressBar.apply {
                    progressBarColor = ContextCompat.getColor(requireActivity(), R.color.completeGreen)
                }
            }
        }
    }

    private fun logData(data: ConvertInfo) {
        Log.d("debug", "Uri: ${data.uri}")
        Log.d("debug", "File Name: ${data.fileName}")
        Log.d("debug", "File Path: ${data.filePath}")
        Log.d("debug", "File Size: ${data.fileSize}")
        Log.d("debug", "File Type: ${data.fileType}")
        Log.d("debug", "Default Convert Types: ${FormatTypes.values()[data.defaultConvertFormat!!]}")
        Log.d("debug", "Specific Convert Types: ${FormatTypes.values()[data.specificConvertFormat!!]}")
        Log.d("debug", "Convert All: ${data.convertAll.toString()}")
    }


}