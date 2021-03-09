package com.yuwin.fileconverterpro.billing

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PurchasesUpdatedListener

class AppBillingClient {

    companion object {

        @Volatile private var instance: BillingClient? = null

        fun getInstance(context: Context, purchasesUpdatedListener: PurchasesUpdatedListener): BillingClient {
            return instance ?: synchronized(this) {
                instance ?: buildBillingClient(context.applicationContext, purchasesUpdatedListener)
            }
        }

        private fun buildBillingClient(
            context: Context,
            purchasesUpdatedListener: PurchasesUpdatedListener
        ): BillingClient {
            return BillingClient.newBuilder(context)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases()
                .build()
        }


    }

}