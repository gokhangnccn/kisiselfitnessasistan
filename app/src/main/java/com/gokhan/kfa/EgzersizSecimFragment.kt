package com.gokhan.kfa

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gokhan.kfa.databinding.FragmentEgzersizSecimBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class EgzersizSecimFragment : Fragment() {

    private var _binding: FragmentEgzersizSecimBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val selectedRoutineExercises = mutableListOf<Egzersiz>()
    private lateinit var exerciseAdapter: EgzersizAdapter
    private var routineId: String? = null
    private var userId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEgzersizSecimBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()

        // userId'yi belirleme (örneğin, kullanıcı giriş yaptıktan sonra belirlenmiş olabilir)
        userId = auth.currentUser?.uid // Bu örnek bir ID'dir, gerçek uygulamada uygun şekilde ayarlayın

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        routineId = arguments?.getString("routineId")

        val scrollDownIcon: ImageView = view.findViewById(R.id.scroll_down_icon)
        val recyclerView: RecyclerView = view.findViewById(R.id.rv_aktif_egzersizler)

        // Scroll listener to show/hide the scroll down icon
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val totalItemCount = recyclerView.layoutManager?.itemCount ?: 0
                val lastVisibleItemPosition =
                    (recyclerView.layoutManager as? LinearLayoutManager)?.findLastVisibleItemPosition()
                        ?: 0

                if (lastVisibleItemPosition == totalItemCount - 1) {
                    fadeOutView(scrollDownIcon)
                } else {
                    fadeInView(scrollDownIcon)
                }
            }
        })

        scrollDownIcon.setOnClickListener {
            recyclerView.smoothScrollToPosition(recyclerView.adapter?.itemCount?.minus(1) ?: 0)
        }

        init()
    }

    private fun fadeInView(view: View) {
        if (view.visibility != View.VISIBLE) {
            val fadeInAnimation = AlphaAnimation(0f, 1f)
            fadeInAnimation.duration = 300
            view.startAnimation(fadeInAnimation)
            view.visibility = View.VISIBLE
        }
    }

    private fun fadeOutView(view: View) {
        if (view.visibility == View.VISIBLE) {
            val fadeOutAnimation = AlphaAnimation(1f, 0f)
            fadeOutAnimation.duration = 300
            view.startAnimation(fadeOutAnimation)
            view.visibility = View.GONE
        }
    }

    private fun init() {
        binding.rvAktifEgzersizler.layoutManager = LinearLayoutManager(context)
        exerciseAdapter = EgzersizAdapter(
            selectedRoutineExercises,
            onExerciseClicked = { exercise -> /* Handle click */ },
            onInfoClicked = { exercise ->
                context?.let { DialogUtils.showExerciseDetailsDialog(it, exercise) }
            },
            onDeleteClicked = { exercise -> deleteExercise(exercise) }, // Added delete action

            isRoutineExercise = true // For exercise selection menu
        )
        binding.rvAktifEgzersizler.adapter = exerciseAdapter

        binding.btnRutineEgzersizEkle.setOnClickListener {
            routineId?.let {
                fragmentManager?.beginTransaction()
                    ?.replace(R.id.frame_layout, EgzersizEkleFragment.newInstance(it))
                    ?.addToBackStack(null)
                    ?.commit()
            }
        }


        // Find the edit button and set its OnClickListener
        binding.btnEditRoutine.setOnClickListener {
            routineId?.let { id ->
                showEditRoutineDialog(id)
            }
        }

        routineId?.let { id ->
            db.collection("users").document(userId!!).collection("routines").document(id).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val routine = document.toObject(Routine::class.java)
                        routine?.let {
                            // Update routine name and description
                            binding.tvRoutineName.text = it.name
                            binding.tvRoutineDescription.text = it.description
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("EgzersizSecimFragment", "Error fetching routine details", e)
                }
        }

        fetchRoutineExercisesFromFirestore() // Ensure this is called after setting up the adapter
    }

    private fun showEditRoutineDialog(routineId: String) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_routine, null)
        val etRoutineName = dialogView.findViewById<EditText>(R.id.et_routine_name)
        val etRoutineDescription = dialogView.findViewById<EditText>(R.id.et_routine_description)

        // Pre-fill the dialog with current routine details
        db.collection("users").document(userId!!).collection("routines").document(routineId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val routine = document.toObject(Routine::class.java)
                    routine?.let {
                        etRoutineName.setText(it.name)
                        etRoutineDescription.setText(it.description)
                        // Güncellemeyi menüde de yap
                        binding.tvRoutineName.text = it.name
                        binding.tvRoutineDescription.text = it.description
                    }
                }
            }

        val dialog = AlertDialog.Builder(context, R.style.Theme_KFA)
            .setTitle("Rutini Düzenle")
            .setView(dialogView)
            .setPositiveButton("Kaydet") { _, _ ->
                val newName = etRoutineName.text.toString()
                val newDescription = etRoutineDescription.text.toString()

                if (newName.isNotEmpty() && newDescription.isNotEmpty()) {
                    updateRoutineDetails(routineId, newName, newDescription)
                    // Firestore'dan güncellenmiş bilgileri alarak kullanıcı arayüzünü güncelle
                    fetchUpdatedRoutineDetails(routineId)
                } else {
                    Toast.makeText(context, "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal", null)
            .create()

        dialog.show()
    }

    private fun updateRoutineDetails(routineId: String, newName: String, newDescription: String) {
        val routineRef = db.collection("users").document(userId!!).collection("routines").document(routineId)
        val updates = mapOf(
            "name" to newName,
            "description" to newDescription
        )

        routineRef.set(updates, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(context, "Rutin güncellendi", Toast.LENGTH_SHORT).show()
                fetchUpdatedRoutineDetails(routineId)
            }
            .addOnFailureListener { e ->
                Log.e("EgzersizSecimFragment", "Error updating routine details", e)
                Toast.makeText(context, "Rutin güncellenirken bir hata oluştu", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchUpdatedRoutineDetails(routineId: String) {
        userId?.let { userId ->
            db.collection("users").document(userId)
                .collection("routines").document(routineId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val routine = document.toObject(Routine::class.java)
                        routine?.let {
                            // Rutin adını ve açıklamasını güncelle
                            binding.tvRoutineName.text = it.name
                            binding.tvRoutineDescription.text = it.description
                        }

                    }
                }
                .addOnFailureListener { e ->
                    Log.e("EgzersizSecimFragment", "Error fetching updated routine details", e)
                }
        }
    }

    private fun fetchRoutineExercisesFromFirestore() {
        Log.d("EgzersizSecimFragment", "Fetching routine exercises")
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
                            Log.d("EgzersizSecimFragment", "Fetched exercise: ${exercise.name} with ID: ${exercise.id}")
                        }
                        selectedRoutineExercises.clear()
                        selectedRoutineExercises.addAll(routineExercises)
                        exerciseAdapter.notifyDataSetChanged()
                        Log.d("EgzersizSecimFragment", "RecyclerView updated with ${selectedRoutineExercises.size} exercises")
                    }
                    .addOnFailureListener { e ->
                        Log.e("EgzersizSecimFragment", "Error fetching routine exercises", e)
                    }
=======
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("EgzersizSecimFragment", "Error fetching updated routine details", e)
                }
        }
    }

    private fun fetchRoutineExercisesFromFirestore() {
        Log.d("EgzersizSecimFragment", "Fetching routine exercises")
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
                            Log.d("EgzersizSecimFragment", "Fetched exercise: ${exercise.name} with ID: ${exercise.id}")
                        }
                        selectedRoutineExercises.clear()
                        selectedRoutineExercises.addAll(routineExercises)
                        exerciseAdapter.notifyDataSetChanged()
                        Log.d("EgzersizSecimFragment", "RecyclerView updated with ${routineExercises.size} exercises")
                    }
                    .addOnFailureListener { e ->
                        Log.e("EgzersizSecimFragment", "Error fetching routine exercises", e)
                    }
            }
        }
    }

    private fun deleteSelectedExerciseFromList() {
        val selectedExercises = exerciseAdapter.getSelectedExercises()
        if (selectedExercises.isEmpty()) {
            Toast.makeText(context, "Lütfen silmek için egzersiz seçin", Toast.LENGTH_SHORT).show()
            return
        }

        // Rutin Id'sini kontrol et
        routineId?.let { routineId ->
            val batch = db.batch()
            for (exercise in selectedExercises) {
                val exerciseRef = db.collection("users").document(userId!!)
                    .collection("routines").document(routineId)
                    .collection("exercises").document(exercise.id)
                batch.delete(exerciseRef)
            }
        }
    }
    private fun deleteExercise(exercise: Egzersiz) {
        // Implement your delete logic here
        // For example, show confirmation dialog or directly delete from Firestore
        // Make sure to update UI after deletion

        val builder = AlertDialog.Builder(context)
        builder.setTitle("Egzersizi Sil")
        builder.setMessage("Bu egzersizi rutinden silmek istediğinizden emin misiniz?")
        builder.setPositiveButton("Evet") { _, _ ->
            userId?.let { userId ->
                routineId?.let { routineId ->
                    val exerciseRef = db.collection("users").document(userId)
                        .collection("routines").document(routineId)
                        .collection("exercises").document(exercise.id!!)
                    exerciseRef.delete()
                        .addOnSuccessListener {
                            Toast.makeText(context, "Egzersiz başarıyla silindi", Toast.LENGTH_SHORT).show()
                            selectedRoutineExercises.remove(exercise)
                            exerciseAdapter.notifyDataSetChanged()
                        }
                        .addOnFailureListener { e ->
                            Log.e("EgzersizSecimFragment", "Error deleting exercise", e)
                            Toast.makeText(context, "Egzersiz silinirken bir hata oluştu", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }
        builder.setNegativeButton("İptal", null)
        builder.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    companion object {
        @JvmStatic
        fun newInstance(routineId: String): EgzersizSecimFragment {
            val fragment = EgzersizSecimFragment()
            val args = Bundle().apply {
                putString("routineId", routineId)
            }
            fragment.arguments = args
            return fragment
        }
    }
}

