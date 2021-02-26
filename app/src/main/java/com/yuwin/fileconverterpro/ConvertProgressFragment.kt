package com.yuwin.fileconverterpro

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.yuwin.fileconverterpro.databinding.FragmentConvertProgressBinding


class ConvertProgressFragment : BaseFragment() {

    private val args by navArgs<ConvertProgressFragmentArgs>()

    override var bottomNavigationVisibility = View.GONE

    private var mInterstitialAd: InterstitialAd? = null
    private var TAG = "ConvertedFilesFragment"
    private var myAdUnitId = "ca-app-pub-9767087107670640/7400777402"
    private var originBtn: Int = 0

    private val binding by lazy {FragmentConvertProgressBinding.inflate(layoutInflater)}
    private lateinit var convertProgressViewModel: ConvertProgressViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val adRequest = AdRequest.Builder().build()
        Log.d(TAG, "Convert Progress ViewModel Before Creation")
        convertProgressViewModel = ViewModelProvider(this, ConvertProgressViewModelFactory(requireActivity().application, args.data.items, args.quality)).get(ConvertProgressViewModel::class.java)

        InterstitialAd.load(requireContext(),myAdUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, adError.message)
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                Log.d(TAG, "Ad was loaded.")
                mInterstitialAd = interstitialAd
                mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Ad was dismissed.")
                        if(originBtn == 1) {
                            originBtn = 0
                            findNavController().navigate(R.id.action_convertProgressFragment_to_home)
                        }else {
                            findNavController().navigate(R.id.action_convertProgressFragment_to_convertedFilesFragment)
                        }
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                        Log.d(TAG, "Ad failed to show.")
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "Ad showed fullscreen content.")
                        mInterstitialAd = null;
                    }
                }
            }
        })

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
                if(progress != null) {
                    binding.progressBarTextView.text = "${progress.toInt()}%"
                    binding.circularProgressBar.progress = progress.toFloat()
                }
                if(progress.toInt() == 100 && progress != null){
                    binding.circularProgressBar.apply {
                        progressBarColor = ContextCompat.getColor(requireActivity(), R.color.completeGreen)
                    }
                }
        })

        convertProgressViewModel.conversionFinished.observe(viewLifecycleOwner, { conversionFinished ->
            if(conversionFinished) {
                binding.cancelButton.visibility = View.GONE
                binding.convertedFileNameTextView.visibility = View.GONE
                binding.showConvertedFilesButton.visibility = View.VISIBLE
                binding.backHomeButton.visibility = View.VISIBLE
            }
        })

        binding.showConvertedFilesButton.setOnClickListener {
            binding.circularProgressBar.progress = 0f
            if (mInterstitialAd != null) {
                mInterstitialAd?.show(requireActivity())
            } else {
                Log.d(TAG, "The interstitial ad wasn't ready yet.")
                findNavController().navigate(R.id.action_convertProgressFragment_to_convertedFilesFragment)
            }
        }
        binding.backHomeButton.setOnClickListener {
            binding.circularProgressBar.progress = 0f
            if (mInterstitialAd != null) {
                originBtn = 1
                mInterstitialAd?.show(requireActivity())
            } else {
                Log.d(TAG, "The interstitial ad wasn't ready yet.")
                findNavController().navigate(R.id.action_convertProgressFragment_to_home)
            }
        }
        val data = args.data.items[0]
        val defaultFileExtension = Util.getFileExtension(data.specificFormat, data.defaultConvertFormat, data.convertAll)
        if (data.convertAll == true && defaultFileExtension == ".pdf" ) {
            convertProgressViewModel.createMultiPagePdf()
        }else {
            convertProgressViewModel.convertFiles()
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