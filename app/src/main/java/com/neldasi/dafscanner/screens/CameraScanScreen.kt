package com.neldasi.dafscanner.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FilterNone
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.neldasi.dafscanner.R
import com.neldasi.dafscanner.extras.DafImageAnalyzer
import com.neldasi.dafscanner.extras.ParsedPart
import com.neldasi.dafscanner.extras.ScanStorage
import com.neldasi.dafscanner.extras.SettingsRepository
import com.neldasi.dafscanner.extras.parseScannedCode
import com.neldasi.dafscanner.navigation.NavKeys
import com.neldasi.dafscanner.ui.theme.DafBlue
import com.neldasi.dafscanner.ui.theme.DafRed
import com.neldasi.dafscanner.ui.theme.JetpackComposeTheme
import com.neldasi.dafscanner.viewmodels.SearchListViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import androidx.compose.ui.tooling.preview.Preview as ComposePreview


data class ScanFeedback(
    val serial: String,
    val isMatch: Boolean,
    val alreadyScanned: Boolean,
    val scanTimestamp: Long? = null,
    val parsedPart: ParsedPart? = null,
)

// Helper to trigger autofocus at center of preview
private fun requestCenterFocus(previewView: PreviewView, camera: Camera?) {
    try {
        if (camera == null) return

        val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
            previewView.width.toFloat(),
            previewView.height.toFloat(),
        )
        val centerPoint = factory.createPoint(
            previewView.width / 2f,
            previewView.height / 2f,
        )

        val action = FocusMeteringAction.Builder(centerPoint, FocusMeteringAction.FLAG_AF)
            .setAutoCancelDuration(2, TimeUnit.SECONDS)
            .build()

        camera.cameraControl.startFocusAndMetering(action)
    } catch (_: Exception) {}
}

