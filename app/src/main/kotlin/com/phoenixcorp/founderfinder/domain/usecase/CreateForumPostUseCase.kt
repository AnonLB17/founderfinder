package com.phoenixcorp.founderfinder.domain.usecase

import com.phoenixcorp.founderfinder.domain.model.ForumPost
import com.phoenixcorp.founderfinder.domain.repository.ForumRepository
import javax.inject.Inject

class CreateForumPostUseCase @Inject constructor(
    private val forumRepository: ForumRepository
) {
    suspend operator fun invoke(post: ForumPost): Result<String> {
        return forumRepository.createPost(post)
    }
}