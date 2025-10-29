package com.example.procard.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.procard.R
import com.example.procard.model.UserProfile
import com.example.procard.ui.theme.LocalThemeController

@Composable
fun AppHeader(
    user: UserProfile,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val themeController = LocalThemeController.current

    Surface(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val avatarDesc = "Foto de perfil"
            AsyncImage(
                model = user.avatar_url,
                contentDescription = avatarDesc,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .semantics { contentDescription = avatarDesc },
                placeholder = painterResource(id = R.drawable.ic_avatar_placeholder),
                error = painterResource(id = R.drawable.ic_avatar_placeholder),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (user.full_name.isBlank()) "Usuario" else user.full_name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            val isDarkTheme = themeController.isDarkTheme
            val icon = if (isDarkTheme) Icons.Rounded.DarkMode else Icons.Rounded.LightMode
            val description = if (isDarkTheme) {
                "Cambiar a modo claro"
            } else {
                "Cambiar a modo oscuro"
            }
            IconButton(onClick = themeController.toggleTheme) {
                Icon(imageVector = icon, contentDescription = description)
            }
        }
    }
}
