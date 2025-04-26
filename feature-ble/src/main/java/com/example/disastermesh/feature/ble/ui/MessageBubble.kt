package com.example.disastermesh.feature.ble.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.disastermesh.core.database.entities.Message
import com.example.disastermesh.core.database.entities.MessageStatus

@Composable
fun MessageBubble(msg: Message) {

    val bubbleColor = if (msg.mine)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.surfaceVariant

    val alignment = if (msg.mine) Alignment.End else Alignment.Start

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.mine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStartPercent  = 16,
                topEndPercent    = 16,
                bottomEndPercent = if (msg.mine) 0 else 16,
                bottomStartPercent = if (msg.mine) 16 else 0
            ),
            tonalElevation = 2.dp,
            modifier = Modifier.widthIn(max = 260.dp)
        ) {
            Column(Modifier.padding(8.dp)) {
                Text(
                    msg.body,
                    color = if (msg.mine) MaterialTheme.colorScheme.onPrimary
                    else           MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (msg.mine) {
                    Spacer(Modifier.height(2.dp))
                    StatusRow(msg.status)
                }
            }
        }
    }

    Spacer(Modifier.height(4.dp))
}

@Composable
private fun StatusRow(s: MessageStatus) {
    val icon = when (s) {
        MessageStatus.SENDING -> Icons.Outlined.Schedule
        MessageStatus.SENT    -> Icons.Outlined.Done
        MessageStatus.ACKED   -> Icons.Outlined.DoneAll
    }
    val tint = when (s) {
        MessageStatus.ACKED -> MaterialTheme.colorScheme.secondary
        else                -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
    }
    Icon(
        icon,
        contentDescription = s.name,
        tint = tint,
        modifier = Modifier.size(14.dp)
    )
}
