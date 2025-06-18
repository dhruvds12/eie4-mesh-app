package com.example.disastermesh.feature.ble.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.disastermesh.core.database.entities.Message
import com.example.disastermesh.core.database.entities.MessageStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
@RequiresApi(Build.VERSION_CODES.O)
private val zone    = ZoneId.systemDefault()

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MessageBubble(msg: Message) {

    val bubbleColor = if (msg.mine)
        MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.mine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color  = bubbleColor,
            shape  = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (msg.mine) 16.dp else 0.dp,
                bottomEnd   = if (msg.mine) 0.dp  else 16.dp
            ),
            tonalElevation = 2.dp,
            modifier = Modifier.widthIn(max = 260.dp)
        ) {
            Column(Modifier.padding(8.dp)) {

                /* body */
                Text(
                    msg.body,
                    color = if (msg.mine)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(2.dp))

                /* clock (+ status ticks for my own msgs) */
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val t = Instant.ofEpochMilli(msg.ts)
                        .atZone(zone).format(timeFmt)

                    Text(
                        t,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (msg.mine)
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = .8f)
                        else
                            MaterialTheme.colorScheme
                                .onSurfaceVariant.copy(alpha = .8f)
                    )

                    if (msg.mine) StatusIcon(msg.status)
                }
            }
        }
    }

    Spacer(Modifier.height(4.dp))
}

@Composable
private fun StatusIcon(s: MessageStatus) {
    val icon = when (s) {
        MessageStatus.SENDING -> Icons.Outlined.Schedule
        MessageStatus.SENT    -> Icons.Outlined.Done
        MessageStatus.ACKED   -> Icons.Outlined.DoneAll
        MessageStatus.FAILED -> Icons.Outlined.Error

    }
    val tint = when (s) {
        MessageStatus.FAILED -> MaterialTheme.colorScheme.error
        MessageStatus.ACKED -> MaterialTheme.colorScheme.onPrimary
        else                -> MaterialTheme.colorScheme.onPrimary.copy(alpha = .7f)
    }
    Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
}
