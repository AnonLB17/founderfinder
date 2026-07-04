package com.phoenixcorp.founderfinder.di

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import com.phoenixcorp.founderfinder.data.repository.NotificationRepositoryImpl
import com.phoenixcorp.founderfinder.data.repository.ProfileRepositoryImpl
import com.phoenixcorp.founderfinder.data.repository.ThreadRepositoryImpl
import com.phoenixcorp.founderfinder.domain.repository.ForumRepository
import com.phoenixcorp.founderfinder.domain.repository.NotificationRepository
import com.phoenixcorp.founderfinder.domain.repository.ProfileRepository
import com.phoenixcorp.founderfinder.domain.repository.ThreadRepository
import com.phoenixcorp.founderfinder.domain.usecase.*
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import dagger.hilt.InstallIn

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = Firebase.firestore

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = Firebase.storage

    // ====================== REPOSITORIES ======================
    @Provides
    @Singleton
    fun provideProfileRepository(
        firestore: FirebaseFirestore
    ): ProfileRepository = ProfileRepositoryImpl(firestore)

    @Provides
    @Singleton
    fun provideNotificationRepository(
        firestore: FirebaseFirestore
    ): NotificationRepository = NotificationRepositoryImpl(firestore)

    // ====================== USE CASES ======================
    @Provides
    @Singleton
    fun provideGetUnreadNotificationsUseCase(
        repository: NotificationRepository
    ): GetUnreadNotificationsUseCase = GetUnreadNotificationsUseCase(repository)

    @Provides
    @Singleton
    fun provideGetAllNotificationsUseCase(
        repository: NotificationRepository
    ): GetAllNotificationsUseCase = GetAllNotificationsUseCase(repository)

    @Provides
    @Singleton
    fun provideCreateThreadNotificationUseCase(
        repository: NotificationRepository
    ): CreateThreadNotificationUseCase = CreateThreadNotificationUseCase(repository)

    @Provides
    @Singleton
    fun provideCreateCommentNotificationUseCase(
        repository: NotificationRepository
    ): CreateCommentNotificationUseCase = CreateCommentNotificationUseCase(repository)

    @Provides
    @Singleton
    fun provideSendFileShareNotificationUseCase(
        repository: NotificationRepository
    ): SendFileShareNotificationUseCase = SendFileShareNotificationUseCase(repository)

    @Provides
    @Singleton
    fun provideCreateThreadUseCase(
        threadRepository: ThreadRepository
        // Notification use case temporarily removed to stop duplicates
    ): CreateThreadUseCase = CreateThreadUseCase(threadRepository)

    @Provides
    @Singleton
    fun provideCreateCommentUseCase(
        forumRepository: ForumRepository,
        createCommentNotificationUseCase: CreateCommentNotificationUseCase
    ): CreateCommentUseCase = CreateCommentUseCase(forumRepository, createCommentNotificationUseCase)
}