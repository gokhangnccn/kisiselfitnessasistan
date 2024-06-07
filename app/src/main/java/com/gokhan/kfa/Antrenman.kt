package com.gokhan.kfa

import RoutineAdapter
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
    private lateinit var exerciseAdapter: EgzersizAdapter
    private var selectedRoutine: Routine? = null
    private val exercises = mutableListOf<Egzersiz>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAntrenmanBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        binding.rvRoutines.layoutManager = LinearLayoutManager(context)
        routineAdapter = RoutineAdapter(routines, this::onRoutineSelected, this::onStartRoutineClicked)
        binding.rvRoutines.adapter = routineAdapter

        exerciseAdapter = EgzersizAdapter(exercises) { exercise ->
            // Handle exercise click if needed
        }

        binding.btnAddRoutine.setOnClickListener {
            showCreateRoutineDialog()
        }

        binding.btnDeleteRoutine.setOnClickListener {
            deleteSelectedRoutines()
        }

        fetchRoutinesFromFirestore()
    }

    private fun onStartRoutineClicked(routine: Routine) {
        fragmentManager?.beginTransaction()
            ?.replace(R.id.frame_layout, EgzersizSecimFragment.newInstance(routine.id))
            ?.addToBackStack(null)
            ?.commit()
    }

    private fun showCreateRoutineDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_routine, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.btn_create_routine).setOnClickListener {
            val routineName = dialogView.findViewById<EditText>(R.id.et_routine_name).text.toString()
            val routineDescription = dialogView.findViewById<EditText>(R.id.et_routine_description).text.toString()
            if (routineName.isNotEmpty()) {
                createRoutine(routineName, routineDescription)
                dialog.dismiss()
            } else {
                Toast.makeText(context, "Lütfen bir rutin adı girin", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun createRoutine(routineName: String, routineDescription: String) {
        val routine = hashMapOf(
            "name" to routineName,
            "description" to routineDescription,
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
                    routine.id = document.id
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
        fetchExercisesForRoutine(routine.id)
    }

    private fun fetchExercisesForRoutine(routineId: String) {
        db.collection("routines").document(routineId).collection("exercises").get()
            .addOnSuccessListener { result ->
                exercises.clear()
                for (document in result) {
                    val exercise = document.toObject(Egzersiz::class.java)
                    exercise.id = document.id
                    exercises.add(exercise)
                }
                exerciseAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.w("Antrenman", "Error fetching exercises for routine", e)
            }
    }

    private fun deleteSelectedRoutines() {
        val selectedRoutines = routineAdapter.getSelectedRoutines()
        if (selectedRoutines.isEmpty()) {
            Toast.makeText(context, "Lütfen silmek için rutin seçin", Toast.LENGTH_SHORT).show()
            return
        }

        val batch = db.batch()
        selectedRoutines.forEach { routine ->
            val routineRef = db.collection("routines").document(routine.id)
            batch.delete(routineRef)        }

        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(context, "Rutinler başarıyla silindi", Toast.LENGTH_SHORT).show()
                fetchRoutinesFromFirestore()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Rutinler silinirken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): Antrenman {
            return Antrenman()
        }
    }
}

