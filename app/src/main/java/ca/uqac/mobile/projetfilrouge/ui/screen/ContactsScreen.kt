package ca.uqac.mobile.projetfilrouge.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ca.uqac.mobile.projetfilrouge.ui.components.AppCard
import ca.uqac.mobile.projetfilrouge.ui.components.ScreenLayout
import ca.uqac.mobile.projetfilrouge.ui.components.ScreenTitle
import ca.uqac.mobile.projetfilrouge.ui.theme.ProjetFilRougeTheme

@Composable
fun ContactsScreen() {
    ScreenLayout {
        Spacer(Modifier.height(24.dp))
        ScreenTitle("Contacts", Modifier.fillMaxWidth())
        IconButton(onClick = {}, modifier = Modifier.align(Alignment.End)) {
            Icon(Icons.Outlined.AddCircleOutline, contentDescription = "Ajouter un contact")
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ContactCard("John Doe")
            ContactCard("Jane Smith")
        }
    }
}

@Composable
private fun ContactCard(name: String) {
    AppCard(contentPadding = PaddingValues(0.dp)) {
        Box(Modifier.fillMaxWidth()) {
            Text(
                text = name,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
            IconButton(onClick = {}, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Outlined.RemoveCircleOutline, contentDescription = "Supprimer $name")
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun ContactsScreenPreview() {
    ProjetFilRougeTheme { ContactsScreen() }
}
