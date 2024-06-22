// Egzersiz.kt
package com.gokhan.kfa

data class Egzersiz(
    var id: String = "",
    val name: String = "",
    val description: String = "",
    val equipment: String = "",
    val nasilYapilir: String = "",
    val targetMuscleGroups: List<String>? = null,
    val secondaryTargetMuscleGroups: List<String>? = null,
    val gifUrl: String = ""

)


