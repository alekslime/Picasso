package app.picasso.tracer.ui.trace

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.picasso.tracer.R
import app.picasso.tracer.ui.theme.PicassoInk
import app.picasso.tracer.ui.theme.PicassoOchre
import app.picasso.tracer.ui.theme.PicassoPaper
import app.picasso.tracer.ui.theme.PicassoPaperMuted
import app.picasso.tracer.ui.theme.PicassoSurface
import kotlin.math.roundToInt

/**
 * Slim control dock under (or beside) the camera: the five quick actions, a
 * label-free opacity slider and one Tools button. Everything else lives in
 * [ToolsSheet], so the camera keeps almost all of the screen.
 */
@Composable
fun TraceDock(
    opacity: Float,
    locked: Boolean,
    torchOn: Boolean,
    onOpacity: (Float) -> Unit,
    onToggleLock: () -> Unit,
    onFlipH: () -> Unit,
    onFlipV: () -> Unit,
    onToggleTorch: () -> Unit,
    onResetView: () -> Unit,
    onOpenTools: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    fun toggled(action: () -> Unit) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        action()
    }
    Surface(
        color = PicassoInk,
        contentColor = PicassoPaper,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                DockButton(
                    icon = if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
                    description = stringResource(if (locked) R.string.unlock_overlay else R.string.lock_overlay),
                    active = locked,
                    onClick = { toggled(onToggleLock) },
                    bounce = true,
                )
                DockButton(Icons.Default.SwapHoriz, stringResource(R.string.flip_horizontal), false, onFlipH)
                DockButton(Icons.Default.SwapVert, stringResource(R.string.flip_vertical), false, onFlipV)
                DockButton(
                    icon = if (torchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                    description = stringResource(R.string.toggle_flashlight),
                    active = torchOn,
                    onClick = { toggled(onToggleTorch) },
                )
                DockButton(Icons.Default.Refresh, stringResource(R.string.reset_view), false, onResetView)
                Spacer(Modifier.width(2.dp))
                DockButton(
                    icon = Icons.Default.Tune,
                    description = stringResource(R.string.tools),
                    active = true,
                    onClick = onOpenTools,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Opacity,
                    contentDescription = stringResource(R.string.opacity),
                    tint = PicassoPaperMuted,
                    modifier = Modifier.size(20.dp),
                )
                PicassoSlider(
                    value = opacity,
                    onValueChange = onOpacity,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp),
                )
                Text(
                    "${(opacity * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = PicassoPaper,
                    modifier = Modifier.widthIn(min = 40.dp),
                )
            }
        }
    }
}

@Composable
private fun DockButton(
    icon: ImageVector,
    description: String,
    active: Boolean,
    onClick: () -> Unit,
    bounce: Boolean = false,
) {
    // Squeeze, then spring back whenever the state flips (not on first composition).
    val scale = remember { Animatable(1f) }
    var seen by remember { mutableStateOf(false) }
    LaunchedEffect(active) {
        if (!seen) { seen = true; return@LaunchedEffect }
        if (bounce) {
            scale.snapTo(0.78f)
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
    }
    val container by animateColorAsState(if (active) PicassoOchre else PicassoSurface, tween(160), label = "dockBg")
    val tint by animateColorAsState(if (active) PicassoInk else PicassoPaper, tween(160), label = "dockTint")
    // Every dock button dips while held and springs back on release; the lock's bounce stacks on top.
    val press = remember { MutableInteractionSource() }
    val pressed by press.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "dockPress",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = scale.value * pressScale
                scaleY = scale.value * pressScale
            }
            .clip(CircleShape)
            .background(container)
            .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onClick),
    ) {
        AnimatedContent(
            targetState = icon,
            transitionSpec = {
                (fadeIn(tween(120, delayMillis = 40)) + scaleIn(tween(180), initialScale = 0.6f)) togetherWith
                    fadeOut(tween(80))
            },
            label = "dockIcon",
        ) { current ->
            Icon(
                current,
                contentDescription = description,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Everything beyond the quick actions, grouped into four short tabs so only a
 * handful of controls are ever on screen at once.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsSheet(
    onDismiss: () -> Unit,
    image: @Composable () -> Unit,
    guides: @Composable () -> Unit,
    assist: @Composable () -> Unit,
    capture: @Composable () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = PicassoInk,
        contentColor = PicassoPaper,
        scrimColor = Color.Black.copy(alpha = 0.2f),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ToolTab(Icons.Default.Image, stringResource(R.string.tab_image), tab == 0, Modifier.weight(1f)) { tab = 0 }
                ToolTab(Icons.Default.GridOn, stringResource(R.string.guides), tab == 1, Modifier.weight(1f)) { tab = 1 }
                ToolTab(Icons.Default.AutoFixHigh, stringResource(R.string.tab_assist), tab == 2, Modifier.weight(1f)) { tab = 2 }
                ToolTab(Icons.Default.Videocam, stringResource(R.string.tab_capture), tab == 3, Modifier.weight(1f)) { tab = 3 }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier
                    .padding(top = 20.dp)
                    .heightIn(min = 200.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        (fadeIn(tween(200, delayMillis = 60)) +
                            slideInHorizontally(tween(260)) { dir * it / 10 }) togetherWith
                            (fadeOut(tween(110)) +
                                slideOutHorizontally(tween(260)) { -dir * it / 10 }) using
                            SizeTransform(clip = false) { _, _ -> tween(220) }
                    },
                    label = "toolsTab",
                ) { current ->
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        when (current) {
                            0 -> image()
                            1 -> guides()
                            2 -> assist()
                            else -> capture()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolTab(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val container by animateColorAsState(if (selected) PicassoOchre else PicassoSurface, tween(180), label = "tabBg")
    val content by animateColorAsState(if (selected) PicassoInk else PicassoPaper, tween(180), label = "tabFg")
    // Tab dips while pressed; the icon pops when the tab becomes selected (not on first composition).
    val press = remember { MutableInteractionSource() }
    val pressed by press.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "tabPress",
    )
    val pop = remember { Animatable(1f) }
    var seen by remember { mutableStateOf(false) }
    LaunchedEffect(selected) {
        if (!seen) { seen = true; return@LaunchedEffect }
        if (selected) {
            pop.snapTo(0.7f)
            pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer { scaleX = pop.value; scaleY = pop.value },
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            maxLines = 1,
        )
    }
}
