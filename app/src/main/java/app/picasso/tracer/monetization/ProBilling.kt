package app.picasso.tracer.monetization

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

sealed interface ProEvent {
    data object Purchased : ProEvent
    data object Restored : ProEvent
    data object Cancelled : ProEvent
    data object Failed : ProEvent
    data object Unavailable : ProEvent
}

/**
 * One-time "Picasso Pro" purchase. The product ID lives in
 * res/values/monetization.xml and must exist in Play Console before it can
 * sell; every call degrades to [ProEvent.Unavailable] without Play Store.
 *
 * Public surface never exposes Billing types so Hilt's processors never read
 * their newer Kotlin metadata — everything Billing stays in method bodies.
 */
@Singleton
class ProBilling @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: app.picasso.tracer.domain.repository.SettingsRepository,
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var client: BillingClient? = null

    private val _events = MutableSharedFlow<ProEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ProEvent> = _events.asSharedFlow()

    fun productId(): String =
        context.getString(app.picasso.tracer.R.string.pro_product_id)

    /** Connects (idempotent) and restores Pro from already-owned purchases. */
    fun connect() {
        val existing = client
        if (existing != null && existing.isReady) {
            scope.launch { restore() }
            return
        }
        val built = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build(),
            )
            .build()
        client = built
        built.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch { restore() }
                } else {
                    _events.tryEmit(ProEvent.Unavailable)
                }
            }

            override fun onBillingServiceDisconnected() {
                // Next buyPro/restore reconnects; no retry loop by design.
            }
        })
    }

    /** Human price for the Pro row, or null when Play is unreachable. */
    suspend fun proPrice(): String? {
        val details = queryProDetails() ?: return null
        return details.oneTimePurchaseOfferDetails?.formattedPrice
    }

    fun buyPro(activity: Activity) {
        scope.launch {
            val details = queryProDetails()
            if (details == null) {
                _events.emit(ProEvent.Unavailable)
                return@launch
            }
            val params = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(details)
                            .build(),
                    ),
                )
                .build()
            val result = client?.launchBillingFlow(activity, params)
            if (result?.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                _events.emit(ProEvent.Cancelled)
            } else if (result?.responseCode != BillingClient.BillingResponseCode.OK) {
                _events.emit(ProEvent.Failed)
            }
        }
    }

    fun restore() {
        scope.launch {
            val owned = queryOwnedPro()
            if (owned) {
                settings.setProUnlocked(true)
                _events.emit(ProEvent.Restored)
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        scope.launch {
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    if (handlePurchases(purchases)) _events.emit(ProEvent.Purchased)
                }
                BillingClient.BillingResponseCode.USER_CANCELED -> _events.emit(ProEvent.Cancelled)
                else -> _events.emit(ProEvent.Failed)
            }
        }
    }

    private suspend fun handlePurchases(purchases: List<Purchase>?): Boolean {
        val purchase = purchases?.firstOrNull {
            it.products.contains(productId()) &&
                it.purchaseState == Purchase.PurchaseState.PURCHASED
        } ?: return false
        if (!purchase.isAcknowledged) {
            val result = acknowledge(purchase.purchaseToken)
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return false
        }
        settings.setProUnlocked(true)
        return true
    }

    private suspend fun queryOwnedPro(): Boolean {
        val c = client?.takeIf { it.isReady } ?: return false
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val result = c.queryPurchasesAsync(params)
        return result.billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
            result.purchasesList.any {
                it.products.contains(productId()) &&
                    it.purchaseState == Purchase.PurchaseState.PURCHASED
            }
    }

    private suspend fun queryProDetails(): ProductDetails? {
        val c = client?.takeIf { it.isReady } ?: run {
            connectAndAwait()
            client?.takeIf { it.isReady }
        } ?: return null
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId())
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                ),
            )
            .build()
        val result = c.queryProductDetails(params)
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) return null
        return result.productDetailsList?.firstOrNull()
    }

    private suspend fun connectAndAwait() {
        suspendCancellableCoroutine { cont ->
            val built = BillingClient.newBuilder(context)
                .setListener(this)
                .enablePendingPurchases(
                    PendingPurchasesParams.newBuilder()
                        .enableOneTimeProducts()
                        .build(),
                )
                .build()
            client = built
            built.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    cont.resume(Unit)
                }

                override fun onBillingServiceDisconnected() {
                    if (cont.isActive) cont.resume(Unit)
                }
            })
        }
    }

    private suspend fun acknowledge(token: String): BillingResult {
        val c = client
        if (c == null || !c.isReady) {
            return BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE)
                .build()
        }
        return c.acknowledgePurchase(
            AcknowledgePurchaseParams.newBuilder().setPurchaseToken(token).build(),
        )
    }
}
