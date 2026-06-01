package com.phoenixcorp.founderfinder.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.phoenixcorp.founderfinder.domain.repository.UserRepository

class AuthViewModel : ViewModel() {
    private val repository = UserRepository()

    fun registerUser(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        repository.registerUser(email, password, onResult)
    }

    fun signInUser(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        repository.signInUser(email, password, onResult)
    }

    fun saveUserInfo(userId: String, firstName: String, lastName: String, birthDate: String, onResult: (Boolean) -> Unit) {
        repository.saveUserInfo(userId, firstName, lastName, birthDate, onResult)
    }

    fun saveEducation(userId: String, education: List<String>, onResult: (Boolean) -> Unit) {
        repository.saveEducation(userId, education, onResult)
    }

    fun saveWorkExperience(userId: String, workExperience: List<String>, onResult: (Boolean) -> Unit) {
        repository.saveWorkExperience(userId, workExperience, onResult)
    }

    fun saveFounderStatus(userId: String, founderStatus: List<String>, onResult: (Boolean) -> Unit) {
        repository.saveFounderStatus(userId, founderStatus, onResult)
    }

    fun saveAmbitionStatement(userId: String, ambitionStatement: String, onResult: (Boolean) -> Unit) {
        repository.saveAmbitionStatement(userId, ambitionStatement, onResult)
    }

    fun saveSocials(userId: String, linkedin: String, twitter: String, facebook: String, instagram: String, website: String, onResult: (Boolean) -> Unit) {
        repository.saveSocials(userId, linkedin, twitter, facebook, instagram, website, onResult)
    }

    fun saveIndustriesOfInterest(userId: String, industries: List<String>, onResult: (Boolean) -> Unit) {
        repository.saveIndustriesOfInterest(userId, industries, onResult)
    }

    fun saveOrganizationsOfInterest(userId: String, organizations: List<String>, onResult: (Boolean) -> Unit) {
        repository.saveOrganizationsOfInterest(userId, organizations, onResult)
    }

    fun saveProfileImageUri(userId: String, profileImageUri: String, onResult: (Boolean) -> Unit) {
        repository.saveProfileImageUri(userId, profileImageUri, onResult)
    }
}
