package ca.uqac.mobile.projetfilrouge.ui.screen

import androidx.compose.ui.res.stringArrayResource
import ca.uqac.mobile.projetfilrouge.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.uqac.mobile.projetfilrouge.AppVibrator
import ca.uqac.mobile.projetfilrouge.ui.components.AppCard
import ca.uqac.mobile.projetfilrouge.ui.components.MemberAvatars
import ca.uqac.mobile.projetfilrouge.ui.components.PillDropdown
import ca.uqac.mobile.projetfilrouge.ui.components.PillTextField
import ca.uqac.mobile.projetfilrouge.ui.components.PrimaryButton
import ca.uqac.mobile.projetfilrouge.ui.components.ScreenLayout
import ca.uqac.mobile.projetfilrouge.ui.components.SecondaryButton
import ca.uqac.mobile.projetfilrouge.ui.components.SwitchRow
import ca.uqac.mobile.projetfilrouge.viewModel.AlarmEditViewModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.filter


@Composable
fun AlarmEditScreen(onBackScreen: ()-> Unit, onOpenVoteRoom: (String) -> Unit = {}) {
    val viewModel : AlarmEditViewModel = viewModel()
    var confirmRemove by remember { mutableStateOf(false) }
    ScreenLayout {
        Spacer(Modifier.height(24.dp))
        if (viewModel.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@ScreenLayout
        }
        viewModel.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        // Les sections se répartissent sur toute la hauteur disponible ; sur un petit écran, on défile
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight),
            verticalArrangement = spreadVertically(min = 16.dp, max = 80.dp),
        ) {
            // Heure et jours
            AppCard(contentPadding = PaddingValues(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    TimeSelector(
                        hour = viewModel.hour,
                        minute = viewModel.minute,
                        onHourChange = viewModel::onHourChange,
                        onMinuteChange = viewModel::onMinuteChange,
                    )
                    DaysRow(selected = viewModel.days, onToggle = viewModel::toggleDay)
                }
            }

            // Nom, sonnerie, rappel
            AppCard(contentPadding = PaddingValues(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PillTextField(value = viewModel.name, onValueChange = { viewModel.onNameChange(it) }, placeholder = stringResource(R.string.alarm_name))
                    PillDropdown(viewModel.ringtone ?: stringResource(R.string.alarm_ringtone), viewModel.ringtones, onSelect = { viewModel.onRingtoneSelected(it) })
                }
            }

            // Vote
            AppCard(contentPadding = PaddingValues(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PillDropdown(
                            viewModel.pollDuration ?: stringResource(R.string.alarm_poll_duration),
                            viewModel.pollDurations,
                            onSelect = { viewModel.onPollDurationSelected(it) },
                            modifier = Modifier.weight(1f),
                        )
                        PrimaryButton(
                            stringResource(R.string.alarm_start_poll),
                            enabled = viewModel.canStartPoll && !viewModel.isSaving,
                            onClick = { viewModel.startPoll { viewModel.alarmId?.let(onOpenVoteRoom) } },
                        )
                    }
                    // Alarme partagée : on explique pourquoi Save ne change pas l'heure, et on donne accès aux votes en cours
                    if (viewModel.needsVote) {
                        Text(
                            stringResource(R.string.alarm_poll_needed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        viewModel.alarmId?.let { id ->
                            TextButton(onClick = { onOpenVoteRoom(id) }) {
                                Text(stringResource(R.string.alarm_open_vote_room))
                            }
                        }
                    }
                }
            }

            // Son et vibrations
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SwitchRow(stringResource(R.string.alarm_sound), checked = viewModel.sound, onCheckedChange = viewModel::onSoundChange)
                SwitchRow(stringResource(R.string.alarm_vibrations), checked = viewModel.vibrations, onCheckedChange = viewModel::onVibrationsChange)
            }

            // Membres
            AppCard(contentPadding = PaddingValues(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.alarm_members), style = MaterialTheme.typography.titleSmall)
                        MemberAvatars(viewModel.members.size.coerceAtMost(6))
                    }
                    IconButton(onClick = viewModel::openMemberPicker) {
                        Icon(Icons.Outlined.PersonAddAlt, contentDescription = stringResource(R.string.alarm_add_member))
                    }
                }
            }
        }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SecondaryButton(stringResource(R.string.cancel), modifier = Modifier.weight(1f), onClick = onBackScreen)
            if (!viewModel.isNew) {
                SecondaryButton(
                    stringResource(if (viewModel.isOwner) R.string.alarm_delete else R.string.alarm_leave),
                    modifier = Modifier.weight(1f),
                    contentColor = MaterialTheme.colorScheme.error,
                    onClick = { confirmRemove = true },
                )
            }
            PrimaryButton(stringResource(R.string.alarm_save), modifier = Modifier.weight(1f), enabled = !viewModel.isSaving, onClick = { viewModel.save(onDone = onBackScreen) })
        }
        Spacer(Modifier.height(16.dp))
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text(stringResource(if (viewModel.isOwner) R.string.alarm_delete_title else R.string.alarm_leave_title)) },
            text = {
                Text(
                    if (viewModel.isOwner) stringResource(R.string.alarm_delete_message)
                    else stringResource(R.string.alarm_leave_message)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmRemove = false
                    viewModel.deleteOrLeave(onDone = onBackScreen)
                }) { Text(stringResource(if (viewModel.isOwner) R.string.alarm_delete else R.string.alarm_leave)) }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    // Choix des membres parmi mes contacts : rien n'est envoyé ici, l'ajout se fait au Save
    if (viewModel.showMemberPicker) {
        AlertDialog(
            onDismissRequest = viewModel::closeMemberPicker,
            title = { Text(stringResource(R.string.alarm_add_member)) },
            text = {
                if (viewModel.friends.isEmpty()) {
                    Text(stringResource(R.string.alarm_members_no_contacts))
                } else {
                    Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                        viewModel.friends.forEach { friend ->
                            val saved = viewModel.isSavedMember(friend.uid)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !saved) { viewModel.toggleMember(friend.uid) },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = friend.uid in viewModel.members,
                                    onCheckedChange = { viewModel.toggleMember(friend.uid) },
                                    enabled = !saved,
                                )
                                Text(friend.displayName, Modifier.weight(1f))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::closeMemberPicker) { Text(stringResource(R.string.alarm_members_done)) }
            },
        )
    }
}

