package com.phoenixcorp.founderfinder.navigation

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.phoenixcorp.founderfinder.ui.theme.FounderfinderTheme
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
                    composable(Screen.Splash.route) {
                        ImprovedSplashScreen(navController)   // Use the improved version
                    }
                    composable(Screen.SignUp.route) {
                        SignUpScreen(navController)
                    }
                    composable(Screen.SignIn.route) {
                        SignInScreen(navController)
                    }
                    composable(Screen.SelectUserType.route) {
                        SelectUserTypeScreen(navController)
                    }
                    composable(Screen.UserInfo.route) {
                        UserInfoScreen(navController)
                    }
                    composable(Screen.Education.route) {
                        EducationScreen(navController)
                    }
                    composable(Screen.WorkExperience.route) {
                        WorkExperienceScreen(navController)
                    }
                    composable(Screen.FounderStatus.route) {
                        FounderStatusScreen(navController)
                    }
                    composable(Screen.AmbitionStatement.route) {
                        AmbitionStatementScreen(navController)
                    }
                    composable(Screen.ConnectSocials.route) {
                        ConnectSocialsScreen(navController)
                    }
                    composable(Screen.IndustriesOfInterest.route) {
                        IndustriesOfInterestScreen(navController)
                    }
                    composable(Screen.OrganizationsOfInterest.route) {
                        OrganizationsOfInterestScreen(navController)
                    }
                    composable(Screen.PublicAppearance.route) {
                        PublicAppearanceScreen(navController)
                    }
                    composable(Screen.InvestorInfo.route) {
                        InvestorInfoScreen(navController)
                    }
                    composable(Screen.PortfolioAndTerms.route) {
                        PortfolioAndTermsScreen(navController)
                    }
                    composable(Screen.Home.route) {
                        HomeScreen(navController)
                    }
                    composable(Screen.Notifications.route) {
                        NotificationsScreen(navController = navController)
                    }
                    composable(
                        route = Screen.InstitutionForum.route,
                        arguments = Screen.InstitutionForum.arguments
                    ) { backStackEntry ->
                        ForumTemplateScreen(
                            navController = navController,
                            institutionName = "${backStackEntry.arguments?.getString("category")}/${backStackEntry.arguments?.getString("forumId")}"
                        )
                    }
                    composable(
                        route = "thread/{category}/{forumId}/{threadId}",
                        arguments = listOf(
                            navArgument("category") { type = NavType.StringType },
                            navArgument("forumId") { type = NavType.StringType },
                            navArgument("threadId") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val category = backStackEntry.arguments?.getString("category") ?: "requestedsolutions"
                        val forumId = backStackEntry.arguments?.getString("forumId") ?: ""
                        val threadId = backStackEntry.arguments?.getString("threadId") ?: ""

                        ThreadScreen(
                            threadId = threadId,
                            forumId = forumId,
                            category = category,
                            navController = navController
                        )
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
                    composable(Screen.AdvisorSignUp.route) {
                        AdvisorSignUpScreen(navController)
                    }
                    composable(Screen.PartnerSignUp.route) {
                        PartnerSignUpScreen(navController)
                    }
                    composable(Screen.PrivateMessages.route) {
                        PrivateMessagesScreen(navController)
                    }
                    composable(
                        route = Screen.PrivateChat.route,
                        arguments = Screen.PrivateChat.arguments
                    ) { backStackEntry ->
                        PrivateChatScreen(
                            navController = navController,
                            conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
                        )
                    }
                    composable(
                        route = Screen.GroupChat.route,
                        arguments = Screen.GroupChat.arguments
                    ) { backStackEntry ->
                        GroupChatScreen(
                            navController = navController,
                            orgId = backStackEntry.arguments?.getString("orgId") ?: ""
                        )
                    }
                    composable(
                        route = Screen.UserProfile.route,
                        arguments = Screen.UserProfile.arguments
                    ) { backStackEntry ->
                        UserProfileScreen(
                            navController = navController,
                            userId = backStackEntry.arguments?.getString("userId") ?: ""
                        )
                    }
                    composable(
                        route = Screen.School.route,
                        arguments = Screen.School.arguments
                    ) { backStackEntry ->
                        SchoolScreen(
                            navController = navController,
                            schoolName = backStackEntry.arguments?.getString("schoolName") ?: ""
                        )
                    }
                    composable(
                        route = Screen.IdeaGeneration.route,
                        arguments = Screen.IdeaGeneration.arguments
                    ) { backStackEntry ->
                        IdeaGenerationScreen(
                            navController = navController,
                            showAddButton = true,
                            category = backStackEntry.arguments?.getString("category")
                        )
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
                    composable(
                        route = Screen.ForumCreation.route,
                        arguments = Screen.ForumCreation.arguments
                    ) { backStackEntry ->
                        ForumCreationScreen(
                            navController = navController,
                            initialCategory = backStackEntry.arguments?.getString("category"),
                            initialLocation = backStackEntry.arguments?.getString("location")
                        )
                    }
                    composable(
                        route = Screen.OrganizationFiles.route,
                        arguments = Screen.OrganizationFiles.arguments
                    ) { backStackEntry ->
                        OrganizationFilesScreen(
                            navController = navController,
                            orgId = backStackEntry.arguments?.getString("orgId") ?: ""
                        )
                    }
                    composable(
                        route = Screen.InstitutionForum.route,
                        arguments = Screen.InstitutionForum.arguments
                    ) { backStackEntry ->
                        ForumTemplateScreen(
                            navController = navController,
                            institutionName = "${backStackEntry.arguments?.getString("category")}/${backStackEntry.arguments?.getString("forumId")}"
                        )
                    }
                    composable(
                        route = Screen.OrganizationDetails.route,
                        arguments = Screen.OrganizationDetails.arguments
                    ) { backStackEntry ->
                        OrganizationDetailsScreen(
                            navController = navController,
                            orgId = backStackEntry.arguments?.getString("orgId") ?: "",
                            invitationId = backStackEntry.arguments?.getString("invitationId") ?: ""
                        )
                    }
                    composable(Screen.FixInvitation.route) {
                        FixInvitationScreen()
                    }
                }
            }
        )
    }
}

