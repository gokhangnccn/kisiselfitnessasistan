package com.gokhan.kfa

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gokhan.kfa.databinding.ActivitySignInBinding
import com.google.firebase.auth.FirebaseAuth

class GirisEkrani : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.girisyap.setOnClickListener {
            val email = binding.email.text.toString()
            val pass = binding.sifre.text.toString()

            if (email.isNotEmpty() && pass.isNotEmpty()) {

                firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener {
                    if (it.isSuccessful) {
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish() // Finish GirisEkrani so user can't go back to it
                    } else {
                        Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Lütfen bilgileri doldurunuz!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.kayitol.setOnClickListener {
            val intent2 = Intent(this, KayitEkrani::class.java)
            startActivity(intent2)
        }
    }

    override fun onStart() {
        super.onStart()
        if (firebaseAuth.currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Finish GirisEkrani so user can't go back to it
        }
    }
}
