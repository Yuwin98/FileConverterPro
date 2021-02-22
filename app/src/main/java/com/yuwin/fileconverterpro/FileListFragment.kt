package com.yuwin.fileconverterpro


import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.yuwin.fileconverterpro.databinding.FragmentMainScreenBinding


class FileListFragment : Fragment() {

    private val binding by lazy{ FragmentMainScreenBinding.inflate(layoutInflater) }

    private lateinit var mAdView: AdView



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)

        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mAdView = view.findViewById(R.id.mainScreenAdView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.home_action_menu, menu)
    }








}