// Improved SplashScreen with Auth Check
@Composable
fun ImprovedSplashScreen(navController: NavHostController) {
    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        } else {
            navController.navigate(Screen.SignIn.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    }

    // Loading UI while deciding where to go
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Loading FounderFinder...")
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenWithHeader(navController: NavHostController, title: String, content: @Composable () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            content()
        }
    }
}

// Placeholder screens
@Composable
fun SplashScreen(navController: NavHostController) { ScreenWithHeader(navController, "Splash Screen") { Text("Splash Screen Content") } }
@Composable
fun SignUpScreen(navController: NavHostController) { ScreenWithHeader(navController, "Sign Up Screen") { Text("Sign Up Screen Content") } }
@Composable
fun SignInScreen(navController: NavHostController) { ScreenWithHeader(navController, "Sign In Screen") { Text("Sign In Screen Content") } }
@Composable
fun SelectUserTypeScreen(navController: NavHostController) { ScreenWithHeader(navController, "Select User Type Screen") { Text("Select User Type Screen Content") } }
@Composable
fun UserInfoScreen(navController: NavHostController) { ScreenWithHeader(navController, "User Info Screen") { Text("User Info Screen Content") } }
@Composable
fun EducationScreen(navController: NavHostController) { ScreenWithHeader(navController, "Education Screen") { Text("Education Screen Content") } }
@Composable
fun WorkExperienceScreen(navController: NavHostController) { ScreenWithHeader(navController, "Work Experience Screen") { Text("Work Experience Screen Content") } }
@Composable
fun FounderStatusScreen(navController: NavHostController) { ScreenWithHeader(navController, "Founder Status Screen") { Text("Founder Status Screen Content") } }
@Composable
fun AmbitionStatementScreen(navController: NavHostController) { ScreenWithHeader(navController, "Ambition Statement Screen") { Text("Ambition Statement Screen Content") } }
@Composable
fun ConnectSocialsScreen(navController: NavHostController) { ScreenWithHeader(navController, "Connect Socials Screen") { Text("Connect Socials Screen Content") } }
@Composable
fun IndustriesOfInterestScreen(navController: NavHostController) { ScreenWithHeader(navController, "Industries of Interest Screen") { Text("Industries of Interest Screen Content") } }
@Composable
fun OrganizationsOfInterestScreen(navController: NavHostController) { ScreenWithHeader(navController, "Organizations of Interest Screen") { Text("Organizations of Interest Screen Content") } }
@Composable
fun PublicAppearanceScreen(navController: NavHostController) { ScreenWithHeader(navController, "Public Appearance Screen") { Text("Public Appearance Screen Content") } }
@Composable
fun InvestorInfoScreen(navController: NavHostController) { ScreenWithHeader(navController, "Investor Info Screen") { Text("Investor Info Screen Content") } }
@Composable
fun PortfolioCompaniesScreen(navController: NavHostController) { ScreenWithHeader(navController, "Portfolio Companies Screen") { Text("Portfolio Companies Screen Content") } }
@Composable
fun TermsAndExpectationsScreen(navController: NavHostController) { ScreenWithHeader(navController, "Terms and Expectations Screen") { Text("Terms and Expectations Screen Content") } }
@Composable
fun HomeScreen(navController: NavHostController) { ScreenWithHeader(navController, "Home Screen") { Text("Home Screen Content") } }
@Composable
fun PartnersScreen(navController: NavHostController) { ScreenWithHeader(navController, "Partners Screen") { Text("Partners Screen Content") } }
@Composable
fun FindPartnersScreen(navController: NavHostController) { ScreenWithHeader(navController, "Find Partners Screen") { Text("Find Partners Screen Content") } }
@Composable
fun IdeaDevelopmentScreen(navController: NavHostController) { ScreenWithHeader(navController, "Idea Development Screen") { Text("Idea Development Screen Content") } }
@Composable
fun AdvisorSearchFeatureScreen(navController: NavHostController) { ScreenWithHeader(navController, "Advisor Search Feature Screen") { Text("Advisor Search Feature Screen Content") } }
@Composable
fun PartnerSearchFeatureScreen(navController: NavHostController) { ScreenWithHeader(navController, "Partner Search Feature Screen") { Text("Partner Search Feature Screen Content") } }
@Composable
fun PrivateMessagesScreen(navController: NavHostController) { ScreenWithHeader(navController, "Private Messages Screen") { Text("Private Messages Screen Content") } }
@Composable
fun SchoolForumsScreen(navController: NavHostController) { ScreenWithHeader(navController, "School Forums Screen") { Text("School Forums Screen Content") } }
@Composable
fun AdvisorSignUpScreen(navController: NavHostController) { ScreenWithHeader(navController, "Advisor Sign Up Screen") { Text("Advisor Sign Up Screen Content") } }
@Composable
fun PartnerSignUpScreen(navController: NavHostController) { ScreenWithHeader(navController, "Partner Sign Up Screen") { Text("Partner Sign Up Screen Content") } }
@Composable
fun CriteriaForConceptScreen(navController: NavHostController) { ScreenWithHeader(navController, "Criteria for Concept Screen") { Text("Criteria for Concept Screen Content") } }
@Composable
fun IncubatorConnectionScreen(navController: NavHostController) { ScreenWithHeader(navController, "Incubator Connection Screen") { Text("Incubator Connection Screen Content") } }
@Composable
fun InvestorSearchScreen(navController: NavHostController) { ScreenWithHeader(navController, "Investor Search Screen") { Text("Investor Search Screen Content") } }
@Composable
fun ForumCreationScreen(navController: NavHostController, initialCategory: String?, initialLocation: String?) {
    ScreenWithHeader(navController, "Forum Creation Screen") { Text("Forum Creation Screen Content") }
}
@Composable
fun GlobalIssuesScreen(navController: NavHostController) { ScreenWithHeader(navController, "Global Issues Screen") { Text("Global Issues Screen Content") } }
@Composable
fun NationalIssuesScreen(navController: NavHostController) { ScreenWithHeader(navController, "National Issues Screen") { Text("National Issues Screen Content") } }
@Composable
fun LocalIssuesScreen(navController: NavHostController) { ScreenWithHeader(navController, "Local Issues Screen") { Text("Local Issues Screen Content") } }
@Composable
fun FutureScreen(navController: NavHostController) { ScreenWithHeader(navController, "Future Screen") { Text("Future Screen Content") } }
@Composable
fun MarketPotentialScreen(navController: NavHostController) { ScreenWithHeader(navController, "Market Potential Screen") { Text("Market Potential Screen Content") } }
@Composable
fun RequestedSolutionsScreen(navController: NavHostController) { ScreenWithHeader(navController, "Requested Solutions Screen") { Text("Requested Solutions Screen Content") } }
@Composable
fun IdeaCreationScreen(navController: NavHostController) { ScreenWithHeader(navController, "Idea Creation Screen") { Text("Idea Creation Screen Content") } }
@Composable
fun OrganizationFilesScreen(navController: NavHostController, orgId: String) {
    ScreenWithHeader(navController, "Organization Files Screen") { Text("Organization Files Screen Content") }
}
@Composable
fun FixInvitationScreen() { Text("Fix Invitation Screen Content") }
@Composable
fun GroupChatScreen(navController: NavHostController, conversationId: String) {
    ScreenWithHeader(navController, "Group Chat Screen") { Text("Group Chat Screen Content") }
}
@Composable
fun OrganizationDetailsScreen(navController: NavHostController, orgId: String, invitationId: String) {
    ScreenWithHeader(navController, "Organization Details Screen") { Text("Organization Details Screen Content") }
}
@Composable
fun SchoolScreen(navController: NavHostController, schoolName: String) {
    ScreenWithHeader(navController, "School Screen") { Text("School Screen Content") }
}
@Composable
fun IdeaGenerationScreen(navController: NavHostController, showAddButton: Boolean, category: String?) {
    ScreenWithHeader(navController, "Idea Generation Screen") { Text("Idea Generation Screen Content") }
}
@Composable
fun ForumTemplateScreen(navController: NavHostController, institutionName: String) {
    ScreenWithHeader(navController, "Forum Template Screen") { Text("Forum Template Screen Content") }
}
@Composable
fun UserProfileScreen(navController: NavHostController, userId: String) {
    ScreenWithHeader(navController, "User Profile Screen") { Text("User Profile Screen Content") }
}