package com.phoenixcorp.founderfinder.navigation

import androidx.navigation.NavType
import androidx.navigation.navArgument

sealed class Screen(val route: String, val arguments: List<androidx.navigation.NamedNavArgument> = emptyList()) {
    object Splash : Screen("splash")
    object SignUp : Screen("signup")
    object SignIn : Screen("signin")
    object SelectUserType : Screen("select_user_type")
    object UserInfo : Screen("user_info")
    object Education : Screen("education")
    object WorkExperience : Screen("work_experience")
    object FounderStatus : Screen("founder_status")
    object AmbitionStatement : Screen("ambition_statement")
    object ConnectSocials : Screen("connect_socials")
    object IndustriesOfInterest : Screen("industries_of_interest")
    object OrganizationsOfInterest : Screen("organizations_of_interest")
    object PublicAppearance : Screen("public_appearance")
    object InvestorInfo : Screen("investor_info")
    object IndustryPreferences : Screen("industry_preferences")
    object InvestmentPhilosophy : Screen("investment_philosophy")
    object PortfolioCompanies : Screen("portfolio_companies")
    object TermsAndExpectations : Screen("terms_and_expectations")
    object Home : Screen("home")
    object Partners : Screen("partners")
    object FindPartners : Screen("find_partners")
    object IdeaDevelopment : Screen("idea_development")
    object AdvisorSearchFeature : Screen("advisor_search_feature")
    object PartnerSearchFeature : Screen("partner_search_feature")
    object SchoolForums : Screen("school_forums")
    object AdvisorSignUp : Screen("advisor_sign_up")
    object PartnerSignUp : Screen("partner_sign_up")
    object PrivateMessages : Screen("private_messages")
    object PrivateChat : Screen(
        route = "private_chat/{conversationId}",
        arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
    ) {
        fun createRoute(conversationId: String) = "private_chat/$conversationId"
    }
    object GroupChat : Screen(
        route = "group_chat/{orgId}",
        arguments = listOf(navArgument("orgId") { type = NavType.StringType })
    ) {
        fun createRoute(orgId: String) = "group_chat/$orgId"
    }
    object UserProfile : Screen(
        route = "user_profile/{userId}",
        arguments = listOf(navArgument("userId") { type = NavType.StringType })
    ) {
        fun createRoute(userId: String) = "user_profile/$userId"
    }
    object InstitutionForum : Screen(
        route = "institution_forum/{category}/{forumId}",
        arguments = listOf(
            navArgument("category") { type = NavType.StringType },
            navArgument("forumId") { type = NavType.StringType }
        )
    ) {
        fun createRoute(category: String, forumId: String) = "institution_forum/$category/$forumId"
    }
    object School : Screen(
        route = "school/{schoolName}",
        arguments = listOf(navArgument("schoolName") { type = NavType.StringType })
    ) {
        fun createRoute(schoolName: String) = "school/$schoolName"
    }
    object LocationSelection : Screen("location_selection")
    object IdeaGeneration : Screen(
        route = "idea_generation?category={category}",
        arguments = listOf(navArgument("category") { nullable = true })
    ) {
        fun createRoute(category: String? = null) = "idea_generation?category=${category ?: "null"}"
    }
    object CriteriaForConcept : Screen("criteria_for_concept")
    object IncubatorConnection : Screen("incubator_connection")
    object InvestorSearch : Screen("investor_search")
    object ForumCreation : Screen(
        route = "forum_creation?category={category}&location={location}",
        arguments = listOf(
            navArgument("category") { nullable = true },
            navArgument("location") { nullable = true }
        )
    ) {
        fun createRoute(category: String? = null, location: String? = null) =
            "forum_creation?category=${category ?: "null"}&location=${location ?: "null"}"
    }
    object GlobalIssues : Screen("global_issues")
    object NationalIssues : Screen("national_issues")
    object LocalIssues : Screen("local_issues")
    object Future : Screen("future")
    object MarketPotential : Screen("market_potential")
    object RequestedSolutions : Screen("requested_solutions")
    object IdeaCreation : Screen("idea_creation")
    object OrganizationFiles : Screen(
        route = "organization_files/{orgId}",
        arguments = listOf(navArgument("orgId") { type = NavType.StringType })
    ) {
        fun createRoute(orgId: String) = "organization_files/$orgId"
    }
    object OrganizationDetails : Screen(
        route = "organization_details/{orgId}/{invitationId}",
        arguments = listOf(
            navArgument("orgId") { type = NavType.StringType },
            navArgument("invitationId") { type = NavType.StringType }
        )
    ) {
        fun createRoute(orgId: String, invitationId: String) = "organization_details/$orgId/$invitationId"
    }
    object FixInvitation : Screen("fix_invitation")
}