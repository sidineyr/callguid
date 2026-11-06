package com.sidineyr.callguide

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import com.google.android.gms.ads.*
import com.google.android.ump.*

/** Adult-only ad requests. Debug always uses Google's official demonstration unit. */
class AdsController(private val activity: AppCompatActivity) {
    private var adult = false
    private var ready = false
    private var adView: AdView? = null
    private val consent by lazy { UserMessagingPlatform.getConsentInformation(activity) }
    fun setAdult(value: Boolean) {
        adult = value && BuildConfig.ADS_ENABLED
        if (!adult) return
        val parameters = ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(false).build()
        consent.requestConsentInfoUpdate(activity, parameters, {
            if (activity.isFinishing || activity.isDestroyed || !activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return@requestConsentInfoUpdate
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ -> initializeIfAllowed() }
        }, { _ -> ready = false })
    }
    private fun initializeIfAllowed() {
        if (!adult || !consent.canRequestAds() || activity.isDestroyed) return
        MobileAds.setRequestConfiguration(RequestConfiguration.Builder()
            .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE)
            .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
            .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G).build())
        MobileAds.initialize(activity) { ready = adult && consent.canRequestAds() }
    }
    fun canShow() = BuildConfig.ADS_ENABLED && adult && ready && consent.canRequestAds()
    fun attach(container: LinearLayout) {
        if (!canShow()) return
        val view = AdView(activity).apply {
            adUnitId = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else BuildConfig.ADMOB_BANNER_ID
        }
        adView = view
        container.addView(view, LinearLayout.LayoutParams(-1, -2))
        container.post {
            if (!container.isAttachedToWindow || adView !== view || !canShow()) return@post
            val widthDp = (container.width / activity.resources.displayMetrics.density).toInt().coerceAtLeast(1)
            view.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, widthDp))
            val extras = Bundle().apply { putString("npa", "1") }
            view.loadAd(AdRequest.Builder().addNetworkExtrasBundle(com.google.ads.mediation.admob.AdMobAdapter::class.java, extras).build())
        }
    }
    fun privacyOptions() {
        if (!adult) {
            Toast.makeText(activity, "Não há anúncios para esta faixa etária.", Toast.LENGTH_LONG).show(); return
        }
        if (consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED) {
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ ->
                ready = consent.canRequestAds()
                destroyView()
            }
        } else Toast.makeText(activity, "Nenhuma opção adicional exigida nesta sessão.", Toast.LENGTH_LONG).show()
    }
    fun pause() { adView?.pause() }
    fun resume() { adView?.resume() }
    fun destroyView() { adView?.destroy(); adView = null }
}
