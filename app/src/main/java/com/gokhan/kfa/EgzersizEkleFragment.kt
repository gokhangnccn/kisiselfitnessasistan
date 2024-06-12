package com.gokhan.kfa

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.gokhan.kfa.databinding.FragmentEgzersizEkleBinding
import com.google.firebase.firestore.FirebaseFirestore

class EgzersizEkleFragment : Fragment() {

    private var _binding: FragmentEgzersizEkleBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private val allExercises = mutableListOf<Egzersiz>()
    private lateinit var exerciseAdapter: EgzersizAdapter
    private var routineId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEgzersizEkleBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()
        routineId = arguments?.getString("routineId")
        return binding.root
    }

    companion object {
        private const val ARG_ROUTINE_ID = "routineId"

        fun newInstance(routineId: String): EgzersizEkleFragment {
            val fragment = EgzersizEkleFragment()
            val args = Bundle()
            args.putString(ARG_ROUTINE_ID, routineId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        binding.rvEgzersizListesi.layoutManager = LinearLayoutManager(context)
        exerciseAdapter = EgzersizAdapter(
            allExercises,
            onExerciseClicked = { exercise ->

            },
            onInfoClicked = { exercise ->
                context?.let { DialogUtils.showExerciseDetailsDialog(it, exercise) }
            },
            isRoutineExercise = false // Egzersiz ekleme menüsü için false
        )
        binding.rvEgzersizListesi.adapter = exerciseAdapter

        fetchExercisesFromFirestore()

        binding.btnCompleteSelection.setOnClickListener {
            addSelectedExercisesToRoutine()
        }
    }






    private fun fetchExercisesFromFirestore() {
        db.collection("exercises").get()
            .addOnSuccessListener { result ->
                val newExercises = mutableListOf<Egzersiz>()
                for (document in result) {
                    val exercise = document.toObject(Egzersiz::class.java)
                    exercise.id = document.id
                    newExercises.add(exercise)
                }
                exerciseAdapter.updateExercises(newExercises)
            }
            .addOnFailureListener { e ->
                Log.w("EgzersizEkleFragment", "Error fetching exercises", e)
            }
    }

    private fun fetchRoutineExercisesFromFirestore() {
        routineId?.let { routineId ->
            db.collection("routines").document(routineId).collection("exercises").get()
                .addOnSuccessListener { result ->
                    val routineExercises = mutableListOf<Egzersiz>()
                    for (document in result) {
                        val exercise = document.toObject(Egzersiz::class.java)
                        exercise.id = document.id
                        routineExercises.add(exercise)
                    }
                    allExercises.clear()
                    allExercises.addAll(routineExercises)
                    exerciseAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Log.w("EgzersizEkleFragment", "Error fetching routine exercises", e)
                }
        }
    }

    private fun addSelectedExercisesToRoutine() {
        val selectedExercises = exerciseAdapter.getSelectedExercises()
        if (selectedExercises.isEmpty()) {
            Toast.makeText(context, "Lütfen eklemek için en az bir egzersiz seçin", Toast.LENGTH_SHORT).show()
            return
        }

        routineId?.let { id ->
            val batch = db.batch()
            selectedExercises.forEach { exercise ->
                val exerciseRef = db.collection("routines").document(id).collection("exercises").document(exercise.id)
                batch.set(exerciseRef, exercise)
            }
            batch.commit()
                .addOnSuccessListener {
                    Toast.makeText(context, "Seçilen egzersizler başarıyla eklendi", Toast.LENGTH_SHORT).show()
                    fetchRoutineExercisesFromFirestore()
                    activity?.supportFragmentManager?.popBackStack()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Seçilen egzersizler eklenirken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
