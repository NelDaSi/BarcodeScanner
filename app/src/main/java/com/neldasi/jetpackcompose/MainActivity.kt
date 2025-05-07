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
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
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
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    val sharedPreferences = remember {
        context.getSharedPreferences("scanned_data", Context.MODE_PRIVATE)
    }

    val itemsSet = remember { mutableStateSetOf<String>() }
    var scannedText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var analysisError by remember { mutableStateOf("") }

    @SuppressLint("UseKtx")
    fun saveItems(items: Set<String>) {
        with(sharedPreferences.edit()) {
            putStringSet("items", items)
            commit()
        }
    }

    // Load saved items on initial composition
    LaunchedEffect(Unit) {
        val savedData = sharedPreferences.getStringSet("items", emptySet()) ?: emptySet()
        itemsSet.addAll(savedData)
    }

    val analyzer = remember {
        ImageAnalysis.Analyzer { imageProxy ->
            // Retrieve the image from the image proxy
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                // Convert the image to an InputImage for ML Kit processing
                val inputImage = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )
                // Process the image with BarcodeScanning client
                BarcodeScanning.getClient().process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        // Find the first barcode that is new and not already in the itemsSet
                        val newBarcode = barcodes.firstOrNull { barcode ->
                            val value = barcode.rawValue
                            value != null && value !in itemsSet
                        }

                        // If a new barcode is found, add it to the set, save it, and update states
                        newBarcode?.rawValue?.let { value ->
                            itemsSet.add(value)
                            saveItems(itemsSet.toSet())
                            scannedText = value

                            // Hide the camera preview after scanning
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


    // Requesting Camera permission
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            showCamera = true
        } else {
            showPermissionDialog = true
        }
    }

    // Layout of the main screen with the topBar and bottomBar
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My App") },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    // Dropdown menu in the top app bar
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
                Button(onClick = { // Handle camera permission check and launch request
                    //checking if permission is granted
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
        // Main content column
        Column(modifier = modifier.padding(innerPadding)) {
            LazyColumn {
                if(itemsSet.isEmpty()){
                    item{
                        CenteredTextItem("No items scanned yet")
                    }
                }else{
                    // Display each item in the set in a lazy list
                    items(itemsSet.toList()) { item ->
                        CenteredTextItem(item)

                    }
                }
                if(analysisError.isNotBlank()){
                    item{
                        CenteredTextItem(analysisError)
                    }
                }
                if (scannedText.isNotEmpty()) {
                    item {
                        CenteredTextItem(scannedText)
                    }
                }
            }

            // Display camera preview if showCamera is true
            if (showCamera) {
                AndroidView(
                    factory = { context ->
                        // Create a PreviewView to display the camera feed
                        val previewView = PreviewView(context).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }

                        // Build a Preview object to use for displaying the camera feed
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        // Define the camera selector (back camera)
                        val selector = CameraSelector.DEFAULT_BACK_CAMERA
                        val imageAnalysis = ImageAnalysis.Builder()
                            // Set the backpressure strategy for the analyzer
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(ContextCompat.getMainExecutor(context), analyzer)
                            }

                        cameraProviderFuture.addListener({
                            // Bind the camera to the lifecycle and start the camera stream
                            try {
                                val cameraProvider = cameraProviderFuture.get()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner, selector, preview, imageAnalysis
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
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
                        // Unbind all use cases when the composable is disposed
                        val cameraProvider = cameraProviderFuture.get()
                        cameraProvider.unbindAll()
                    }
                }
                Button(onClick = { showCamera = false }) {
                    Text("Close Camera")
                }
            }
            // Alert Dialog for permission request
            //Permission request Alert
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