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
import com.google.firebase.firestore.FirebaseFirestore

class EgzersizSecimFragment : Fragment() {

    private var _binding: FragmentEgzersizSecimBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private val exercises = mutableListOf<Egzersiz>()
    private lateinit var exerciseAdapter: EgzersizAdapter
    private var routineId: String? = null
    private var selectedRoutine: Routine? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEgzersizSecimBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()
        routineId = arguments?.getString("routineId")
        return binding.root
    }

    companion object {
        private const val ARG_ROUTINE_ID = "routineId"

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
        init()
    }

    private fun init() {
        binding.rvAktifEgzersizler.layoutManager = LinearLayoutManager(context)
        exerciseAdapter = EgzersizAdapter(exercises) { exercise ->
            // Handle exercise click if needed
        }
        binding.rvAktifEgzersizler.adapter = exerciseAdapter
        fetchExercisesForRoutine()

        binding.btnRutineEgzersizEkle.setOnClickListener {
            if (selectedRoutine != null) {
                val fragment = EgzersizEkleFragment.newInstance(selectedRoutine!!.id)
                fragmentManager?.beginTransaction()
                    ?.replace(R.id.frame_layout, fragment)
                    ?.addToBackStack(null)
                    ?.commit()
            } else {
                Toast.makeText(context, "Lütfen bir rutin seçin", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun fetchExercisesForRoutine() {
        routineId?.let { id ->
            db.collection("routines").document(id).collection("exercises").get()
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
                    Log.w("EgzersizSecimFragment", "Error fetching exercises for routine", e)
                }
        }
    }

    fun onRoutineSelected(routine: Routine) {
        selectedRoutine = routine
        fetchExercisesForRoutine()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
