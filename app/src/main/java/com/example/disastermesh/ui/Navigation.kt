/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import com.example.disastermesh.feature.ble.BleChatScreen
import com.example.disastermesh.feature.ble.BleConnectScreen
import com.example.disastermesh.feature.disastermessage.ui.DisasterMessageScreen
import com.example.disastermesh.feature.ble.BleScanScreen



@Composable
fun MainNavigation() {
    val navController = rememberNavController()

    NavHost(navController, startDestination = "main") {
        composable("main") {
            Column(Modifier.padding(16.dp)) {
                DisasterMessageScreen(modifier = Modifier.weight(1f))
                Button(
                    onClick = { navController.navigate("bleScan") },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Go to BLE Scan")
                }
            }
        }

        composable("bleScan") {
            //TODO: BleScanScreen required version >= 31 min version is 23 in project
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                BleScanScreen(modifier = Modifier.padding(16.dp), navController = navController)
            } else {
                Text("BLE scanning requires Android 12+")
            }
        }

        composable(
            "bleConnect/{address}",
            arguments = listOf(navArgument("address") { type = NavType.StringType })
        ) { backStack ->
            val addr = backStack.arguments!!.getString("address")!!
            BleConnectScreen(address = addr,
                navController = navController)
        }

        composable(
            "bleChat/{address}",
            arguments = listOf(navArgument("address") {
                type = NavType.StringType
            })
        ) { backStack ->
            val address = backStack.arguments!!.getString("address")!!
            BleChatScreen(
                address = address,
                navController = navController
            )
        }
    }
}
