package ca.uqac.mobile.projetfilrouge.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ca.uqac.mobile.projetfilrouge.ui.components.AppCard
import ca.uqac.mobile.projetfilrouge.ui.components.BackHeader
import ca.uqac.mobile.projetfilrouge.ui.components.PrimaryButton
import ca.uqac.mobile.projetfilrouge.ui.components.ScreenLayout
import ca.uqac.mobile.projetfilrouge.ui.components.SecondaryButton
import ca.uqac.mobile.projetfilrouge.ui.theme.ProjetFilRougeTheme
import ca.uqac.mobile.projetfilrouge.ui.theme.VoteAgainst
import ca.uqac.mobile.projetfilrouge.ui.theme.VoteCircle
import ca.uqac.mobile.projetfilrouge.ui.theme.VoteFor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VoteRoomScreen() {
    ScreenLayout {
        BackHeader("Salle de Vote")
        Spacer(Modifier.height(24.dp))
        AppCard {
            ProposalTabs()
            Spacer(Modifier.height(16.dp))
            VoteCircle(
                listOf(VoteFor, VoteFor, VoteAgainst, VoteAgainst, VoteFor, VoteAgainst, VoteAgainst, VoteAgainst),
                Modifier.padding(horizontal = 8.dp),
            )
            Text(
                "2 h",
                modifier = Modifier.align(Alignment.End),
                color = VoteAgainst,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        ) {
            SecondaryButton("Against", contentColor = VoteAgainst)
            PrimaryButton("For", contentColor = VoteFor)
        }
    }
}

@Composable
private fun ProposalTabs() {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf("2 : 00", "4 : 00", "sound", "5:00", "8:00").forEachIndexed { index, label ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (index == 0) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.background
                    )
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
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
    ProjetFilRougeTheme { VoteRoomScreen() }
}
