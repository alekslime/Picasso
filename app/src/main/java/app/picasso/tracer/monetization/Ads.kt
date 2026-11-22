package app.picasso.tracer.monetization

import android.app.Activity
import android.content.Context
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Consent-first AdMob setup. The UMP form is required in the EEA/UK and
 * harmless elsewhere; ads only load after [canRequestAds] is true, and the
 * trace camera never shows ads at all.
 */
@Singleton
class Ads @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private var started = false

    /** Shows the UMP consent form when required, then initializes AdMob. */
    fun ensureConsent(activity: Activity) {
        if (started) return
        started = true
        val consent = UserMessagingPlatform.getConsentInformation(context)
        consent.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    publishReadiness(consent)
                }
            },
            { publishReadiness(consent) },
        )
    }

    private fun publishReadiness(consent: com.google.android.ump.ConsentInformation) {
        if (consent.canRequestAds()) {
            MobileAds.initialize(context) {}
        }
        _ready.value = consent.canRequestAds()
    }

    /** Non-personalized request; consent state is already enforced via [ready]. */
    fun adRequest(): AdRequest = AdRequest.Builder()
        .addNetworkExtrasBundle(AdMobAdapter::class.java, android.os.Bundle())
        .build()
}
