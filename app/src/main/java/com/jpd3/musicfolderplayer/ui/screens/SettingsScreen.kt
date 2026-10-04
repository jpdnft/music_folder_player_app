package com.jpd3.musicfolderplayer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import com.jpd3.musicfolderplayer.ui.theme.PlayerButton as Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun SettingsScreen(
    navController: NavController,
    libraryFolder: String,
    onChooseLibrary: () -> Unit,
    onClearState: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Library settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text("Saved library folder: $libraryFolder")
        Text("This folder is remembered between visits. Choose the main folder containing your artists and albums; browse them from Browse folders.")
        Button(onClick = { navController.popBackStack() }) {
            Text("Back to browsing")
        }
        Button(
            onClick = onChooseLibrary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
        ) {
            Text("Change saved library folder")
        }
        Button(
            onClick = onClearState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Text("Reset library and playback settings")
        }
    }
}
