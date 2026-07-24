package com.phoenixcorp.founderfinder.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.phoenixcorp.founderfinder.ui.screens.AdvisorSearchFeatureScreen
import com.phoenixcorp.founderfinder.ui.screens.AdvisorSignUpScreen
import com.phoenixcorp.founderfinder.ui.screens.AmbitionStatementScreen
import com.phoenixcorp.founderfinder.ui.screens.ConnectSocialsScreen
import com.phoenixcorp.founderfinder.ui.screens.CriteriaForConceptScreen
import com.phoenixcorp.founderfinder.ui.screens.EducationScreen
import com.phoenixcorp.founderfinder.ui.screens.FindPartnersScreen
import com.phoenixcorp.founderfinder.ui.screens.FixInvitationScreen
import com.phoenixcorp.founderfinder.ui.screens.ForumCreationScreen
import com.phoenixcorp.founderfinder.ui.screens.ForumTemplateScreen
import com.phoenixcorp.founderfinder.ui.screens.FounderStatusScreen
import com.phoenixcorp.founderfinder.ui.screens.FutureScreen
import com.phoenixcorp.founderfinder.ui.screens.GlobalIssuesScreen
import com.phoenixcorp.founderfinder.ui.screens.GroupChatScreen
import com.phoenixcorp.founderfinder.ui.screens.HomeScreen
import com.phoenixcorp.founderfinder.ui.screens.IdeaCreationScreen
import com.phoenixcorp.founderfinder.ui.screens.IdeaDevelopmentScreen
import com.phoenixcorp.founderfinder.ui.screens.IdeaGenerationScreen
import com.phoenixcorp.founderfinder.ui.screens.IncubatorConnectionScreen
import com.phoenixcorp.founderfinder.ui.screens.IndustriesOfInterestScreen
import com.phoenixcorp.founderfinder.ui.screens.InvestorInfoScreen
import com.phoenixcorp.founderfinder.ui.screens.InvestorSearchScreen
import com.phoenixcorp.founderfinder.ui.screens.LocalIssuesScreen
import com.phoenixcorp.founderfinder.ui.screens.LocationSelectionScreen
import com.phoenixcorp.founderfinder.ui.screens.MarketPotentialScreen
import com.phoenixcorp.founderfinder.ui.screens.NationalIssuesScreen
import com.phoenixcorp.founderfinder.ui.screens.NotificationsScreen
import com.phoenixcorp.founderfinder.ui.screens.OrganizationDetailsScreen
import com.phoenixcorp.founderfinder.ui.screens.OrganizationFilesScreen
import com.phoenixcorp.founderfinder.ui.screens.OrganizationsOfInterestScreen
import com.phoenixcorp.founderfinder.ui.screens.PartnerSearchFeatureScreen
import com.phoenixcorp.founderfinder.ui.screens.PartnerSignUpScreen
import com.phoenixcorp.founderfinder.ui.screens.PartnersScreen
import com.phoenixcorp.founderfinder.ui.screens.PortfolioAndTermsScreen
import com.phoenixcorp.founderfinder.ui.screens.PrivateChatScreen
import com.phoenixcorp.founderfinder.ui.screens.PrivateMessagesScreen
import com.phoenixcorp.founderfinder.ui.screens.PublicAppearanceScreen
import com.phoenixcorp.founderfinder.ui.screens.RequestedSolutionsScreen
import com.phoenixcorp.founderfinder.ui.screens.SchoolForumsScreen
import com.phoenixcorp.founderfinder.ui.screens.SchoolScreen
import com.phoenixcorp.founderfinder.ui.screens.SelectUserTypeScreen
import com.phoenixcorp.founderfinder.ui.screens.SignInScreen
import com.phoenixcorp.founderfinder.ui.screens.SignUpScreen
import com.phoenixcorp.founderfinder.ui.screens.SplashScreen
import com.phoenixcorp.founderfinder.ui.screens.ThreadScreen
import com.phoenixcorp.founderfinder.ui.screens.UserInfoScreen
import com.phoenixcorp.founderfinder.ui.screens.UserProfileScreen
import com.phoenixcorp.founderfinder.ui.screens.WorkExperienceScreen
import com.phoenixcorp.founderfinder.ui.theme.FounderfinderTheme
import com.phoenixcorp.founderfinder.ui.viewmodel.OnboardingViewModel

