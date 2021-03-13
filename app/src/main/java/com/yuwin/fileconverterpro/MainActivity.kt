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
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.*
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.yuwin.fileconverterpro.Constants.Companion.FREE_IMAGE_LIMIT
import com.yuwin.fileconverterpro.Constants.Companion.PREMIUM_IMAGE_LIMIT
import com.yuwin.fileconverterpro.Util.Companion.observeOnce
import com.yuwin.fileconverterpro.db.ConvertedFile
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.*

private const val MEDIA_LOCATION_PERMISSION_REQUEST_CODE = 999
private const val IMAGE_REQUEST_CODE = 200
private const val PDF_REQUEST_CODE = 500


open class MainActivity : AppCompatActivity(), PurchasesUpdatedListener {

    private lateinit var weakRef: WeakReference<MainActivity>

    private var navController: NavController? = null
    private var bottomNavigationView: BottomNavigationView? = null
    private var imgLimit = FREE_IMAGE_LIMIT
    private var mInterstitialAd: InterstitialAd? = null
    private var adRequest: AdRequest? = null
    private var myAdUnitId = ""
    private var mAdView: AdView? = null
    private var parentView: ConstraintLayout? = null

    private var tag = "MainActivity"

    private var mainViewModel: MainViewModel? = null

    private lateinit var manager: ReviewManager
    private lateinit var reviewInfo: ReviewInfo

    private var billingClient: BillingClient? = null

