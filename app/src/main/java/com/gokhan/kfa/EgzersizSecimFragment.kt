package com.gokhan.kfa

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.gokhan.kfa.databinding.FragmentEgzersizSecimBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EgzersizSecimFragment : Fragment() {

    private var _binding: FragmentEgzersizSecimBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private val exercises = mutableListOf<Egzersiz>()
    private lateinit var exerciseAdapter: EgzersizAdapter
    private var routineId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEgzersizSecimBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()
        return binding.root
    }

    companion object {
        private const val ARG_ROUTINE_ID = "routine_id"

        fun newInstance(routineId: String): EgzersizSecimFragment {
            val fragment = EgzersizSecimFragment()
            val args = Bundle()
            args.putString(ARG_ROUTINE_ID, routineId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        routineId = arguments?.getString(ARG_ROUTINE_ID)
        init()
    }

    private fun init() {
        binding.rvEgzersizSecim.layoutManager = LinearLayoutManager(context)
        exerciseAdapter = EgzersizAdapter(exercises)
        binding.rvEgzersizSecim.adapter = exerciseAdapter

        binding.btnSecimiTamamla.setOnClickListener {
            saveSelectedExercises()
        }

        fetchExercisesFromFirestore()
    }

    private fun fetchExercisesFromFirestore() {
        db.collection("exercises").get()
            .addOnSuccessListener { result ->
                exercises.clear()
                for (document in result) {
                    val exercise = document.toObject(Egzersiz::class.java)
                    exercise.id = document.id  // Set the document ID to the id property
                    exercises.add(exercise)
                }
                exerciseAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.w("EgzersizSecimFragment", "Error fetching exercises", e)
            }
    }

    private fun saveSelectedExercises() {
        val selectedExercises = exerciseAdapter.getSelectedExercises()
        if (selectedExercises.isEmpty()) {
            Toast.makeText(context, "Lütfen eklemek için egzersiz seçin", Toast.LENGTH_SHORT).show()
            return
        }

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(context, "Lütfen önce giriş yapın", Toast.LENGTH_SHORT).show()
            return
        }

        val batch = db.batch()
        val routineRef = db.collection("routines").document(routineId!!)
        selectedExercises.forEach { exercise ->
            val exerciseRef = routineRef.collection("exercises").document(exercise.id)
            batch.set(exerciseRef, exercise)
        }

        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(context, "Egzersizler başarıyla eklendi", Toast.LENGTH_SHORT).show()
                fragmentManager?.popBackStack()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Egzersizler eklenirken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
