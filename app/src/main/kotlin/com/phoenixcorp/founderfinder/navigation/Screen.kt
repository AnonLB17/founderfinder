package com.phoenixcorp.founderfinder.navigation

import androidx.navigation.NavType
import androidx.navigation.navArgument

/**
 * Navigation routes for FounderFinder.
 *
 * Onboarding architecture (4 separate role flows, one shared ViewModel):
 *
 *   OnboardingGraph  ("onboarding")          ← parent; scopes OnboardingViewModel
 *   ├── SelectUserType                       ← hub / role picker
 *   ├── FounderFlow  ("founder_flow")        ← nested
 *   │     UserInfo → Education → WorkExperience → FounderStatus
 *   │     → Ambition → ConnectSocials → Industries → Organizations → PublicAppearance
 *   ├── InvestorFlow ("investor_flow")       ← nested
 *   │     InvestorInfo → PortfolioAndTerms
 *   ├── AdvisorFlow  ("advisor_flow")        ← nested
 *   │     AdvisorSignUp
 *   └── PartnerFlow  ("partner_flow")        ← nested
 *         PartnerSignUp
 *
 * Back works inside each flow. Leaving a flow returns to SelectUserType
 * (or clears the stack when finishing → Home).
 */
sealed class Screen(
    val route: String,
    val arguments: List<androidx.navigation.NamedNavArgument> = emptyList()
) {
    // ── Auth ──────────────────────────────────────────────────────
    object Splash : Screen("splash")
    object SignUp : Screen("signup")
    object SignIn : Screen("signin")

    // ── Onboarding parent (shared OnboardingViewModel scope) ──────
    object OnboardingGraph : Screen("onboarding")

    /** Role picker – start destination of OnboardingGraph */
    object SelectUserType : Screen("select_user_type")

    // ── Nested role-flow graph routes ─────────────────────────────
    object FounderFlow : Screen("founder_flow")
    object InvestorFlow : Screen("investor_flow")
    object AdvisorFlow : Screen("advisor_flow")
    object PartnerFlow : Screen("partner_flow")

    // ── Founder flow steps ────────────────────────────────────────
    object UserInfo : Screen("user_info")
    object Education : Screen("education")
    object WorkExperience : Screen("work_experience")
    object FounderStatus : Screen("founder_status")
    object AmbitionStatement : Screen("ambition_statement")
    object ConnectSocials : Screen("connect_socials")
    object IndustriesOfInterest : Screen("industries_of_interest")
    object OrganizationsOfInterest : Screen("organizations_of_interest")
    object PublicAppearance : Screen("public_appearance")

    // ── Investor flow steps ───────────────────────────────────────
    object InvestorInfo : Screen("investor_info")
    object IndustryPreferences : Screen("industry_preferences")
    object PortfolioAndTerms : Screen("portfolio_and_terms")

    // ── Advisor / Partner flow steps ──────────────────────────────
    object AdvisorSignUp : Screen("advisor_sign_up")
    object PartnerSignUp : Screen("partner_sign_up")

    // ── Main app ──────────────────────────────────────────────────
    object Home : Screen("home")
    object Notifications : Screen("notifications")
    object Partners : Screen("partners")
    object FindPartners : Screen("find_partners")
    object IdeaDevelopment : Screen("idea_development")
    object AdvisorSearchFeature : Screen("advisor_search_feature")
    object PartnerSearchFeature : Screen("partner_search_feature")
    object SchoolForums : Screen("school_forums")
    object PrivateMessages : Screen("private_messages")
    object LocationSelection : Screen("location_selection")
    object CriteriaForConcept : Screen("criteria_for_concept")
    object IncubatorConnection : Screen("incubator_connection")
    object InvestorSearch : Screen("investor_search")
    object GlobalIssues : Screen("global_issues")
    object NationalIssues : Screen("national_issues")
    object LocalIssues : Screen("local_issues")
    object Future : Screen("future")
    object MarketPotential : Screen("market_potential")
    object RequestedSolutions : Screen("requested_solutions")
    object IdeaCreation : Screen("idea_creation")
    object FixInvitation : Screen("fix_invitation")

    // ── Parameterized ─────────────────────────────────────────────
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
        fun createRoute(category: String, forumId: String) =
            "institution_forum/$category/$forumId"
    }

    object School : Screen(
        route = "school/{schoolName}",
        arguments = listOf(navArgument("schoolName") { type = NavType.StringType })
    ) {
        fun createRoute(schoolName: String) = "school/$schoolName"
    }

    object IdeaGeneration : Screen(
        route = "idea_generation?category={category}",
        arguments = listOf(navArgument("category") { nullable = true })
    ) {
        fun createRoute(category: String? = null) =
            "idea_generation?category=${category ?: "null"}"
    }

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
        fun createRoute(orgId: String, invitationId: String) =
            "organization_details/$orgId/$invitationId"
    }
}

/** Progress totals for the founder linear path */
object OnboardingSteps {
    const val TOTAL_FOUNDER = 9
}