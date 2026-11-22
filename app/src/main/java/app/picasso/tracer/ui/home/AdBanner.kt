package app.picasso.tracer.ui.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import app.picasso.tracer.R
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Minimal AdMob banner (Home/Library and Settings only — never the camera).
 * Shown only after UMP consent and only for non-Pro users; the caller gates
 * both via [visible].
 */
@Composable
fun AdBanner(
    visible: Boolean,
    adRequest: () -> com.google.android.gms.ads.AdRequest,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = ctx.getString(R.string.admob_banner_id)
                val observer = object : DefaultLifecycleObserver {
                    override fun onResume(owner: LifecycleOwner) = resume()
                    override fun onPause(owner: LifecycleOwner) = pause()
                    override fun onDestroy(owner: LifecycleOwner) = destroy()
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                loadAd(adRequest())
            }
        },
        update = { it.loadAd(adRequest()) },
        onRelease = { it.destroy() },
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    )
}
