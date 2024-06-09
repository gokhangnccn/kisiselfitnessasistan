package com.gokhan.kfa

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gokhan.kfa.databinding.ActivitySifreSifirlaBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class SifremiUnuttum : AppCompatActivity() {

    private lateinit var binding: ActivitySifreSifirlaBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySifreSifirlaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.btnSifreSifirla.setOnClickListener {
            val email = binding.etEmail.text.toString()

            if (email.isNotEmpty()) {
                firebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Şifre sıfırlama e-postası gönderildi.", Toast.LENGTH_SHORT).show()
                        finish() // Close the activity after successful email sent
                    } else {
                        val exception = task.exception
                        if (exception is FirebaseAuthInvalidUserException) {
                            Toast.makeText(this, "Bu e-posta adresi ile kayıtlı bir kullanıcı bulunamadı.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, exception?.message ?: "Hata oluştu.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Lütfen e-posta adresinizi giriniz.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
