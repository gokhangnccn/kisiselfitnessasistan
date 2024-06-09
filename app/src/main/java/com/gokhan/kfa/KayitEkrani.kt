package com.gokhan.kfa

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class KayitEkrani : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var nameEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var zatenKayiliyim: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Initialize views
        emailEditText = findViewById(R.id.email)
        passwordEditText = findViewById(R.id.sifre)
        nameEditText = findViewById(R.id.ad)
        registerButton = findViewById(R.id.kayitol)
        zatenKayiliyim = findViewById(R.id.zatenKayitliyim)

        // Register button click listener
        registerButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = firebaseAuth.currentUser
                        user?.let {
                            // Optionally, save additional user info in Firestore
                            saveUserInfo(it.uid, nameEditText.text.toString())
                        }
                        val intent = Intent(this, GirisEkrani::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Lütfen bilgileri doldurunuz!", Toast.LENGTH_SHORT).show()
            }
        }

        zatenKayiliyim.setOnClickListener{
            val intent = Intent(this, GirisEkrani::class.java)
            startActivity(intent)
            finish()
        }

    }

    private fun saveUserInfo(userId: String, name: String) {
        val userMap = hashMapOf(
            "name" to name,
            "email" to emailEditText.text.toString()
        )
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("users").document(userId).set(userMap)
            .addOnSuccessListener {
                Toast.makeText(this, "User info saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save user info", Toast.LENGTH_SHORT).show()
            }
    }
}
