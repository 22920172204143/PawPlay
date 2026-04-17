package com.pawhunt.app.service

import android.app.Activity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class BillingManager(private val activity: Activity) : PurchasesUpdatedListener {

    companion object {
        const val PRODUCT_PRO = "pawplay_pro"
        const val PRODUCT_YEARLY = "pawplay_yearly"
    }

    private var billingClient: BillingClient? = null
    private var proProductDetails: ProductDetails? = null
    private var yearlyProductDetails: ProductDetails? = null

    private val _isPro = MutableStateFlow(false)
    val isPro = _isPro.asStateFlow()

    private val _proPrice = MutableStateFlow("¥38")
    val proPrice = _proPrice.asStateFlow()

    private val _yearlyPrice = MutableStateFlow("¥68/year")
    val yearlyPrice = _yearlyPrice.asStateFlow()

    fun init() {
        billingClient = BillingClient.newBuilder(activity)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProducts()
                    queryExistingPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Retry on next user action
            }
        })
    }

    private fun queryProducts() {
        val client = billingClient ?: return

        val inAppProduct = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_PRO)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        val subsProduct = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_YEARLY)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(inAppProduct, subsProduct))
            .build()

        client.queryProductDetailsAsync(params) { result, productDetailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                for (details in productDetailsList) {
                    when (details.productId) {
                        PRODUCT_PRO -> {
                            proProductDetails = details
                            details.oneTimePurchaseOfferDetails?.formattedPrice?.let {
                                _proPrice.value = it
                            }
                        }
                        PRODUCT_YEARLY -> {
                            yearlyProductDetails = details
                            details.subscriptionOfferDetails?.firstOrNull()
                                ?.pricingPhases?.pricingPhaseList?.firstOrNull()
                                ?.formattedPrice?.let {
                                    _yearlyPrice.value = "$it/year"
                                }
                        }
                    }
                }
            }
        }
    }

    private fun queryExistingPurchases() {
        val client = billingClient ?: return

        val inAppParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        client.queryPurchasesAsync(inAppParams) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                checkPurchases(purchases)
            }
        }

        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        client.queryPurchasesAsync(subsParams) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                checkPurchases(purchases)
            }
        }
    }

    private fun checkPurchases(purchases: List<Purchase>) {
        for (purchase in purchases) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                _isPro.value = true
                if (!purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                }
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val client = billingClient ?: return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        client.acknowledgePurchase(params) { /* acknowledged */ }
    }

    fun launchProPurchase() {
        val details = proProductDetails ?: return
        val client = billingClient ?: return

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            )
            .build()

        client.launchBillingFlow(activity, params)
    }

    fun launchYearlyPurchase() {
        val details = yearlyProductDetails ?: return
        val client = billingClient ?: return
        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()

        client.launchBillingFlow(activity, params)
    }

    fun restorePurchases() {
        queryExistingPurchases()
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            checkPurchases(purchases)
        }
    }

    fun destroy() {
        billingClient?.endConnection()
        billingClient = null
    }
}