@Composable
fun CameraScanScreen(
    navController: NavController,
    isVerifyMode: Boolean = false,
    searchViewModel: SearchListViewModel? = null,
) {
    val context = LocalContext.current
    val prefs = remember { ScanStorage.prefs(context) }
    
    var vibrateEnabled by remember { mutableStateOf(prefs.getBoolean(ScanStorage.Keys.VIBRATE_ENABLED, true)) }
    var continuousScanEnabled by remember { mutableStateOf(prefs.getBoolean(ScanStorage.Keys.CONTINUOUS_SCAN_ENABLED, false)) }
    var screenAlwaysOn by remember { mutableStateOf(prefs.getBoolean(ScanStorage.Keys.SCREEN_ALWAYS_ON, true)) }
    val cameraErrorString = stringResource(R.string.camera_error)

    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    val barcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_DATA_MATRIX).build()
        BarcodeScanning.getClient(options)
    }

    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    val allowedTypes = remember { SettingsRepository.loadAllowedTypes(context).toMutableStateList() }

    var showNotAllowedDialog by remember { mutableStateOf(value = false) }
    var scannedNotAllowedType by remember { mutableStateOf("") }
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(value = null) }
    var scannedResult by remember { mutableStateOf<String?>(value = null) }
    var cameraError by remember { mutableStateOf<String?>(value = null) }
    var isCameraReady by remember { mutableStateOf(value = false) }
    var lastSerial by remember { mutableStateOf<String?>(value = null) }
    var lastFlashedCode by remember { mutableStateOf<String?>(value = null) }
    var camera: Camera? by remember { mutableStateOf(value = null) }
    var isTorchOn by remember { mutableStateOf(value = false) }

    // State for Feedback and Cooldowns
    var scanFeedback by remember { mutableStateOf<ScanFeedback?>(null) }
    var continuousCooldown by remember { mutableIntStateOf(0) }
    var cooldownJob: kotlinx.coroutines.Job? by remember { mutableStateOf(null) }
    val verifyScope = rememberCoroutineScope()
    var isPaused by remember { mutableStateOf(value = false) }
    
    // Custom Toast State
    var toastMessage by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2000)
            toastMessage = null
        }
    }

    // To prevent a new scan from immediately triggering its own duplicate warning
    var lastProcessedCode by remember { mutableStateOf<String?>(null) }
    var lastProcessedTimestamp by remember { mutableLongStateOf(0L) }

    val existingParts = remember {
        navController.previousBackStackEntry?.savedStateHandle?.get<Map<String, Long>>("EXISTING_PARTS") ?: emptyMap()
    }
    val sessionScanned = remember { mutableStateMapOf<String, Long>() }

    var zoomRatio by remember { mutableFloatStateOf(1f) }
    val transformableState = rememberTransformableState { _, zoomChange, _, _ ->
        val newZoom = (zoomRatio * zoomChange).coerceIn(1f, 10f)
        zoomRatio = newZoom
        camera?.cameraControl?.setZoomRatio(newZoom)
    }

    val flashAlpha = remember { Animatable(0f) }
    val flashScope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        onDispose {
            lastFlashedCode = null
            cameraExecutor.shutdown()
            cameraProvider?.unbindAll()
            barcodeScanner.close()
        }
    }

    LaunchedEffect(scannedResult) {
        scannedResult?.let { value ->
            val now = System.currentTimeMillis()
            
            // 1. Debounce rapid-fire scans of the exact same code in Continuous Mode
            if ((continuousScanEnabled && value == lastProcessedCode) && (now - lastProcessedTimestamp) < 2000) {
                scannedResult = null
                isPaused = false
                return@let
            }

            // 2. NEW/VALID SCAN: Trigger vibration feedback here instead of analyzer
            if (vibrateEnabled) {
                val vibrator = context.getSystemService(android.os.Vibrator::class.java)
                vibrator?.vibrate(
                    android.os.VibrationEffect.createOneShot(
                        100L,
                        android.os.VibrationEffect.DEFAULT_AMPLITUDE,
                    ),
                )
            }

            val parsed = parseScannedCode(value)
            lastSerial = parsed?.serialNumber ?: extractSerial(value)

            // 3. Flash visual feedback
            if (value != lastFlashedCode) {
                lastFlashedCode = value
                flashScope.launch {
                    try {
                        flashAlpha.snapTo(0f)
                        flashAlpha.animateTo(0.4f, tween(80))
                        flashAlpha.animateTo(0f, tween(180))
                    } catch (_: Exception) {}
                }
            }

            // 4. Process the scan based on mode
            if (isVerifyMode) {
                val serial = parsed?.serialNumber ?: extractSerial(value)
                if (searchViewModel != null) {
                    val currentItems = searchViewModel.searchItems.value
                    val matchItem = currentItems.find { it.serialNumber == serial }
                    val isMatch = matchItem != null
                    val alreadyScanned = matchItem?.scanTimestamp != null
                    searchViewModel.checkScannedCode(value)
                    scanFeedback = ScanFeedback(serial, isMatch, alreadyScanned, matchItem?.scanTimestamp, parsed)
                } else {
                    val serialList = navController.previousBackStackEntry?.savedStateHandle?.get<List<String>>("SERIAL_LIST") ?: emptyList()
                    val scannedSerials = navController.previousBackStackEntry?.savedStateHandle?.get<List<String>>("SCANNED_SERIALS") ?: emptyList()
                    val isMatch = serialList.contains(serial)
                    val alreadyScanned = scannedSerials.contains(serial)
                    scanFeedback = ScanFeedback(serial, isMatch, alreadyScanned, parsedPart = parsed)
                    if (isMatch && !alreadyScanned) {
                        val updatedScanned = scannedSerials + serial
                        navController.previousBackStackEntry?.savedStateHandle?.set("SCANNED_SERIALS", updatedScanned)
                    }
                }

                isPaused = true
                navController.previousBackStackEntry?.savedStateHandle?.set(NavKeys.SCANNED_RESULT, value)
            } else {
                // Main Scanning Mode logic
                val sessionTimestamp = sessionScanned[value]
                val existingTimestamp = existingParts[value]
                val finalTimestamp = sessionTimestamp ?: existingTimestamp
                
                if (finalTimestamp != null) {
                    // DUPLICATE DETECTED
                    val serial = parsed?.serialNumber ?: extractSerial(value)
                    scanFeedback = ScanFeedback(serial, isMatch = false, alreadyScanned = true, scanTimestamp = finalTimestamp, parsedPart = parsed)
                    isPaused = true
                } else {
                    // NEW SCAN SUCCESS
                    lastProcessedCode = value
                    lastProcessedTimestamp = now
                    
                    if (continuousScanEnabled) {
                        navController.previousBackStackEntry?.savedStateHandle?.set(NavKeys.SCANNED_RESULT, value)
                        navController.previousBackStackEntry?.savedStateHandle?.set("SCANNED_TIMESTAMP", now)
                        appendPendingScan(context, value, now)
                        sessionScanned[value] = now
                        
                        // Give it a small pause so it doesn't immediately scan the same thing again
                        continuousCooldown = 2
                        cooldownJob = verifyScope.launch {
                            while (continuousCooldown > 0) {
                                delay(1000)
                                continuousCooldown--
                            }
                            scannedResult = null
                            isPaused = false
                        }
                    } else {
                        navController.previousBackStackEntry?.savedStateHandle?.set(NavKeys.SCANNED_RESULT, value)
                        if (navController.previousBackStackEntry != null) {
                            navController.popBackStack()
                        }
                    }
                }
            }
        }
    }

    CameraScanScreenContent(
        isCameraReady = isCameraReady,
        cameraError = cameraError,
        lastSerial = lastSerial,
        isTorchOn = isTorchOn,
        flashAlpha = flashAlpha.value,
        transformableState = transformableState,
        onToggleTorch = {
            val newState = !isTorchOn
            isTorchOn = newState
            camera?.cameraControl?.enableTorch(newState)
            previewViewRef?.let { requestCenterFocus(it, camera) }
        },
        onClose = {
            searchViewModel?.clearResult()
            if (navController.previousBackStackEntry != null) {
                navController.popBackStack()
            }
        },
        onAndroidViewFactory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            previewViewRef = previewView
            cameraProviderFuture.addListener({
                try {
                    val provider = cameraProviderFuture.get()
                    cameraProvider = provider
                    val preview = Preview.Builder()
                        .setResolutionSelector(
                            ResolutionSelector.Builder()
                                .setAspectRatioStrategy(AspectRatioStrategy(AspectRatio.RATIO_16_9, AspectRatioStrategy.FALLBACK_RULE_AUTO))
                                .build(),
                        )
                        .build().also { it.surfaceProvider = previewView.surfaceProvider }

                    val analyzer = buildImageAnalyzer(
                        barcodeScanner = barcodeScanner,
                        cameraExecutor = cameraExecutor,
                        shouldProcess = { !isPaused && scannedResult == null },
                    ) { scannedValue ->
                        val type = scannedValue.take(7)
                        if (type in allowedTypes) {
                            if (scannedResult == null && !isPaused) {
                                isPaused = true
                                scannedResult = scannedValue
                            }
                        } else {
                            scannedNotAllowedType = type
                            showNotAllowedDialog = true
                            isPaused = true
                        }
                    }
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    provider.unbindAll()
                    val boundCamera = provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, analyzer)
                    camera = boundCamera
                    try { boundCamera.cameraControl.enableTorch(isTorchOn) } catch (_: Exception) {}
                    try { boundCamera.cameraControl.setExposureCompensationIndex(2) } catch (_: Exception) {}
                    isCameraReady = true
                    requestCenterFocus(previewView, boundCamera)
                } catch (_: Exception) {
                    cameraError = cameraErrorString
                    isCameraReady = false
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        verifyResult = scanFeedback,
        continuousCooldown = continuousCooldown,
        isVerifyMode = isVerifyMode,
        vibrateEnabled = vibrateEnabled,
        continuousScanEnabled = continuousScanEnabled,
        screenAlwaysOn = screenAlwaysOn,
        onToggleVibrate = {
            vibrateEnabled = !vibrateEnabled
            prefs.edit { putBoolean(ScanStorage.Keys.VIBRATE_ENABLED, vibrateEnabled) }
            toastMessage = if (vibrateEnabled) R.string.vibration_on else R.string.vibration_off
        },
        onToggleContinuous = {
            continuousScanEnabled = !continuousScanEnabled
            prefs.edit { putBoolean(ScanStorage.Keys.CONTINUOUS_SCAN_ENABLED, continuousScanEnabled) }
            toastMessage = if (continuousScanEnabled) R.string.continuous_scan_on else R.string.continuous_scan_off
        },
        onToggleScreenOn = {
            screenAlwaysOn = !screenAlwaysOn
            prefs.edit { putBoolean(ScanStorage.Keys.SCREEN_ALWAYS_ON, screenAlwaysOn) }
            toastMessage = if (screenAlwaysOn) R.string.screen_always_on_on else R.string.screen_always_on_off
        },
        onDismissVerify = {
            scanFeedback = null
            isPaused = false
            scannedResult = null
        },
        onDismissCooldown = {
            cooldownJob?.cancel()
            continuousCooldown = 0
            scannedResult = null
            isPaused = false
        },
        toastMessage = toastMessage,
    )


    LaunchedEffect(isCameraReady, camera) {
        if (!isCameraReady || (camera == null)) return@LaunchedEffect
        while (isActive && isCameraReady && (camera != null)) {
            previewViewRef?.let { requestCenterFocus(it, camera) }
            delay(2000)
        }
    }

    if (showNotAllowedDialog) {
        AlertDialog(
            onDismissRequest = { showNotAllowedDialog = false; isPaused = false },
            icon = {
                Icon(
                    Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = DafRed,
                    modifier = Modifier.size(40.dp),
                )
            },
            title = {
                Text(
                    stringResource(R.string.unsupported_code_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.unsupported_code_text),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(24.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stringResource(R.string.type_code_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = DafBlue,
                            )
                            Text(
                                text = scannedNotAllowedType,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = {
                            allowedTypes.add(scannedNotAllowedType)
                            SettingsRepository.saveAllowedTypes(context, allowedTypes)
                            showNotAllowedDialog = false
                            isPaused = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DafBlue),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.add_to_allowed), fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Type Code", scannedNotAllowedType)
                            clipboard.setPrimaryClip(clip)
                            android.widget.Toast.makeText(context, R.string.type_copied, android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DafBlue),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DafBlue),
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.copy_type), fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = { showNotAllowedDialog = false; isPaused = false },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.ok), color = MaterialTheme.colorScheme.outline)
                    }
                }
            },
        )
    }
}

