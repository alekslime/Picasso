package app.picasso.tracer.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import app.picasso.tracer.ui.home.AdBanner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.picasso.tracer.R
import app.picasso.tracer.domain.model.ThemeMode
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    val errorRes by viewModel.errorRes.collectAsState()
    val proPrice by viewModel.proPrice.collectAsState()
    val adsReady by viewModel.adsReady.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.loadProPrice()
        activity?.let { viewModel.ensureAdsConsent(it) }
    }
    LaunchedEffect(errorRes) {
        errorRes?.let {
            snackbar.showSnackbar(context.getString(it))
            viewModel.clearError()
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            SectionLabel(stringResource(R.string.appearance))
            Spacer(Modifier.height(12.dp))
            ThemeRow(
                label = stringResource(R.string.theme_system),
                selected = settings.themeMode == ThemeMode.SYSTEM,
                onSelect = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
            )
            Spacer(Modifier.height(8.dp))
            ThemeRow(
                label = stringResource(R.string.theme_light),
                selected = settings.themeMode == ThemeMode.LIGHT,
                onSelect = { viewModel.setThemeMode(ThemeMode.LIGHT) },
            )
            Spacer(Modifier.height(8.dp))
            ThemeRow(
                label = stringResource(R.string.theme_dark),
                selected = settings.themeMode == ThemeMode.DARK,
                onSelect = { viewModel.setThemeMode(ThemeMode.DARK) },
            )
            Spacer(Modifier.height(24.dp))
            AccountSection(
                email = settings.accountEmail,
                name = settings.accountName,
                onSignIn = { activity?.let { viewModel.signIn(it) } },
                onSignOut = viewModel::signOut,
                onDelete = { confirmDelete = true },
            )
            Spacer(Modifier.height(24.dp))
            ProSection(
                proUnlocked = settings.proUnlocked,
                price = proPrice,
                onBuy = { activity?.let { viewModel.buyPro(it) } },
                onRestore = viewModel::restorePro,
            )
            Spacer(Modifier.height(24.dp))
            SectionLabel(stringResource(R.string.new_project_defaults))
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.default_opacity),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = settings.defaultOpacity,
                    onValueChange = viewModel::setDefaultOpacity,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.onSurface,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${(settings.defaultOpacity * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.widthIn(min = 44.dp),
                )
            }
            GridStepper(
                label = stringResource(R.string.grid_rows),
                value = settings.defaultGridRows,
                onValue = { viewModel.setDefaultGrid(it, settings.defaultGridCols) },
            )
            GridStepper(
                label = stringResource(R.string.grid_columns),
                value = settings.defaultGridCols,
                onValue = { viewModel.setDefaultGrid(settings.defaultGridRows, it) },
            )
            Spacer(Modifier.height(24.dp))
            // About card from the Figma Settings frame.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(16.dp),
            ) {
                Text("Picasso 1.0.0", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.settings_about),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AdBanner(
                visible = adsReady && !settings.proUnlocked,
                adRequest = viewModel.ads::adRequest,
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_account_title)) },
            text = { Text(stringResource(R.string.delete_account_text)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.deleteAccount() }) {
                    Text(
                        stringResource(R.string.delete_account),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

/** Figma section header: Ochre JetBrains Mono label. */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun ThemeRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun GridStepper(
    label: String,
    value: Int,
    onValue: (Int) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onValue(value - 1) }) {
            Icon(Icons.Default.Remove, contentDescription = null)
        }
        Text("$value", style = MaterialTheme.typography.labelMedium)
        IconButton(onClick = { onValue(value + 1) }) {
            Icon(Icons.Default.Add, contentDescription = null)
        }
    }
}

@Composable
private fun AccountSection(
    email: String?,
    name: String?,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionLabel(stringResource(R.string.account))
        Spacer(Modifier.height(12.dp))
        if (email == null) {
            Button(
                onClick = onSignIn,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(stringResource(R.string.sign_in_google))
            }
        } else {
            Text(name ?: email, style = MaterialTheme.typography.titleSmall)
            Text(
                email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Row {
                TextButton(onClick = onSignOut) {
                    Text(stringResource(R.string.sign_out))
                }
                TextButton(onClick = onDelete) {
                    Text(
                        stringResource(R.string.delete_account),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProSection(
    proUnlocked: Boolean,
    price: String?,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionLabel(stringResource(R.string.pro))
        Spacer(Modifier.height(4.dp))
        if (proUnlocked) {
            Text(
                stringResource(R.string.pro_unlocked),
                style = MaterialTheme.typography.titleSmall,
            )
        } else {
            Text(
                stringResource(R.string.pro_tagline),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onBuy,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(
                    if (price != null) "${stringResource(R.string.pro_buy)} — $price"
                    else stringResource(R.string.pro_buy),
                )
            }
        }
        TextButton(onClick = onRestore) {
            Text(stringResource(R.string.pro_restore))
        }
    }
}
