package com.gem.keeppeer.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gem.keeppeer.R
import com.gem.keeppeer.ui.list.PasswordListScreen

/**
 * Navigation route definitions for the app.
 */
@Suppress("unused")
object Routes {
    const val LOCK = "lock"
    const val PASSWORD_LIST = "password_list"
    const val PASSWORD_EDIT = "password_edit"
    const val SETTINGS = "settings"
    const val EXPORT = "export"
}

/**
 * Root composable that hosts the navigation graph.
 * Phase 0: minimal shell — shows a placeholder password list screen.
 * Phase 1 will add real CRUD screens and navigation.
 * Phase 2 will add the lock screen as the start destination.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeeppeerApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.PASSWORD_LIST,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable(Routes.PASSWORD_LIST) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                            )
                        },
                    )
                },
            ) { innerPadding ->
                PasswordListScreen(modifier = Modifier.padding(innerPadding))
            }
        }
    }
}
