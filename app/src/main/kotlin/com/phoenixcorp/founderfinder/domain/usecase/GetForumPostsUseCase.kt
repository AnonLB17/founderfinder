package com.phoenixcorp.founderfinder.domain.usecase

import com.phoenixcorp.founderfinder.domain.model.ForumPost
import com.phoenixcorp.founderfinder.domain.repository.ForumRepository
import javax.inject.Inject

class GetForumPostsUseCase @Inject constructor(
    private val forumRepository: ForumRepository
) {
    suspend operator fun invoke(category: String? = null, school: String? = null): List<ForumPost> {
        return if (category != null) {
            forumRepository.getPostsByCategory(category)
        } else if (school != null) {
            forumRepository.getPostsBySchool(school)
        } else {
            emptyList()
        }
    }
}