@Composable
fun CameraScanScreenContent(
    isCameraReady: Boolean,
    cameraError: String?,
    lastSerial: String?,
    isTorchOn: Boolean,
    flashAlpha: Float,
    transformableState: androidx.compose.foundation.gestures.TransformableState,
    onToggleTorch: () -> Unit,
    onClose: () -> Unit,
    onAndroidViewFactory: (Context) -> android.view.View,
    modifier: Modifier = Modifier,
    verifyResult: ScanFeedback? = null,
    continuousCooldown: Int = 0,
    isVerifyMode: Boolean = false,
    vibrateEnabled: Boolean = true,
    continuousScanEnabled: Boolean = false,
    screenAlwaysOn: Boolean = true,
    onToggleVibrate: () -> Unit = {},
    onToggleContinuous: () -> Unit = {},
    onToggleScreenOn: () -> Unit = {},
    onDismissVerify: () -> Unit = {},
    onDismissCooldown: () -> Unit = {},
    toastMessage: Int? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .transformable(state = transformableState)
            .then(
                if (continuousCooldown > 0) {
                    Modifier.clickable(onClick = onDismissCooldown)
                } else {
                    Modifier
                }
            ),
    ) {
        AndroidView(
            factory = onAndroidViewFactory, 
            modifier = Modifier.fillMaxSize(),
            update = { view -> view.keepScreenOn = screenAlwaysOn },
        )

        // Flash overlay
        Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = flashAlpha)))

        CameraOverlay(isCameraReady = isCameraReady, errorMessage = cameraError)

        LastScanBar(
            serial = lastSerial,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp, start = 24.dp, end = 24.dp),
        )

        AnimatedVisibility(
            visible = continuousCooldown > 0,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier.align(Alignment.Center).padding(top = 320.dp),
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.next_scan_in, continuousCooldown),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        if (verifyResult != null) {
            val backgroundColor = when {
                verifyResult.alreadyScanned && !isVerifyMode -> Color.DarkGray
                verifyResult.isMatch -> Color(0xFFD32F2F) // Red for Match
                else -> Color(0xFF388E3C) // Green for No Match
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor.copy(alpha = 0.9f))
                    .clickable { onDismissVerify() },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp),
                ) {
                    Icon(
                        when {
                            verifyResult.alreadyScanned -> Icons.Rounded.History
                            verifyResult.isMatch -> Icons.Rounded.Warning
                            else -> Icons.Rounded.CheckCircle
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(100.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    
                    if (verifyResult.alreadyScanned) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(bottom = 8.dp),
                            ) {
                                Text(
                                    stringResource(R.string.already_scanned),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            
                            verifyResult.scanTimestamp?.let { ts ->
                                val locale = LocalConfiguration.current.locales[0]
                                val dateStr = SimpleDateFormat("dd MMM, HH:mm:ss", locale).format(Date(ts))
                                Text(
                                    text = stringResource(R.string.first_seen, dateStr),
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }

                    Text(
                        text = when {
                            verifyResult.alreadyScanned && !isVerifyMode -> stringResource(R.string.duplicate_code)
                            verifyResult.isMatch -> stringResource(R.string.match_found)
                            else -> stringResource(R.string.no_match)
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${stringResource(R.string.hex_prefix)}${verifyResult.serial}",
                        color = Color.White,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "${stringResource(R.string.dec_prefix)}${verifyResult.parsedPart?.decSerial ?: try { verifyResult.serial.toLong(16).toString() } catch(_:Exception) { "N/A" }}",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    
                    verifyResult.parsedPart?.let {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = it.format.name,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(48.dp))
                    
                    Button(
                        onClick = onDismissVerify,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = backgroundColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.dismiss),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                }
            }
        }

        BottomScannerBar(
            isTorchOn = isTorchOn,
            onToggleTorch = onToggleTorch,
            vibrateEnabled = vibrateEnabled,
            onToggleVibrate = onToggleVibrate,
            continuousScanEnabled = continuousScanEnabled,
            onToggleContinuous = onToggleContinuous,
            screenAlwaysOn = screenAlwaysOn,
            onToggleScreenOn = onToggleScreenOn,
            onClose = onClose,
            isVerifyMode = isVerifyMode,
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
        )

        // Custom Toast Overlay
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 180.dp),
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            ) {
                Text(
                    text = toastMessage?.let { stringResource(it) } ?: "",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun LastScanBar(
    serial: String?,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = serial != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        Surface(
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            shape = RoundedCornerShape(24.dp),
            color = Color.Black.copy(alpha = 0.8f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Rounded.QrCodeScanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.last_scan_label, serial ?: ""),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun CameraOverlay(isCameraReady: Boolean, errorMessage: String?) {
    Box(Modifier.fillMaxSize()) {
        if (!isCameraReady && errorMessage == null) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
            )
        }

        if (isCameraReady && errorMessage == null) {
            // Darken the area outside the scanner frame
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
            ) {
                val frameSize = 260.dp.toPx()
                val left = (size.width - frameSize) / 2
                val top = (size.height - frameSize) / 2
                
                // Draw semi-transparent background
                drawRect(Color.Black.copy(alpha = 0.5f))

                // Cut out the center
                drawRect(
                    color = Color.Transparent,
                    topLeft = Offset(left, top),
                    size = Size(frameSize, frameSize),
                    blendMode = BlendMode.Clear,
                )
            }
            
            // Draw the frame corners and scanning line
            ScannerFrame(modifier = Modifier.size(260.dp).align(Alignment.Center))
        }

        errorMessage?.let {
            Surface(
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.8f),
            ) {
                Text(
                    text = it,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ScannerFrame(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scannerLine")
    val lineOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "lineOffset",
    )

    Canvas(modifier = modifier) {
        val strokeWidth = 4.dp.toPx()
        val cornerLen = 40.dp.toPx()
        val color = Color.White

        // 1. Darken surroundings (Simple approach for preview)
        // Note: For a true cutout effect, we'd use BlendMode.Clear on a layer, but let's stick to simple corners for now.
        
        // 2. Draw 4 corners
        // Top Left
        drawLine(color, Offset(0f, 0f), Offset(cornerLen, 0f), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(0f, 0f), Offset(0f, cornerLen), strokeWidth, StrokeCap.Round)
        // Top Right
        drawLine(color, Offset(size.width, 0f), Offset(size.width - cornerLen, 0f), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(size.width, 0f), Offset(size.width, cornerLen), strokeWidth, StrokeCap.Round)
        // Bottom Left
        drawLine(color, Offset(0f, size.height), Offset(cornerLen, size.height), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(0f, size.height), Offset(0f, size.height - cornerLen), strokeWidth, StrokeCap.Round)
        // Bottom Right
        drawLine(color, Offset(size.width, size.height), Offset(size.width - cornerLen, size.height), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(size.width, size.height), Offset(size.width, size.height - cornerLen), strokeWidth, StrokeCap.Round)

        // 3. Scanning line
        val y = size.height * lineOffset
        drawLine(
            color = color.copy(alpha = 0.5f),
            start = Offset(10.dp.toPx(), y),
            end = Offset(size.width - 10.dp.toPx(), y),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun BottomScannerBar(
    isTorchOn: Boolean,
    onToggleTorch: () -> Unit,
    vibrateEnabled: Boolean,
    onToggleVibrate: () -> Unit,
    continuousScanEnabled: Boolean,
    onToggleContinuous: () -> Unit,
    screenAlwaysOn: Boolean,
    onToggleScreenOn: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    isVerifyMode: Boolean = false,
) {
    Surface(
        modifier = modifier.navigationBarsPadding(),
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(32.dp),
        color = Color.Black.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Torch
                ScannerOptionButton(
                    icon = if (isTorchOn) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                    isActive = isTorchOn,
                    activeColor = Color.Yellow,
                    onClick = onToggleTorch,
                )
                
                // Continuous Scan (Hidden in Verify Mode)
                if (!isVerifyMode) {
                    ScannerOptionButton(
                        icon = Icons.Rounded.FilterNone,
                        isActive = continuousScanEnabled,
                        activeColor = MaterialTheme.colorScheme.primary,
                        onClick = onToggleContinuous,
                    )
                }

                // Vibrate
                ScannerOptionButton(
                    icon = Icons.Rounded.NotificationsActive,
                    isActive = vibrateEnabled,
                    activeColor = MaterialTheme.colorScheme.primary,
                    onClick = onToggleVibrate,
                )

                // Screen Always On
                ScannerOptionButton(
                    icon = Icons.Rounded.Smartphone,
                    isActive = screenAlwaysOn,
                    activeColor = MaterialTheme.colorScheme.primary,
                    onClick = onToggleScreenOn,
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f), CircleShape),
            ) {
                Icon(Icons.Rounded.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun ScannerOptionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .background(if (isActive) activeColor.copy(alpha = 0.2f) else Color.Transparent, CircleShape),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) activeColor else Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(22.dp),
        )
    }
}

private fun appendPendingScan(context: Context, code: String, timestamp: Long) {
    val prefs = ScanStorage.prefs(context)
    val existing = prefs.getString(ScanStorage.Keys.PENDING_SCANS, null)
    val list = try {
        if (existing.isNullOrBlank()) mutableListOf() 
        else com.google.gson.Gson().fromJson(existing, Array<ScanStorage.PendingScan>::class.java).toMutableList()
    } catch (e: Exception) { 
        Log.e("CameraScan", "Error parsing pending scans", e)
        mutableListOf() 
    }
    list.add(ScanStorage.PendingScan(code, timestamp))
    prefs.edit { putString(ScanStorage.Keys.PENDING_SCANS, com.google.gson.Gson().toJson(list)) }
}

private fun buildImageAnalyzer(
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    cameraExecutor: ExecutorService,
    shouldProcess: () -> Boolean,
    onScannedValue: (String) -> Unit,
): ImageAnalysis {
    val mainHandler = Handler(Looper.getMainLooper())
    val dafAnalyzer = DafImageAnalyzer(
        barcodeScanner = barcodeScanner,
        executor = cameraExecutor,
        // DafImageAnalyzer's listeners run on cameraExecutor (see `executor` above), so hop
        // back to the main thread here before this touches any Compose state.
        onScannedValue = { value -> mainHandler.post { onScannedValue(value) } },
    )
    return ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
        .also { analysis ->
            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                dafAnalyzer.process(imageProxy, shouldProcess())
            }
        }
}

private fun extractSerial(fullCode: String): String {
    return parseScannedCode(fullCode)?.serialNumber ?: (if (fullCode.length >= 18) fullCode.substring(12, 18) else fullCode.take(6))
}

@RequiresApi(Build.VERSION_CODES.S)
@ComposePreview(showBackground = true, name = "Normal Scan", apiLevel = 34)
@Composable
fun CameraScanScreenPreview() {
    JetpackComposeTheme {
        CameraScanScreenContent(
            isCameraReady = true,
            cameraError = null,
            lastSerial = "123456",
            isTorchOn = false,
            flashAlpha = 0f,
            transformableState = rememberTransformableState { _, _, _, _ -> },
            onToggleTorch = {},
            onClose = {},
            onAndroidViewFactory = { android.view.View(it) },
            isVerifyMode = false,
        )
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@ComposePreview(showBackground = true, name = "Duplicate Scan", apiLevel = 34)
@Composable
fun CameraScanScreenDuplicatePreview() {
    JetpackComposeTheme {
        CameraScanScreenContent(
            isCameraReady = true,
            cameraError = null,
            lastSerial = "123456",
            isTorchOn = false,
            flashAlpha = 0f,
            transformableState = rememberTransformableState { _, _, _, _ -> },
            onToggleTorch = {},
            onClose = {},
            onAndroidViewFactory = { android.view.View(it) },
            verifyResult = ScanFeedback(
                serial = "01C821",
                isMatch = false,
                alreadyScanned = true,
                scanTimestamp = System.currentTimeMillis() - 3600000,
            ),
            isVerifyMode = false,
        )
    }
}
