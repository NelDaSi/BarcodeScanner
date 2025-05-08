package com.neldasi.jetpackcompose

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.neldasi.jetpackcompose.ui.theme.JetpackComposeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JetpackComposeTheme {
                Scaffold { innerPadding ->
                    MainScreenContent(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(modifier: Modifier = Modifier) {
    val isInPreview = LocalInspectionMode.current

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember(context) { ProcessCameraProvider.getInstance(context) }

    val sharedPreferences = remember {
        context.getSharedPreferences("scanned_data", Context.MODE_PRIVATE)
    }

    val itemsSet = remember { mutableStateSetOf<String>() }
    var scannedText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var analysisError by remember { mutableStateOf("") }

    @SuppressLint("ApplySharedPref")
    fun saveItems(items: Set<String>) {
        with(sharedPreferences.edit()) {
            putStringSet("items", items)
            commit()
        }
    }

    LaunchedEffect(Unit) {
        val savedData = sharedPreferences.getStringSet("items", emptySet()) ?: emptySet()
        itemsSet.addAll(savedData)
    }

    val analyzer = remember {
        ImageAnalysis.Analyzer { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val inputImage = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )
                BarcodeScanning.getClient().process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        val newBarcode = barcodes.firstOrNull { barcode ->
                            val value = barcode.rawValue
                            value != null && value !in itemsSet
                        }
                        newBarcode?.rawValue?.let { value ->
                            itemsSet.add(value)
                            saveItems(itemsSet.toSet())
                            scannedText = value
                            showCamera = false
                            analysisError = ""
                        }
                    }
                    .addOnFailureListener {
                        Log.e("qrAnalyzer", "Failed to process image", it)
                        analysisError = "Failed to process image, please try again"
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            showCamera = true
        } else {
            showPermissionDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My App") },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Open Menu")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(text = { Text("Settings") }, onClick = { expanded = false })
                        DropdownMenuItem(text = { Text("About") }, onClick = { expanded = false })
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(onClick = {
                    val permissionCheck = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.CAMERA
                    )
                    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                        showCamera = true
                    } else {
                        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }) {
                    Text("Scan")
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = modifier.padding(innerPadding)) {
            LazyColumn {
                if(itemsSet.isEmpty()){
                    item { CenteredTextItem("No items scanned yet") }
                } else {
                    items(itemsSet.toList()) { item -> CenteredTextItem(item) }
                }
                if(analysisError.isNotBlank()){
                    item { CenteredTextItem(analysisError) }
                }
                if (scannedText.isNotEmpty()) {
                    item { CenteredTextItem(scannedText) }
                }
            }

            if (!isInPreview && showCamera) {
                AndroidView(
                    factory = { context ->
                        val previewView = PreviewView(context).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }

                        val preview = androidx.camera.core.Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val selector = CameraSelector.DEFAULT_BACK_CAMERA
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(ContextCompat.getMainExecutor(context), analyzer)
                            }

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner, selector, preview, imageAnalysis
                            )
                        }, ContextCompat.getMainExecutor(context))

                        previewView
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color.Black)
                )
                DisposableEffect(cameraProviderFuture, lifecycleOwner) {
                    onDispose {
                        val cameraProvider = cameraProviderFuture.get()
                        cameraProvider.unbindAll()
                    }
                }
                Button(onClick = { showCamera = false }) {
                    Text("Close Camera")
                }
            }

            if (showPermissionDialog) {
                AlertDialog(
                    onDismissRequest = { showPermissionDialog = false },
                    title = { Text("Permission Required") },
                    text = { Text("Camera permission is required to scan QR codes.") },
                    confirmButton = {
                        Button(onClick = {
                            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }) {
                            Text("Grant")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showPermissionDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun CenteredTextItem(text:String){
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        textAlign = TextAlign.Center
    )
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    JetpackComposeTheme {
        MainScreenContent()
    }
}
