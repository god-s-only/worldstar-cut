package com.worldstar.cut.features.media_picker.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.worldstar.cut.core.ui.theme.*

/**
 * Full-screen permission rationale shown when storage access is not granted.
 */
@Composable
fun PermissionRationale(
    onGrantClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement   = Arrangement.Center,
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector        = Icons.Default.PhotoLibrary,
            contentDescription = null,
            modifier           = Modifier.size(80.dp),
            tint               = WorldstarPurpleLight
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text      = "Access Your Media",
            style     = MaterialTheme.typography.headlineSmall,
            color     = TextPrimaryDark,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Worldstar Cut needs access to your photos and videos " +
                    "to let you edit them. Your media never leaves your device.",
            style     = MaterialTheme.typography.bodyMedium,
            color     = TextSecondaryDark,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onGrantClick,
            colors  = ButtonDefaults.buttonColors(
                containerColor = WorldstarPurpleLight,
                contentColor   = TextPrimaryDark
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Allow Access", style = MaterialTheme.typography.labelLarge)
        }
    }
}
