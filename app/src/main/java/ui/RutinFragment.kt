package ui

import adapter.EgzersizAdapter
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.CheckBox
import android.widget.Chronometer
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gokhan.kfa.ChronometerService
import com.gokhan.kfa.DialogUtils
import com.gokhan.kfa.R
import com.gokhan.kfa.databinding.FragmentRutinBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import model.Egzersiz
import model.Routine
import viewModel.RoutineViewModel

class RutinFragment : Fragment() {

    private var _binding: FragmentRutinBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val selectedRoutineExercises = mutableListOf<Egzersiz>()
    private lateinit var exerciseAdapter: EgzersizAdapter

    private var routineId: String? = null
    private var userId: String? = null

    private lateinit var chronometer: Chronometer
    private var isChronometerRunning = false
    private var chronometerBaseTime: Long = 0L

    private val routineViewModel: RoutineViewModel by activityViewModels()

    private var exerciseSetsMap: MutableMap<String, MutableList<Map<String, Any>>> = mutableMapOf()



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRutinBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()
        userId = auth.currentUser?.uid
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        routineId = arguments?.getString("routineId")
        val elapsedTime = arguments?.getLong("elapsedTime") ?: 0L

        val scrollDownIcon: ImageView = view.findViewById(R.id.scroll_down_icon)
        val recyclerView: RecyclerView = view.findViewById(R.id.rv_aktif_egzersizler)

        chronometer = view.findViewById(R.id.chronometer)
        chronometer.base = SystemClock.elapsedRealtime() - elapsedTime

        routineViewModel.finishRoutineEvent.observe(viewLifecycleOwner, Observer {
            finishRoutine()
        })

        // If routine is active, resume the chronometer
        routineViewModel.elapsedTime.observe(viewLifecycleOwner) { elapsedTime ->
            if (routineViewModel.isRoutineActive.value == true) {
                chronometer.base = SystemClock.elapsedRealtime() - elapsedTime
                chronometer.start()
            }
        }
        // Observe LiveData in onViewCreated or onCreateView
        routineViewModel.selectedRoutineExercises.observe(viewLifecycleOwner) { exercises ->
            // Update UI with exercises
            selectedRoutineExercises.clear()
            selectedRoutineExercises.addAll(exercises)
            exerciseAdapter.notifyDataSetChanged()
        }


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
        binding.btnFinishRoutine.setOnClickListener {
            finishRoutine()
        }

        binding.btnPauseRoutine.setOnClickListener {
            toggleChronometer()
        }

