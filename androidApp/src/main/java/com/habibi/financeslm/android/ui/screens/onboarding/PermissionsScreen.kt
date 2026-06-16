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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    val context = LocalContext.current

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
            TopAppBar(title = { Text("Permissions & Finance Apps") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Scrollable content area
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Select Finance Apps to Monitor",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Toggle which apps Finance SLM should read screen data from. Data never leaves your device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // App toggle list
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LazyColumn(
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
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

                Spacer(modifier = Modifier.height(24.dp))

                // Accessibility Service section
                Text(
                    text = "Accessibility Service",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Screen reading requires the Accessibility Service to be enabled in your system settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

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
                                "✓ Accessibility Service is enabled"
                            else
                                "✗ Accessibility Service is not enabled",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Open system settings button
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        accessibilitySettingsLauncher.launch(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Accessibility Settings")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "After enabling, find \"Finance SLM\" in the list of installed apps and turn it on.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start
                )

                // Skip reminder
                if (showSkipReminder) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Text(
                            text = "You can enable the Accessibility Service later from Settings. Screen reading won't work until then.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Bottom buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                    Text("Continue — Ready!")
                }

                TextButton(
                    onClick = {
                        showSkipReminder = true
                        onContinue()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Skip for now")
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
            onCheckedChange = onCheckedChange
        )
    }

    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}
