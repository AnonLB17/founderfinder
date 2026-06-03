package com.phoenixcorp.founderfinder.di

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import com.phoenixcorp.founderfinder.data.repository.ProfileRepositoryImpl
import com.phoenixcorp.founderfinder.domain.repository.ProfileRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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

    // You can add more repositories here later (e.g. OrganizationRepository)
}