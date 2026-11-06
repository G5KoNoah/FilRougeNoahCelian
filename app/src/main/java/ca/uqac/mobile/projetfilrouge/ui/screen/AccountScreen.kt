package ca.uqac.mobile.projetfilrouge.ui.screen


import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.uqac.mobile.projetfilrouge.R
import ca.uqac.mobile.projetfilrouge.data.AccountData
import ca.uqac.mobile.projetfilrouge.viewModel.AccountDetailsViewModel
import ca.uqac.mobile.projetfilrouge.viewModel.SettingsViewModel
import ca.uqac.mobile.projetfilrouge.data.supabase
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import ca.uqac.mobile.projetfilrouge.ui.components.UserAvatar

// Écran du profil : avatar, infos et réglages
@Composable
fun AccountScreen(onLogout: () -> Unit) {
    val accountViewModel: AccountDetailsViewModel = viewModel()
    val account by accountViewModel.account.collectAsStateWithLifecycle()
    val error by accountViewModel.error.collectAsStateWithLifecycle()
    val isUploadingAvatar by accountViewModel.isUploadingAvatar.collectAsStateWithLifecycle()
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) accountViewModel.uploadAvatar(uri)
    }

    Scaffold(modifier = Modifier.fillMaxSize(), containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            error?.let { Text(it, color = Color.Red, fontWeight = FontWeight.Bold) }
            MainInfo(
                account = account,
                isUploadingAvatar = isUploadingAvatar,
                onChangeAvatar = {
                    pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onRemoveAvatar = accountViewModel::removeAvatar,
            )
            OtherInfo(account = account)
            SettingsInfo(onLogout = onLogout)
        }
    }
}

// Style des cartes du profil
@Composable
fun Modifier.cardStyle(): Modifier {
    val forme = RoundedCornerShape(16.dp)
    return this
        .fillMaxWidth()
        .background(color = MaterialTheme.colorScheme.surface, shape = forme)
        .border(width = 2.dp, color = MaterialTheme.colorScheme.surfaceVariant, shape = forme)
        .padding(24.dp)
}

// Carte avec l'avatar et le nom
@Composable
fun MainInfo(
    account: AccountData?,
    isUploadingAvatar: Boolean,
    onChangeAvatar: () -> Unit,
    onRemoveAvatar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.cardStyle(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Avatar(
            avatarUrl = account?.avatarUrl,
            name = account?.displayName,
            isUploading = isUploadingAvatar,
            onClick = onChangeAvatar,
        )
        if (account?.avatarUrl != null) {
            TextButton(onClick = onRemoveAvatar, enabled = !isUploadingAvatar) {
                Text(stringResource(R.string.avatar_remove), color = MaterialTheme.colorScheme.error)
            }
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(account?.firstName ?: "...", color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(account?.lastName ?: "...", color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// Avatar cliquable pour changer la photo
@Composable
private fun Avatar(avatarUrl: String?, name: String?, isUploading: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clickable(
                enabled = !isUploading,
                onClickLabel = stringResource(R.string.avatar_change),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        UserAvatar(avatarUrl, name, size = 120.dp)
        if (isUploading) {
            CircularProgressIndicator()
        }
        Icon(
            Icons.Outlined.PhotoCamera,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .padding(6.dp),
        )
    }
}

// Carte de contact (email)
@Composable
fun OtherInfo(account: AccountData?, modifier: Modifier = Modifier) {
    Column(modifier = modifier.cardStyle()) {
        Text(stringResource(R.string.account_contact), color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(account?.email ?: "...", color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// Carte des réglages : thème et déconnexion
@Composable
fun SettingsInfo(modifier: Modifier = Modifier, onLogout: () -> Unit) {
    val scope = rememberCoroutineScope()
    val viewModel: SettingsViewModel = viewModel()
    val estSombre by viewModel.estSombre.collectAsStateWithLifecycle()
    Column(modifier = modifier.cardStyle()) {
        Text(stringResource(R.string.account_settings), color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.account_theme), color = MaterialTheme.colorScheme.onSurface)
            Switch(
                checked = estSombre,
                onCheckedChange = { nouvelleValeur ->
                    viewModel.basculerThemeSombre(nouvelleValeur) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color.Black
                )
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                scope.launch {
                    runCatching { supabase.auth.signOut() }
                    onLogout()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            )
        ) {
            Text(stringResource(R.string.account_logout))
        }
    }
}