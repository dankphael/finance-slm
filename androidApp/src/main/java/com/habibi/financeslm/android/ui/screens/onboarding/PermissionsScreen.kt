package com.habibi.financeslm.android.ui.screens.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.habibi.financeslm.android.R
import com.habibi.financeslm.android.ui.components.OnboardingProgress
import com.habibi.financeslm.android.ui.theme.Spacing
import com.habibi.financeslm.platform.AndroidScreenReader

/**
 * Data class representing a finance app the user can optionally monitor.
 */
private data class FinanceAppInfo(
    val packageName: String,
    val displayName: String,
    val description: String
)

/** The 7 Singapore finance apps the AccessibilityService can monitor. */
private val FINANCE_APPS = listOf(
    FinanceAppInfo("com.dbs.sg.dbsmbanking", "DBS / PayNow", "Banking, transfers, PayNow"),
    FinanceAppInfo("com.dbs.paylah", "PayLah!", "Payments and rewards"),
    FinanceAppInfo("com.ocbc.mobile", "OCBC Mobile", "Banking and transfers"),
    FinanceAppInfo("com.uob.mighty.app", "UOB Mighty", "Banking and transfers"),
    FinanceAppInfo("com.grabtaxi.passenger", "Grab", "Payments, GrabPay"),
    FinanceAppInfo("com.moomoo.sg", "Moomoo SG", "Stock trading and investing"),
    FinanceAppInfo("com.tigerbrokers.stock", "Tiger Brokers", "Stock trading and investing")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    onContinue: () -> Unit
) {
    // Track toggled packages — all enabled by default
    val selectedPackages = remember { mutableStateMapOf(*FINANCE_APPS.map { it.packageName to true }.toTypedArray()) }

    var serviceEnabled by remember { mutableStateOf(false) }
    var serviceCheckAttempted by remember { mutableStateOf(false) }
    var showSkipReminder by remember { mutableStateOf(false) }

    // Launcher for returning from system Accessibility Settings
    val accessibilitySettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // Check if the service was enabled while the user was in Settings
        val reader = com.habibi.financeslm.platform.createScreenReader() as? AndroidScreenReader
        serviceEnabled = reader?.isServiceEnabled() ?: false
        serviceCheckAttempted = true
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.permissions_title)) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.xl),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Scrollable content area
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Spacer(modifier = Modifier.height(Spacing.sm))

                Text(
                    text = stringResource(R.string.permissions_apps_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = stringResource(R.string.permissions_apps_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                // App toggle list
                Card(modifier = Modifier.fillMaxWidth()) {
                    LazyColumn(modifier = Modifier.padding(vertical = Spacing.xs)) {
                        items(FINANCE_APPS) { app ->
                            SwitchListItem(
                                app = app,
                                isChecked = selectedPackages[app.packageName] ?: true,
                                onCheckedChange = { checked ->
                                    selectedPackages[app.packageName] = checked
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.xl))

                // Accessibility Service section
                Text(
                    text = stringResource(R.string.permissions_service_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = stringResource(R.string.permissions_service_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                // Service status indicator
                if (serviceCheckAttempted) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (serviceEnabled)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = if (serviceEnabled)
                                stringResource(R.string.permissions_service_enabled)
                            else
                                stringResource(R.string.permissions_service_disabled),
                            modifier = Modifier.padding(Spacing.lg),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.md))
                }

                // Open system settings button
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        accessibilitySettingsLauncher.launch(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.permissions_open_settings))
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                Text(
                    text = stringResource(R.string.permissions_after_enabling),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start
                )

                // Skip reminder
                if (showSkipReminder) {
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.permissions_skip_reminder),
                            modifier = Modifier.padding(Spacing.lg),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Bottom buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OnboardingProgress(step = 3, total = 3)
                Spacer(modifier = Modifier.height(Spacing.xs))
                Button(
                    onClick = {
                        // Check if service is enabled before continuing
                        val reader = com.habibi.financeslm.platform.createScreenReader() as? AndroidScreenReader
                        serviceEnabled = reader?.isServiceEnabled() ?: false
                        serviceCheckAttempted = true

                        if (!serviceEnabled) {
                            showSkipReminder = true
                            // Still allow proceeding (gentle reminder pattern)
                        }
                        onContinue()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.permissions_continue))
                }

                TextButton(
                    onClick = {
                        showSkipReminder = true
                        onContinue()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.permissions_skip))
                }
            }
        }
    }
}

@Composable
private fun SwitchListItem(
    app: FinanceAppInfo,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val toggleDescription = stringResource(R.string.toggle_monitor_app, app.displayName)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = app.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.semantics { contentDescription = toggleDescription }
        )
    }

    HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.lg))
}
