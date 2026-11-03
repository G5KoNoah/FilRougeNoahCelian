package ca.uqac.mobile.projetfilrouge.ui.screen

import ca.uqac.mobile.projetfilrouge.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.uqac.mobile.projetfilrouge.data.AlarmPoll
import ca.uqac.mobile.projetfilrouge.data.PollVote
import ca.uqac.mobile.projetfilrouge.ui.components.AppCard
import ca.uqac.mobile.projetfilrouge.ui.components.BackHeader
import ca.uqac.mobile.projetfilrouge.ui.components.PrimaryButton
import ca.uqac.mobile.projetfilrouge.ui.components.ScreenLayout
import ca.uqac.mobile.projetfilrouge.ui.components.SecondaryButton
import ca.uqac.mobile.projetfilrouge.ui.theme.ProjetFilRougeTheme
import ca.uqac.mobile.projetfilrouge.ui.theme.VoteAgainst
import ca.uqac.mobile.projetfilrouge.ui.theme.VoteCircle
import ca.uqac.mobile.projetfilrouge.ui.theme.VoteFor
import ca.uqac.mobile.projetfilrouge.viewModel.VoteRoomViewModel
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VoteRoomScreen(onBack: () -> Unit = {}) {
    val viewModel: VoteRoomViewModel = viewModel()
    val alarm by viewModel.alarm.collectAsStateWithLifecycle()
    val polls by viewModel.polls.collectAsStateWithLifecycle()
    val votes by viewModel.votes.collectAsStateWithLifecycle()

    VoteRoomContent(
        polls = polls,
        votes = votes,
        members = alarm?.members.orEmpty(),
        selectedPollId = viewModel.selectedPollId,
        error = viewModel.error,
        isVoting = viewModel.isVoting,
        onBack = onBack,
        onSelect = viewModel::selectPoll,
        onVote = viewModel::vote,
    )
}

