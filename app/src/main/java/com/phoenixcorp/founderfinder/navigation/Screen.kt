package com.phoenixcorp.founderfinder.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object SignUp : Screen("signup")
    object SignIn : Screen("signin")
    object SelectUserType : Screen("select_user_type")

    // Regular User Flow
    object UserInfo : Screen("user_info")
    object Education : Screen("education")
    object WorkExperience : Screen("work_experience")
    object FounderStatus : Screen("founder_status")
    object AmbitionStatement : Screen("ambition_statement")
    object ConnectSocials : Screen("connect_socials")
    object IndustriesOfInterest : Screen("industries_of_interest")
    object OrganizationsOfInterest : Screen("organizations_of_interest")
    object PublicAppearance : Screen("public_appearance")

    // Investor User Flow
    object InvestorInfo : Screen("investor_info")
    object IndustryPreferences : Screen("industry_preferences")
    object InvestmentPhilosophy : Screen("investment_philosophy")
    object PortfolioCompanies : Screen("portfolio_companies")
    object TermsAndExpectations : Screen("terms_and_expectations")

    // Main Screen flows
    object Home : Screen("home")
    object Profile : Screen("profile")
    object Partners : Screen("partners")
    object FindPartners : Screen("find_partners")
    object IdeaDevelopment : Screen("idea_development")

    // Find Partners Flow
    object AdvisorSearchFeature : Screen("advisor_search_feature")
    object PartnerSearchFeature : Screen("partner_search_feature")
    object SchoolForums : Screen("school_forums")

    // Advisor Search Flow
    object AdvisorSignUp : Screen("advisor_sign_up")

    // Partner Search Flow
    object PartnerSignUp : Screen("partner_sign_up")

    // Private Messages
    object PrivateMessages : Screen("private_messages/{recipientId}") {
        fun createRoute(recipientId: String) = "private_messages/$recipientId"
    }
    object UserProfile : Screen("user_profile/{userId}") {
        fun createRoute(userId: String) = "user_profile/$userId"
    }

    // Idea Development Flow
    object IdeaGeneration : Screen("idea_generation")
    object CriteriaForConcept : Screen("criteria_for_concept")
    object IncubatorConnection : Screen("incubator_connection")
    object InvestorSearch : Screen("investor_search")

    // Idea Generation Flow
    object ForumCreation : Screen("forum_creation")
    object GlobalIssues : Screen("global_issues")
    object NationalIssues : Screen("national_issues")
    object LocalIssues : Screen("local_issues")
    object Future : Screen("future")
    object MarketPotential : Screen("market_potential")
    object RequestedSolutions : Screen("requested_solutions")

    // Criteria for Concept Flow
    object IdeaCreation : Screen("idea_creation")
    object BusinessPlan : Screen("business_plan")
    object PartnershipAgreement : Screen("partnership_agreement")
    object ProposalForFinancing : Screen("proposal_for_financing")
    object PitchReviewSubmit : Screen("pitch_review_submit")

    // Incubator Connection Flow
    object AddIncubator : Screen("add_incubator")
}