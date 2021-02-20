package com.yuwin.fileconverterpro

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.navArgs
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.mikhaellopez.circularprogressbar.CircularProgressBar


class ConvertProgressFragment : Fragment() {

    private val args by navArgs<ConvertProgressFragmentArgs>()

    lateinit var circularProgressBar: CircularProgressBar
    lateinit var progressText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(requireActivity())

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_convert_progress, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val data = args.data.items[0]
        Log.d("debug", "Uri: ${data.uri}")
        Log.d("debug", "File Name: ${data.fileName}")
        Log.d("debug", "File Path: ${data.filePath}")
        Log.d("debug", "File Size: ${data.fileSize}")
        Log.d("debug", "File Type: ${data.fileType}")
        Log.d("debug", "Default Convert Types: ${FormatTypes.values()[data.defaultConvertFormat!!]}")
        Log.d("debug", "Specific Convert Types: ${FormatTypes.values()[data.specificConvertFormat!!]}")
        Log.d("debug", "Convert All: ${data.convertAll.toString()}")


        val mAdView: AdView = view.findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        progressText = view.findViewById(R.id.progressBarTextView)
        circularProgressBar = view.findViewById(R.id.circularProgressBar)
        circularProgressBar.let {
            circularProgressBar.apply {
                progressMax = 100f
                setProgressWithAnimation(100f, 10000)
                progressBarColor = ContextCompat.getColor(requireActivity(), R.color.colorPrimary)
                backgroundProgressBarColor = ContextCompat.getColor(requireActivity(), R.color.cardBackground)
                progressBarWidth = 10f // in DP
                backgroundProgressBarWidth = 12f // in DP
                roundBorder = true
            }
        }

        circularProgressBar.onProgressChangeListener = {progress ->
            progressText.text = "${progress.toInt()}%"
            if(progress == 100f){
                circularProgressBar.apply {
                    progressBarColor = ContextCompat.getColor(requireActivity(), R.color.completeGreen)
                }
            }
        }
    }


}