package app.picasso.tracer.ui.trace

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.picasso.tracer.MainActivity
import app.picasso.tracer.R
import app.picasso.tracer.filters.GpuFilters
import app.picasso.tracer.ui.permission.CameraPermissionScreen
import java.io.File

@Composable
fun TraceScreen(
    onBack: () -> Unit,
    viewModel: TraceViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
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
    val snackbar = remember { SnackbarHostState() }
    var showAudioDialog by remember { mutableStateOf(false) }

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
            LaunchedEffect(lifecycleOwner) {
                viewModel.cameraManager.bindPreview(lifecycleOwner)
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
                OverlayImage(
                    content = stepOrFiltered?.let { OverlayContent.Pixels(it) }
                        ?: OverlayContent.FileImage(File(project.imagePath)),
                    transform = state.transform,
                    opacity = state.opacity,
                    locked = state.locked,
                    colorFilter = if (stepActive) null
                    else remember(state.filter) { GpuFilters.colorFilterFor(state.filter) },
                    onGesture = viewModel::onOverlayGesture,
                )
            }
            if (state.gridVisible) {
                ProportionGrid(rows = state.gridRows, columns = state.gridCols)
            }

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
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                ),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(8.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }

            SnackbarHost(
                snackbar,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 56.dp),
            )

            TraceControls(
                opacity = state.opacity,
                locked = state.locked,
                torchOn = cameraState.torchOn,
                onOpacity = viewModel::setOpacity,
                onToggleLock = viewModel::toggleLock,
                onFlipH = viewModel::flipHorizontal,
                onFlipV = viewModel::flipVertical,
                onToggleTorch = { viewModel.setTorch(!cameraState.torchOn) },
                onResetView = viewModel::resetView,
                extraContent = {
                    FilterBar(selected = state.filter, onSelect = viewModel::setFilter)
                    GridBar(
                        visible = state.gridVisible,
                        rows = state.gridRows,
                        columns = state.gridCols,
                        onToggle = viewModel::toggleGrid,
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
                    RecordBar(
                        recording = cameraState.recording,
                        onToggle = ::onRecordToggle,
                    )
                },
                modifier = if (landscape) {
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
                },
            )

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
