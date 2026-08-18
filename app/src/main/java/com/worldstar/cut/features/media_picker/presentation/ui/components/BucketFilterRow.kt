package com.worldstar.cut.features.media_picker.presentation.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.worldstar.cut.core.domain.model.MediaBucket
import com.worldstar.cut.core.ui.theme.*

/**
 * A horizontally scrollable row of filter chips, one per [MediaBucket].
 * "All" is always the first chip (bucketId = null).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BucketFilterRow(
    buckets: List<MediaBucket>,
    selectedBucketId: Long?,
    onBucketSelected: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (buckets.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "All" chip
        FilterChip(
            selected = selectedBucketId == null,
            onClick  = { onBucketSelected(null) },
            label    = { Text("All") },
            colors   = bucketChipColors(selected = selectedBucketId == null)
        )

        buckets.forEach { bucket ->
            FilterChip(
                selected = selectedBucketId == bucket.id,
                onClick  = { onBucketSelected(bucket.id) },
                label = {
                    Text(
                        text = "${bucket.name} (${bucket.itemCount})",
                        maxLines = 1
                    )
                },
                colors = bucketChipColors(selected = selectedBucketId == bucket.id),
                shape  = RoundedCornerShape(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun bucketChipColors(selected: Boolean) = FilterChipDefaults.filterChipColors(
    selectedContainerColor     = WorldstarPurpleLight,
    selectedLabelColor         = TextPrimaryDark,
    containerColor             = SurfaceVariantDark,
    labelColor                 = TextSecondaryDark,
    selectedLeadingIconColor   = TextPrimaryDark
)