/**
 * Main NavGraph.
 *
 * Four separate role flows under one parent [Screen.OnboardingGraph]:
 *  - FounderFlow
 *  - InvestorFlow
 *  - AdvisorFlow
 *  - PartnerFlow
 *
 * Shared [OnboardingViewModel] is scoped to the parent so draft profile
 * data survives Back inside any flow.
 *
 * Intermediate Next never uses popUpTo(inclusive=true).
 * Only finish → Home clears the stack.
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavGraph(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    FounderfinderTheme {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            content = { paddingValues ->
                NavHost(
                    navController = navController,
                    startDestination = Screen.Splash.route,
                    modifier = modifier.padding(paddingValues)
                ) {
                    // ── Auth ──────────────────────────────────────
                    composable(Screen.Splash.route) {
                        SplashScreen(navController)
                    }
                    composable(Screen.SignUp.route) {
                        SignUpScreen(navController)
                    }
                    composable(Screen.SignIn.route) {
                        SignInScreen(navController)
                    }

                    // ── Onboarding parent (shared VM scope) ───────
                    navigation(
                        startDestination = Screen.SelectUserType.route,
                        route = Screen.OnboardingGraph.route
                    ) {
                        // Role hub
                        composable(Screen.SelectUserType.route) { entry ->
                            SelectUserTypeScreen(
                                navController = navController,
                                onboardingViewModel = onboardingVm(navController, entry)
                            )
                        }

                        // ── Founder flow ──────────────────────────
                        navigation(
                            startDestination = Screen.UserInfo.route,
                            route = Screen.FounderFlow.route
                        ) {
                            composable(Screen.UserInfo.route) { entry ->
                                UserInfoScreen(
                                    navController = navController,
                                    onboardingViewModel = onboardingVm(navController, entry)
                                )
                            }
                            composable(Screen.Education.route) { entry ->
                                EducationScreen(
                                    navController = navController,
                                    onboardingViewModel = onboardingVm(navController, entry)
                                )
                            }
                            composable(Screen.WorkExperience.route) { entry ->
                                WorkExperienceScreen(
                                    navController = navController,
                                    onboardingViewModel = onboardingVm(navController, entry)
                                )
                            }
                            composable(Screen.FounderStatus.route) { entry ->
                                FounderStatusScreen(
                                    navController = navController,
                                    onboardingViewModel = onboardingVm(navController, entry)
                                )
                            }
                            composable(Screen.AmbitionStatement.route) { entry ->
                                AmbitionStatementScreen(
                                    navController = navController,
                                    onboardingViewModel = onboardingVm(navController, entry)
                                )
                            }
                            composable(Screen.ConnectSocials.route) { entry ->
                                ConnectSocialsScreen(
                                    navController = navController,
                                    onboardingViewModel = onboardingVm(navController, entry)
                                )
                            }
                            composable(Screen.IndustriesOfInterest.route) { entry ->
                                IndustriesOfInterestScreen(
                                    navController = navController,
                                    onboardingViewModel = onboardingVm(navController, entry)
                                )
                            }
                            composable(Screen.OrganizationsOfInterest.route) { entry ->
                                OrganizationsOfInterestScreen(
                                    navController = navController,
                                    onboardingViewModel = onboardingVm(navController, entry)
                                )
                            }
                            composable(Screen.PublicAppearance.route) { entry ->
                                PublicAppearanceScreen(
                                    navController = navController,
                                    onboardingViewModel = onboardingVm(navController, entry)
                                )
                            }
                        }

                        // ── Investor flow ─────────────────────────
                        // Investor / Advisor / Partner keep their own ViewModels & Firestore logic.
                        // Do NOT pass onboardingViewModel — signatures stay as in the real screens.
                        navigation(
                            startDestination = Screen.InvestorInfo.route,
                            route = Screen.InvestorFlow.route
                        ) {
                            composable(Screen.InvestorInfo.route) {
                                InvestorInfoScreen(navController = navController)
                            }
                            composable(Screen.PortfolioAndTerms.route) {
                                PortfolioAndTermsScreen(navController = navController)
                            }
                        }

                        // ── Advisor flow ──────────────────────────
                        navigation(
                            startDestination = Screen.AdvisorSignUp.route,
                            route = Screen.AdvisorFlow.route
                        ) {
                            composable(Screen.AdvisorSignUp.route) {
                                AdvisorSignUpScreen(navController = navController)
                            }
                        }

                        // ── Partner flow ──────────────────────────
                        navigation(
                            startDestination = Screen.PartnerSignUp.route,
                            route = Screen.PartnerFlow.route
                        ) {
                            composable(Screen.PartnerSignUp.route) {
                                PartnerSignUpScreen(navController = navController)
                            }
                        }
                    }

                    // ── Main App ──────────────────────────────────
                    composable(Screen.Home.route) {
                        HomeScreen(navController)
                    }
                    composable(Screen.Notifications.route) {
                        NotificationsScreen(navController = navController)
                    }
                    composable(Screen.Partners.route) {
                        PartnersScreen(navController)
                    }
                    composable(Screen.FindPartners.route) {
                        FindPartnersScreen(navController)
                    }
                    composable(Screen.IdeaDevelopment.route) {
                        IdeaDevelopmentScreen(navController)
                    }
                    composable(Screen.AdvisorSearchFeature.route) {
                        AdvisorSearchFeatureScreen(navController)
                    }
                    composable(Screen.PartnerSearchFeature.route) {
                        PartnerSearchFeatureScreen(navController)
                    }
                    composable(Screen.SchoolForums.route) {
                        SchoolForumsScreen(navController)
                    }
                    composable(Screen.PrivateMessages.route) {
                        PrivateMessagesScreen(navController)
                    }
                    composable(Screen.LocationSelection.route) {
                        LocationSelectionScreen(navController)
                    }
                    composable(Screen.CriteriaForConcept.route) {
                        CriteriaForConceptScreen(navController)
                    }
                    composable(Screen.IncubatorConnection.route) {
                        IncubatorConnectionScreen(navController)
                    }
                    composable(Screen.InvestorSearch.route) {
                        InvestorSearchScreen(navController)
                    }
                    composable(Screen.GlobalIssues.route) {
                        GlobalIssuesScreen(navController)
                    }
                    composable(Screen.NationalIssues.route) {
                        NationalIssuesScreen(navController)
                    }
                    composable(Screen.LocalIssues.route) {
                        LocalIssuesScreen(navController)
                    }
                    composable(Screen.Future.route) {
                        FutureScreen(navController)
                    }
                    composable(Screen.MarketPotential.route) {
                        MarketPotentialScreen(navController)
                    }
                    composable(Screen.RequestedSolutions.route) {
                        RequestedSolutionsScreen(navController)
                    }
                    composable(Screen.IdeaCreation.route) {
                        IdeaCreationScreen(navController)
                    }
                    composable(Screen.FixInvitation.route) {
                        FixInvitationScreen()
                    }

                    // Parameterized
                    composable(
                        route = Screen.PrivateChat.route,
                        arguments = Screen.PrivateChat.arguments
                    ) { entry ->
                        PrivateChatScreen(
                            navController = navController,
                            conversationId = entry.arguments?.getString("conversationId") ?: ""
                        )
                    }
                    composable(
                        route = Screen.GroupChat.route,
                        arguments = Screen.GroupChat.arguments
                    ) { entry ->
                        GroupChatScreen(
                            navController = navController,
                            orgId = entry.arguments?.getString("orgId") ?: ""
                        )
                    }
                    composable(
                        route = Screen.UserProfile.route,
                        arguments = Screen.UserProfile.arguments
                    ) { entry ->
                        UserProfileScreen(
                            navController = navController,
                            userId = entry.arguments?.getString("userId") ?: ""
                        )
                    }
                    composable(
                        route = Screen.InstitutionForum.route,
                        arguments = Screen.InstitutionForum.arguments
                    ) { entry ->
                        ForumTemplateScreen(
                            navController = navController,
                            institutionName = "${entry.arguments?.getString("category")}/${entry.arguments?.getString("forumId")}"
                        )
                    }
                    composable(
                        route = "thread/{category}/{forumId}/{threadId}",
                        arguments = listOf(
                            navArgument("category") { type = NavType.StringType },
                            navArgument("forumId") { type = NavType.StringType },
                            navArgument("threadId") { type = NavType.StringType }
                        )
                    ) { entry ->
                        ThreadScreen(
                            threadId = entry.arguments?.getString("threadId") ?: "",
                            forumId = entry.arguments?.getString("forumId") ?: "",
                            category = entry.arguments?.getString("category") ?: "requestedsolutions",
                            navController = navController
                        )
                    }
                    composable(
                        route = Screen.School.route,
                        arguments = Screen.School.arguments
                    ) { entry ->
                        SchoolScreen(
                            navController = navController,
                            schoolName = entry.arguments?.getString("schoolName") ?: ""
                        )
                    }
                    composable(
                        route = Screen.IdeaGeneration.route,
                        arguments = Screen.IdeaGeneration.arguments
                    ) { entry ->
                        IdeaGenerationScreen(
                            navController = navController,
                            showAddButton = true,
                            category = entry.arguments?.getString("category")
                        )
                    }
                    composable(
                        route = Screen.ForumCreation.route,
                        arguments = Screen.ForumCreation.arguments
                    ) { entry ->
                        ForumCreationScreen(
                            navController = navController,
                            initialCategory = entry.arguments?.getString("category"),
                            initialLocation = entry.arguments?.getString("location")
                        )
                    }
                    composable(
                        route = Screen.OrganizationFiles.route,
                        arguments = Screen.OrganizationFiles.arguments
                    ) { entry ->
                        OrganizationFilesScreen(
                            navController = navController,
                            orgId = entry.arguments?.getString("orgId") ?: ""
                        )
                    }
                    composable(
                        route = Screen.OrganizationDetails.route,
                        arguments = Screen.OrganizationDetails.arguments
                    ) { entry ->
                        OrganizationDetailsScreen(
                            navController = navController,
                            orgId = entry.arguments?.getString("orgId") ?: "",
                            invitationId = entry.arguments?.getString("invitationId") ?: ""
                        )
                    }
                }
            }
        )
    }
}

/**
 * Shared OnboardingViewModel scoped to [Screen.OnboardingGraph].
 * Works from any child of the parent graph (including nested role flows).
 */
@Composable
private fun onboardingVm(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry
): OnboardingViewModel {
    val parentEntry = remember(backStackEntry) {
        navController.getBackStackEntry(Screen.OnboardingGraph.route)
    }
    return hiltViewModel(parentEntry)
}