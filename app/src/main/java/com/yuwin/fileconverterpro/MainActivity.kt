package com.yuwin.fileconverterpro

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var bottomNavigationView: BottomNavigationView
    private val IMAGE_LIMIT = 50
    private var mInterstitialAd: InterstitialAd? = null
    private val myAdUnitId = "ca-app-pub-9767087107670640/7400777402"

    private var TAG = "MainActivity"


    private val onNavigationViewSelectedItemListener =
        BottomNavigationView.OnNavigationItemSelectedListener { menuItem ->

            when (menuItem.itemId) {
                R.id.home -> {
                    findNavController(R.id.navHostFragment).navigate(R.id.home)
                    return@OnNavigationItemSelectedListener true
                }
                R.id.settings -> {
                    findNavController(R.id.navHostFragment).navigate(R.id.settings)
                    return@OnNavigationItemSelectedListener true
                }
                R.id.favorites -> {
                    findNavController(R.id.navHostFragment).navigate(R.id.favorite)
                    return@OnNavigationItemSelectedListener true
                }

                R.id.chooseImage -> {
                    chooseImages()
                    return@OnNavigationItemSelectedListener true
                }
            }
            false
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val testDeviceIds = listOf("8E8E9F036820B3A24447A0A1B4D2F2DF")
        val config = RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()

        MobileAds.setRequestConfiguration(config)
        MobileAds.initialize(this)

        requestInterstitial()

        val adRequest = AdRequest.Builder().build()
        val mAdView: AdView = findViewById(R.id.bannerAdView)
        mAdView.loadAd(adRequest)




        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        navController = findNavController(R.id.navHostFragment)

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.home,
                R.id.settings,
                R.id.favorite,
                R.id.convert,
                R.id.convertProgressFragment
            )
        )

        bottomNavigationView.setupWithNavController(navController)
        bottomNavigationView.setOnNavigationItemSelectedListener(
            onNavigationViewSelectedItemListener
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

    }


    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    fun requestInterstitial() {
        val adRequest = AdRequest.Builder().build()
        createInterstitial(adRequest)
    }

    private fun createInterstitial(adRequest: AdRequest) {

        val fullScreenCallback: FullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad was dismissed.")
                navController.navigate(R.id.action_convertProgressFragment_to_home)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                Log.d(TAG, "Ad failed to show.")
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad showed fullscreen content.")
                mInterstitialAd = null
            }
        }
        val adCallBack: InterstitialAdLoadCallback = object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, adError.message)
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                Log.d(TAG, "Ad was loaded.")
                mInterstitialAd = interstitialAd
                mInterstitialAd?.fullScreenContentCallback = fullScreenCallback
            }
        }
        InterstitialAd.load(this, myAdUnitId, adRequest, adCallBack)
    }

    fun showInterstitial() {
        if(mInterstitialAd != null) {
            mInterstitialAd?.show(this)
        }else {
            navController.navigate(R.id.action_convertProgressFragment_to_home)
        }
    }


    private fun chooseImages() {
        val intent = Intent()
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Images"), 200)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        var uriList = mutableListOf<Uri>()
        if (data != null && requestCode == 200 && resultCode == Activity.RESULT_OK) {
            if (data.clipData != null) {
                val count = data.clipData!!.itemCount
                for (i in 0 until count) {
                    val imageUri = data.clipData!!.getItemAt(i).uri
                    uriList.add(imageUri)
                }
            } else {
                val imageUri = data.data!!
                uriList.add(imageUri)
            }

            if (uriList.size > IMAGE_LIMIT) {
                uriList = uriList.take(IMAGE_LIMIT).toMutableList()
                Toast.makeText(this, "Max Convert Limit $IMAGE_LIMIT", Toast.LENGTH_SHORT).show()
            }

            val newUriList = UriList(uriList)


            var action = FileListFragmentDirections.actionHomeToConvert(newUriList)

            when (navController.currentDestination?.id) {
                R.id.convert -> {
                    action = ConvertFragmentDirections.actionConvertSelf(newUriList)
                }
                R.id.settings -> {
                    action = SettingsFragmentDirections.actionSettingsToConvert(newUriList)
                }
                R.id.favorite -> {
                    action = FavoriteFragmentDirections.actionInfoToConvert(newUriList)
                }
                R.id.convertProgressFragment -> {
                    action =
                        ConvertProgressFragmentDirections.actionConvertProgressFragmentToConvert(
                            newUriList
                        )
                }

            }
            findNavController(R.id.navHostFragment).navigate(action)
        } else if (requestCode == 200) {
            findNavController(R.id.navHostFragment).navigate(R.id.home)
        }
    }

    fun setBottomNavigationViewVisibility(visibility: Int) {
        bottomNavigationView.visibility = visibility
    }
}