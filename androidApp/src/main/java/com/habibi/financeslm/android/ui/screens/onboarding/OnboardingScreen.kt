package com.habibi.financeslm.android.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.habibi.financeslm.android.R
import com.habibi.financeslm.android.ui.components.OnboardingProgress
import com.habibi.financeslm.android.ui.theme.Spacing

@Composable
fun OnboardingScreen(
    onContinue: () -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(Spacing.xxl))

            // Brand mark — the launcher icon foreground on a primary-tinted circle.
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(88.dp)
                )
            }

            Spacer(Modifier.height(Spacing.xl))
            Text(
                text = stringResource(R.string.onboarding_welcome_title),
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = stringResource(R.string.onboarding_welcome_body),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(Spacing.xxl))

            FeatureRow(
                icon = Icons.Default.Lock,
                title = stringResource(R.string.onboarding_feature_private_title),
                body = stringResource(R.string.onboarding_feature_private_body)
            )
            Spacer(Modifier.height(Spacing.lg))
            FeatureRow(
                icon = Icons.Default.CloudOff,
                title = stringResource(R.string.onboarding_feature_offline_title),
                body = stringResource(R.string.onboarding_feature_offline_body)
            )
            Spacer(Modifier.height(Spacing.lg))
            FeatureRow(
                icon = Icons.Default.AutoAwesome,
                title = stringResource(R.string.onboarding_feature_insights_title),
                body = stringResource(R.string.onboarding_feature_insights_body)
            )

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(Spacing.xxl))

            OnboardingProgress(step = 1, total = 3)
            Spacer(Modifier.height(Spacing.lg))
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.onboarding_get_started))
            }
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, body: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(Spacing.lg))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
