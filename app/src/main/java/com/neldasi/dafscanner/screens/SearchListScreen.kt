package com.neldasi.dafscanner.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.PrecisionManufacturing
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.neldasi.dafscanner.R
import com.neldasi.dafscanner.data.SearchItem
import com.neldasi.dafscanner.navigation.CameraRoute
import com.neldasi.dafscanner.ui.theme.JetpackComposeTheme
import com.neldasi.dafscanner.viewmodels.SearchListViewModel
import com.neldasi.dafscanner.viewmodels.SearchSortOption
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SearchListScreen(
    navController: NavController,
    viewModel: SearchListViewModel = viewModel(),
) {
    val context = LocalContext.current
    val searchItems by viewModel.filteredItems.collectAsStateWithLifecycle()
    val allFilteredItems by viewModel.allFilteredItems.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val machineFilter by viewModel.machineFilter.collectAsStateWithLifecycle()
    val typeFilter by viewModel.typeFilter.collectAsStateWithLifecycle()
    val availableMachines by viewModel.availableMachines.collectAsStateWithLifecycle()
    val availableTypes by viewModel.availableTypes.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val hasMoreItems by viewModel.hasMoreItems.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val scannedCount by viewModel.scannedCount.collectAsStateWithLifecycle()
    
    var showDeleteConfirmation by remember { mutableStateOf(value = false) }
    var showShareOptions by remember { mutableStateOf(value = false) }
    var isSearchActive by remember { mutableStateOf(searchQuery.isNotEmpty()) }

    val statusFound = stringResource(R.string.status_found)
    val statusMissing = stringResource(R.string.status_missing)
    val shareChooserTitle = stringResource(R.string.share_csv_chooser)
    val summaryTitle = stringResource(R.string.verification_summary_title)
    val progressLabel = stringResource(R.string.scanned_progress)
    val foundSectionLabel = stringResource(R.string.found_section)
    val missingSectionLabel = stringResource(R.string.missing_section)
    val summarySubject = stringResource(R.string.verification_summary_subject)
    val summaryChooserTitle = stringResource(R.string.share_summary_chooser)

    LaunchedEffect(Unit) {
        viewModel.initStorage(context)
    }

    val csvPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let { viewModel.loadCsv(context, it) }
    }

    LaunchedEffect(searchItems) {
        navController.currentBackStackEntry?.savedStateHandle?.set("SERIAL_LIST", searchItems.map { it.serialNumber })
        val scannedSerials = searchItems.asSequence()
            .filter { it.scanTimestamp != null }
            .map { it.serialNumber }
            .toList()
        navController.currentBackStackEntry?.savedStateHandle?.set("SCANNED_SERIALS", scannedSerials)
    }

    SearchListContent(
        searchItems = searchItems,
        searchQuery = searchQuery,
        onSearchQueryChange = { viewModel.onSearchQueryChange(context, it) },
        isSearchActive = isSearchActive,
        onSearchActiveChange = { isSearchActive = it },
        sortOption = sortOption,
        onSortOptionChange = { viewModel.onSortOptionChange(context, it) },
        machineFilter = machineFilter,
        onMachineFilterChange = { viewModel.onMachineFilterChange(context, it) },
        typeFilter = typeFilter,
        onTypeFilterChange = { viewModel.onTypeFilterChange(context, it) },
        availableMachines = availableMachines,
        availableTypes = availableTypes,
        isLoading = isLoading,
        onLoadMore = { viewModel.loadMore() },
        showDeleteConfirmation = showDeleteConfirmation,
        onShowDeleteConfirmationChange = { showDeleteConfirmation = it },
        showShareOptions = showShareOptions,
        onShowShareOptionsChange = { showShareOptions = it },
        onBackClick = { 
            if (navController.previousBackStackEntry != null) {
                navController.popBackStack()
            }
        },
        onClearListClick = { viewModel.clearList(context) },
        onScanClick = { navController.navigate(CameraRoute(isVerifyMode = true)) },
        onImportCsvClick = {
            if (!isLoading) {
                csvPickerLauncher.launch("text/comma-separated-values")
            }
        },
        hasMoreItems = hasMoreItems,
        totalCount = totalCount,
        scannedCount = scannedCount,
        onShareCsv = {
            if (allFilteredItems.isEmpty()) return@SearchListContent
            
            val sb = StringBuilder()
            sb.append("Order;Status;HEX;DEC;Type;Machine;Scan Time\n")
            
            val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            allFilteredItems.forEach { item ->
                val isFound = item.scanTimestamp != null
                val status = if (isFound) statusFound else statusMissing
                val order = item.scanOrder?.toString() ?: "-"
                val time = if (item.scanTimestamp != null) timeFormatter.format(Date(item.scanTimestamp)) else "-"
                
                sb.append("$order;$status;${item.serialNumber};${item.decSerial};${item.typeCode};${item.machine ?: "-"};$time\n")
            }
            
            try {
                val cachePath = File(context.cacheDir, "exports")
                cachePath.mkdirs()
                val file = File(cachePath, "verification_results.csv")
                file.writeText(sb.toString())
                
                val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, shareChooserTitle))
            } catch (e: Exception) {
                Log.e("SearchListScreen", "Error sharing CSV file", e)
            }
        },
        onShareSummary = {
            if (allFilteredItems.isEmpty()) return@SearchListContent
            
            val total = allFilteredItems.size
            val foundItems = allFilteredItems.filter { it.scanTimestamp != null }.sortedBy { it.scanOrder }
            val missingItems = allFilteredItems.filter { it.scanTimestamp == null }
            
            val sb = StringBuilder()
            val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            sb.append(summaryTitle + "\n")
            
            // Add filter info to summary if applicable
            if ((machineFilter != null) || (typeFilter != null)) {
                sb.append("Filtered by: ")
                val filters = mutableListOf<String>()
                machineFilter?.let { filters.add("Machine: $it") }
                typeFilter?.let { filters.add("Type: $it") }
                sb.append(filters.joinToString(", ") + "\n")
            }

            sb.append(progressLabel.format(foundItems.size, total) + "\n\n")
            
            if (foundItems.isNotEmpty()) {
                sb.append(foundSectionLabel.format(foundItems.size) + "\n")
                foundItems.forEach { item ->
                    val time = if (item.scanTimestamp != null) timeFormatter.format(Date(item.scanTimestamp)) else "-"
                    sb.append("${item.scanOrder}. HEX: ${item.serialNumber} (DEC: ${item.decSerial}) - ${item.typeCode} - $time\n")
                }
                sb.append("\n")
            }
            
            if (missingItems.isNotEmpty()) {
                sb.append(missingSectionLabel.format(missingItems.size) + "\n")
                missingItems.forEach { item ->
                    sb.append("- HEX: ${item.serialNumber} (DEC: ${item.decSerial}) - ${item.typeCode}\n")
                }
            }
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, summarySubject.format(foundItems.size, total))
                putExtra(Intent.EXTRA_TEXT, sb.toString())
            }
            context.startActivity(Intent.createChooser(shareIntent, summaryChooserTitle))
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchListContent(
    searchItems: List<SearchItem>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isSearchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    sortOption: SearchSortOption,
    onSortOptionChange: (SearchSortOption) -> Unit,
    machineFilter: String?,
    onMachineFilterChange: (String?) -> Unit,
    typeFilter: String?,
    onTypeFilterChange: (String?) -> Unit,
    availableMachines: List<String>,
    availableTypes: List<String>,
    isLoading: Boolean,
    onLoadMore: () -> Unit,
    showDeleteConfirmation: Boolean,
    onShowDeleteConfirmationChange: (Boolean) -> Unit,
    showShareOptions: Boolean,
    onShowShareOptionsChange: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    onClearListClick: () -> Unit,
    onScanClick: () -> Unit,
    onImportCsvClick: () -> Unit,
    hasMoreItems: Boolean,
    totalCount: Int,
    scannedCount: Int,
    onShareCsv: () -> Unit,
    onShareSummary: () -> Unit,
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showMachineMenu by remember { mutableStateOf(false) }
    var showTypeMenu by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { onShowDeleteConfirmationChange(false) },
            title = { Text(stringResource(R.string.clear_list_title)) },
            text = { Text(stringResource(R.string.clear_list_text)) },
            confirmButton = {
                Button(
                    onClick = {
                        onClearListClick()
                        onShowDeleteConfirmationChange(false)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { onShowDeleteConfirmationChange(false) }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showShareOptions) {
        AlertDialog(
            onDismissRequest = { onShowShareOptionsChange(false) },
            title = { Text(stringResource(R.string.share_results_title)) },
            text = { Text(stringResource(R.string.share_results_text)) },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onShareSummary()
                            onShowShareOptionsChange(false)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Description, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.summary_report))
                    }
                    Button(
                        onClick = {
                            onShareCsv()
                            onShowShareOptionsChange(false)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Rounded.TableChart, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.csv_excel_format))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = { onShowShareOptionsChange(false) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.verify_serials_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    scrolledContainerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (searchItems.isNotEmpty() || searchQuery.isNotEmpty() || machineFilter != null || typeFilter != null) {
                        // Sort Menu
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.Sort,
                                    contentDescription = stringResource(R.string.sort_by),
                                    tint = if (sortOption != SearchSortOption.DEFAULT) Color.Red else Color.White
                                )
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface).width(220.dp)
                            ) {
                                DropdownHeader(stringResource(R.string.sort_by), Icons.AutoMirrored.Rounded.Sort)
                                SortMenuItem(SearchSortOption.DEFAULT, R.string.sort_default, Icons.Rounded.FilterList, sortOption, onSortOptionChange) { showSortMenu = false }
                                SortMenuItem(SearchSortOption.MACHINE, R.string.sort_machine, Icons.Rounded.PrecisionManufacturing, sortOption, onSortOptionChange) { showSortMenu = false }
                                SortMenuItem(SearchSortOption.TYPE, R.string.sort_type, Icons.Rounded.Inventory2, sortOption, onSortOptionChange) { showSortMenu = false }
                                SortMenuItem(SearchSortOption.FOUND_TIME, R.string.sort_found_time, Icons.Rounded.Schedule, sortOption, onSortOptionChange) { showSortMenu = false }
                                SortMenuItem(SearchSortOption.PRODUCTION_TIME, R.string.sort_production_time, Icons.Rounded.TableChart, sortOption, onSortOptionChange) { showSortMenu = false }
                            }
                        }
                        IconButton(onClick = { onShowShareOptionsChange(true) }) {
                            Icon(Icons.Rounded.Share, contentDescription = stringResource(R.string.share))
                        }
                        IconButton(onClick = { onShowDeleteConfirmationChange(true) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                if (searchItems.isEmpty() && searchQuery.isEmpty() && machineFilter == null && typeFilter == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            modifier = Modifier
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Rounded.FileUpload,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                stringResource(R.string.no_list_loaded),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                stringResource(R.string.import_csv_instruction),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = onImportCsvClick,
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isLoading
                            ) {
                                Icon(Icons.Rounded.FileOpen, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.import_csv))
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 140.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (searchItems.isEmpty()) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                        Text("No results match your filters.", color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }

                            items(searchItems, key = { it.serialNumber }) { item ->
                                val isScanned = item.scanTimestamp != null
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isScanned) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                                                         else MaterialTheme.colorScheme.surface
                                    ),
                                    border = if (isScanned) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.secondary) 
                                             else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                stringResource(R.string.hex_prefix),
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isScanned) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                                            )
                                                            Text(
                                                                text = item.serialNumber,
                                                                style = MaterialTheme.typography.headlineMedium,
                                                                fontWeight = FontWeight.Black,
                                                                color = if (isScanned) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurface
                                                            )
                                                        }
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                stringResource(R.string.dec_prefix),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isScanned) MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f) else Color(0xFF388E3C)
                                                            )
                                                            Text(
                                                                text = item.decSerial,
                                                                style = MaterialTheme.typography.titleMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isScanned) Color(0xFFD32F2F).copy(alpha = 0.8f) else MaterialTheme.colorScheme.secondary
                                                            )
                                                        }
                                                    }

                                                    Column(
                                                        horizontalAlignment = Alignment.End,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        if ((isScanned && item.scanOrder != null)) {
                                                            Surface(
                                                                color = MaterialTheme.colorScheme.secondary,
                                                                shape = CircleShape,
                                                                modifier = Modifier.size(32.dp)
                                                            ) {
                                                                Box(contentAlignment = Alignment.Center) {
                                                                    Text(
                                                                        text = item.scanOrder.toString(),
                                                                        style = MaterialTheme.typography.titleMedium,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = Color.White
                                                                    )
                                                                }
                                                            }
                                                        } else {
                                                            Icon(
                                                                Icons.Rounded.Tag,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.secondary,
                                                                modifier = Modifier.size(28.dp)
                                                            )
                                                        }

                                                        if (isScanned) {
                                                            Spacer(Modifier.height(4.dp))
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text(
                                                                    text = stringResource(R.string.found_at_label),
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                                                                )
                                                                Spacer(Modifier.width(4.dp))
                                                                Text(
                                                                    text = timeFormatter.format(Date(item.scanTimestamp)),
                                                                    style = MaterialTheme.typography.bodyMedium,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                HorizontalDivider(
                                                    modifier = Modifier.padding(vertical = 8.dp),
                                                    thickness = 0.5.dp,
                                                    color = MaterialTheme.colorScheme.outlineVariant
                                                )

                                                if (!item.machine.isNullOrBlank()) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            Icons.Rounded.PrecisionManufacturing,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp),
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                        Spacer(Modifier.width(6.dp))
                                                        Text(
                                                            text = item.machine,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                }

                                                if (!item.outputMaterial.isNullOrBlank()) {
                                                    Row(
                                                        modifier = Modifier.padding(top = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            Icons.Rounded.Inventory2,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp),
                                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                                        )
                                                        Spacer(Modifier.width(6.dp))
                                                        Text(
                                                            text = item.outputMaterial,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 1
                                                        )
                                                    }
                                                } else if (item.typeCode != "UNKNOWN") {
                                                    Row(
                                                        modifier = Modifier.padding(top = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            Icons.Rounded.Inventory2,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp),
                                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                                        )
                                                        Spacer(Modifier.width(6.dp))
                                                        Text(
                                                            text = item.typeCode,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }

                                                val dateTimeText = remember(item) {
                                                    when {
                                                        item.startDate != null && item.startTime != null -> {
                                                            val start = "${item.startDate} ${item.startTime}"
                                                            if (item.completeTime != null) {
                                                                if (item.startDate == item.completeDate || item.completeDate == null) {
                                                                    "$start - ${item.completeTime}"
                                                                } else {
                                                                    "$start - ${item.completeDate} ${item.completeTime}"
                                                                }
                                                            } else {
                                                                start
                                                            }
                                                        }
                                                        else -> null
                                                    }
                                                }

                                                if (dateTimeText != null) {
                                                    Spacer(Modifier.height(6.dp))
                                                    Surface(
                                                        color = Color(0xFFD32F2F).copy(alpha = 0.08f),
                                                        shape = RoundedCornerShape(4.dp),
                                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFD32F2F).copy(alpha = 0.2f))
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                Icons.Rounded.Schedule,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(14.dp),
                                                                tint = Color(0xFFD32F2F)
                                                            )
                                                            Spacer(Modifier.width(6.dp))
                                                            Text(
                                                                text = stringResource(R.string.production_label),
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = Color(0xFFD32F2F)
                                                            )
                                                            Spacer(Modifier.width(4.dp))
                                                            Text(
                                                                text = dateTimeText,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFFD32F2F)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            if (hasMoreItems) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }
                                    LaunchedEffect(Unit) {
                                        onLoadMore()
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Floating Search and Scan Bar (MainScreen Style)
            if (searchItems.isNotEmpty() || searchQuery.isNotEmpty() || machineFilter != null || typeFilter != null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                        .imePadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (totalCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp,
                            shadowElevation = 4.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.BarChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                val countColor = if (scannedCount == totalCount && totalCount > 0) Color(0xFF2196F3) else Color(0xFFD32F2F)
                                Text(
                                    text = buildAnnotatedString {
                                        append(stringResource(R.string.progress_label))
                                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = countColor)) {
                                            append(" $scannedCount")
                                        }
                                        append(" / $totalCount")
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(32.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AnimatedVisibility(
                                visible = isSearchActive,
                                enter = fadeIn(animationSpec = tween(600)) + expandHorizontally(
                                    animationSpec = tween(600),
                                    expandFrom = Alignment.End
                                ),
                                exit = fadeOut(animationSpec = tween(600)) + shrinkHorizontally(
                                    animationSpec = tween(600),
                                    shrinkTowards = Alignment.End
                                )
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = onSearchQueryChange,
                                    placeholder = { 
                                        Text(
                                            stringResource(R.string.search_items),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        ) 
                                    },
                                    modifier = Modifier.fillMaxWidth(0.75f),
                                    shape = RoundedCornerShape(24.dp),
                                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    trailingIcon = {
                                        IconButton(onClick = { 
                                            onSearchQueryChange("")
                                            onSearchActiveChange(false)
                                        }) {
                                            Icon(Icons.Rounded.Clear, contentDescription = stringResource(R.string.clear_search), tint = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        focusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }

                            if (!isSearchActive) {
                                FloatingActionButton(
                                    onClick = { onSearchActiveChange(true) },
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape,
                                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                                ) {
                                    Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.search_items))
                                }

                                // Machine Filter
                                Box {
                                    FloatingActionButton(
                                        onClick = { showMachineMenu = true },
                                        containerColor = if (machineFilter != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = if (machineFilter != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                        shape = CircleShape,
                                        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                                    ) {
                                        Icon(Icons.Rounded.PrecisionManufacturing, contentDescription = "Machine Filter")
                                    }
                                    DropdownMenu(
                                        expanded = showMachineMenu, 
                                        onDismissRequest = { showMachineMenu = false },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surface).width(220.dp)
                                    ) {
                                        DropdownHeader(stringResource(R.string.filter_by_machine), Icons.Rounded.PrecisionManufacturing)
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.all_machines), fontWeight = if (machineFilter == null) FontWeight.Bold else FontWeight.Normal) },
                                            onClick = { onMachineFilterChange(null); showMachineMenu = false },
                                            leadingIcon = { Icon(Icons.Rounded.Clear, contentDescription = null, tint = MaterialTheme.colorScheme.outline) },
                                            trailingIcon = { if (machineFilter == null) Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                                        )
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
                                        availableMachines.forEach { machine ->
                                            DropdownMenuItem(
                                                text = { Text(machine, style = MaterialTheme.typography.bodyMedium) },
                                                onClick = { onMachineFilterChange(machine); showMachineMenu = false },
                                                trailingIcon = { if (machine == machineFilter) Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                                            )
                                        }
                                    }
                                }

                                // Type Filter
                                Box {
                                    FloatingActionButton(
                                        onClick = { showTypeMenu = true },
                                        containerColor = if (typeFilter != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = if (typeFilter != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                        shape = CircleShape,
                                        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                                    ) {
                                        Icon(Icons.Rounded.Inventory2, contentDescription = "Type Filter")
                                    }
                                    DropdownMenu(
                                        expanded = showTypeMenu, 
                                        onDismissRequest = { showTypeMenu = false },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surface).width(220.dp)
                                    ) {
                                        DropdownHeader(stringResource(R.string.filter_by_type), Icons.Rounded.Inventory2)
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.all_types), fontWeight = if (typeFilter == null) FontWeight.Bold else FontWeight.Normal) },
                                            onClick = { onTypeFilterChange(null); showTypeMenu = false },
                                            leadingIcon = { Icon(Icons.Rounded.Clear, contentDescription = null, tint = MaterialTheme.colorScheme.outline) },
                                            trailingIcon = { if (typeFilter == null) Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                                        )
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
                                        availableTypes.forEach { type ->
                                            DropdownMenuItem(
                                                text = { Text(type, style = MaterialTheme.typography.bodyMedium) },
                                                onClick = { onTypeFilterChange(type); showTypeMenu = false },
                                                trailingIcon = { if (type == typeFilter) Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                                            )
                                        }
                                    }
                                }

                                FloatingActionButton(
                                    onClick = onScanClick,
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary,
                                    shape = CircleShape,
                                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                                ) {
                                    Icon(Icons.Rounded.QrCodeScanner, contentDescription = "Scan")
                                }
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text(stringResource(R.string.loading))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DropdownHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    }
}

@Composable
private fun SortMenuItem(
    option: SearchSortOption,
    labelRes: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    currentOption: SearchSortOption,
    onOptionChange: (SearchSortOption) -> Unit,
    onDismiss: () -> Unit
) {
    val isSelected = currentOption == option
    DropdownMenuItem(
        text = { 
            Text(
                stringResource(labelRes), 
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.bodyMedium
            ) 
        },
        onClick = {
            onOptionChange(option)
            onDismiss()
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (isSelected) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

@RequiresApi(Build.VERSION_CODES.S)
@Preview(showBackground = true, apiLevel = 36)
@Composable
fun SearchListEmptyPreview() {
    JetpackComposeTheme {
        SearchListContent(
            searchItems = emptyList(),
            searchQuery = "",
            onSearchQueryChange = {},
            isSearchActive = false,
            onSearchActiveChange = {},
            sortOption = SearchSortOption.DEFAULT,
            onSortOptionChange = {},
            machineFilter = null,
            onMachineFilterChange = {},
            typeFilter = null,
            onTypeFilterChange = {},
            availableMachines = emptyList(),
            availableTypes = emptyList(),
            isLoading = false,
            onLoadMore = {},
            showDeleteConfirmation = false,
            onShowDeleteConfirmationChange = {},
            showShareOptions = false,
            onShowShareOptionsChange = {},
            onBackClick = {},
            onClearListClick = {},
            onScanClick = {},
            onImportCsvClick = {},
            hasMoreItems = false,
            totalCount = 0,
            scannedCount = 0,
            onShareCsv = {},
            onShareSummary = {}
        )
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@Preview(showBackground = true, apiLevel = 36)
@Composable
fun SearchListWithDataPreview() {
    val mockItems = listOf(
        SearchItem(
            typeCode = "2261325", 
            serialNumber = "01C821", 
            decSerial = "116769", 
            scanTimestamp = System.currentTimeMillis(), 
            scanOrder = 1,
            machine = "19602 (JUNKER CNC)",
            outputMaterial = "2261325 (MX13 MY21)",
            startDate = "11-5-2023",
            startTime = "00:02:09",
            completeTime = "00:17:07"
        ),
        SearchItem(
            typeCode = "2002046", 
            serialNumber = "01C822", 
            decSerial = "116770",
            machine = "19602 (JUNKER CNC)",
            outputMaterial = "2002046 (NOKKENAS)",
            startDate = "11-5-2023",
            startTime = "00:17:20",
            completeTime = "00:28:56"
        ),
        SearchItem(
            typeCode = "TYPE789", 
            serialNumber = "01C823", 
            decSerial = "116771", 
            scanTimestamp = System.currentTimeMillis(), 
            scanOrder = 2,
            machine = "19602 (JUNKER CNC)",
            outputMaterial = "TYPE789 (MATERIAL)",
            startDate = "10-5-2023",
            startTime = "23:33:17",
            completeDate = "11-5-2023",
            completeTime = "00:01:55"
        )
    )
    JetpackComposeTheme {
        SearchListContent(
            searchItems = mockItems,
            searchQuery = "",
            onSearchQueryChange = {},
            isSearchActive = false,
            onSearchActiveChange = {},
            sortOption = SearchSortOption.DEFAULT,
            onSortOptionChange = {},
            machineFilter = null,
            onMachineFilterChange = {},
            typeFilter = null,
            onTypeFilterChange = {},
            availableMachines = emptyList(),
            availableTypes = emptyList(),
            isLoading = false,
            onLoadMore = {},
            showDeleteConfirmation = false,
            onShowDeleteConfirmationChange = {},
            showShareOptions = false,
            onShowShareOptionsChange = {},
            onBackClick = {},
            onClearListClick = {},
            onScanClick = {},
            onImportCsvClick = {},
            hasMoreItems = false,
            totalCount = mockItems.size,
            scannedCount = mockItems.count { it.scanTimestamp != null },
            onShareCsv = {},
            onShareSummary = {}
        )
    }
}
