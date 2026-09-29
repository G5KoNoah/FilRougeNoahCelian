package ca.uqac.mobile.projetfilrouge.ui.screen

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ca.uqac.mobile.projetfilrouge.ui.components.BackHeader
import ca.uqac.mobile.projetfilrouge.ui.components.CardShape
import ca.uqac.mobile.projetfilrouge.ui.components.PrimaryButton
import ca.uqac.mobile.projetfilrouge.ui.components.ScreenLayout
import ca.uqac.mobile.projetfilrouge.ui.theme.ProjetFilRougeTheme

@Composable
fun AddContactScreen() {
    ScreenLayout {
        BackHeader("Ajouter\nun Contact")
        Spacer(Modifier.height(24.dp))
        TextField(
            value = "",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            prefix = { Text("@") },
            placeholder = { Text("jhonDoe") },
            singleLine = true,
            shape = CardShape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        )
        Spacer(Modifier.weight(1f))
        PrimaryButton(
            text = "Ajouter",
            modifier = Modifier
                .align(Alignment.End)
                .padding(bottom = 24.dp),
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun AddContactScreenPreview() {
    ProjetFilRougeTheme { AddContactScreen() }
}