        init()
    loadExerciseSets() // Load sets when view is created
    }

    private fun init() {
        binding.rvAktifEgzersizler.layoutManager = LinearLayoutManager(context)
        exerciseAdapter = EgzersizAdapter(
            selectedRoutineExercises,
            onExerciseClicked = { exercise -> /* Handle click */ },
            onInfoClicked = { exercise ->
                context?.let { DialogUtils.showExerciseDetailsDialog(it, exercise) }
            },
            onDeleteClicked = { exercise -> deleteExercise(exercise) },
            onAddSetClicked = { exercise -> addNewSetRow(exercise) },
            isRoutineExercise = true
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
                    Log.e("RutinFragment", "Error fetching routine details", e)
                }
        }

        fetchRoutineExercisesFromFirestore()
    }
    private fun addNewSetRow(exercise: Egzersiz) {
        val tableLayout: TableLayout = view?.findViewById(R.id.tableLayout) ?: return

        val setNumberTextView = TextView(context).apply {
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.5f).apply {
                gravity = Gravity.CENTER
            }
            text = (tableLayout.childCount).toString() // Count of existing child rows
            textSize = 18f
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
            setPadding(4, 32, 4, 32)
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
            setPadding(4, 32, 4, 32)
        }

        val completedCheckBoxContainer = LinearLayout(context).apply {
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
            addView(CheckBox(context).apply {
                setTextColor(ContextCompat.getColor(requireContext(), R.color.c4))
                gravity = Gravity.CENTER
            })
        }

        val newTableRow = TableRow(context).apply {
            layoutParams = TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
            addView(setNumberTextView)
            addView(repetitionsEditText)
            addView(weightEditText)
            addView(completedCheckBoxContainer)
        }

        tableLayout.addView(newTableRow)

        // Save the new set data to Firestore
        val setData = hashMapOf(
            "setNumber" to setNumberTextView.text.toString(),
            "repetitions" to repetitionsEditText.text.toString(),
            "weight" to weightEditText.text.toString(),
            "completed" to false // Initially, the set is not completed
        )
        exercise.id?.let { exerciseId ->
            // Add the set data to exerciseSetsMap for local tracking
            exerciseSetsMap.getOrPut(exerciseId) { mutableListOf() }.add(setData)

            userId?.let { userId ->
                routineId?.let { routineId ->
                    db.collection("users").document(userId)
                        .collection("routines").document(routineId)
                        .collection("exercises").document(exercise.id!!)
                        .collection("sets").add(setData)
                        .addOnSuccessListener {
                            Log.d("RutinFragment", "New set added for exercise: ${exercise.name}")
                            fetchExerciseSetsFromFirestore(exercise) // Fetch updated sets
                        }
                        .addOnFailureListener { e ->
                            Log.e("RutinFragment", "Error adding new set", e)
                        }
                }
            }
        }
    }




    private fun loadExerciseSets() {
        Log.d("RutinFragment", "Loading exercise sets")
        userId?.let { userId ->
            routineId?.let { routineId ->
                db.collection("users").document(userId)
                    .collection("routines").document(routineId)
                    .collection("exercises").get()
                    .addOnSuccessListener { result ->
                        for (document in result) {
                            val exercise = document.toObject(Egzersiz::class.java)
                            fetchExerciseSetsFromFirestore(exercise)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("RutinFragment", "Error fetching exercises", e)
                    }
            }
        }
    }


    private fun fetchExerciseSetsFromFirestore(exercise: Egzersiz) {
        userId?.let { userId ->
            routineId?.let { routineId ->
                db.collection("users").document(userId)
                    .collection("routines").document(routineId)
                    .collection("exercises").document(exercise.id!!)
                    .collection("sets").get()
                    .addOnSuccessListener { result ->
                        val setsList = mutableListOf<Map<String, Any>>()
                        for (document in result) {
                            val setData = document.data
                            setsList.add(setData)
                        }
                        exerciseSetsMap[exercise.id!!] = setsList

                        // Update UI
                        updateSetsUI(exercise)
                    }
                    .addOnFailureListener { e ->
                        Log.e("RutinFragment", "Error fetching sets for exercise: ${exercise.name}", e)
                    }
            }
        }
    }

    private fun updateSetsUI(exercise: Egzersiz) {
        val tableLayout: TableLayout = view?.findViewById(R.id.tableLayout) ?: return
        tableLayout.removeAllViews()

        exerciseSetsMap[exercise.id]?.forEachIndexed { index, setData ->
            addSetRowToTable(tableLayout, setData)
        }
    }


    private fun addSetRowToTable(tableLayout: TableLayout, setData: Map<String, Any>) {
        val newTableRow = TableRow(context).apply {
            layoutParams = TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
        }

        val setNumberTextView = TextView(context).apply {
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.5f).apply {
                gravity = Gravity.CENTER
            }
            text = setData["setNumber"].toString()
            textSize = 18f
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
            setPadding(4, 32, 4, 32)
            setText(setData["repetitions"].toString())
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
            setPadding(4, 32, 4, 32)
            setText(setData["weight"].toString())
        }

        val completedCheckBoxContainer = LinearLayout(context).apply {
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
            addView(CheckBox(context).apply {
                setTextColor(ContextCompat.getColor(requireContext(), R.color.c4))
                gravity = Gravity.CENTER
                isChecked = setData["completed"] as Boolean
            })
        }

        newTableRow.addView(setNumberTextView)
        newTableRow.addView(repetitionsEditText)
        newTableRow.addView(weightEditText)
        newTableRow.addView(completedCheckBoxContainer)

        tableLayout.addView(newTableRow)
    }





    private fun toggleChronometer() {
        if (isChronometerRunning) {
            chronometerBaseTime = SystemClock.elapsedRealtime() - chronometer.base
            chronometer.stop()

            val intent = Intent(context, ChronometerService::class.java).apply {
                action = ChronometerService.ACTION_STOP
            }
            context?.startService(intent)
        } else {
            chronometer.base = SystemClock.elapsedRealtime() - chronometerBaseTime
            chronometer.start()

            val intent = Intent(context, ChronometerService::class.java).apply {
                action = ChronometerService.ACTION_START
                putExtra("BASE_TIME", chronometer.base)
            }
            context?.startService(intent)
        }
        isChronometerRunning = !isChronometerRunning
    }

    override fun onPause() {
        super.onPause()
        if (isChronometerRunning) {
            chronometerBaseTime = SystemClock.elapsedRealtime() - chronometer.base
            chronometer.stop()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isChronometerRunning) {
            chronometer.base = SystemClock.elapsedRealtime() - chronometerBaseTime
            chronometer.start()
        }
        loadExerciseSets()
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



    private fun finishRoutine() {
        val userId = userId
        val routineId = routineId
        val context = context

        if (userId == null || routineId == null || context == null) {
            Log.e("RutinFragment", "User ID, Routine ID, or Context is null")
            Toast.makeText(context, "Eksik bilgi. Lütfen tekrar deneyin.", Toast.LENGTH_SHORT).show()
            return
        }

        val routineRef = db.collection("users").document(userId)
            .collection("routines").document(routineId)

        routineRef.get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    Log.e("RutinFragment", "Routine document does not exist")
                    return@addOnSuccessListener
                }

                val routine = document.toObject(Routine::class.java)
                if (routine == null) {
                    Log.e("RutinFragment", "Routine object is null")
                    return@addOnSuccessListener
                }

                // Inflate the custom layout
                val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_save_routine, null)
                val textInputEditText = dialogView.findViewById<TextInputEditText>(R.id.textInputEditTextNote)
                val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)

                val alertDialog = AlertDialog.Builder(context)
                    .setView(dialogView)
                    .create()

                dialogView.findViewById<Button>(R.id.cancelButton).setOnClickListener {
                    alertDialog.dismiss()
                }

                dialogView.findViewById<Button>(R.id.confirmButton).setOnClickListener {
                    val note = textInputEditText.text.toString()
                    val rating = ratingBar.rating

                    val completedRoutineRef = db.collection("users").document(userId)
                        .collection("completedRoutines").document(routineId)

                    // Calculate total duration in seconds
                    val elapsedMillis = SystemClock.elapsedRealtime() - chronometer.base
                    val durationInSeconds = (elapsedMillis / 1000).toInt()

                    // Convert duration to hh:mm:ss format
                    val durationString = convertSecondsToHms(durationInSeconds)

                    // Create a map to store completed routine details
                    val completedRoutineData = hashMapOf(
                        "routineName" to routine.name,
                        "routineDescription" to routine.description,
                        "status" to "completed",
                        "completionTime" to System.currentTimeMillis(),
                        "durationFormatted" to durationString,
                        "note" to note, // Store the note
                        "rating" to rating // Store the rating
                    )

                    val exercisesCollectionRef = completedRoutineRef.collection("exercises")

                    // Use a batch to add exercises for better performance
                    val batch = db.batch()
                    selectedRoutineExercises.forEach { exercise ->
                        val exerciseData = hashMapOf(
                            "exerciseName" to exercise.name
                        )
                        val exerciseRef = exercisesCollectionRef.document()
                        batch.set(exerciseRef, exerciseData)
                    }

                    // Commit the batch and set the completed routine data in a transaction
                    db.runTransaction { transaction ->
                        transaction.set(completedRoutineRef, completedRoutineData)
                    }.addOnSuccessListener {
                        batch.commit()
                            .addOnSuccessListener {
                                Toast.makeText(context, "Egzersiz rutini tamamlandı ve tamamlanan rutinlere taşındı", Toast.LENGTH_SHORT).show()
                                activity?.finish() // Close the fragment or navigate back
                            }
                            .addOnFailureListener { e ->
                                Log.e("RutinFragment", "Error adding exercises", e)
                                Toast.makeText(context, "Egzersizler eklenirken bir hata oluştu", Toast.LENGTH_SHORT).show()
                            }
                    }.addOnFailureListener { e ->
                        Log.e("RutinFragment", "Error finishing exercise routine", e)
                        Toast.makeText(context, "Egzersiz rutini tamamlanırken bir hata oluştu", Toast.LENGTH_SHORT).show()
                    }

                    alertDialog.dismiss()
                }

                alertDialog.show()
            }
            .addOnFailureListener { e ->
                Log.e("RutinFragment", "Error fetching routine details", e)
                Toast.makeText(context, "Rutin detayları alınırken bir hata oluştu", Toast.LENGTH_SHORT).show()
            }
    }









    private fun convertSecondsToHms(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
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
                Log.e("RutinFragment", "Error updating routine details", e)
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
                    Log.e("RutinFragment", "Error fetching updated routine details", e)
                }
        }
    }

    private fun fetchRoutineExercisesFromFirestore() {
        Log.d("RutinFragment", "Fetching routine exercises")
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
                            Log.d("RutinFragment", "Fetched exercise: ${exercise.name} with ID: ${exercise.id}")
                        }
                        selectedRoutineExercises.clear()
                        selectedRoutineExercises.addAll(routineExercises)
                        exerciseAdapter.notifyDataSetChanged()
                        Log.d("RutinFragment", "RecyclerView updated with ${selectedRoutineExercises.size} exercises")

                        // Fetch sets for each exercise
                        selectedRoutineExercises.forEach { exercise ->
                            fetchExerciseSetsFromFirestore(exercise)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("RutinFragment", "Error fetching routine exercises", e)
                    }
            }
        }
    }

    private fun deleteExercise(exercise: Egzersiz) {
        // Implement your delete logic here
        // For example, show confirmation dialog or directly delete from Firestore
        // Make sure to update UI after deletion

        val builder = AlertDialog.Builder(context, R.style.CustomAlertDialogTheme_Dark)
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
                            Log.e("RutinFragment", "Error deleting exercise", e)
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
        val intent = Intent(context, ChronometerService::class.java)
        context?.stopService(intent)
    }
    companion object {
        @JvmStatic
        fun newInstance(routineId: String, elapsedTime: Long) =
            RutinFragment().apply {
                arguments = Bundle().apply {
                    putString("routineId", routineId)
                    putLong("elapsedTime", elapsedTime)
                }
            }
    }
}
