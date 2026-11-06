package ca.uqac.mobile.projetfilrouge.ui.components

import androidx.annotation.StringRes
import ca.uqac.mobile.projetfilrouge.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.PermContactCalendar
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import ca.uqac.mobile.projetfilrouge.data.Friend
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.uqac.mobile.projetfilrouge.AppVibrator
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage

val CardShape = RoundedCornerShape(12.dp)
private val PillShape = RoundedCornerShape(8.dp)

private val AvatarColors = listOf(
    Color(0xFF4F6BED), Color(0xFFC239B3), Color(0xFF0078D4), Color(0xFF038387),
    Color(0xFFCA5010), Color(0xFF8764B8), Color(0xFF498205), Color(0xFFD13438),
)

// La couleur dépend du nom : une même personne garde toujours la même
private fun avatarColor(name: String): Color = AvatarColors[(name.hashCode() and Int.MAX_VALUE) % AvatarColors.size]

// "Jean Dupont" -> "JD", "jean" -> "J"
fun initialsOf(name: String): String {
    val words = name.split(' ').filter { it.isNotBlank() }
    val letters = if (words.size >= 2) listOf(words.first(), words.last()) else words
    return letters.joinToString("") { it.first().uppercase() }
}

enum class NavTab(val route: String, val icon: ImageVector, @StringRes val labelRes: Int) {
    Alarms("alarms", Icons.Outlined.Alarm, R.string.tab_alarms),
    Contacts("contacts", Icons.Outlined.PermContactCalendar, R.string.tab_contacts),
    Profile("account", Icons.Outlined.Person, R.string.tab_profile),
}

@Composable
fun ScreenLayout(content: @Composable ColumnScope.() -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            content = content,
        )
    }
}

@Composable
fun BottomNavBar(selectedTab: NavTab, onTabSelected: (NavTab) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            NavTab.entries.forEach { tab ->
                Box(
                    Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(
                            if (tab == selectedTab) MaterialTheme.colorScheme.surfaceVariant
                            else Color.Transparent
                        )
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(tab.icon, contentDescription = stringResource(tab.labelRes))
                }
            }
        }
    }
}

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        fontSize = 30.sp,
        lineHeight = 30.sp,
    )
}

@Composable
fun BackHeader(title: String, onBack: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
        }
        ScreenTitle(
            title,
            Modifier
                .align(Alignment.Center)
                .padding(horizontal = 80.dp),
        )
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    enabled: Boolean = true,
    onClick: ()-> Unit = {}
) {
    val context = LocalContext.current
    val vibrator = remember { AppVibrator(context) }
    Button(
        onClick = {
            vibrator.click()
            onClick()},
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(contentColor = contentColor),
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    onClick: ()-> Unit = {}
) {
    Button(
        onClick = {onClick()},
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = contentColor,
        ),
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SmallSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)? = null) {
    var shown by remember(checked) { mutableStateOf(checked) }
    Switch(
        checked = shown,
        onCheckedChange = onCheckedChange?.let { callback ->
            { newValue ->
                shown = newValue
                callback(newValue)
            }
        },
        modifier = Modifier.scale(0.7f),
        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
    )
}

@Composable
fun SwitchRow(
    label: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: ((Boolean) -> Unit)? = null,
) {
    AppCard(modifier, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            SmallSwitch(checked, onCheckedChange)
        }
    }
}

@Composable
fun MemberAvatars(count: Int, modifier: Modifier = Modifier) {
    MemberAvatars(List(count) { null }, modifier)
}

@Composable
fun MemberAvatars(members: List<Friend?>, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        members.forEach { UserAvatar(it?.avatarUrl, it?.displayName) }
    }
}

// Photo de profil, ou initiales sur un fond coloré (comme Teams) quand il n'y en a pas
@Composable
fun UserAvatar(avatarUrl: String?, name: String?, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    val initials = name?.let(::initialsOf).orEmpty()
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(if (initials.isEmpty()) Color.Transparent else avatarColor(name.orEmpty())),
        contentAlignment = Alignment.Center,
    ) {
        if (initials.isEmpty()) {
            Icon(Icons.Outlined.AccountCircle, contentDescription = null, modifier = Modifier.matchParentSize())
        } else {
            Text(
                initials,
                color = Color.White,
                fontSize = with(LocalDensity.current) { (size * 0.4f).toSp() },
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape),
            )
        }
    }
}

@Composable
fun PillTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
        modifier = modifier
            .fillMaxWidth()
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        decorationBox = { innerTextField ->
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (value.isEmpty()) Text(placeholder, style = textStyle)
                innerTextField()
            }
        },
    )
}

@Composable
fun PillDropdown(
    label: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(PillShape)
                .background(MaterialTheme.colorScheme.background)
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, Modifier.size(20.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
