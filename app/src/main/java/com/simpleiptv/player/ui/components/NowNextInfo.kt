package com.simpleiptv.player.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.simpleiptv.player.core.model.EpgProgram

@Composable
fun NowNextInfo(
    nowProgram: EpgProgram?,
    nextProgram: EpgProgram?
) {
    if (nowProgram == null && nextProgram == null) {
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        nowProgram?.let { program ->
            Text(
                text = "Now: ${program.title}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = program.formattedTimeRange(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        nextProgram?.let { program ->
            Text(
                text = "Next: ${program.title}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = program.formattedTimeRange(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}