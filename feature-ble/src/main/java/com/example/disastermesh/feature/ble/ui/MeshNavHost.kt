package com.example.disastermesh.feature.ble.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.disastermesh.feature.ble.BleChatScreen
import com.example.disastermesh.feature.ble.BleScanScreen
import com.example.disastermesh.feature.ble.nav.Screen

@Composable
fun MeshNavHost(nav: NavHostController, start: String = Screen.Landing.route) {
    NavHost(navController = nav, startDestination = start) {

        composable(Screen.Landing.route)       { LandingScreen(nav) }

        composable(Screen.Scan.route)          { BleScanScreen(navController = nav) }

        composable(
            Screen.Chat.route,
            arguments = listOf(
                navArgument("chatId") { type = NavType.LongType },
                navArgument("title")  { type = NavType.StringType }
            )
        ) { be ->
            BleChatScreen(

                navController = nav,
                chatId        = be.arguments!!.getLong("chatId"),
                chatTitle     = be.arguments!!.getString("title")!!
            )
        }

        composable(
            Screen.ChatList.route,
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) { be ->
            ChatListScreen(
                type = be.arguments!!.getString("type")!!,
                nav  = nav
            )
        }
    }
}



