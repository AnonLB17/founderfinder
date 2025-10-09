package com.phoenixcorp.founderfinder.data

data class File(
    val fileId: String = "",
    val name: String = "",
    val url: String = "",
    val uploadedAt: Long = 0L,
    val uploaderId: String = "",
    val type: String = "",
    val sharedWith: List<String> = emptyList(),
    val orgId: String = ""
)