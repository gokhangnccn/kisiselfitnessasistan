package com.gokhan.kfa

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.gokhan.kfa.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Base_Theme_KFA)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu -> {
                    replaceFragment(Menu.newInstance("param1_value", "param2_value"))
                    true
                }
                R.id.profile -> {
                    replaceFragment(Profile.newInstance())
                    true
                }
                R.id.antrenman -> {
                    replaceFragment(Antrenman.newInstance())
                    true
                }
                else -> false
            }
        }

        if (savedInstanceState == null) {
            binding.bottomNavigationView.selectedItemId = R.id.menu
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout, fragment)
        fragmentTransaction.commit()
    }


}
