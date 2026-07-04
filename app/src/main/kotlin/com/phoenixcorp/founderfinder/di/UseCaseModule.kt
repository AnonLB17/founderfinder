package com.phoenixcorp.founderfinder.di

import com.phoenixcorp.founderfinder.domain.repository.AdvisorRepository
import com.phoenixcorp.founderfinder.domain.repository.ChatRepository
import com.phoenixcorp.founderfinder.domain.repository.ForumRepository
import com.phoenixcorp.founderfinder.domain.repository.InvestorRepository
import com.phoenixcorp.founderfinder.domain.repository.MatchRepository
import com.phoenixcorp.founderfinder.domain.repository.NotificationRepository
import com.phoenixcorp.founderfinder.domain.repository.PartnerRepository
import com.phoenixcorp.founderfinder.domain.repository.UserRepository
import com.phoenixcorp.founderfinder.domain.usecase.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import jakarta.inject.Singleton

@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {

    @Provides
    @ViewModelScoped
    fun provideSearchAdvisorsUseCase(
        advisorRepository: AdvisorRepository
    ): SearchAdvisorsUseCase = SearchAdvisorsUseCase(advisorRepository)

    @Provides
    @ViewModelScoped
    fun provideSearchInvestorsUseCase(
        investorRepository: InvestorRepository
    ): SearchInvestorsUseCase = SearchInvestorsUseCase(investorRepository)

    @Provides
    @ViewModelScoped
    fun provideSearchPartnersUseCase(
        partnerRepository: PartnerRepository
    ): SearchPartnersUseCase = SearchPartnersUseCase(partnerRepository)

    @Provides
    @ViewModelScoped
    fun provideGetCurrentUserUseCase(
        userRepository: UserRepository
    ): GetCurrentUserUseCase = GetCurrentUserUseCase(userRepository)

    @Provides
    @ViewModelScoped
    fun provideUpdateUserProfileUseCase(
        userRepository: UserRepository
    ): UpdateUserProfileUseCase = UpdateUserProfileUseCase(userRepository)

    // === NEW: Provide SendPrivateChatNotificationUseCase ===
    @Provides
    @ViewModelScoped
    fun provideSendPrivateChatNotificationUseCase(
        notificationRepository: NotificationRepository
    ): SendPrivateChatNotificationUseCase = SendPrivateChatNotificationUseCase(notificationRepository)

    @Provides
    @ViewModelScoped
    fun provideSendChatMessageUseCase(
        chatRepository: ChatRepository,
        sendPrivateChatNotificationUseCase: SendPrivateChatNotificationUseCase
    ): SendChatMessageUseCase {
        return SendChatMessageUseCase(chatRepository, sendPrivateChatNotificationUseCase)
    }

    @Provides
    @ViewModelScoped
    fun provideGetChatMessagesUseCase(
        chatRepository: ChatRepository
    ): GetChatMessagesUseCase = GetChatMessagesUseCase(chatRepository)

    @Provides
    @ViewModelScoped
    fun provideCreateForumPostUseCase(
        forumRepository: ForumRepository
    ): CreateForumPostUseCase = CreateForumPostUseCase(forumRepository)

    @Provides
    @ViewModelScoped
    fun provideGetForumPostsUseCase(
        forumRepository: ForumRepository
    ): GetForumPostsUseCase = GetForumPostsUseCase(forumRepository)

    @Provides
    @ViewModelScoped
    fun provideGetRecommendedMatchesUseCase(
        matchRepository: MatchRepository
    ): GetRecommendedMatchesUseCase = GetRecommendedMatchesUseCase(matchRepository)
}