@Composable
private fun VoteRoomContent(
    polls: List<AlarmPoll>,
    votes: List<PollVote>,
    members: List<String>,
    selectedPollId: String?,
    error: String?,
    isVoting: Boolean,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
    onVote: (pollId: String, vote: Boolean) -> Unit,
) {
    // Horloge de l'écran : le temps restant se met à jour chaque seconde
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
    }

    // On cache les propositions dont le temps est écoulé (la base ne les ferme qu'au prochain vote)
    val openPolls = polls.filter { it.isOpen(now) }
    val selected = openPolls.find { it.id == selectedPollId } ?: openPolls.firstOrNull()

    ScreenLayout {
        BackHeader(stringResource(R.string.vote_title), onBack = onBack)
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(24.dp))

        if (selected == null) {
            Text(
                stringResource(R.string.vote_no_polls),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            return@ScreenLayout
        }

        // Le vote de chaque membre pour la proposition choisie (absent = pas encore voté)
        val pollVotes = votes.filter { it.pollId == selected.id }.associate { it.voterId to it.vote }
        val forCount = pollVotes.values.count { it }

        AppCard {
            ProposalTabs(openPolls, selected.id, onSelect)
            Spacer(Modifier.height(12.dp))
            changeLines(selected).forEach { line ->
                Text(line, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(16.dp))
            VoteCircle(
                members.map { uid ->
                    when (pollVotes[uid]) {
                        true -> VoteFor
                        false -> VoteAgainst
                        null -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                },
                Modifier.padding(horizontal = 8.dp),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(R.string.vote_progress, forCount, selected.requiredVotes),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    stringResource(R.string.vote_time_left, remainingText(selected.endsAtMs - now)),
                    color = VoteAgainst,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SecondaryButton(
                stringResource(R.string.vote_against),
                modifier = Modifier.weight(1f),
                contentColor = VoteAgainst,
                enabled = !isVoting,
                onClick = { onVote(selected.id, false) },
            )
            PrimaryButton(
                stringResource(R.string.vote_for),
                modifier = Modifier.weight(1f),
                contentColor = VoteFor,
                enabled = !isVoting,
                onClick = { onVote(selected.id, true) },
            )
        }
    }
}

@Composable
private fun ProposalTabs(polls: List<AlarmPoll>, selectedId: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        polls.forEach { poll ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (poll.id == selectedId) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.background
                    )
                    .clickable { onSelect(poll.id) }
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Text(tabLabel(poll), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// --- Lecture du JSON "changes" / "previous" d'une proposition ---

private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull
private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
private fun JsonObject.intList(key: String): List<Int>? =
    (this[key] as? JsonArray)?.mapNotNull { it.jsonPrimitive.intOrNull }

private fun timeText(hour: Int?, minute: Int?) = "%02d:%02d".format(hour ?: 0, minute ?: 0)

// Nouvelle heure proposée (les valeurs non modifiées viennent de "previous")
private fun AlarmPoll.newTime() = timeText(changes.int("hour") ?: previous.int("hour"), changes.int("minute") ?: previous.int("minute"))
private fun AlarmPoll.oldTime() = timeText(previous.int("hour"), previous.int("minute"))
private val AlarmPoll.changesTime get() = "hour" in changes || "minute" in changes

@Composable
private fun daysText(days: List<Int>): String {
    val names = stringArrayResource(R.array.day_names_short)
    return if (days.isEmpty() || days.size == 7) stringResource(R.string.alarms_every_day)
    else days.sorted().joinToString(", ") { names[it] }
}

// Libellé court de l'onglet : le changement principal de la proposition
@Composable
private fun tabLabel(poll: AlarmPoll): String = when {
    poll.changesTime -> poll.newTime()
    "days" in poll.changes -> daysText(poll.changes.intList("days").orEmpty())
    "ringtone" in poll.changes -> poll.changes.text("ringtone").orEmpty()
    else -> poll.changes.text("snooze").orEmpty()
}

// Une ligne "avant → après" par réglage modifié
@Composable
private fun changeLines(poll: AlarmPoll): List<String> = buildList {
    if (poll.changesTime) add(stringResource(R.string.vote_change, poll.oldTime(), poll.newTime()))
    if ("days" in poll.changes) add(
        stringResource(
            R.string.vote_change,
            daysText(poll.previous.intList("days").orEmpty()),
            daysText(poll.changes.intList("days").orEmpty()),
        )
    )
    if ("ringtone" in poll.changes) add(
        stringResource(
            R.string.vote_ringtone,
            stringResource(R.string.vote_change, poll.previous.text("ringtone").orEmpty(), poll.changes.text("ringtone").orEmpty()),
        )
    )
    if ("snooze" in poll.changes) add(
        stringResource(
            R.string.vote_snooze,
            stringResource(R.string.vote_change, poll.previous.text("snooze").orEmpty(), poll.changes.text("snooze").orEmpty()),
        )
    )
}

// "4:05" (minutes:secondes), ou "1:02:30" au-delà d'une heure
private fun remainingText(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0) + 999) / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds / 60) % 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

@Composable
private fun VoteCircle(memberColors: List<Color>, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        val avatarSize = 28.dp
        val radius = (maxWidth - avatarSize) / 2

        Box(
            Modifier
                .size(radius * 2)
                .clip(CircleShape)
                .background(VoteCircle)
        )

        memberColors.forEachIndexed { index, color ->
            val angle = -PI / 2 + 2 * PI * index / memberColors.size
            Icon(
                Icons.Outlined.AccountCircle,
                contentDescription = null,
                tint = color,
                modifier = Modifier
                    .offset(x = radius * cos(angle).toFloat(), y = radius * sin(angle).toFloat())
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(VoteCircle),
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun VoteRoomScreenPreview() {
    val poll = AlarmPoll(
        id = "p1",
        changes = JsonObject(mapOf("hour" to JsonPrimitive(6), "minute" to JsonPrimitive(30))),
        previous = JsonObject(mapOf("hour" to JsonPrimitive(7), "minute" to JsonPrimitive(0))),
        requiredVotes = 3,
        endsAtMs = System.currentTimeMillis() + 4 * 60_000,
    )
    ProjetFilRougeTheme {
        VoteRoomContent(
            polls = listOf(poll),
            votes = listOf(PollVote("p1", "a", true), PollVote("p1", "b", false)),
            members = listOf("a", "b", "c", "d"),
            selectedPollId = null,
            error = null,
            isVoting = false,
            onBack = {},
            onSelect = {},
            onVote = { _, _ -> },
        )
    }
}
