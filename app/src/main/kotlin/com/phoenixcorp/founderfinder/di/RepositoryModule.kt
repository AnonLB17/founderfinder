package com.phoenixcorp.founderfinder.di

import com.phoenixcorp.founderfinder.data.repository.*
import com.phoenixcorp.founderfinder.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        impl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindAdvisorRepository(
        impl: AdvisorRepositoryImpl
    ): AdvisorRepository

    @Binds
    @Singleton
    abstract fun bindInvestorRepository(
        impl: InvestorRepositoryImpl
    ): InvestorRepository

    @Binds
    @Singleton
    abstract fun bindPartnerRepository(
        impl: PartnerRepositoryImpl
    ): PartnerRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        impl: ChatRepositoryImpl
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindForumRepository(
        impl: ForumRepositoryImpl
    ): ForumRepository

    @Binds
    @Singleton
    abstract fun bindMatchRepository(
        impl: MatchRepositoryImpl
    ): MatchRepository
}