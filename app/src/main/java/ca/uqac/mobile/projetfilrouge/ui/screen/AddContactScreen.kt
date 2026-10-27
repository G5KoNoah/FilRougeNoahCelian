package ca.uqac.mobile.projetfilrouge.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.uqac.mobile.projetfilrouge.data.Friend
import ca.uqac.mobile.projetfilrouge.data.FriendStatus
import ca.uqac.mobile.projetfilrouge.data.UserSearchResult
import ca.uqac.mobile.projetfilrouge.ui.components.AppCard
import ca.uqac.mobile.projetfilrouge.ui.components.BackHeader
import ca.uqac.mobile.projetfilrouge.ui.components.CardShape
import ca.uqac.mobile.projetfilrouge.ui.components.PrimaryButton
import ca.uqac.mobile.projetfilrouge.ui.components.ScreenLayout
import ca.uqac.mobile.projetfilrouge.ui.theme.ProjetFilRougeTheme
import ca.uqac.mobile.projetfilrouge.viewModel.AddContactViewModel

@Composable
fun AddContactScreen(onBack: () -> Unit) {
    val viewModel: AddContactViewModel = viewModel()
    val results by viewModel.results.collectAsStateWithLifecycle()

    AddContactContent(
        query = viewModel.query,
        results = results,
        isSearching = viewModel.isSearching,
        error = viewModel.error,
        pendingUids = viewModel.pendingUids,
        onQueryChange = viewModel::onQueryChange,
        onSendRequest = viewModel::sendRequest,
        onAcceptRequest = viewModel::acceptRequest,
        onBack = onBack,
    )
}

@Composable
private fun AddContactContent(
    query: String,
    results: List<UserSearchResult>,
    isSearching: Boolean,
    error: String?,
    pendingUids: Set<String>,
    onQueryChange: (String) -> Unit,
    onSendRequest: (Friend) -> Unit,
    onAcceptRequest: (Friend) -> Unit,
    onBack: () -> Unit,
) {
    ScreenLayout {
        BackHeader("Add\na Contact", onBack = onBack)
        Spacer(Modifier.height(24.dp))
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (isSearching) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            },
            placeholder = { Text("Name or email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            shape = CardShape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        )
        Spacer(Modifier.height(12.dp))

        when {
            error != null -> Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            query.trim().length < 2 -> HintText("Type at least 2 letters to search")
            !isSearching && results.isEmpty() -> HintText("No accounts found")
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            items(results, key = { it.user.uid }) { result ->
                UserCard(
                    result = result,
                    isPending = result.user.uid in pendingUids,
                    onSendRequest = { onSendRequest(result.user) },
                    onAcceptRequest = { onAcceptRequest(result.user) },
                )
            }
        }
    }
}

@Composable
private fun HintText(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun UserCard(
    result: UserSearchResult,
    isPending: Boolean,
    onSendRequest: () -> Unit,
    onAcceptRequest: () -> Unit,
) {
    AppCard(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(result.user.displayName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    result.user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when (result.status) {
                FriendStatus.NONE -> PrimaryButton("Add", enabled = !isPending, onClick = onSendRequest)
                FriendStatus.REQUEST_RECEIVED -> PrimaryButton("Accept", enabled = !isPending, onClick = onAcceptRequest)
                FriendStatus.REQUEST_SENT -> HintText("Sent")
                FriendStatus.FRIEND -> HintText("Friend")
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun AddContactScreenPreview() {
    ProjetFilRougeTheme {
        AddContactContent(
            query = "jea",
            results = listOf(
                UserSearchResult(Friend("1", "jean.dupont@mail.com", "Jean", "Dupont"), FriendStatus.NONE),
                UserSearchResult(Friend("2", "jeanne.martin@mail.com", "Jeanne", "Martin"), FriendStatus.REQUEST_SENT),
                UserSearchResult(Friend("3", "jean.roy@mail.com", "Jean", "Roy"), FriendStatus.REQUEST_RECEIVED),
                UserSearchResult(Friend("4", "jeannot@mail.com", "Jeannot", "Tremblay"), FriendStatus.FRIEND),
            ),
            isSearching = false,
            error = null,
            pendingUids = emptySet(),
            onQueryChange = {},
            onSendRequest = {},
            onAcceptRequest = {},
            onBack = {},
        )
    }
}
