package app.picasso.tracer.ui.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.picasso.tracer.R
import app.picasso.tracer.domain.model.PicassoSettings
import app.picasso.tracer.domain.model.ThemeMode
import app.picasso.tracer.domain.repository.SettingsRepository
import app.picasso.tracer.monetization.Ads
import app.picasso.tracer.monetization.GoogleAuth
import app.picasso.tracer.monetization.ProBilling
import app.picasso.tracer.monetization.ProEvent
import app.picasso.tracer.monetization.SignInResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val googleAuth: GoogleAuth,
    private val proBilling: ProBilling,
    val ads: Ads,
) : ViewModel() {

    val adsReady: StateFlow<Boolean> = ads.ready

    fun ensureAdsConsent(activity: Activity) {
        ads.ensureConsent(activity)
    }

    init {
        proBilling.connect()
        viewModelScope.launch {
            proBilling.events.collect { event ->
                when (event) {
                    ProEvent.Purchased -> _errorRes.value = R.string.pro_welcome
                    ProEvent.Restored -> _errorRes.value = R.string.pro_restored
                    ProEvent.Failed -> _errorRes.value = R.string.pro_failed
                    ProEvent.Unavailable -> _errorRes.value = R.string.pro_unavailable
                    ProEvent.Cancelled -> Unit
                }
            }
        }
    }

    private val _proPrice = MutableStateFlow<String?>(null)
    val proPrice: StateFlow<String?> = _proPrice.asStateFlow()

    fun loadProPrice() {
        if (_proPrice.value != null) return
        viewModelScope.launch { _proPrice.value = proBilling.proPrice() }
    }

    fun buyPro(activity: Activity) {
        proBilling.buyPro(activity)
    }

    fun restorePro() {
        proBilling.restore()
    }

    private val _errorRes = MutableStateFlow<Int?>(null)
    val errorRes: StateFlow<Int?> = _errorRes.asStateFlow()

    fun clearError() {
        _errorRes.value = null
    }

    val settings: StateFlow<PicassoSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PicassoSettings())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setDefaultOpacity(opacity: Float) {
        viewModelScope.launch { repository.setDefaultOpacity(opacity) }
    }

    fun setDefaultGrid(rows: Int, cols: Int) {
        viewModelScope.launch { repository.setDefaultGrid(rows, cols) }
    }

    fun signIn(activity: Activity) {
        viewModelScope.launch {
            when (val result = googleAuth.signIn(activity)) {
                is SignInResult.SignedIn -> {
                    repository.setAccount(result.account.email, result.account.name)
                }
                SignInResult.NotConfigured -> _errorRes.value = R.string.sign_in_not_configured
                SignInResult.Failed -> _errorRes.value = R.string.sign_in_failed
                SignInResult.Cancelled -> Unit
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            googleAuth.signOut()
            repository.clearAccount()
        }
    }

    /**
     * Account deletion (Play requirement for apps with login). The account is
     * local-only, so this erases the stored profile; tracings stay on the
     * device as files until the user clears app data (stated in the policy).
     */
    fun deleteAccount() {
        viewModelScope.launch {
            googleAuth.signOut()
            repository.clearAccount()
        }
    }
}
