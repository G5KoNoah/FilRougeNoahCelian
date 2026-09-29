package ca.uqac.mobile.projetfilrouge.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.uqac.mobile.projetfilrouge.ui.components.AppCard
import ca.uqac.mobile.projetfilrouge.ui.components.MemberAvatars
import ca.uqac.mobile.projetfilrouge.ui.components.PillDropdown
import ca.uqac.mobile.projetfilrouge.ui.components.PillTextField
import ca.uqac.mobile.projetfilrouge.ui.components.PrimaryButton
import ca.uqac.mobile.projetfilrouge.ui.components.ScreenLayout
import ca.uqac.mobile.projetfilrouge.ui.components.SecondaryButton
import ca.uqac.mobile.projetfilrouge.ui.components.SwitchRow
import ca.uqac.mobile.projetfilrouge.ui.theme.ProjetFilRougeTheme

private val Ringtones = listOf("Classique", "Radar", "Carillon", "Réveil doux")
private val SnoozeOptions = listOf("1 x 5 min", "2 x 3 min", "3 x 5 min")
private val PollDurations = listOf("2 min", "5 min", "10 min")

@Composable
fun AlarmEditScreen() {
    var name by remember { mutableStateOf("") }
    var ringtone by remember { mutableStateOf<String?>(null) }
    var snooze by remember { mutableStateOf("2 x 3 min") }
    var pollDuration by remember { mutableStateOf<String?>(null) }

    ScreenLayout {
        Spacer(Modifier.height(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeSelector()
                    DaysRow()
                    PillTextField(value = name, onValueChange = { name = it }, placeholder = "Alarm name")
                    PillDropdown(ringtone ?: "Ringtone", Ringtones, onSelect = { ringtone = it })
                    PillDropdown(snooze, SnoozeOptions, onSelect = { snooze = it })
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PillDropdown(
                            pollDuration ?: "Poll duration",
                            PollDurations,
                            onSelect = { pollDuration = it },
                            modifier = Modifier.weight(1f),
                        )
                        PrimaryButton("Start poll")
                    }
                }
            }

            SwitchRow("Sound", checked = false)
            SwitchRow("Vibrations", checked = false)

            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Members", style = MaterialTheme.typography.labelMedium)
                        MemberAvatars(5)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.PersonAddAlt, contentDescription = "Ajouter un membre")
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        ) {
            SecondaryButton("Cancel")
            PrimaryButton("Save")
        }
    }
}

@Composable
private fun TimeSelector() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NumberWheel(count = 24, initial = 6)
        Text(":", fontSize = 32.sp, modifier = Modifier.padding(horizontal = 8.dp))
        NumberWheel(count = 60, initial = 0)
    }
}

private val WheelItemHeight = 44.dp
private const val WheelLoops = 1000

@Composable
private fun NumberWheel(count: Int, initial: Int) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = count * WheelLoops / 2 + initial - 1
    )
    val itemHeightPx = with(LocalDensity.current) { WheelItemHeight.toPx() }
    val centerIndex by remember {
        derivedStateOf {
            val pastHalf = listState.firstVisibleItemScrollOffset > itemHeightPx / 2
            listState.firstVisibleItemIndex + if (pastHalf) 2 else 1
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = rememberSnapFlingBehavior(listState),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .height(WheelItemHeight * 3)
            .width(64.dp),
    ) {
        items(count * WheelLoops) { index ->
            val isCenter = index == centerIndex
            Box(Modifier.height(WheelItemHeight), contentAlignment = Alignment.Center) {
                Text(
                    "%02d".format(index % count),
                    fontSize = if (isCenter) 32.sp else 22.sp,
                    color = if (isCenter) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DaysRow() {
    val days = listOf("S", "M", "T", "W", "T", "F", "S")
    val selected = setOf(2, 4)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.background)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        days.forEachIndexed { index, letter ->
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (index in selected) MaterialTheme.colorScheme.surfaceVariant
                        else Color.Transparent
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(letter, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun AlarmEditScreenPreview() {
    ProjetFilRougeTheme { AlarmEditScreen() }
}
