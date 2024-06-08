// Egzersiz.kt
package com.gokhan.kfa

data class Egzersiz(
    var id: String = "",
    val name: String = "",
    val description: String = "",
    val targetMuscleGroups: List<String>? = null,
    val gifUrl: String = ""
)