package com.gokhan.kfa

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.gokhan.kfa.databinding.FragmentEgzersizEkleBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EgzersizEkleFragment : Fragment() {

    private var _binding: FragmentEgzersizEkleBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val allExercises = mutableListOf<Egzersiz>()
    private lateinit var exerciseAdapter: EgzersizAdapter
    private var routineId: String? = null
    private var userId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEgzersizEkleBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        routineId = arguments?.getString("routineId")
        userId = auth.currentUser?.uid
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
            onExerciseClicked = { exercise -> },
            onInfoClicked = { exercise ->
                context?.let { DialogUtils.showExerciseDetailsDialog(it, exercise) }
            },
            onDeleteClicked = {  },
            onAddSetClicked = { exercise -> addNewSetRow(exercise) },
            isRoutineExercise = false // Egzersiz ekleme menüsü için false
        )
        binding.rvEgzersizListesi.adapter = exerciseAdapter
        binding.rvEgzersizListesi.visibility = View.VISIBLE // Ensure visibility

        fetchExercisesFromFirestore()

        binding.btnCompleteSelection.setOnClickListener {
            addSelectedExercisesToRoutine()
        }

    }

    private fun addNewSetRow(exercise: Egzersiz) {
        val tableLayout: TableLayout = view?.findViewById(R.id.tableLayout) ?: return

        val newTableRow = TableRow(context).apply {
            layoutParams = TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 4, 0, 4)
            }
        }

        val setNumberTextView = TextView(context).apply {
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.5f).apply {
                gravity = Gravity.CENTER
            }
            text = (tableLayout.childCount).toString()
            textSize = 14f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.c4))
            gravity = Gravity.CENTER
            setPadding(4, 4, 4, 4)
        }

        val repetitionsEditText = EditText(context).apply {
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f).apply {
                gravity = Gravity.CENTER
            }
            hint = "Tekrar Sayısı"
            textSize = 14f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.c4))
            gravity = Gravity.CENTER
            inputType = InputType.TYPE_CLASS_NUMBER
            setPadding(4, 4, 4, 4)
        }

        val weightEditText = EditText(context).apply {
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f).apply {
                gravity = Gravity.CENTER
            }
            hint = "Ağırlık (kg)"
            textSize = 14f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.c4))
            gravity = Gravity.CENTER
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setPadding(8, 8, 8, 8)
        }

        val completedCheckBox = CheckBox(context).apply {
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f).apply {
                gravity = Gravity.CENTER
            }
            setTextColor(ContextCompat.getColor(requireContext(), R.color.c4))
            gravity = Gravity.CENTER
            setPadding(8, 8, 8, 8)
        }

        newTableRow.addView(setNumberTextView)
        newTableRow.addView(repetitionsEditText)
        newTableRow.addView(weightEditText)
        newTableRow.addView(completedCheckBox)

        tableLayout.addView(newTableRow)
        Log.d("RutinFragment", "New set row added for exercise: ${exercise.name}")
    }


    private fun fetchExercisesFromFirestore() {
        db.collection("exercises").get()
            .addOnSuccessListener { result ->
                val newExercises = mutableListOf<Egzersiz>()
                for (document in result) {
                    val exercise = document.toObject(Egzersiz::class.java)
                    exercise.id = document.id
                    newExercises.add(exercise)
                    Log.d("FetchExercises", "Fetched exercise: ${exercise.name} with ID: ${exercise.id}")
                }
                exerciseAdapter.updateExercises(newExercises)
                Log.d("FetchExercises", "Total exercises fetched: ${newExercises.size}")
            }
            .addOnFailureListener { e ->
                Log.w("FetchExercises", "Error fetching exercises", e)
            }
    }


    private fun addSelectedExercisesToRoutine() {
        val selectedExercises = exerciseAdapter.getSelectedExercises()
        if (selectedExercises.isEmpty()) {
            Toast.makeText(context, "Please select at least one exercise to add", Toast.LENGTH_SHORT).show()
            return
        }

        userId?.let { userId ->
            routineId?.let { routineId ->
                val batch = db.batch()
                selectedExercises.forEach { exercise ->
                    val exerciseRef = db.collection("users").document(userId)
                        .collection("routines").document(routineId)
                        .collection("exercises").document(exercise.id)
                    batch.set(exerciseRef, exercise)
                }
                batch.commit()
                    .addOnSuccessListener {
                        Log.d("EgzersizEkleFragment", "Selected exercises added successfully")
                        Toast.makeText(context, "Selected exercises added successfully", Toast.LENGTH_SHORT).show()
                        fetchRoutineExercisesFromFirestore() // Fetch and update the routine exercises
                    }
                    .addOnFailureListener { e ->
                        Log.e("EgzersizEkleFragment", "Error adding selected exercises: ${e.message}")
                        Toast.makeText(context, "Error adding selected exercises: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun fetchRoutineExercisesFromFirestore() {
        userId?.let { userId ->
            routineId?.let { routineId ->
                db.collection("users").document(userId)
                    .collection("routines").document(routineId)
                    .collection("exercises").get()
                    .addOnSuccessListener { result ->
                        val routineExercises = mutableListOf<Egzersiz>()
                        for (document in result) {
                            val exercise = document.toObject(Egzersiz::class.java)
                            exercise.id = document.id
                            routineExercises.add(exercise)
                        }
                        Log.d("EgzersizEkleFragment", "Fetched ${routineExercises.size} exercises")
                        allExercises.clear()
                        allExercises.addAll(routineExercises)
                        exerciseAdapter.notifyDataSetChanged() // Notify adapter that data has changed
                        Log.d("EgzersizEkleFragment", "RecyclerView updated")
                        activity?.supportFragmentManager?.popBackStack() // Pop back after update
                    }
                    .addOnFailureListener { e ->
                        Log.e("EgzersizEkleFragment", "Error fetching routine exercises: ${e.message}")
                    }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
