package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

private const val FACEBOOK_PROFILE_URL = "https://www.facebook.com/profile.php?id=61594951162159"

@Composable
fun TermsAndLicenseScreen(
    onAccept: () -> Unit,
    isReadOnly: Boolean = false,
    onClose: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // iOS System Dark Theme: pure black background with frosted translucent cards
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // iOS Navigation Bar / Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1C1C1E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_bubble_logo),
                            contentDescription = "Logo Ferox",
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Ferox",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (isReadOnly && onClose != null) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1C1C1E))
                            .testTag("close_license_screen")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = Color(0xFF8E8E93),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    // iOS Pill Badge "Bêta 1.0"
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = Color(0xFF1C1C1E),
                        border = BorderStroke(0.5.dp, Color(0xFF38383A))
                    ) {
                        Text(
                            text = "Bêta 1.0 • Gratuit",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0A84FF),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Scrollable Content area (leaves room for the pinned sticky button below)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(6.dp))

                // Card 1: Official Creator Card (iOS Style Grouped Inset Card)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1C1C1E),
                    border = BorderStroke(0.5.dp, Color(0xFF2C2C2E)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(FACEBOOK_PROFILE_URL)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                        .testTag("creator_profile_card")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User's Real Facebook Avatar
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .border(BorderStroke(1.5.dp, Color(0xFF0A84FF)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.creator_profile_avatar),
                                contentDescription = "Photo de delt slay",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "delt slay",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Badge officiel",
                                    tint = Color(0xFF0A84FF),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Créateur et propriétaire de l'application",
                                fontSize = 12.sp,
                                color = Color(0xFF8E8E93)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Toucher pour ouvrir le profil Facebook",
                                fontSize = 11.5.sp,
                                color = Color(0xFF0A84FF),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = Color(0xFF8E8E93),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Card 2: iOS-Style Anti-Scam Alert Banner
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF2C1E14),
                    border = BorderStroke(0.5.dp, Color(0xFFFF9F0A).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("anti_scam_warning_card")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF9F0A),
                            modifier = Modifier
                                .size(20.dp)
                                .padding(top = 1.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Avertissement : Application 100% Gratuite",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD60A)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Ne payez jamais pour cette application. Méfiez-vous des escroqueries (scams), des faux vendeurs et des copies payantes.",
                                fontSize = 12.sp,
                                color = Color(0xFFFFE58F),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Card 3: Conditions Générales & Licence (Clean, concise, without bubble details)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1C1C1E),
                    border = BorderStroke(0.5.dp, Color(0xFF2C2C2E)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("terms_content_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Conditions d'utilisation & Licence",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        IosTermsRow(
                            label = "Propriété exclusive",
                            value = "Application officielle créée et détenue exclusivement par delt slay."
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        IosTermsRow(
                            label = "Respect communautaire",
                            value = "Le respect et la courtoisie sont requis entre les chauffeurs de Truckers of Europe."
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        IosTermsRow(
                            label = "Version Bêta",
                            value = "Ferox évolue en continu pour offrir la meilleure expérience vocale possible."
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // STICKY BOTTOM BAR (iOS Style Glass Footer): Button is DIRECTLY visible without scrolling!
            Surface(
                color = Color(0xFF121214).copy(alpha = 0.95f),
                border = BorderStroke(0.5.dp, Color(0xFF2C2C2E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Main iOS Action Button: Directly Visible
                    Button(
                        onClick = onAccept,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("accept_terms_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0A84FF), // iOS System Blue
                            contentColor = Color.White
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isReadOnly) "Fermer" else "Accéder à l'application",
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (isReadOnly) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (!isReadOnly) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "En continuant, vous acceptez les conditions de Ferox",
                            fontSize = 11.sp,
                            color = Color(0xFF8E8E93),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IosTermsRow(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0A84FF)
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            color = Color(0xFFD1D1D6),
            lineHeight = 16.sp
        )
    }
}
