package com.gokhan.kfa

import viewModel.RoutineViewModel
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Chronometer
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.gokhan.kfa.databinding.ActivityMainBinding
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import ui.AntrenmanFragment
import ui.EgzersizEkleFragment
import ui.RutinFragment

class MainActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: ActivityMainBinding

    private lateinit var routineViewModel: RoutineViewModel
    private lateinit var chronometer: Chronometer
    private lateinit var overlayLayout: MaterialCardView
    private lateinit var elapsedTimeTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Base_Theme_KFA)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu -> {
                    replaceFragment(Menu.newInstance())
                    true
                }
                R.id.profile -> {
                    replaceFragment(Profile.newInstance())
                    true
                }
                R.id.antrenman -> {
                    replaceFragment(AntrenmanFragment.newInstance())
                    true
                }
                else -> false
            }
        }

        if (savedInstanceState == null) {
            binding.bottomNavigationView.selectedItemId = R.id.menu
        }

        routineViewModel = ViewModelProvider(this).get(RoutineViewModel::class.java)
        overlayLayout = findViewById(R.id.overlay_layout)
        elapsedTimeTextView = findViewById(R.id.tv_elapsed_time)
        chronometer = findViewById(R.id.chronometer)

        routineViewModel.isRoutineActive.observe(this) { isActive ->
            updateOverlayVisibility()
            if (isActive) {
                chronometer.start()
            } else {
                chronometer.stop()
            }
        }

        routineViewModel.elapsedTime.observe(this) { elapsedTime ->
            chronometer.base = SystemClock.elapsedRealtime() - elapsedTime
            elapsedTimeTextView.text = "Geçen Süre: ${formatElapsedTime(elapsedTime)}"
        }

        findViewById<Button>(R.id.btn_continue).setOnClickListener {
            continueRoutine()
        }

        findViewById<Button>(R.id.btn_finish).setOnClickListener {
            finishRoutine()
        }

        supportFragmentManager.addOnBackStackChangedListener {
            updateOverlayVisibility()
        }
    }

    private fun formatElapsedTime(elapsedTime: Long): String {
        val hours = (elapsedTime / 3600000).toInt()
        val minutes = (elapsedTime % 3600000 / 60000).toInt()
        val seconds = (elapsedTime % 60000 / 1000).toInt()
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout, fragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

    private fun updateOverlayVisibility() {
        if (routineViewModel.isRoutineActive.value == true && getCurrentFragment().let { it !is RutinFragment && it !is EgzersizEkleFragment }) {
            overlayLayout.visibility = View.VISIBLE
        } else {
            overlayLayout.visibility = View.GONE
        }
    }

    // Continue Routine
    private fun continueRoutine() {
        overlayLayout.visibility = View.GONE
        routineViewModel.resumeRoutine()
        navigateToEgzersizSecimFragment(routineViewModel.currentRoutineId)
    }

    // Finish Routine
    private fun finishRoutine() {
        overlayLayout.visibility = View.GONE
        chronometer.stop()
        routineViewModel.stopRoutine()
        routineViewModel.triggerFinishRoutine()
        saveRoutineDetails() // Call method to save routine details
    }

    // Navigate to RutinFragment
    private fun navigateToEgzersizSecimFragment(routineId: String?) {
        routineId?.let {
            val elapsedTime = routineViewModel.elapsedTime.value ?: 0L
            val fragment = RutinFragment.newInstance(it, elapsedTime)
            supportFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, fragment)
                .addToBackStack(null)
                .commit()
        } ?: run {
            // Handle the case where routineId is null, if necessary
            Log.e("NavigationError", "Routine ID is null")
        }
    }




    // Save Routine Details
    private fun saveRoutineDetails() {
        // Logic to save routine details to Firestore
    }

    // Get current fragment
    private fun getCurrentFragment(): Fragment? {
        return supportFragmentManager.findFragmentById(R.id.frame_layout)
    }
}
