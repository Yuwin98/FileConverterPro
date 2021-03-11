package com.yuwin.fileconverterpro

import android.app.Application
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.yuwin.fileconverterpro.databinding.FragmentConvertProgressBinding
import java.math.RoundingMode
import java.text.DecimalFormat


class ConvertProgressFragment : BaseFragment() {

    private val args by navArgs<ConvertProgressFragmentArgs>()

    override var bottomNavigationVisibility = View.GONE


    private val binding by lazy { FragmentConvertProgressBinding.inflate(layoutInflater) }
    private lateinit var convertProgressViewModel: ConvertProgressViewModel
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (activity as MainActivity).requestInterstitial()

        convertProgressViewModel = ViewModelProvider(
            this,
            ConvertProgressViewModelFactory(
                requireContext().applicationContext as Application,
                args.data.items,
                args.quality,
                args.pdfQuality
            )
        ).get(ConvertProgressViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = convertProgressViewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        binding.circularProgressBar.apply {
            progressMax = 100f
            progressBarColor = ContextCompat.getColor(requireActivity(), R.color.colorPrimary)
            backgroundProgressBarColor =
                ContextCompat.getColor(requireActivity(), R.color.cardBackground)
            progressBarWidth = 10f
            backgroundProgressBarWidth = 12f
            roundBorder = true
        }
        convertProgressViewModel.completePercentage.observe(viewLifecycleOwner, { progress ->
            if (progress != null) {
                val progressOneDecimal = roundOffDecimal(progress)
                val progressString = "$progressOneDecimal%"
                binding.progressBarTextView.text = progressString
                binding.circularProgressBar.progress = progressOneDecimal.toFloat()
            }
            if (progress != null && progress.toInt() == 100) {
                binding.circularProgressBar.apply {
                    progressBarColor =
                        ContextCompat.getColor(requireActivity(), R.color.completeGreen)
                }
            }
        })

        convertProgressViewModel.conversionFinished.observe(
            viewLifecycleOwner,
            { conversionFinished ->
                if (conversionFinished) {
                    (requireActivity() as AppCompatActivity).supportActionBar?.title =
                        "Conversion Finished"
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
            binding.backHomeButton.visibility = View.GONE
            findNavController().navigate(R.id.action_convertProgressFragment_to_home)
            mainViewModel.readIsPremium.observe(viewLifecycleOwner, {isPremium ->
                if(isPremium == 0) {
                    (activity as MainActivity).showInterstitial()
                }
            })
        }


        startConversion()
    }




    private fun startConversion() {
        val data = args.data.items[0]
        val defaultFileExtension = data.isPdfConversion?.let {
            Util.getFileExtension(
                data.specificFormat, data.defaultConvertFormat, data.convertAll,
                it
            )
        }

        when (data.isPdfConversion) {
            true -> {
                if (data.convertAll == true && defaultFileExtension == ".mergepdf") {
                    binding.pauseButton.text = getString(R.string.stopText)
                    binding.resumeButton.text = getString(R.string.restartText)
                    convertProgressViewModel.mergePDF()
                } else {
                    binding.pauseButton.text = getString(R.string.stopText)
                    binding.resumeButton.text = getString(R.string.restartText)
                    convertProgressViewModel.pdfIntoImage()
                }
            }

            false -> {
                if (data.convertAll == true && defaultFileExtension == ".mergeintopdf") {
                    binding.pauseButton.text = getString(R.string.stopText)
                    binding.resumeButton.text = getString(R.string.restartText)
                    convertProgressViewModel.createMultiPagePdf()
                } else {
                    convertProgressViewModel.convertFiles()
                }
            }
        }


    }

    private fun roundOffDecimal(number: Double): Double {
        val df = DecimalFormat("#.#")
        df.roundingMode = RoundingMode.CEILING
        return df.format(number).toDouble()
    }


}