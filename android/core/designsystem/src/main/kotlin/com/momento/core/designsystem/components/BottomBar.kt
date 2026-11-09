package com.momento.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.momento.core.designsystem.theme.PrimaryYellow
import com.momento.core.designsystem.theme.SurfaceWhite
import com.momento.core.designsystem.theme.TextDark
import com.momento.core.designsystem.theme.TextPrimary
import com.momento.core.designsystem.theme.TextSecondary

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val isCreate: Boolean = false,
    val showNotificationDot: Boolean = false
)

object BottomNavItems {
    val items = listOf(
        BottomNavItem("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
        BottomNavItem("discover", "Discover", Icons.Filled.Explore, Icons.Outlined.Explore),
        BottomNavItem("create", "Create", Icons.Filled.Add, Icons.Filled.Add, isCreate = true),
        BottomNavItem("pod", "Pod", Icons.Filled.GraphicEq, Icons.Outlined.GraphicEq, showNotificationDot = true),
        BottomNavItem("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person)
    )
}

@Composable
fun MomentoBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(32.dp), spotColor = Color.Black.copy(alpha = 0.18f))
                .clip(RoundedCornerShape(32.dp))
                .background(SurfaceWhite.copy(alpha = 0.94f))
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            BottomNavItems.items.forEach { item ->
                if (item.isCreate) {
                    CreateNavButton(
                        selected = currentRoute == item.route,
                        onClick = { onNavigate(item.route) }
                    )
                } else {
                    NavButton(
                        item = item,
                        selected = currentRoute == item.route,
                        onClick = { onNavigate(item.route) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavButton(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.label,
                tint = if (selected) TextDark else TextSecondary,
                modifier = Modifier.size(22.dp)
            )
            if (item.showNotificationDot) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-2).dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(com.momento.core.designsystem.theme.ErrorRed)
                )
            }
        }
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) TextPrimary else TextSecondary
        )
        Box(
            modifier = Modifier
                .size(width = 18.dp, height = 3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) PrimaryYellow else Color.Transparent)
        )
    }
}

@Composable
private fun CreateNavButton(
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .offset(y = (-14).dp)
            .size(54.dp)
            .shadow(8.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.25f))
            .clip(CircleShape)
            .background(if (selected) TextDark else PrimaryYellow)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Create",
            tint = if (selected) PrimaryYellow else TextDark,
            modifier = Modifier.size(26.dp)
        )
    }
}
