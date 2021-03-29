package com.yuwin.fileconverterpro


import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
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
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.yuwin.fileconverterpro.Constants.Companion.FREE_IMAGE_LIMIT
import com.yuwin.fileconverterpro.Constants.Companion.PREMIUM_IMAGE_LIMIT
import com.yuwin.fileconverterpro.Util.Companion.observeOnce
import com.yuwin.fileconverterpro.db.ConvertedFile
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.*


open class MainActivity : AppCompatActivity(), PurchasesUpdatedListener {

    private lateinit var weakRef: WeakReference<MainActivity>

    private var navController: NavController? = null
    private var bottomNavigationView: BottomNavigationView? = null
    private var imgLimit = FREE_IMAGE_LIMIT
    private var mInterstitialAd: InterstitialAd? = null
    private var myAdUnitId = ""
    private var adRequest: AdRequest? = null
    private lateinit var adView: AdView
    private lateinit var adViewContainer: FrameLayout
    private var initialLayoutComplete = false
    private var parentView: ConstraintLayout? = null

    private var tag = "MainActivity"

    private var mainViewModel: MainViewModel? = null

    private lateinit var manager: ReviewManager
    private lateinit var reviewInfo: ReviewInfo

    private var billingClient: BillingClient? = null

    var currentUserDirectory: String = ""
    var isCopyOperation = false
    lateinit var filesToModify: MutableList<ConvertedFile>

    private val adSize: AdSize
        get() {
            val display = windowManager.defaultDisplay
            val outMetrics = DisplayMetrics()
            display.getMetrics(outMetrics)

            val density = outMetrics.density

            var adWidthPixels = outMetrics.widthPixels.toFloat()
            if (adWidthPixels == 0f) {
                adWidthPixels = outMetrics.widthPixels.toFloat()
            }

            val adWidth = (adWidthPixels / density).toInt()
            return AdSize.getCurrentOrientationBannerAdSizeWithWidth(this, adWidth)
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_FileConverterPro)
        setContentView(R.layout.activity_main)

        Log.d("adnotworking", "OnCreate Called")


        supportActionBar?.setBackgroundDrawable(
            ColorDrawable(ContextCompat.getColor(this,R.color.navigationBarColor)))

        adRequest = AdRequest.Builder().build()
        currentUserDirectory =
            ContextCompat.getExternalFilesDirs(applicationContext, null)[0].absolutePath
        myAdUnitId = getString(R.string.appInterstitialAdId)
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
                adViewContainer = findViewById(R.id.bannerAdView)
                adViewContainer.visibility = View.GONE


            } else {
                this.imgLimit = FREE_IMAGE_LIMIT
                MobileAds.initialize(this)
                adViewContainer = findViewById(R.id.bannerAdView)
                adView = AdView(this)
                adViewContainer.addView(adView)
                adViewContainer.viewTreeObserver.addOnGlobalLayoutListener {
                    if (!initialLayoutComplete) {
                        initialLayoutComplete = true
                        loadBanner()
                    }
                }


            }
        })


        parentView = findViewById(R.id.mainConstraintLayout)
        manager = ReviewManagerFactory.create(this)


        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.dashboardFragment,
                R.id.files,
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun loadBanner() {
        adView.adUnitId = getString(R.string.appBannerAdId)
        adView.adSize = adSize
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    fun requestInterstitial() {
        mainViewModel?.readIsPremium?.observeOnce(this, { isPremium ->
            if (isPremium == 0) {
                val adRequest = AdRequest.Builder().build()
                createInterstitial(adRequest)
            }
        })
    }

    private fun createInterstitial(adRequest: AdRequest) {
        val fullScreenCallback: FullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
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
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                mInterstitialAd = interstitialAd
                mInterstitialAd?.fullScreenContentCallback = fullScreenCallback
            }
        }
        InterstitialAd.load(this, myAdUnitId, adRequest, adCallBack)
    }

    fun showInterstitial() {
        mainViewModel?.readIsPremium?.observeOnce(this, { isPremium ->
            if (mInterstitialAd != null && isPremium == 0) {
                mInterstitialAd?.show(this)
            }
        })

    }

    override fun onPause() {
        if(this::adView.isInitialized) {
            adView.pause()
        }
        super.onPause()

    }

    override fun onResume() {
        super.onResume()
        if (this::adView.isInitialized) {
            adView.resume()
        }
    }

    override fun onDestroy() {
        if(this::adView.isInitialized) {
            adView.destroy()
        }
        mainViewModel = null
        mInterstitialAd = null
        adRequest = null
        bottomNavigationView = null
        navController = null
        billingClient?.endConnection()
        billingClient = null
        super.onDestroy()
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

        if (navController?.currentDestination?.id == R.id.convert) {
            navController?.navigate(R.id.action_convert_to_dashboardFragment)
            return
        }

        if (navController?.currentDestination?.id == R.id.directoryViewFragment) {
            val parent = File(currentUserDirectory).parent
            if (parent != null) {
                currentUserDirectory = parent
            }
        }
        super.onBackPressed()
    }

    override fun onSupportNavigateUp(): Boolean {
        navController?.let {
            val parent = File(currentUserDirectory).parent
            if (parent != null) {
                currentUserDirectory = parent
            }
            return it.navigateUp() || super.onSupportNavigateUp()
        }
        return super.onSupportNavigateUp()
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
                R.id.files -> {
                    findNavController(R.id.navHostFragment).navigate(R.id.files)
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

                R.id.dashboard -> {
                    findNavController(R.id.navHostFragment).navigate(R.id.dashboardFragment)
                    return@OnNavigationItemSelectedListener true
                }
            }
            false
        }

    private fun promptReview() {
        mainViewModel?.readAppOpenedTimes?.observeOnce(this, { opened ->
            mainViewModel?.readReviewPrompted?.observeOnce(this, { prompted ->
                if (opened > 2 && !prompted) {
                    mainViewModel?.incrementAppOpenedTimes()
                    inAppReviewRequest()
                } else {
                    mainViewModel?.incrementAppOpenedTimes()
                }
            })

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

                    mainViewModel?.readIsPremium?.observeOnce(this, {
                        if (it == 0) {
                            mainViewModel?.setPremiumStatus(1)
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

