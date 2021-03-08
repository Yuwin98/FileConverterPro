package com.yuwin.fileconverterpro

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.yuwin.fileconverterpro.Util.Companion.observeOnce
import java.util.*

private const val MEDIA_LOCATION_PERMISSION_REQUEST_CODE = 999
private const val IMAGE_REQUEST_CODE = 200
private const val PDF_REQUEST_CODE = 500

class MainActivity : AppCompatActivity() {

    private var navController: NavController? = null
    private var bottomNavigationView: BottomNavigationView? = null
    private val IMAGE_LIMIT = 50
    private var mInterstitialAd: InterstitialAd? = null
    private var adRequest: AdRequest? = null
    private val myAdUnitId = "ca-app-pub-3940256099942544/1033173712"
    private var mAdView: AdView? = null
    private var parentView: ConstraintLayout? = null

    private var TAG = "MainActivity"

    private var mainViewModel: MainViewModel? = null

    private lateinit var manager: ReviewManager
    private lateinit var reviewInfo: ReviewInfo



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_FileConverterPro)
        setContentView(R.layout.activity_main)
        Log.d("MainActivity1", "On Create")
        parentView = findViewById(R.id.mainConstraintLayout)
        mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        setCurrentSettings()
        manager = ReviewManagerFactory.create(this)

        MobileAds.initialize(this)
        mAdView = findViewById(R.id.bannerAdView)
        adRequest = AdRequest.Builder().build()
        requestInterstitial()

        mAdView?.loadAd(adRequest)


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

        navController?.let { navController ->
            bottomNavigationView?.setupWithNavController(navController)
            bottomNavigationView?.setOnNavigationItemSelectedListener(
                onNavigationViewSelectedItemListener
            )
            setupActionBarWithNavController(navController, appBarConfiguration)
        }

        mainViewModel?.readReviewPrompted?.observeOnce(this, {
            if(!it) {
                promptReview()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity1", "On Destroy")
        mainViewModel = null
        mInterstitialAd  = null
        adRequest = null
        bottomNavigationView = null
        navController = null
        parentView?.removeView(mAdView)
        parentView?.removeAllViewsInLayout()
        mAdView?.destroy()
        mAdView = null
        parentView = null
    }

    override fun onBackPressed() {
        if(navController?.currentDestination?.id == R.id.convertProgressFragment) {
            navController?.navigate(R.id.action_convertProgressFragment_to_home)
            showInterstitial()
            return
        }
        super.onBackPressed()
    }

    override fun onSupportNavigateUp(): Boolean {
        navController?.let {
            return it.navigateUp() || super.onSupportNavigateUp()
        }
        return super.onSupportNavigateUp()
    }

    private fun convertChoices() {
        val items = arrayOf("Image Conversion", "PDF Conversion")
        MaterialAlertDialogBuilder(this)
            .setTitle("Converstion Type")
            .setItems(items) { dialog, which ->
               when(which) {
                   0 -> {
                        chooseImageIfPermissionGranted()
                       dialog.dismiss()
                   }
                   1 -> {
                       choosePdfIfPermissionGranted()
                       dialog.dismiss()
                   }
               }
            }
            .show()

    }

    private var onNavigationViewSelectedItemListener =
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
                    convertChoices()
                    return@OnNavigationItemSelectedListener true
                }
            }
            false
        }

    private fun promptReview() {
        mainViewModel?.readAppOpenedTimes?.observeOnce(this, {
            if(it == 2) {
                inAppReviewRequest()
                mainViewModel?.incrementAppOpenedTimes()
            }else {
                mainViewModel?.incrementAppOpenedTimes()
            }
        })
    }

    private fun inAppReviewRequest() {
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { req ->
            if (req.isSuccessful) {
                reviewInfo = req.result
                startInAppReview(reviewInfo)
            }
        }
    }

    private fun startInAppReview(reviewInfo: ReviewInfo) {
        val flow = manager.launchReviewFlow(this, reviewInfo)
        flow.addOnCompleteListener { _ ->
            mainViewModel?.setReviewPrompted(true)
            Log.d("AppReviewFC", "App review Completed")

        }
    }


    fun requestInterstitial() {
        val adRequest = AdRequest.Builder().build()
        createInterstitial(adRequest)
    }

    private fun createInterstitial(adRequest: AdRequest) {

        val fullScreenCallback: FullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad was dismissed.")
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
        }
    }




    private fun choosePdf() {
        val intent = Intent()
        intent.type = "application/pdf"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.action = Intent.ACTION_GET_CONTENT
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        startActivityForResult(Intent.createChooser(intent, "Select PDF"), PDF_REQUEST_CODE)
    }

    private fun chooseImages() {
        val intent = Intent()
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Images"), IMAGE_REQUEST_CODE)
    }

    private fun chooseImageIfPermissionGranted() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            chooseImages()
        } else {
            if (isTherePermissionForMediaAccess(this)) {
                chooseImages()
            } else {
                requestPermissionForMediaAccess(this)
            }
        }
    }

    private fun choosePdfIfPermissionGranted() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
           choosePdf()
        } else {
            if (isTherePermissionForMediaAccess(this)) {
                choosePdf()
            } else {
                requestPermissionForMediaAccess(this)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun isTherePermissionForMediaAccess(context: Context): Boolean {
        val permission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_MEDIA_LOCATION
        )
        return permission == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestPermissionForMediaAccess(context: Context) {
        ActivityCompat.requestPermissions(
            context as Activity,
            arrayOf(android.Manifest.permission.ACCESS_MEDIA_LOCATION,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE),
            MEDIA_LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode){
            MEDIA_LOCATION_PERMISSION_REQUEST_CODE -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    chooseImages()
                }else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        var uriList = mutableListOf<Uri>()
        if (data != null && (requestCode == IMAGE_REQUEST_CODE || requestCode == PDF_REQUEST_CODE) && resultCode == Activity.RESULT_OK) {
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
                Toast.makeText(this, "Max Convert Limit $IMAGE_LIMIT reached", Toast.LENGTH_SHORT).show()
            }

            val newUriList = UriList(uriList)


            var action = FileListFragmentDirections.actionHomeToConvert(newUriList)

            when (navController?.currentDestination?.id) {
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
        } else if (requestCode == IMAGE_REQUEST_CODE || requestCode == PDF_REQUEST_CODE) {
            findNavController(R.id.navHostFragment).navigate(R.id.home)
        }
    }

    fun setBottomNavigationViewVisibility(visibility: Int) {
        bottomNavigationView?.visibility = visibility
    }

    private fun setCurrentSettings() {

    }




}