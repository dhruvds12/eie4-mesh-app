package com.example.disastermesh.ui

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.disastermesh.feature.disastermessage.ui.DisasterMessageScreen
import com.example.disastermesh.feature.ble.BleScanScreen
import com.example.disastermesh.feature.ble.BleChatScreen
import com.example.disastermesh.feature.ble.ui.ChatListScreen
import com.example.disastermesh.feature.ble.ui.LandingScreen
import com.example.disastermesh.feature.ble.nav.Screen   // ← sealed-class routes

@Composable
fun MainNavigation() {

    val navController = rememberNavController()

    /* ------------------------------------------------------------------ */
    /*  Root navigation graph                                             */
    /* ------------------------------------------------------------------ */
    NavHost(
        navController  = navController,
        startDestination = Screen.Landing.route   // new start page
    ) {

        /* ------------ landing = connect / 3 buttons ------------------- */
        composable(Screen.Landing.route) {
            LandingScreen(nav = navController)
        }

        /* ------------ BLE scan list ----------------------------------- */
        composable(Screen.Scan.route) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                BleScanScreen(
                    modifier     = Modifier.padding(16.dp),
                    navController = navController
                )
            } else {
                Text("BLE scanning requires Android 12 or higher")
            }
        }

        /* ------------ list of node-to-node OR user-to-user chats ------ */
        composable(
            Screen.ChatList.route,
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) { back ->
            ChatListScreen(
                type = back.arguments!!.getString("type")!!,
                nav  = navController
            )
        }

        /* ------------ individual chat (any type) ---------------------- */
        composable(
            Screen.Chat.route,
            arguments = listOf(
                navArgument("chatId") { type = NavType.LongType },
                navArgument("title")  { type = NavType.StringType }
            )
        ) { back ->
            BleChatScreen(
                chatId    = back.arguments!!.getLong("chatId"),
                chatTitle = back.arguments!!.getString("title")!!,
                navController = navController
            )
        }

        /* ------------ legacy “main” screen keeps demo feature --------- */
        composable("main-demo") {
            Column(Modifier.padding(16.dp)) {
                DisasterMessageScreen(modifier = Modifier.weight(1f))
                Button(
                    onClick = { navController.navigate(Screen.Landing.route) },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Go to Mesh Chat")
                }
            }
        }
    }
}
