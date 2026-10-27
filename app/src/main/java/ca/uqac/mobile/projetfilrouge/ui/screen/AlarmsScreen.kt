package ca.uqac.mobile.projetfilrouge.ui.screen

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.HowToVote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.uqac.mobile.projetfilrouge.ui.components.AppCard
import ca.uqac.mobile.projetfilrouge.ui.components.MemberAvatars
import ca.uqac.mobile.projetfilrouge.ui.components.ScreenLayout
import ca.uqac.mobile.projetfilrouge.ui.components.ScreenTitle
import ca.uqac.mobile.projetfilrouge.ui.components.SmallSwitch
import ca.uqac.mobile.projetfilrouge.ui.theme.ProjetFilRougeTheme

@Composable
fun AlarmsScreen(onAlarmEdit: ()-> Unit) {
    // Android 13+ : sans cette permission, la notification de l'alarme ne s'affiche pas.
    // Demandée une seule fois, à l'ouverture de l'écran.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    ScreenLayout {
        Spacer(Modifier.height(24.dp))
        ScreenTitle("Alarm in 10 hours\nand 32 minutes", Modifier.fillMaxWidth())
        IconButton(onClick = {}, modifier = Modifier.align(Alignment.End)) {
            Icon(Icons.Outlined.AddCircleOutline, contentDescription = "Ajouter une alarme", Modifier.clickable {onAlarmEdit()})
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AlarmCard("09:00", enabled = true, memberCount = 1)
            AlarmCard("08:00", enabled = true, memberCount = 3, isPoll = true)
            AlarmCard("07:00", enabled = true, memberCount = 3)
        }
    }
}

@Composable
private fun AlarmCard(
    time: String,
    enabled: Boolean,
    memberCount: Int,
    isPoll: Boolean = false,
) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = time,
                modifier = Modifier.weight(1f),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            )
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPoll) {
                        Icon(
                            Icons.Outlined.HowToVote,
                            contentDescription = "Vote en cours",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    SmallSwitch(enabled)
                }
                MemberAvatars(memberCount)
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun AlarmsScreenPreview() {
    ProjetFilRougeTheme { AlarmsScreen(onAlarmEdit = {}) }
}
