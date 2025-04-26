package com.example.disastermesh.feature.ble.ui.model

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.disastermesh.core.database.entities.Message


sealed interface ChatItem {
    data class Header(val label: String) : ChatItem
    data class Bubble(val msg: Message) : ChatItem
}


@RequiresApi(Build.VERSION_CODES.O)
fun List<Message>.withDateHeaders(): List<ChatItem> {
    if (isEmpty()) return emptyList()

    val fmt      = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy")
    val zone     = java.time.ZoneId.systemDefault()

    val out = mutableListOf<ChatItem>()
    var lastDay: java.time.LocalDate? = null

    for (m in this) {
        val day = java.time.Instant.ofEpochMilli(m.ts).atZone(zone).toLocalDate()
        if (day != lastDay) {
            out += ChatItem.Header(day.format(fmt))
            lastDay = day
        }
        out += ChatItem.Bubble(m)
    }
    return out
}