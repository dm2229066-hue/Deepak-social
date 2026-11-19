package com.example.ui.components

import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.ui.theme.EmeraldSuccess
import com.example.util.MediaUtils

@Composable
fun UserAvatar(
    avatarData: String?,
    displayName: String,
    size: Dp = 44.dp,
    showOnline: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(avatarData) { MediaUtils.decodeBase64Bitmap(avatarData) }
    Box(modifier = modifier.size(size)) {
        when {
            bitmap != null -> {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "$displayName avatar",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            !avatarData.isNullOrBlank() && avatarData.startsWith("http") -> {
                AsyncImage(
                    model = avatarData,
                    contentDescription = "$displayName avatar",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                val initial = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "D"
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
        if (showOnline) {
            Box(
                modifier = Modifier
                    .size((size * 0.28f).coerceAtLeast(10.dp))
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(EmeraldSuccess)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

@Composable
fun MediaImage(
    mediaData: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val bitmap = remember(mediaData) { MediaUtils.decodeBase64Bitmap(mediaData) }
    when {
        bitmap != null -> {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
        !mediaData.isNullOrBlank() && (mediaData.startsWith("http") || mediaData.startsWith("content://")) -> {
            AsyncImage(
                model = mediaData,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
        else -> {
            Box(
                modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.VideocamOff,
                    contentDescription = contentDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun VideoPlayerSurface(
    videoSource: String?,
    thumbnailData: String?,
    isPlaying: Boolean,
    isMuted: Boolean,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    var hasPlaybackError by remember(videoSource) { mutableStateOf(false) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }

    val canPlayUri = remember(videoSource) {
        !videoSource.isNullOrBlank() &&
            (videoSource.startsWith("http") || videoSource.startsWith("content://") || videoSource.startsWith("file://"))
    }

    DisposableEffect(isMuted, mediaPlayerRef) {
        mediaPlayerRef?.let { mp ->
            val vol = if (isMuted) 0f else 1f
            try {
                mp.setVolume(vol, vol)
            } catch (_: Exception) {
            }
        }
        onDispose {}
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .clickable(onClick = onTogglePlay),
        contentAlignment = Alignment.Center
    ) {
        if (canPlayUri && !hasPlaybackError && isPlaying) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setOnPreparedListener { mp ->
                            mediaPlayerRef = mp
                            mp.isLooping = true
                            val vol = if (isMuted) 0f else 1f
                            mp.setVolume(vol, vol)
                            start()
                        }
                        setOnErrorListener { _, _, _ ->
                            hasPlaybackError = true
                            true
                        }
                        setVideoURI(Uri.parse(videoSource))
                    }
                },
                update = { view ->
                    if (isPlaying && !view.isPlaying) {
                        view.start()
                    } else if (!isPlaying && view.isPlaying) {
                        view.pause()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            if (!thumbnailData.isNullOrBlank()) {
                MediaImage(
                    mediaData = thumbnailData,
                    contentDescription = "Video preview thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play video",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }
        }
    }
}

@Composable
fun VerifiedCreatorBadge(
    monetizationStatus: String,
    modifier: Modifier = Modifier
) {
    if (monetizationStatus == "approved") {
        Icon(
            imageVector = Icons.Filled.Verified,
            contentDescription = "Verified monetized creator",
            tint = MaterialTheme.colorScheme.primary,
            modifier = modifier.size(16.dp)
        )
    }
}

@Composable
fun ReportDialog(
    targetType: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    val presets = listOf("Spam or scam", "Harassment or abuse", "Misleading monetization", "Inappropriate media")

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Flag,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Report ${targetType.replaceFirstChar { it.uppercase() }}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Help keep Deepak Social safe. Choose or describe the issue below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                presets.forEach { preset ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (reason == preset) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { reason = preset }
                    ) {
                        Text(
                            text = preset,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it.take(300) },
                    label = { Text("Details (min 3 chars)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_reason_input"),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(reason) },
                enabled = reason.trim().length >= 3,
                modifier = Modifier.testTag("submit_report_button")
            ) {
                Text("Submit Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TermsAndPrivacyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Terms & Privacy Policy") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Deepak Social Community & Security Policy", fontWeight = FontWeight.Bold)
                Text(
                    "1. Authentication & Privacy: Sign-in is secured via Google Identity Services and Firebase Authentication. Private account data (email, payout UPI/bank details, and blocked lists) is strictly isolated under your user record.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "2. Creator Monetization & Fraud Prevention: Creator earnings are calculated strictly from verified ad impressions, eligible views, and approved referrals. Self-referrals, artificial engagement, and spam result in suspension.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "3. Content & Safety: You may report abusive posts, Reels, or accounts at any time, block users, switch profile visibility between Public and Friends, or permanently delete your account from Settings.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.testTag("close_terms_button")) {
                Text("I Understand")
            }
        }
    )
}

@Composable
fun StatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
