package com.phoenixcorp.founderfinder.domain.model

data class Category(
    val id: String,
    val name: String,
    val icon: String? = null,           // emoji or drawable name
    val description: String? = null
)