    var currentUserDirectory: String = ""
    var isCopyOperation = false
    lateinit var filesToModify: MutableList<ConvertedFile>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_FileConverterPro)
        setContentView(R.layout.activity_main)
        currentUserDirectory =
            ContextCompat.getExternalFilesDirs(applicationContext, null)[0].absolutePath
        myAdUnitId = getString(R.string.appInterstitalAdId)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController
        mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        weakRef = WeakReference<MainActivity>(this)


        billingClient =
            newBuilder(weakRef.get()!!.applicationContext).enablePendingPurchases()
                .setListener(this)
                .build()

        billingClient?.let {
            it.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {

                    if (billingResult.responseCode == BillingResponseCode.OK) {
                        val queryPurchase = it.queryPurchases(SkuType.SUBS)
                        val queryPurchases = queryPurchase.purchasesList
                        if (queryPurchase != null && queryPurchases?.size!! > 0) {
                            handlePurchases(queryPurchases)
                        } else {
                            mainViewModel?.setPremiumStatus(0)
                        }
                    }


                }

                override fun onBillingServiceDisconnected() {
                    Toast.makeText(applicationContext, "Service Disconnected", Toast.LENGTH_SHORT)
                        .show()
                }

            })
        }


        mainViewModel?.readIsPremium?.observeOnce(this, { isPremium ->
            if (isPremium == 1) {
                this.imgLimit = PREMIUM_IMAGE_LIMIT
                mAdView = findViewById(R.id.bannerAdView)
                mAdView?.visibility = View.GONE


            } else {
                this.imgLimit = FREE_IMAGE_LIMIT
                MobileAds.initialize(this)
                mAdView = findViewById(R.id.bannerAdView)
                mAdView?.visibility = View.VISIBLE
                adRequest = AdRequest.Builder().build()
                mAdView?.loadAd(adRequest)
            }
        })


        parentView = findViewById(R.id.mainConstraintLayout)
        manager = ReviewManagerFactory.create(this)


        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.home,
                R.id.settings,
                R.id.favorite,
                R.id.convert,
                R.id.convertProgressFragment,
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
            if (!it) {
                promptReview()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity1", "On Destroy")
        mainViewModel = null
        mInterstitialAd = null
        adRequest = null
        bottomNavigationView = null
        navController = null
        parentView?.removeView(mAdView)
        parentView?.removeAllViewsInLayout()
        mAdView?.destroy()
        mAdView = null
        parentView = null
        billingClient?.endConnection()
        billingClient = null
    }

    override fun onBackPressed() {
        if (navController?.currentDestination?.id == R.id.convertProgressFragment) {
            navController?.navigate(R.id.action_convertProgressFragment_to_home)
            mainViewModel?.readIsPremium?.observeOnce(this, { isPremium ->
                if (isPremium == 0) {
                    showInterstitial()
                }
            })
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
                when (which) {
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

    // Subscriptions Start

    fun subscribe() {
        if (billingClient?.isReady == true) {
            initiatePurchase()
        } else {
            billingClient = newBuilder(this).enablePendingPurchases().setListener(this).build()
            billingClient?.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingResponseCode.OK) {
                        initiatePurchase()
                    } else {
                        Toast.makeText(
                            applicationContext,
                            "Error ${billingResult.debugMessage}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Toast.makeText(
                        applicationContext,
                        "Disconnected from server",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            })

        }
    }

    private fun initiatePurchase() {
        val skuList = mutableListOf<String>()
        skuList.add(getString(R.string.appPremiumSku))
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(SkuType.SUBS)
        val billingResultSupported = billingClient!!.isFeatureSupported(FeatureType.SUBSCRIPTIONS)
        if (billingResultSupported.responseCode == BillingResponseCode.OK) {
            billingClient!!.querySkuDetailsAsync(params.build()) { billingResult, skuDetailList ->
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    if (skuDetailList != null && skuDetailList.size > 0) {
                        val flowParams = BillingFlowParams.newBuilder()
                            .setSkuDetails(skuDetailList[0])
                            .build()
                        billingClient!!.launchBillingFlow(this, flowParams)
                    } else {
                        Toast.makeText(applicationContext, "Item not found", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    Toast.makeText(
                        applicationContext,
                        "Error ${billingResult.debugMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            Toast.makeText(
                applicationContext,
                "Subscription not supported on device",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Subscription End


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
            if (it == 2) {
                inAppReviewRequest()
                mainViewModel?.incrementAppOpenedTimes()
            } else {
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
        flow.addOnCompleteListener {
            mainViewModel?.setReviewPrompted(true)
            Log.d("AppReviewFC", "App review Completed")

        }
    }


    fun requestInterstitial() {
        Log.d("showInterstitial", "Interstitial requested")

            mainViewModel?.readIsPremium?.observeOnce(this, { isPremium ->
            if (isPremium == 0) {
                this.adRequest?.let { createInterstitial(it) }
            }
        })
    }

    private fun createInterstitial(adRequest: AdRequest) {
        Log.d("showInterstitial", "Interstitial create")
        val fullScreenCallback: FullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                mInterstitialAd = null
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
            }

            override fun onAdShowedFullScreenContent() {
                mInterstitialAd = null
            }
        }
        val adCallBack: InterstitialAdLoadCallback = object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(tag, adError.message)
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                mInterstitialAd = interstitialAd
                mInterstitialAd?.fullScreenContentCallback = fullScreenCallback
            }
        }
        InterstitialAd.load(this, myAdUnitId, adRequest, adCallBack)
    }

    fun showInterstitial() {
        Log.d("showInterstitial", "Interstitial Shown")
        mainViewModel?.readIsPremium?.observe(this, { isPremium ->
            if (mInterstitialAd != null && isPremium == 0) {
                mInterstitialAd?.show(this)
            }
        })

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
            arrayOf(
                android.Manifest.permission.ACCESS_MEDIA_LOCATION,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ),
            MEDIA_LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            MEDIA_LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    chooseImages()
                } else {
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

            if (requestCode == PDF_REQUEST_CODE) {
                imgLimit = 2
            }

            if (uriList.size > imgLimit) {
                uriList = uriList.take(imgLimit).toMutableList()
                Toast.makeText(this, "Max Convert Limit $imgLimit reached", Toast.LENGTH_SHORT)
                    .show()
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


            }
            findNavController(R.id.navHostFragment).navigate(action)
        } else if (requestCode == IMAGE_REQUEST_CODE || requestCode == PDF_REQUEST_CODE) {
            findNavController(R.id.navHostFragment).navigate(R.id.home)
        }
    }

    fun setBottomNavigationViewVisibility(visibility: Int) {
        bottomNavigationView?.visibility = visibility
    }


    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {

        if (billingResult.responseCode == BillingResponseCode.OK && purchases != null) {
            handlePurchases(purchases)
        } else if (billingResult.responseCode == BillingResponseCode.ITEM_ALREADY_OWNED) {
            val queryAlreadyPurchasedResult = billingClient!!.queryPurchases(SkuType.SUBS)
            val alreadyPurchased = queryAlreadyPurchasedResult.purchasesList
            alreadyPurchased?.let { handlePurchases(it) }
        } else if (billingResult.responseCode == BillingResponseCode.USER_CANCELED) {
            Toast.makeText(applicationContext, "Purchase Cancelled", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                applicationContext,
                "Error ${billingResult.debugMessage}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun handlePurchases(purchases: MutableList<Purchase>?) {

        purchases?.forEach { purchase ->

            if (getString(R.string.appPremiumSku) == purchase.sku
                && purchase.purchaseState == Purchase.PurchaseState.PURCHASED
            ) {

                if (!verifySignature(purchase.originalJson, purchase.signature)) {

                    Toast.makeText(
                        applicationContext,
                        "Error: Invalid Purchase",
                        Toast.LENGTH_SHORT
                    ).show()
                    return

                }

                if (!purchase.isAcknowledged) {
                    val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()

                    billingClient!!.acknowledgePurchase(
                        acknowledgePurchaseParams,
                        purchaseAcknowledgement
                    )
                } else {

                    mainViewModel?.readIsPremium?.observe(this, {
                        if (it == 0) {
                            mainViewModel?.setPremiumStatus(1)
                            Toast.makeText(applicationContext, "Item Purchased", Toast.LENGTH_SHORT)
                                .show()
                            recreate()
                        }
                    })

                }


            } else if (getString(R.string.appPremiumSku) == purchase.sku
                && purchase.purchaseState == Purchase.PurchaseState.PENDING
            ) {
                Toast.makeText(
                    applicationContext,
                    "Purchase is pending: Please Complete transaction",
                    Toast.LENGTH_SHORT
                ).show()

            } else if (getString(R.string.appPremiumSku) == purchase.sku
                && purchase.purchaseState == Purchase.PurchaseState.UNSPECIFIED_STATE
            ) {
                mainViewModel?.setPremiumStatus(0)
                Toast.makeText(applicationContext, "Purchase State Unknown", Toast.LENGTH_SHORT)
                    .show()
            }


        }

    }

    private val purchaseAcknowledgement = AcknowledgePurchaseResponseListener { billingResult ->
        if (billingResult.responseCode == BillingResponseCode.OK) {
            mainViewModel?.setPremiumStatus(1)
            recreate()
        }
    }

    private fun verifySignature(signedData: String, signature: String): Boolean {
        return try {
            val privateBase64Key = getString(R.string.base64Key)
            Security.verifyPurchase(privateBase64Key, signedData, signature)

        } catch (e: IOException) {
            false
        }
    }


}

