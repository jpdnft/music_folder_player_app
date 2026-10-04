package com.jpd3.musicfolderplayer.ui.screens

sealed class Screen(val route: String) {
    object Setup : Screen("setup")
    object FolderBrowser : Screen("folder_browser")
    object NowPlaying : Screen("now_playing")
    object Recovery : Screen("recovery")
    object Settings : Screen("settings")
}
