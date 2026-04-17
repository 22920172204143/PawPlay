package com.pawhunt.app.service

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdManager(private val context: Context) {

    companion object {
        const val BANNER_AD_UNIT = "ca-app-pub-3940256099942544/6300978111"
        const val INTERSTITIAL_AD_UNIT = "ca-app-pub-3940256099942544/1033173712"
        const val INTERSTITIAL_FREQUENCY = 3
    }

    private var interstitialAd: InterstitialAd? = null
    private val _isAdReady = MutableStateFlow(false)
    val isAdReady = _isAdReady.asStateFlow()

    fun initialize() {
        MobileAds.initialize(context) {}
        loadInterstitial()
    }

    fun createBannerAdView(): AdView {
        return AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = BANNER_AD_UNIT
            loadAd(AdRequest.Builder().build())
        }
    }

    private fun loadInterstitial() {
        val request = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT,
            request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    _isAdReady.value = true

                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            interstitialAd = null
                            _isAdReady.value = false
                            loadInterstitial()
                        }

                        override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                            interstitialAd = null
                            _isAdReady.value = false
                            loadInterstitial()
                        }
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    _isAdReady.value = false
                }
            }
        )
    }

    fun showInterstitialIfReady(activity: Activity, gameExitCount: Int): Boolean {
        if (gameExitCount % INTERSTITIAL_FREQUENCY != 0) return false
        val ad = interstitialAd ?: return false
        ad.show(activity)
        return true
    }

    fun destroy() {
        interstitialAd = null
    }
}
