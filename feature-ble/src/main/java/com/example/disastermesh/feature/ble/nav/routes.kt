package com.example.disastermesh.feature.ble.nav


sealed class Screen(val route: String) {
    object Landing   : Screen("landing")
    object Scan      : Screen("scan")
    object Chat      : Screen("chat/{chatId}/{title}")      // args: Long, String
    object ChatList  : Screen("list/{type}")                // args: NODE | USER
}
