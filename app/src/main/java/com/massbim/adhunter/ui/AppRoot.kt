/*
 * Navigasi utama AdHunter.
 * Dua layar berbagi satu NavigationBar sederhana; setiap layar tetap memakai
 * Scaffold dan TopAppBar sendiri.
 */
package com.massbim.adhunter.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.massbim.adhunter.R

@Composable
fun AppRoot() {
    var selectedTab by rememberSaveable { mutableIntStateOf(TAB_AUDIT) }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (selectedTab) {
                TAB_MONITOR -> MonitorScreen()
                else -> AuditScreen()
            }
        }
        NavigationBar {
            NavigationBarItem(
                selected = selectedTab == TAB_AUDIT,
                onClick = { selectedTab = TAB_AUDIT },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_shield),
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(R.string.tab_audit)) },
            )
            NavigationBarItem(
                selected = selectedTab == TAB_MONITOR,
                onClick = { selectedTab = TAB_MONITOR },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_monitor),
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(R.string.tab_monitor)) },
            )
        }
    }
}

private const val TAB_AUDIT = 0
private const val TAB_MONITOR = 1
