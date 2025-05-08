package com.neldasi.jetpackcompose

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.neldasi.jetpackcompose.ui.theme.JetpackComposeTheme
import androidx.camera.core.Preview as CameraPreview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JetpackComposeTheme {
                // Setup navigation
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "main_screen") {
                    composable("main_screen") { MainScreen(navController) }
                    composable("camera_screen") { CameraScreen(navController) }
                }
            }
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    val sharedPreferences = remember {
        context.getSharedPreferences("scanned_data", Context.MODE_PRIVATE)
    }

    val itemsSet = remember { mutableStateSetOf<String>() }
    var scannedText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var analysisError by remember { mutableStateOf("") }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            navController.navigate("camera_screen")
        } else {
            showPermissionDialog = true
        }
    }

    // Main content column
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
                        navController.navigate("camera_screen")
                    } else {
                        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }) {
                    Text("Scan")
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            LazyColumn {
                if (itemsSet.isEmpty()) {
                    item { CenteredTextItem("No items scanned yet") }
                } else {
                    items(itemsSet.toList()) { item -> CenteredTextItem(item) }
                }
                if (analysisError.isNotBlank()) {
                    item { CenteredTextItem(analysisError) }
                }
                if (scannedText.isNotEmpty()) {
                    item { CenteredTextItem(scannedText) }
                }
            }
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

@ExperimentalGetImage
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    val analyzer = remember {
        ImageAnalysis.Analyzer { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                BarcodeScanning.getClient().process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        val barcode = barcodes.firstOrNull()
                        barcode?.rawValue?.let {
                            Log.d("Camera", "Scanned barcode: $it")
                        }
                    }
                    .addOnFailureListener {
                        Log.e("Camera", "Failed to process image", it)
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                val previewView = PreviewView(context).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val preview = CameraPreview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val selector = CameraSelector.DEFAULT_BACK_CAMERA
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(ContextCompat.getMainExecutor(context), analyzer) }

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalysis)
                }, ContextCompat.getMainExecutor(context))

                previewView
            },
            modifier = Modifier.fillMaxSize().background(Color.Black)
        )

        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
        ) {
            Text("Close Camera")
        }
    }
}

@Composable
fun CenteredTextItem(text: String) {
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
        MainScreen(navController = rememberNavController())
    }
}
