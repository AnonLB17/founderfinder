package com.phoenixcorp.founderfinder.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.phoenixcorp.founderfinder.ui.screens.SplashScreen
import com.phoenixcorp.founderfinder.ui.screens.SignUpScreen
import com.phoenixcorp.founderfinder.ui.screens.SignInScreen
import com.phoenixcorp.founderfinder.ui.screens.SelectUserTypeScreen
import com.phoenixcorp.founderfinder.ui.screens.UserInfoScreen
import com.phoenixcorp.founderfinder.ui.screens.EducationScreen
import com.phoenixcorp.founderfinder.ui.screens.WorkExperienceScreen
import com.phoenixcorp.founderfinder.ui.screens.FounderStatusScreen
import com.phoenixcorp.founderfinder.ui.screens.AmbitionStatementScreen
import com.phoenixcorp.founderfinder.ui.screens.ConnectSocialsScreen
import com.phoenixcorp.founderfinder.ui.screens.IndustriesOfInterestScreen
import com.phoenixcorp.founderfinder.ui.screens.OrganizationsOfInterestScreen
import com.phoenixcorp.founderfinder.ui.screens.PublicAppearanceScreen
import com.phoenixcorp.founderfinder.ui.screens.HomeScreen
import com.phoenixcorp.founderfinder.ui.screens.ProfileScreen
import com.phoenixcorp.founderfinder.ui.screens.PartnersScreen
import com.phoenixcorp.founderfinder.ui.screens.FindPartnersScreen
import com.phoenixcorp.founderfinder.ui.screens.IdeaDevelopmentScreen
import com.phoenixcorp.founderfinder.ui.screens.InvestorInfoScreen
import com.phoenixcorp.founderfinder.ui.screens.IndustryPreferencesScreen
import com.phoenixcorp.founderfinder.ui.screens.InvestmentPhilosophyScreen
import com.phoenixcorp.founderfinder.ui.screens.PortfolioCompaniesScreen
import com.phoenixcorp.founderfinder.ui.screens.TermsAndExpectationsScreen
import com.phoenixcorp.founderfinder.ui.screens.AdvisorSearchFeatureScreen
import com.phoenixcorp.founderfinder.ui.screens.PartnerSearchFeatureScreen
import com.phoenixcorp.founderfinder.ui.screens.PrivateMessagesScreen
import com.phoenixcorp.founderfinder.ui.screens.UserProfileScreen
import com.phoenixcorp.founderfinder.ui.screens.SchoolForumsScreen
import com.phoenixcorp.founderfinder.ui.screens.AdvisorSignUpScreen
import com.phoenixcorp.founderfinder.ui.screens.PartnerSignUpScreen
import com.phoenixcorp.founderfinder.ui.screens.IdeaGenerationScreen
import com.phoenixcorp.founderfinder.ui.screens.CriteriaForConceptScreen
import com.phoenixcorp.founderfinder.ui.screens.IncubatorConnectionScreen
import com.phoenixcorp.founderfinder.ui.screens.InvestorSearchScreen
import com.phoenixcorp.founderfinder.ui.screens.ForumCreationScreen
import com.phoenixcorp.founderfinder.ui.screens.GlobalIssuesScreen
import com.phoenixcorp.founderfinder.ui.screens.NationalIssuesScreen
import com.phoenixcorp.founderfinder.ui.screens.LocalIssuesScreen
import com.phoenixcorp.founderfinder.ui.screens.FutureScreen
import com.phoenixcorp.founderfinder.ui.screens.MarketPotentialScreen
import com.phoenixcorp.founderfinder.ui.screens.RequestedSolutionsScreen
import com.phoenixcorp.founderfinder.ui.screens.IdeaCreationScreen
import com.phoenixcorp.founderfinder.ui.screens.BusinessPlanScreen
import com.phoenixcorp.founderfinder.ui.screens.PartnershipAgreementScreen
import com.phoenixcorp.founderfinder.ui.screens.ProposalForFinancingScreen
import com.phoenixcorp.founderfinder.ui.screens.PitchReviewSubmitScreen
import com.phoenixcorp.founderfinder.ui.screens.AddIncubatorScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.material3.Text
import androidx.navigation.NavType
import androidx.navigation.navArgument

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Splash.route) {

        // Authentication Flow
        composable(Screen.Splash.route) { SplashScreen(navController) }
        composable(Screen.SignUp.route) { SignUpScreen(navController) }
        composable(Screen.SignIn.route) { SignInScreen(navController) }
        composable(Screen.SelectUserType.route) { SelectUserTypeScreen(navController) }

        // Regular User Flow
        composable(Screen.UserInfo.route) { UserInfoScreen(navController) }
        composable(Screen.Education.route) { EducationScreen(navController) }
        composable(Screen.WorkExperience.route) { WorkExperienceScreen(navController) }
        composable(Screen.FounderStatus.route) { FounderStatusScreen(navController) }
        composable(Screen.AmbitionStatement.route) { AmbitionStatementScreen(navController) }
        composable(Screen.ConnectSocials.route) { ConnectSocialsScreen(navController) }
        composable(Screen.IndustriesOfInterest.route) { IndustriesOfInterestScreen(navController) }
        composable(Screen.OrganizationsOfInterest.route) { OrganizationsOfInterestScreen(navController) }
        composable(Screen.PublicAppearance.route) { PublicAppearanceScreen(navController) }

        // Investor User Flow
        composable(Screen.InvestorInfo.route) { InvestorInfoScreen(navController) }
        composable(Screen.IndustryPreferences.route) { IndustryPreferencesScreen(navController) }
        composable(Screen.InvestmentPhilosophy.route) { InvestmentPhilosophyScreen(navController) }
        composable(Screen.PortfolioCompanies.route) { PortfolioCompaniesScreen(navController) }
        composable(Screen.TermsAndExpectations.route) { TermsAndExpectationsScreen(navController) }

        // Main App Flow with Bottom Navigation
        composable(Screen.Home.route) { HomeScreen(navController) }
        composable(Screen.Profile.route) { ProfileScreen(navController) }
        composable(Screen.Partners.route) { PartnersScreen(navController) }
        composable(Screen.FindPartners.route) { FindPartnersScreen(navController) }
        composable(Screen.IdeaDevelopment.route) { IdeaDevelopmentScreen(navController) }

        // Find Partners Flow
        composable(Screen.AdvisorSearchFeature.route) { AdvisorSearchFeatureScreen(navController) }
        composable(Screen.PartnerSearchFeature.route) { PartnerSearchFeatureScreen(navController) }
        composable(Screen.SchoolForums.route) { SchoolForumsScreen(navController) }

        // Advisor Flow
        composable(Screen.AdvisorSignUp.route) { AdvisorSignUpScreen(navController) }

        // Partner Flow
        composable(Screen.PartnerSignUp.route) { PartnerSignUpScreen(navController) }

        // Private Messages
        composable(
            route = Screen.PrivateMessages.route,
            arguments = listOf(navArgument("recipientId") { type = NavType.StringType })
        ) { backStackEntry ->
            PrivateMessagesScreen(
                navController = navController,
                recipientId = backStackEntry.arguments?.getString("recipientId") ?: ""
            )
        }

        // User Profile
        composable(
            route = Screen.UserProfile.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            UserProfileScreen(
                navController = navController,
                userId = backStackEntry.arguments?.getString("userId") ?: ""
            )
        }

        // Idea Development Flow
        composable(Screen.IdeaGeneration.route) { IdeaGenerationScreen(navController) }
        composable(Screen.CriteriaForConcept.route) { CriteriaForConceptScreen(navController) }
        composable(Screen.IncubatorConnection.route) { IncubatorConnectionScreen(navController) }
        composable(Screen.InvestorSearch.route) { InvestorSearchScreen(navController) }

        // Idea Generation Flow
        composable(Screen.ForumCreation.route) { ForumCreationScreen(navController) }
        composable(Screen.GlobalIssues.route) { GlobalIssuesScreen(navController) }
        composable(Screen.NationalIssues.route) { NationalIssuesScreen(navController) }
        composable(Screen.LocalIssues.route) { LocalIssuesScreen(navController) }
        composable(Screen.Future.route) { FutureScreen(navController) }
        composable(Screen.MarketPotential.route) { MarketPotentialScreen(navController) }
        composable(Screen.RequestedSolutions.route) { RequestedSolutionsScreen(navController) }

        // Criteria for Concept Flow
        composable(Screen.IdeaCreation.route) { IdeaCreationScreen(navController) }
        composable(Screen.BusinessPlan.route) { BusinessPlanScreen(navController) }
        composable(Screen.PartnershipAgreement.route) { PartnershipAgreementScreen(navController) }
        composable(Screen.ProposalForFinancing.route) { ProposalForFinancingScreen(navController) }
        composable(Screen.PitchReviewSubmit.route) { PitchReviewSubmitScreen(navController) }

        // Incubator Connection Flow
        composable(Screen.AddIncubator.route) { AddIncubatorScreen(navController) }
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
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Ensures content doesn't overlap with AppBar
        ) {
            content()
        }
    }
}

