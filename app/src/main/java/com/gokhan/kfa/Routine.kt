// Routine.kt
package com.gokhan.kfa

data class Routine(
    var id: String = "",
    val name: String = "",
    val exercises: MutableList<Egzersiz> = mutableListOf()
) {
    // Parametresiz constructor
    constructor() : this("", "", mutableListOf())
}