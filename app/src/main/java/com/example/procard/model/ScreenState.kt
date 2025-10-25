package com.example.procard.model


data class ScreenState(
    val loading: Boolean = false,
    val error: String? = null,
    val empty: Boolean = false
)