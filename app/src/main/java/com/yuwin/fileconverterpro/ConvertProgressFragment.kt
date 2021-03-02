package com.yuwin.fileconverterpro

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.yuwin.fileconverterpro.databinding.FragmentConvertProgressBinding


class ConvertProgressFragment : BaseFragment() {

    private val args by navArgs<ConvertProgressFragmentArgs>()

    override var bottomNavigationVisibility = View.GONE




    private val binding by lazy {FragmentConvertProgressBinding.inflate(layoutInflater)}
    private lateinit var convertProgressViewModel: ConvertProgressViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (activity as MainActivity).requestInterstitial()

        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        convertProgressViewModel = ViewModelProvider(this, ConvertProgressViewModelFactory(requireActivity().application, args.data.items, args.quality)).get(ConvertProgressViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = convertProgressViewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        binding.circularProgressBar.apply {
                progressMax = 100f
                progressBarColor = ContextCompat.getColor(requireActivity(), R.color.colorPrimary)
                backgroundProgressBarColor = ContextCompat.getColor(requireActivity(), R.color.cardBackground)
                progressBarWidth = 10f
                backgroundProgressBarWidth = 12f
                roundBorder = true
        }
        convertProgressViewModel.completePercentage.observe(viewLifecycleOwner, { progress ->
            if (progress != null) {
                binding.progressBarTextView.text = "${progress.toInt()}%"
                binding.circularProgressBar.progress = progress.toFloat()
            }
            if (progress.toInt() == 100 && progress != null) {
                binding.circularProgressBar.apply {
                    progressBarColor = ContextCompat.getColor(requireActivity(), R.color.completeGreen)
                }
            }
        })

        convertProgressViewModel.conversionFinished.observe(viewLifecycleOwner, { conversionFinished ->
            if (conversionFinished) {
                binding.pauseButton.visibility = View.GONE
                binding.convertedFileNameTextView.visibility = View.GONE
                binding.backHomeButton.visibility = View.VISIBLE
                binding.resumeButton.visibility = View.GONE

            }
        })

        binding.pauseButton.setOnClickListener {
            convertProgressViewModel.pauseConversion()
            convertProgressViewModel.setConversionPaused(true)
            binding.pauseButton.visibility = View.GONE
            binding.backHomeButton.visibility = View.VISIBLE
            binding.resumeButton.visibility = View.VISIBLE

        }

        binding.resumeButton.setOnClickListener {
            binding.backHomeButton.visibility = View.GONE
            binding.resumeButton.visibility = View.GONE
            binding.pauseButton.visibility = View.VISIBLE
            startConversion()
        }

        binding.backHomeButton.setOnClickListener {
            (activity as MainActivity).showInterstitial()
        }



       startConversion()
    }

    override fun onPause() {
        super.onPause()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    private fun startConversion() {
        val data = args.data.items[0]
        val defaultFileExtension = Util.getFileExtension(data.specificFormat, data.defaultConvertFormat, data.convertAll)
        if (data.convertAll == true && defaultFileExtension == ".mergeintopdf" ) {
            binding.pauseButton.text = "Stop"
            binding.resumeButton.text = "Restart"
            convertProgressViewModel.createMultiPagePdf()
        }else {
            convertProgressViewModel.convertFiles()
        }
    }


    private fun elementFadeIn(delta: Long): Animation {
        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.interpolator = AccelerateInterpolator()
        fadeIn.duration = delta
        return fadeIn
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