// Placeholder screens
@Composable
fun SplashScreen(navController: NavHostController) { PlaceholderScreen("Splash Screen") }
@Composable
fun SignUpScreen(navController: NavHostController) { PlaceholderScreen("Sign Up Screen") }
@Composable
fun SignInScreen(navController: NavHostController) { PlaceholderScreen("Sign In Screen") }
@Composable
fun SelectUserTypeScreen(navController: NavHostController) { PlaceholderScreen("Select User Type Screen") }
@Composable
fun UserInfoScreen(navController: NavHostController) { PlaceholderScreen("User Info Screen") }
@Composable
fun EducationScreen(navController: NavHostController) { PlaceholderScreen("Education Screen") }
@Composable
fun WorkExperienceScreen(navController: NavHostController) { PlaceholderScreen("Work Experience Screen") }
@Composable
fun FounderStatusScreen(navController: NavHostController) { PlaceholderScreen("Founder Status Screen") }
@Composable
fun AmbitionStatementScreen(navController: NavHostController) { PlaceholderScreen("Ambition Statement Screen") }
@Composable
fun ConnectSocialsScreen(navController: NavHostController) { PlaceholderScreen("Connect Socials Screen") }
@Composable
fun IndustriesOfInterestScreen(navController: NavHostController) { PlaceholderScreen("Industries of Interest Screen") }
@Composable
fun OrganizationsOfInterestScreen(navController: NavHostController) { PlaceholderScreen("Organizations of Interest Screen") }
@Composable
fun PublicAppearanceScreen(navController: NavHostController) { PlaceholderScreen("Public Appearance Screen") }
@Composable
fun HomeScreen(navController: NavHostController) { PlaceholderScreen("Home Screen") }
@Composable
fun ProfileScreen(navController: NavHostController) { PlaceholderScreen("Profile Screen") }
@Composable
fun PartnersScreen(navController: NavHostController) { PlaceholderScreen("Partners Screen") }
@Composable
fun FindPartnersScreen(navController: NavHostController) { PlaceholderScreen("Find Partners Screen") }
@Composable
fun IdeaDevelopmentScreen(navController: NavHostController) { PlaceholderScreen("Idea Development Screen") }
@Composable
fun InvestorInfoScreen(navController: NavHostController) { PlaceholderScreen("Investor Info Screen") }
@Composable
fun IndustryPreferencesScreen(navController: NavHostController) { PlaceholderScreen("Industry Preferences Screen") }
@Composable
fun InvestmentPhilosophyScreen(navController: NavHostController) { PlaceholderScreen("Investment Philosophy Screen") }
@Composable
fun PortfolioCompaniesScreen(navController: NavHostController) { PlaceholderScreen("Portfolio Companies Screen") }
@Composable
fun TermsAndExpectationsScreen(navController: NavHostController) { PlaceholderScreen("Terms and Expectations Screen") }
@Composable
fun AdvisorSearchFeatureScreen(navController: NavHostController) { PlaceholderScreen("Advisor Search Feature Screen") }
@Composable
fun PartnerSearchFeatureScreen(navController: NavHostController) { PlaceholderScreen("Partner Search Feature Screen") }
@Composable
fun PrivateMessagesScreen(navController: NavHostController) { PlaceholderScreen("Private Messages Screen") }
@Composable
fun UserProfileScreen(navController: NavHostController) { PlaceholderScreen("User Profile Screen") }
@Composable
fun SchoolForumsScreen(navController: NavHostController) { PlaceholderScreen("School Forums Screen") }
@Composable
fun AdvisorSignUpScreen(navController: NavHostController) { PlaceholderScreen("Advisor Sign Up Screen") }
@Composable
fun PartnerSignUpScreen(navController: NavHostController) { PlaceholderScreen("Partner Sign Up Screen") }
@Composable
fun IdeaGenerationScreen(navController: NavHostController) { PlaceholderScreen("Idea Generation Screen") }
@Composable
fun CriteriaForConceptScreen(navController: NavHostController) { PlaceholderScreen("Criteria for Concept Screen") }
@Composable
fun IncubatorConnectionScreen(navController: NavHostController) { PlaceholderScreen("Incubator Connection Screen") }
@Composable
fun InvestorSearchScreen(navController: NavHostController) { PlaceholderScreen("Investor Search Screen") }
@Composable
fun ForumCreationScreen(navController: NavHostController) { PlaceholderScreen("Forum Creation Screen") }
@Composable
fun GlobalIssuesScreen(navController: NavHostController) { PlaceholderScreen("Global Issues Screen") }
@Composable
fun NationalIssuesScreen(navController: NavHostController) { PlaceholderScreen("National Issues Screen") }
@Composable
fun LocalIssuesScreen(navController: NavHostController) { PlaceholderScreen("Local Issues Screen") }
@Composable
fun FutureScreen(navController: NavHostController) { PlaceholderScreen("Future Screen") }
@Composable
fun MarketPotentialScreen(navController: NavHostController) { PlaceholderScreen("Market Potential Screen") }
@Composable
fun RequestedSolutionsScreen(navController: NavHostController) { PlaceholderScreen("Requested Solutions Screen") }
@Composable
fun IdeaCreationScreen(navController: NavHostController) { PlaceholderScreen("Idea Creation Screen") }
@Composable
fun BusinessPlanScreen(navController: NavHostController) { PlaceholderScreen("Business Plan Screen") }
@Composable
fun PartnershipAgreementScreen(navController: NavHostController) { PlaceholderScreen("Partnership Agreement Screen") }
@Composable
fun ProposalForFinancingScreen(navController: NavHostController) { PlaceholderScreen("Proposal for Financing Screen") }
@Composable
fun PitchReviewSubmitScreen(navController: NavHostController) { PlaceholderScreen("Pitch Review and Submit Screen") }
@Composable
fun AddIncubatorScreen(navController: NavHostController) { PlaceholderScreen("Add Incubator Screen") }

@Composable
fun PlaceholderScreen(name: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = name)
    }
}
