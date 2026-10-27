package ca.uqac.mobile.projetfilrouge.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.uqac.mobile.projetfilrouge.data.Friend
import ca.uqac.mobile.projetfilrouge.data.FriendRequest
import ca.uqac.mobile.projetfilrouge.ui.components.AppCard
import ca.uqac.mobile.projetfilrouge.ui.components.PrimaryButton
import ca.uqac.mobile.projetfilrouge.ui.components.ScreenLayout
import ca.uqac.mobile.projetfilrouge.ui.components.ScreenTitle
import ca.uqac.mobile.projetfilrouge.ui.theme.ProjetFilRougeTheme
import ca.uqac.mobile.projetfilrouge.viewModel.FriendsViewModel

@Composable
fun ContactsScreen(onAddContact: () -> Unit) {
    val viewModel: FriendsViewModel = viewModel()
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    val requests by viewModel.requests.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    ContactsContent(
        friends = friends,
        requests = requests,
        error = error,
        onAddContact = onAddContact,
        onAccept = viewModel::acceptRequest,
        onDecline = viewModel::declineRequest,
        onRemove = viewModel::removeFriend,
    )
}

@Composable
private fun ContactsContent(
    friends: List<Friend>,
    requests: List<FriendRequest>,
    error: String?,
    onAddContact: () -> Unit,
    onAccept: (FriendRequest) -> Unit,
    onDecline: (FriendRequest) -> Unit,
    onRemove: (Friend) -> Unit,
) {
    var friendToRemove by remember { mutableStateOf<Friend?>(null) }

    ScreenLayout {
        Spacer(Modifier.height(24.dp))
        ScreenTitle("Contacts", Modifier.fillMaxWidth())
        IconButton(onClick = onAddContact, modifier = Modifier.align(Alignment.End)) {
            Icon(Icons.Outlined.AddCircleOutline, contentDescription = "Add a contact")
        }
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (requests.isNotEmpty()) {
                item { SectionLabel("Received requests") }
                items(requests, key = { "request-${it.id}" }) { request ->
                    RequestCard(request, onAccept = { onAccept(request) }, onDecline = { onDecline(request) })
                }
                item { SectionLabel("My contacts") }
            }
            if (friends.isEmpty()) {
                item {
                    Text(
                        "No contacts yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            items(friends, key = { "friend-${it.uid}" }) { friend ->
                ContactCard(friend, onRemove = { friendToRemove = friend })
            }
        }
    }

    friendToRemove?.let { friend ->
        AlertDialog(
            onDismissRequest = { friendToRemove = null },
            title = { Text("Remove this contact?") },
            text = { Text("${friend.displayName} will be removed from your contacts, and you from theirs.") },
            confirmButton = {
                TextButton(onClick = {
                    onRemove(friend)
                    friendToRemove = null
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { friendToRemove = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ContactCard(friend: Friend, onRemove: () -> Unit) {
    AppCard(contentPadding = PaddingValues(0.dp)) {
        Box(Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Text(friend.displayName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    friend.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRemove, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Outlined.RemoveCircleOutline, contentDescription = "Remove ${friend.displayName}")
            }
        }
    }
}

@Composable
private fun RequestCard(request: FriendRequest, onAccept: () -> Unit, onDecline: () -> Unit) {
    AppCard {
        Text(request.fromName.ifEmpty { request.fromEmail }, style = MaterialTheme.typography.bodyLarge)
        Text(
            request.fromEmail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onDecline) { Text("Decline") }
            PrimaryButton("Accept", onClick = onAccept)
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun ContactsScreenPreview() {
    ProjetFilRougeTheme {
        ContactsContent(
            friends = listOf(
                Friend("1", "john.doe@mail.com", "John", "Doe"),
                Friend("2", "jane.smith@mail.com", "Jane", "Smith"),
            ),
            requests = listOf(FriendRequest("3_me", "3", "Jean Dupont", "jean.dupont@mail.com")),
            error = null,
            onAddContact = {},
            onAccept = {},
            onDecline = {},
            onRemove = {},
        )
    }
}
