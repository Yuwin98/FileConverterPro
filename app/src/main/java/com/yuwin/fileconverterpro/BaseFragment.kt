package com.yuwin.fileconverterpro

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment

abstract class BaseFragment: Fragment() {

    protected open var bottomNavigationVisibility = View.VISIBLE

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if(activity is MainActivity) {
            val mainActivity = activity as MainActivity
            mainActivity.setBottomNavigationViewVisibility(bottomNavigationVisibility)
        }
    }

    override fun onResume() {
        super.onResume()
        if(activity is  MainActivity) {
            val mainActivity = activity as MainActivity
            mainActivity.setBottomNavigationViewVisibility(bottomNavigationVisibility)
        }
    }



}