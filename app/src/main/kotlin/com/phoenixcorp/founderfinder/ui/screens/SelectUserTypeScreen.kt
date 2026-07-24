package com.phoenixcorp.founderfinder.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.OnboardingScaffold
import com.phoenixcorp.founderfinder.ui.viewmodel.OnboardingViewModel

/**
 * Role hub for onboarding.
 *
 * Order (top → bottom):
 *  1. Spectator – view-only, no create/update
 *  2. Founder / Regular User – full profile path
 *  3. Partner / Co-founder – requires Founder profile first
 *  4. Advisor – requires Founder profile first
 *  5. Investor – requires Founder profile first
 */
@Composable
fun SelectUserTypeScreen(
    navController: NavHostController,
    onboardingViewModel: OnboardingViewModel
) {
    val profile by onboardingViewModel.profile.collectAsState()
    val isLoading by onboardingViewModel.isLoading.collectAsState()
    val isInitialized by onboardingViewModel.isInitialized.collectAsState()
    val context = LocalContext.current

    // Basic profile = Founder path completed enough to have a name
    val hasBasicProfile = !profile.firstName.isNullOrBlank()

    OnboardingScaffold(
        navController = navController,
        title = "Choose Your Path",
        showBack = true,
        isLoading = isLoading && !isInitialized
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Welcome to FounderFinder",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Pick the path that matches how you want to use the platform.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!isInitialized) {
                CircularProgressIndicator()
            } else {
                // 1 ── Spectator ────────────────────────────────────
                RoleCard(
                    title = "Spectator",
                    subtitle = "Browse the app as a fly on the wall — view only, no creating or editing",
                    icon = Icons.Default.Visibility,
                    iconTint = MaterialTheme.colorScheme.outline,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    buttonLabel = "Continue as Spectator",
                    onClick = {
                        onboardingViewModel.updateRole("SPECTATOR")
                        onboardingViewModel.completeOnboarding { ok ->
                            if (ok) {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.OnboardingGraph.route) { inclusive = true }
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Could not finish setup. Try again.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                )

                // 2 ── Founder / Regular User ──────────────────────
                RoleCard(
                    title = "Founder / Regular User",
                    subtitle = "Build your profile, ideas, and connections. Required before Partner, Advisor, or Investor.",
                    icon = Icons.Default.Person,
                    iconTint = MaterialTheme.colorScheme.primary,
                    containerColor = MaterialTheme.colorScheme.surface,
                    buttonLabel = "Continue as Founder",
                    onClick = {
                        onboardingViewModel.updateRole("FOUNDER")
                        navController.navigate(Screen.FounderFlow.route)
                    }
                )

                // 3 ── Partner / Co-founder ─────────────────────────
                RoleCard(
                    title = "Partner / Co-founder",
                    subtitle = "Find collaborators and join ventures",
                    icon = Icons.Default.Group,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    containerColor = MaterialTheme.colorScheme.surface,
                    buttonLabel = if (hasBasicProfile) {
                        "Continue as Partner"
                    } else {
                        "Complete Founder Profile First"
                    },
                    onClick = {
                        if (hasBasicProfile) {
                            onboardingViewModel.updateRole("PARTNER")
                            navController.navigate(Screen.PartnerFlow.route)
                        } else {
                            Toast.makeText(
                                context,
                                "Complete the Founder / Regular User profile first.",
                                Toast.LENGTH_LONG
                            ).show()
                            onboardingViewModel.updateRole("FOUNDER")
                            navController.navigate(Screen.FounderFlow.route)
                        }
                    },
                    footer = if (!hasBasicProfile) {
                        "You must complete a Founder profile before becoming a Partner."
                    } else null
                )

                // 4 ── Advisor ─────────────────────────────────────
                RoleCard(
                    title = "Advisor",
                    subtitle = "Mentor founders with your expertise",
                    icon = Icons.Default.Star,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    containerColor = MaterialTheme.colorScheme.surface,
                    buttonLabel = if (hasBasicProfile) {
                        "Continue as Advisor"
                    } else {
                        "Complete Founder Profile First"
                    },
                    onClick = {
                        if (hasBasicProfile) {
                            onboardingViewModel.updateRole("ADVISOR")
                            navController.navigate(Screen.AdvisorFlow.route)
                        } else {
                            Toast.makeText(
                                context,
                                "Complete the Founder / Regular User profile first.",
                                Toast.LENGTH_LONG
                            ).show()
                            onboardingViewModel.updateRole("FOUNDER")
                            navController.navigate(Screen.FounderFlow.route)
                        }
                    },
                    footer = if (!hasBasicProfile) {
                        "You must complete a Founder profile before becoming an Advisor."
                    } else null
                )

                // 5 ── Investor ────────────────────────────────────
                RoleCard(
                    title = "Investor",
                    subtitle = "Finance promising ideas and entrepreneurs",
                    icon = Icons.Default.TrendingUp,
                    iconTint = Color(0xFFD4AF37),
                    containerColor = Color(0xFFFFF8E1),
                    titleColor = Color(0xFFB8860B),
                    subtitleColor = Color(0xFF8B5A00),
                    buttonLabel = if (hasBasicProfile) {
                        "Become an Investor"
                    } else {
                        "Complete Founder Profile First"
                    },
                    buttonColors = if (hasBasicProfile) {
                        ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD4AF37),
                            contentColor = Color.Black
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    },
                    onClick = {
                        if (hasBasicProfile) {
                            onboardingViewModel.updateRole("INVESTOR")
                            navController.navigate(Screen.InvestorFlow.route)
                        } else {
                            Toast.makeText(
                                context,
                                "Complete the Founder / Regular User profile first.",
                                Toast.LENGTH_LONG
                            ).show()
                            onboardingViewModel.updateRole("FOUNDER")
                            navController.navigate(Screen.FounderFlow.route)
                        }
                    },
                    footer = if (!hasBasicProfile) {
                        "You must complete a Founder profile before becoming an Investor."
                    } else null
                )
            }
        }
    }
}

@Composable
private fun RoleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    containerColor: Color,
    buttonLabel: String,
    onClick: () -> Unit,
    titleColor: Color = Color.Unspecified,
    subtitleColor: Color = Color.Unspecified,
    buttonColors: androidx.compose.material3.ButtonColors = ButtonDefaults.buttonColors(),
    footer: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = iconTint
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = if (titleColor == Color.Unspecified) {
                    MaterialTheme.colorScheme.onSurface
                } else titleColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = if (subtitleColor == Color.Unspecified) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else subtitleColor
            )
            if (footer != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = footer,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = buttonColors
            ) {
                Text(buttonLabel, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}