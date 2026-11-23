package app.picasso.tracer.ui.trace

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.view.TextureView
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.picasso.tracer.MainActivity
import app.picasso.tracer.R
import app.picasso.tracer.camera.ArEngine
import app.picasso.tracer.camera.OverlayMode
import app.picasso.tracer.domain.model.TraceFilter
import app.picasso.tracer.filters.GpuFilters
import app.picasso.tracer.ui.permission.CameraPermissionScreen
import app.picasso.tracer.ui.theme.PicassoBlack
import app.picasso.tracer.ui.theme.PicassoBlack
import app.picasso.tracer.ui.theme.PicassoOchre
import app.picasso.tracer.ui.theme.PicassoInk
import app.picasso.tracer.ui.theme.PicassoPaper
import app.picasso.tracer.ui.theme.PicassoPaperMuted
import java.io.File

@Composable
fun TraceScreen(
    onBack: () -> Unit,
    viewModel: TraceViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val state by viewModel.uiState.collectAsState()
    val layerName = stringResource(R.string.layer_default_name, state.layers.size + 1)
    val pickLayerImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) viewModel.addLayer(uri, layerName)
    }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var permanentlyDenied by remember { mutableStateOf(false) }
    val requestPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
        if (!granted) {
            val activity = context as? MainActivity
            permanentlyDenied = activity?.let {
                !ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
            } ?: true
        }
    }

    if (!hasPermission) {
        CameraPermissionScreen(
            permanentlyDenied = permanentlyDenied,
            onGrant = { requestPermission.launch(Manifest.permission.CAMERA) },
            onOpenSettings = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    },
                )
            },
        )
        return
    }

    // Tracing sessions run long; never let the screen sleep here.
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            viewModel.saveNow()
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraState by viewModel.cameraState.collectAsState()
    val recordingElapsedSec by viewModel.recordingElapsedSec.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var showAudioDialog by remember { mutableStateOf(false) }
    var panelVisible by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    val noticeRes = state.noticeRes
    LaunchedEffect(noticeRes) {
        if (noticeRes != null) {
            snackbar.showSnackbar(context.getString(noticeRes))
            viewModel.clearNotice()
        }
    }

    fun isGranted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    // Re-checks the real permission state so partially-granted results still record.
    val requestRecordPermissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        val storageOk = Build.VERSION.SDK_INT > Build.VERSION_CODES.P ||
            isGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (storageOk) viewModel.startRecording(isGranted(Manifest.permission.RECORD_AUDIO))
        else viewModel.notifyRecordingFailed()
    }

    fun onRecordToggle() {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        if (cameraState.recording) {
            viewModel.stopRecording()
            return
        }
        if (isGranted(Manifest.permission.RECORD_AUDIO)) {
            viewModel.startRecording(withAudio = true)
        } else {
            showAudioDialog = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight
        Box(modifier = Modifier.fillMaxSize()) {
            val coreCamera = state.overlayMode == OverlayMode.AR && state.arEngine == ArEngine.CORE
            if (coreCamera) {
                ArCameraPreview(
                    previewSize = state.arPreviewSize,
                    onSurfaceReady = { texture, w, h ->
                        activity?.let { viewModel.startCoreAnchor(it, texture, w, h) }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        }
                    },
                    update = { previewView ->
                        viewModel.cameraManager.preview.setSurfaceProvider(previewView.surfaceProvider)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            LaunchedEffect(lifecycleOwner, coreCamera) {
                // ARCore owns the camera in core mode; every other state uses CameraX.
                if (!coreCamera) viewModel.rebindCamera(lifecycleOwner)
            }
            DisposableEffect(lifecycleOwner) {
                // Catches the return from the ARCore install flow: upgrades sensor AR silently.
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        activity?.let { viewModel.retryArCore(it) }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            val overlayVisible = !state.strobeOn || state.strobePhase
            state.project?.let { project ->
                if (!overlayVisible) return@let
                val stepBitmap = state.stepIndex?.let { index ->
                    state.steps.getOrNull(index)?.bitmap
                }
                val stepActive = state.stepIndex != null
                val filtered = state.filteredBitmap?.takeIf { !stepActive && state.filter.needsBitmap() }
                val stepOrFiltered = stepBitmap ?: filtered
                // Crossfade only on content/filter/step switches, never on gestures
                // or slider drags: each slot renders its own key, transform stays live.
                val overlayContent = stepOrFiltered?.let { OverlayContent.Pixels(it) }
                    ?: OverlayContent.FileImage(File(state.activeImagePath ?: project.imagePath))
                val effectiveFilter = if (stepActive) TraceFilter.ORIGINAL else state.filter
                Crossfade(targetState = overlayContent to effectiveFilter, label = "overlay") { (content, filter) ->
                    OverlayImage(
                        content = content,
                        transform = viewModel.effectiveTransform(state),
                        opacity = state.opacity,
                        locked = state.locked,
                        // Light tuning applies on top of everything, including step layers.
                        colorFilter = remember(filter, state.brightness, state.contrast) {
                            GpuFilters.colorFilterFor(filter, state.brightness, state.contrast)
                        },
                        onGesture = viewModel::onOverlayGesture,
                    )
                }
            }
            if (state.gridVisible) {
                ProportionGrid(type = state.guideType, rows = state.gridRows, columns = state.gridCols)
            }

            // Figma viewfinder dressing: focus ring + marks over the camera area.
            FocusMarks(
                opacity = state.opacity,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 72.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(340.dp),
            )

            if (state.loading || state.filterLoading || state.stepsLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            state.errorRes?.let { Text(stringResource(it), modifier = Modifier.align(Alignment.Center)) }
            cameraState.error?.let {
                Text(
                    stringResource(R.string.camera_error, it),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            FilledIconButton(
                onClick = onBack,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = PicassoBlack.copy(alpha = 0.65f),
                    contentColor = PicassoPaper,
                ),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(8.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }

            // Figma top readout: TRACE + active filter, mono, top-end over the camera.
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp)
                    .background(PicassoBlack.copy(alpha = 0.45f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    stringResource(R.string.trace_readout),
                    style = MaterialTheme.typography.labelSmall,
                    color = PicassoPaperMuted,
                )
                Text(
                    state.filter.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = PicassoPaper,
                )
            }

            if (cameraState.recording) {
                RecordingPill(
                    elapsedSec = recordingElapsedSec,
                    onStop = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.stopRecording()
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 8.dp),
                )
            }

            SnackbarHost(
                snackbar,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 56.dp),
            )

            val panelModifier = if (landscape) {
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(8.dp)
                    .widthIn(max = 320.dp)
            } else {
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(8.dp)
                    .fillMaxWidth()
            }
            AnimatedVisibility(
                visible = panelVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = panelModifier,
            ) {
            TraceControls(
                opacity = state.opacity,
                locked = state.locked,
                torchOn = state.torchOn,
                onOpacity = viewModel::setOpacity,
                onToggleLock = viewModel::toggleLock,
                onFlipH = viewModel::flipHorizontal,
                onFlipV = viewModel::flipVertical,
                onToggleTorch = { viewModel.setTorch(!state.torchOn) },
                onResetView = viewModel::resetView,
                onCollapse = { panelVisible = false },
                extraContent = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        AnchorBar(
                            mode = state.overlayMode,
                            engine = state.arEngine,
                            status = state.arStatus,
                            isPro = state.proUnlocked,
                            onToggle = { activity?.let { viewModel.toggleAnchorMode(it) } },
                        )
                        LayersBar(
                            layers = state.layers,
                            activeLayerId = state.activeLayerId,
                            busy = state.layerBusy,
                            onSelect = viewModel::selectLayer,
                            onAdd = {
                                pickLayerImage.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                            onDelete = viewModel::deleteLayer,
                        )
                        FilterBar(selected = state.filter, onSelect = viewModel::setFilter)
                        LightBar(
                            expanded = state.lightExpanded,
                            brightness = state.brightness,
                            contrast = state.contrast,
                            onToggle = viewModel::toggleLight,
                            onBrightness = viewModel::setBrightness,
                            onContrast = viewModel::setContrast,
                            onReset = viewModel::resetLight,
                        )
                        GridBar(
                            visible = state.gridVisible,
                            type = state.guideType,
                            rows = state.gridRows,
                            columns = state.gridCols,
                            onToggle = viewModel::toggleGrid,
                            onType = viewModel::setGuideType,
                            onRows = viewModel::setGridRows,
                            onColumns = viewModel::setGridCols,
                        )
                        StrobeBar(
                            enabled = state.strobeOn,
                            hz = state.strobeHz,
                            onToggle = viewModel::toggleStrobe,
                            onHz = viewModel::setStrobeHz,
                        )
                        StepsBar(
                            active = state.stepIndex != null,
                            steps = state.steps,
                            stepIndex = state.stepIndex ?: 0,
                            onToggle = viewModel::toggleSteps,
                            onPrevious = viewModel::previousStep,
                            onNext = viewModel::nextStep,
                        )
                        TimelapseBar(
                            active = state.timelapseActive,
                            intervalSec = state.timelapseIntervalSec,
                            frameCount = state.timelapseCount,
                            onToggle = viewModel::toggleTimelapse,
                            onInterval = viewModel::setTimelapseInterval,
                        )
                        RecordBar(
                            recording = cameraState.recording,
                            onToggle = ::onRecordToggle,
                        )
                        BeforeAfterBar(
                            saving = state.beforeAfterSaving,
                            onClick = viewModel::captureBeforeAfter,
                        )
                        WallModeBar(onClick = viewModel::toggleWallMode)
                    }
                },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (!panelVisible) {
                // Default state is the Figma hero deck, not the full panel.
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    val filters = TraceFilter.entries
                    Column {
                        if (state.showTutorial) {
                            TutorialCard(
                                step = state.tutorialStep,
                                onNext = viewModel::nextTutorialStep,
                                onSkip = viewModel::skipTutorial,
                                modifier = Modifier.padding(bottom = 12.dp),
                            )
                        }
                        CleanTraceDeck(
                        opacity = state.opacity,
                        locked = state.locked,
                        torchOn = state.torchOn,
                        filter = state.filter,
                        gridVisible = state.gridVisible,
                        gridLabel = if (state.gridVisible) {
                            when (state.guideType) {
                                app.picasso.tracer.domain.model.GuideType.GRID -> "${state.gridRows}×${state.gridCols}"
                                else -> state.guideType.name
                            }
                        } else stringResource(R.string.guide_off).uppercase(),
                        recording = cameraState.recording,
                        anchorActive = state.overlayMode == app.picasso.tracer.camera.OverlayMode.AR,
                        onToggleAnchor = { activity?.let { viewModel.toggleAnchorMode(it) } },
                        onOpacity = viewModel::setOpacity,
                        onToggleLock = viewModel::toggleLock,
                        onFlipH = viewModel::flipHorizontal,
                        onFlipV = viewModel::flipVertical,
                        onToggleTorch = { viewModel.setTorch(!state.torchOn) },
                        onToggleGrid = viewModel::toggleGrid,
                        onResetView = viewModel::resetView,
                        onCycleFilter = {
                            viewModel.setFilter(filters[(state.filter.ordinal + 1) % filters.size])
                        },
                        onRecord = ::onRecordToggle,
                            onExpand = { viewModel.onTutorialExpand(); panelVisible = true },
                        )
                    }
                }
            }

            if (state.wallMode) {
                // Full-screen reference for projectors and big screens. The phone
                // screen is what gets mirrored — no chrome, just the image.
                BackHandler { viewModel.toggleWallMode() }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PicassoBlack),
                ) {
                    state.project?.let { project ->
                        OverlayImage(
                            content = OverlayContent.FileImage(File(state.activeImagePath ?: project.imagePath)),
                            transform = viewModel.effectiveTransform(state),
                            opacity = 1f,
                            locked = false,
                            colorFilter = remember(state.filter, state.brightness, state.contrast) {
                                GpuFilters.colorFilterFor(state.filter, state.brightness, state.contrast)
                            },
                            onGesture = viewModel::onOverlayGesture,
                        )
                    }
                    if (state.gridVisible) {
                        ProportionGrid(
                            type = state.guideType,
                            rows = state.gridRows,
                            columns = state.gridCols,
                        )
                    }
                    Text(
                        stringResource(R.string.wall_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = PicassoPaper,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(20.dp),
                    )
                    FilledIconButton(
                        onClick = viewModel::toggleWallMode,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = PicassoBlack.copy(alpha = 0.65f),
                            contentColor = PicassoPaper,
                        ),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .statusBarsPadding()
                            .padding(8.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.close))
                    }
                    PicassoFilterChip(
                        selected = state.gridVisible,
                        onClick = viewModel::toggleGrid,
                        label = stringResource(R.string.grid),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(12.dp),
                    )
                }
            }

            if (showAudioDialog) {
                AlertDialog(
                    onDismissRequest = { showAudioDialog = false },
                    text = { Text(stringResource(R.string.audio_permission_text)) },
                    confirmButton = {
                        TextButton(onClick = {
                            showAudioDialog = false
                            val wanted = buildList {
                                add(Manifest.permission.RECORD_AUDIO)
                                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                }
                            }
                            requestRecordPermissions.launch(wanted.toTypedArray())
                        }) {
                            Text(stringResource(R.string.audio_permission_grant))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showAudioDialog = false
                            viewModel.startRecording(withAudio = false)
                        }) {
                            Text(stringResource(R.string.skip))
                        }
                    },
                )
            }
        }
    }
}

/**
 * ARCore viewfinder: a plain TextureView fed by the shared-camera session.
 * Center-crop fill matches the CameraX PreviewView behaviour.
 */
@Composable
private fun ArCameraPreview(
    previewSize: Pair<Int, Int>?,
    onSurfaceReady: (SurfaceTexture, Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(
                        texture: SurfaceTexture,
                        width: Int,
                        height: Int,
                    ) {
                        onSurfaceReady(texture, width, height)
                    }

                    override fun onSurfaceTextureSizeChanged(
                        texture: SurfaceTexture,
                        width: Int,
                        height: Int,
                    ) = Unit

                    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture) = true

                    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
                }
            }
        },
        update = { view ->
            val size = previewSize
            if (size != null && view.width > 0 && view.height > 0) {
                val (bufferW, bufferH) = size
                val scale = maxOf(
                    view.width / bufferW.toFloat(),
                    view.height / bufferH.toFloat(),
                )
                view.setTransform(
                    Matrix().apply {
                        setScale(scale, scale)
                        postTranslate(
                            (view.width - bufferW * scale) / 2f,
                            (view.height - bufferH * scale) / 2f,
                        )
                    },
                )
            }
        },
        modifier = modifier,
    )
}
