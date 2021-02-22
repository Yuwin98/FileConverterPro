package com.yuwin.fileconverterpro

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.yuwin.fileconverterpro.databinding.FragmentConvertProgressBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class ConvertProgressFragment : Fragment() {

    private val args by navArgs<ConvertProgressFragmentArgs>()


    private val binding by lazy {FragmentConvertProgressBinding.inflate(layoutInflater)}
    private val convertProgressViewModel: ConvertProgressViewModel by viewModels {
        ConvertProgressViewModelFactory(context?.applicationContext as Application, args.data.items, args.quality)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = convertProgressViewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mAdView: AdView = view.findViewById(R.id.convertProgressAdView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        binding.circularProgressBar.apply {
                progressMax = 100f
                progressBarColor = ContextCompat.getColor(requireActivity(), R.color.colorPrimary)
                backgroundProgressBarColor = ContextCompat.getColor(requireActivity(), R.color.cardBackground)
                progressBarWidth = 10f
                backgroundProgressBarWidth = 12f
                roundBorder = true
        }
        convertProgressViewModel.completePercentage.observe(viewLifecycleOwner, { progress ->
                binding.progressBarTextView.text = "${progress.toInt()}%"
                binding.circularProgressBar.progress = progress.toFloat()
                if(progress.toFloat() == 100f){
                    binding.circularProgressBar.apply {
                        progressBarColor = ContextCompat.getColor(requireActivity(), R.color.completeGreen)
                    }
                    binding.cancelButton.visibility = View.GONE
                    binding.showConvertedFilesButton.visibility = View.VISIBLE
                }

        })

        binding.showConvertedFilesButton.setOnClickListener {
            findNavController().navigate(R.id.convertedFilesFragment)
        }

        convertProgressViewModel.convertImages()


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