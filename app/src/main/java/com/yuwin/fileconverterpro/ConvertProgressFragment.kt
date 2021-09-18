package com.yuwin.fileconverterpro

import android.app.Application
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.yuwin.fileconverterpro.databinding.FragmentConvertProgressBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Exception
import java.lang.NumberFormatException
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



        convertProgressViewModel = ViewModelProvider(
            this,
            ConvertProgressViewModelFactory(
                requireContext().applicationContext as Application,
                args.data.items,
                args.quality,
                args.padding,
                args.fileQuality,
                args.pageSize,
                args.convertInto,
                args.pageInfoList
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
        activity?.let {
            val moveCopy = it.findViewById<MotionLayout>(R.id.moveCopyLayout)
            moveCopy.visibility = View.GONE
        }


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
            if (progress != null && progress != 0.0) {
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
                    binding.backHomeButton.visibility = View.GONE
                    lifecycleScope.launch {
                        delay(300)
                        (activity as MainActivity).mainViewModel?.setIsLoading(true)
                        findNavController().navigate(R.id.action_convertProgressFragment_to_home)
                        mainViewModel.readIsPremium.observe(viewLifecycleOwner, {isPremium ->
                            if(isPremium == 0) {
                                (activity as MainActivity).hideBigBanner()
                                (activity as MainActivity).showInterstitial()
                                (activity as MainActivity).showSmallBanner()
                            }

                        })
                    }


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

        }


        startConversion()
    }




    private fun startConversion() {

        mainViewModel.readIsPremium.observe(viewLifecycleOwner, {isPremium ->
            if(isPremium == 0) {
                (activity as MainActivity).hideSmallBanner()
                (activity as MainActivity).showBigBanner()
            }
        })

        when {
            args.pdfMerge -> {
                binding.pauseButton.text = getString(R.string.stopText)
                binding.resumeButton.text = getString(R.string.restartText)
                convertProgressViewModel.mergePDF()
            }
            args.mergeImagesIntoPdf -> {
                binding.pauseButton.text = getString(R.string.stopText)
                binding.resumeButton.text = getString(R.string.restartText)
                convertProgressViewModel.createMultiPagePdf()
            }
            args.pdfIntoImages -> {
                binding.pauseButton.text = getString(R.string.stopText)
                binding.resumeButton.text = getString(R.string.restartText)
                convertProgressViewModel.pdfIntoImage()
            }
            else -> {
                convertProgressViewModel.convertFiles()
            }
        }



    }

    private fun roundOffDecimal(number: Double): Double {
        val df = DecimalFormat("#.#")
        df.roundingMode = RoundingMode.CEILING
        var result: Double = number
        try {
           result =  df.format(number).toDouble()
        }catch (e: NumberFormatException) {

        }catch (e: Exception) {

        }finally {
            return result
        }

    }


}