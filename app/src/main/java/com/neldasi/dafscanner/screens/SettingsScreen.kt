
@file:OptIn(ExperimentalMaterial3Api::class)

package com.neldasi.dafscanner.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FilterNone
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material.icons.rounded.TextFormat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.neldasi.dafscanner.R
import com.neldasi.dafscanner.components.UpdateDialog
import com.neldasi.dafscanner.extras.ScanStorage
import com.neldasi.dafscanner.extras.SettingsRepository
import com.neldasi.dafscanner.extras.UpdateManager
import com.neldasi.dafscanner.ui.theme.JetpackComposeTheme
import com.neldasi.dafscanner.viewmodels.SettingsViewModel


@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val prefs = remember { ScanStorage.prefs(context) }
    
    var vibrateEnabled by remember { mutableStateOf(value = true) }
    var screenAlwaysOn by remember { mutableStateOf(value = true) }
    var continuousScanEnabled by remember { mutableStateOf(value = false) }
    var currentTheme by remember { mutableStateOf(SettingsRepository.getTheme(context)) }
    var fontSizeScale by remember { mutableFloatStateOf(SettingsRepository.getFontSizeScale(context)) }

    val updateInfo by viewModel.updateInfo.collectAsState()
    val isCheckingUpdates by viewModel.isCheckingUpdates.collectAsState()
    val updateMessage by viewModel.updateMessage.collectAsState()

    LaunchedEffect(updateMessage) {
        updateMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearUpdateMessage()
        }
    }

    LaunchedEffect(Unit) {
        vibrateEnabled = prefs.getBoolean(ScanStorage.Keys.VIBRATE_ENABLED, true)
        screenAlwaysOn = prefs.getBoolean(ScanStorage.Keys.SCREEN_ALWAYS_ON, true)
        continuousScanEnabled = prefs.getBoolean(ScanStorage.Keys.CONTINUOUS_SCAN_ENABLED, false)
    }

    SettingsScreenContent(
        navController = navController,
        vibrateEnabled = vibrateEnabled,
        onVibrateChange = {
            vibrateEnabled = it
            prefs.edit { putBoolean(ScanStorage.Keys.VIBRATE_ENABLED, it) }
        },
        screenAlwaysOn = screenAlwaysOn,
        onScreenAlwaysOnChange = {
            screenAlwaysOn = it
            prefs.edit { putBoolean(ScanStorage.Keys.SCREEN_ALWAYS_ON, it) }
        },
        continuousScanEnabled = continuousScanEnabled,
        onContinuousScanChange = {
            continuousScanEnabled = it
            prefs.edit { putBoolean(ScanStorage.Keys.CONTINUOUS_SCAN_ENABLED, it) }
        },
        currentTheme = currentTheme,
        onThemeChange = {
            currentTheme = it
            SettingsRepository.setTheme(context, it)
        },
        fontSizeScale = fontSizeScale,
        onFontSizeChange = {
            fontSizeScale = it
            SettingsRepository.setFontSizeScale(context, it)
        },
        onClearAllData = {
            viewModel.clearAllData()
            // Reset local UI state to default values immediately
            vibrateEnabled = true
            screenAlwaysOn = true
            continuousScanEnabled = false
            currentTheme = "DAF"
            fontSizeScale = 1.0f
        },
        isCheckingUpdates = isCheckingUpdates,
        updateInfo = updateInfo,
        onCheckForUpdates = { viewModel.checkForUpdates() },
        onDismissUpdate = { viewModel.dismissUpdateDialog() },
    )
}

