package com.example.procard.model


data class UserProfile(
    val id: String,
    val full_name: String,
    val avatar_url: String? = null
)