@Composable
private fun TimeSelector(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NumberWheel(count = 24, initial = hour, onValueChange = onHourChange)
        Text(":", fontSize = 48.sp, modifier = Modifier.padding(horizontal = 12.dp))
        NumberWheel(count = 60, initial = minute, onValueChange = onMinuteChange)
    }
}

private val WheelItemHeight = 68.dp
private const val WheelLoops = 1000

@Composable
private fun NumberWheel(count: Int, initial: Int, onValueChange: (Int) -> Unit) {
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
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { !it }
            .collect { currentOnValueChange(centerIndex % count) }
    }

    LazyColumn(
        state = listState,
        flingBehavior = rememberSnapFlingBehavior(listState),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .height(WheelItemHeight * 3)
            .width(104.dp),
    ) {
        items(count * WheelLoops) { index ->
            val isCenter = index == centerIndex
            Box(Modifier.height(WheelItemHeight), contentAlignment = Alignment.Center) {
                Text(
                    "%02d".format(index % count),
                    fontSize = if (isCenter) 48.sp else 28.sp,
                    color = if (isCenter) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DaysRow(selected: Set<Int>, onToggle: (Int) -> Unit) {
    val days = stringArrayResource(R.array.day_letters)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.background)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        days.forEachIndexed { index, letter ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (index in selected) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
                    .clickable { onToggle(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    letter,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (index in selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

// Comme SpaceBetween, mais l'écart entre deux éléments reste entre min et max.
// S'il reste de la place, le bloc est centré verticalement ; s'il en manque, on garde l'écart min (et l'écran défile).
private fun spreadVertically(min: Dp, max: Dp) = object : Arrangement.Vertical {
    override val spacing = min

    override fun Density.arrange(totalSize: Int, sizes: IntArray, outPositions: IntArray) {
        val gaps = (sizes.size - 1).coerceAtLeast(1)
        val free = totalSize - sizes.sum()
        val gap = (free / gaps).coerceIn(min.roundToPx(), max.roundToPx())
        val used = sizes.sum() + gap * (sizes.size - 1)
        var y = ((totalSize - used) / 2).coerceAtLeast(0)
        sizes.forEachIndexed { index, size ->
            outPositions[index] = y
            y += size + gap
        }
    }
}