@Composable
fun SettingsScreenContent(
    navController: NavController,
    vibrateEnabled: Boolean,
    onVibrateChange: (Boolean) -> Unit,
    screenAlwaysOn: Boolean,
    onScreenAlwaysOnChange: (Boolean) -> Unit,
    continuousScanEnabled: Boolean,
    onContinuousScanChange: (Boolean) -> Unit,
    currentTheme: String,
    onThemeChange: (String) -> Unit,
    fontSizeScale: Float,
    onFontSizeChange: (Float) -> Unit,
    onClearAllData: () -> Unit,
    isCheckingUpdates: Boolean,
    updateInfo: UpdateManager.ReleaseInfo?,
    onCheckForUpdates: () -> Unit,
    onDismissUpdate: () -> Unit,
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val allowedTypes = remember { mutableStateListOf<String>() }
    val defaultAllowedTypes = SettingsRepository.defaultAllowedTypes

    LaunchedEffect(Unit) {
        allowedTypes.addAll(SettingsRepository.loadAllowedTypes(context))
    }
    
    var showAddDialog by remember { mutableStateOf(value = false) }
    var newType by remember { mutableStateOf(value = "") }
    var showClearAllDialog by remember { mutableStateOf(value = false) }
    var showThemeDialog by remember { mutableStateOf(value = false) }
    var showFontSizeDialog by remember { mutableStateOf(value = false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_screen_title), fontWeight = FontWeight.ExtraBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    scrolledContainerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                navigationIcon = {
                    IconButton(onClick = { 
                        if (navController.previousBackStackEntry != null) {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                SettingsSection(
                    title = stringResource(R.string.settings_section_appearance),
                    collapsible = true,
                    initialExpanded = true
                ) {
                    SettingsClickableItem(
                        icon = Icons.Rounded.Palette,
                        title = stringResource(R.string.theme_setting_title),
                        subtitle = when (currentTheme) {
                            "LIGHT" -> stringResource(R.string.theme_light)
                            "DARK" -> stringResource(R.string.theme_dark)
                            "DAF" -> stringResource(R.string.theme_daf)
                            else -> stringResource(R.string.theme_system)
                        },
                        onClick = { showThemeDialog = true }
                    )
                    SettingsDivider()
                    SettingsClickableItem(
                        icon = Icons.Rounded.TextFormat,
                        title = stringResource(R.string.font_size_label),
                        subtitle = when (fontSizeScale) {
                            0.85f -> stringResource(R.string.font_size_small)
                            1.15f -> stringResource(R.string.font_size_large)
                            else -> stringResource(R.string.font_size_medium)
                        },
                        onClick = { showFontSizeDialog = true }
                    )
                }
            }

            item {
                SettingsSection(
                    title = stringResource(R.string.settings_section_scanning),
                    collapsible = true,
                    initialExpanded = false
                ) {
                    SettingsSwitchItem(
                        icon = Icons.Rounded.NotificationsActive,
                        title = stringResource(R.string.vibrate_on_scan_label),
                        checked = vibrateEnabled,
                        onCheckedChange = onVibrateChange
                    )
                    SettingsDivider()
                    SettingsSwitchItem(
                        icon = Icons.Rounded.FilterNone,
                        title = stringResource(R.string.multi_scan_mode_label),
                        checked = continuousScanEnabled,
                        onCheckedChange = onContinuousScanChange
                    )
                    SettingsDivider()
                    SettingsSwitchItem(
                        icon = Icons.Rounded.Smartphone,
                        title = stringResource(R.string.screen_always_on_label),
                        checked = screenAlwaysOn,
                        onCheckedChange = onScreenAlwaysOnChange
                    )
                }
            }

            item {
                var expanded by remember { mutableStateOf(value = false) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(R.string.allowed_types_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { expanded = !expanded }) {
                                Icon(
                                    imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            FloatingActionButton(
                                onClick = { showAddDialog = true },
                                modifier = Modifier.size(36.dp),
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                shape = RoundedCornerShape(10.dp),
                                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_type), modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = expanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                allowedTypes.forEachIndexed { index, type ->
                                    TypeItem(
                                        type = type,
                                        isDefault = type in defaultAllowedTypes,
                                        onDelete = {
                                            allowedTypes.remove(type)
                                            SettingsRepository.saveAllowedTypes(context, allowedTypes)
                                        }
                                    )
                                    if (index < allowedTypes.size - 1) {
                                        SettingsDivider()
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                SettingsSection(
                    title = stringResource(R.string.settings_section_system),
                    collapsible = true,
                    initialExpanded = true
                ) {
                    SettingsClickableItem(
                        icon = Icons.Rounded.CameraAlt,
                        title = stringResource(R.string.camera_permission_settings),
                        subtitle = stringResource(R.string.camera_permission_settings_desc),
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.data = Uri.parse("package:${context.packageName}")
                            context.startActivity(intent)
                        }
                    )
                    SettingsDivider()
                    SettingsClickableItem(
                        icon = Icons.Rounded.SystemUpdateAlt,
                        title = if (isCheckingUpdates) stringResource(R.string.checking_updates) else stringResource(R.string.check_for_updates),
                        subtitle = stringResource(R.string.check_github_desc),
                        onClick = onCheckForUpdates
                    )
                    SettingsDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showClearAllDialog = true }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = null,
                                modifier = Modifier.padding(8.dp).size(24.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_clear_all),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.settings_clear_all_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
            item {
                val packageInfo = remember {
                    try {
                        context.packageManager.getPackageInfo(context.packageName, 0)
                    } catch (_: Exception) {
                        null
                    }
                }
                val versionText = packageInfo?.let {
                    val vCode = androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(it)
                    "v${it.versionName} ($vCode)"
                } ?: "v1.0 (1)"

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = versionText,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text(stringResource(R.string.add_allowed_type_title), fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = newType,
                        onValueChange = { newType = it },
                        label = { Text(stringResource(R.string.type_code_label)) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newType.isNotBlank()) {
                                allowedTypes.add(newType)
                                SettingsRepository.saveAllowedTypes(context, allowedTypes)
                                newType = ""
                                showAddDialog = false
                            } else {
                                Toast.makeText(context, R.string.type_not_empty_message, Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.add))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        if (showClearAllDialog) {
            AlertDialog(
                onDismissRequest = { showClearAllDialog = false },
                title = { Text(stringResource(R.string.clear_confirm_title), fontWeight = FontWeight.Bold) },
                text = { Text(stringResource(R.string.clear_confirm_text)) },
                confirmButton = {
                    Button(
                        onClick = {
                            onClearAllData()
                            // Clear and reset allowedTypes locally
                            allowedTypes.clear()
                            allowedTypes.addAll(defaultAllowedTypes)
                            Toast.makeText(context, R.string.reset_complete, Toast.LENGTH_SHORT).show()
                            showClearAllDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.yes))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearAllDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        if (showFontSizeDialog) {
            AlertDialog(
                onDismissRequest = { showFontSizeDialog = false },
                title = { Text(stringResource(R.string.font_size_label), fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeOption(
                            title = stringResource(R.string.font_size_small),
                            selected = fontSizeScale == 0.85f,
                            onClick = { onFontSizeChange(0.85f); showFontSizeDialog = false }
                        )
                        ThemeOption(
                            title = stringResource(R.string.font_size_medium),
                            selected = fontSizeScale == 1.0f,
                            onClick = { onFontSizeChange(1.0f); showFontSizeDialog = false }
                        )
                        ThemeOption(
                            title = stringResource(R.string.font_size_large),
                            selected = fontSizeScale == 1.15f,
                            onClick = { onFontSizeChange(1.15f); showFontSizeDialog = false }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFontSizeDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        if (showThemeDialog) {
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                title = { Text(stringResource(R.string.theme_setting_title), fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeOption(
                            title = stringResource(R.string.theme_daf),
                            selected = currentTheme == "DAF",
                            onClick = { onThemeChange("DAF"); showThemeDialog = false }
                        )
                        ThemeOption(
                            title = stringResource(R.string.theme_system),
                            selected = currentTheme == "SYSTEM",
                            onClick = { onThemeChange("SYSTEM"); showThemeDialog = false }
                        )
                        ThemeOption(
                            title = stringResource(R.string.theme_light),
                            selected = currentTheme == "LIGHT",
                            onClick = { onThemeChange("LIGHT"); showThemeDialog = false }
                        )
                        ThemeOption(
                            title = stringResource(R.string.theme_dark),
                            selected = currentTheme == "DARK",
                            onClick = { onThemeChange("DARK"); showThemeDialog = false }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showThemeDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        updateInfo?.let { info ->
            UpdateDialog(info = info, onDismiss = onDismissUpdate)
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    collapsible: Boolean = false,
    initialExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(initialExpanded) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = collapsible) { expanded = !expanded }
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            if (collapsible) {
                Icon(
                    imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        AnimatedVisibility(
            visible = !collapsible || expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(4.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
private fun ThemeOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SettingsClickableItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(8.dp).size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(8.dp).size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun TypeItem(
    type: String,
    isDefault: Boolean,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            type,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        if (!isDefault) {
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = CircleShape
            ) {
                Text(
                    "Default",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@Preview(showBackground = true, apiLevel = 36)
@Composable
fun SettingsScreenPreview() {
    JetpackComposeTheme {
        SettingsScreenContent(
            navController = rememberNavController(),
            vibrateEnabled = true,
            onVibrateChange = {},
            screenAlwaysOn = true,
            onScreenAlwaysOnChange = {},
            continuousScanEnabled = true,
            onContinuousScanChange = {},
            currentTheme = "SYSTEM",
            onThemeChange = {},
            fontSizeScale = 1.0f,
            onFontSizeChange = {},
            onClearAllData = {},
            isCheckingUpdates = false,
            updateInfo = null,
            onCheckForUpdates = {},
            onDismissUpdate = {}
        )
    }
}
