package com.gokhan.kfa

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.gokhan.kfa.databinding.FragmentAntrenmanBinding
import com.google.firebase.firestore.FirebaseFirestore

class Antrenman : Fragment() {

    private var _binding: FragmentAntrenmanBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private val routines = mutableListOf<Routine>()
    private lateinit var routineAdapter: RoutineAdapter
    private var selectedRoutine: Routine? = null
    private lateinit var exerciseAdapter: EgzersizAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAntrenmanBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()
        return binding.root
    }

    companion object {
        fun newInstance(): Antrenman {
            return Antrenman()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        binding.rvRoutines.layoutManager = LinearLayoutManager(context)
        routineAdapter = RoutineAdapter(routines, { routine -> onRoutineSelected(routine) }, { routine -> onStartRoutine(routine) })
        binding.rvRoutines.adapter = routineAdapter

        binding.btnAddRoutine.setOnClickListener {
            showCreateRoutineDialog()
        }

        binding.btnAddExercise.setOnClickListener {
            if (selectedRoutine != null) {
                fragmentManager?.beginTransaction()
                    ?.replace(R.id.frame_layout, EgzersizSecimFragment.newInstance(selectedRoutine!!.id))
                    ?.addToBackStack(null)
                    ?.commit()
            } else {
                Toast.makeText(context, "Lütfen bir rutin seçin", Toast.LENGTH_SHORT).show()
            }
        }

        binding.rvExercises.layoutManager = LinearLayoutManager(context)
        exerciseAdapter = EgzersizAdapter(mutableListOf())
        binding.rvExercises.adapter = exerciseAdapter

        fetchRoutinesFromFirestore()
    }

    private fun showCreateRoutineDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_routine, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.btn_create_routine).setOnClickListener {
            val routineName = dialogView.findViewById<EditText>(R.id.et_routine_name).text.toString()
            if (routineName.isNotEmpty()) {
                createRoutine(routineName)
                dialog.dismiss()
            } else {
                Toast.makeText(context, "Lütfen bir rutin adı girin", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun createRoutine(routineName: String) {
        val routine = hashMapOf(
            "name" to routineName,
            "exercises" to emptyList<Egzersiz>()
        )

        db.collection("routines").add(routine)
            .addOnSuccessListener {
                Toast.makeText(context, "Rutin başarıyla oluşturuldu", Toast.LENGTH_SHORT).show()
                fetchRoutinesFromFirestore()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Rutin oluşturulurken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchRoutinesFromFirestore() {
        db.collection("routines").get()
            .addOnSuccessListener { result ->
                routines.clear()
                for (document in result) {
                    val routine = document.toObject(Routine::class.java)
                    routine.id = document.id // Ensure the routine has its document ID
                    routines.add(routine)
                }
                routineAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.w("Antrenman", "Error fetching routines", e)
            }
    }

    private fun onRoutineSelected(routine: Routine) {
        selectedRoutine = routine
        binding.tvSelectedRoutine.text = "Seçili Rutin: ${routine.name}"
        fetchExercisesForRoutine(routine)
    }

    private fun fetchExercisesForRoutine(routine: Routine) {
        db.collection("routines").document(routine.id).collection("exercises").get()
            .addOnSuccessListener { result ->
                val exercises = mutableListOf<Egzersiz>()
                for (document in result) {
                    val exercise = document.toObject(Egzersiz::class.java)
                    exercise.id = document.id  // Set the document ID to the id property
                    exercises.add(exercise)
                }
                exerciseAdapter.setExercises(exercises)
            }
            .addOnFailureListener { e ->
                Log.w("Antrenman", "Error fetching exercises for routine", e)
            }
    }


    private fun onStartRoutine(routine: Routine) {
        // Implement the logic to start the routine
        Toast.makeText(context, "${routine.name} rutini başlatıldı", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
