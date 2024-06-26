package ui

import adapter.RoutineAdapter
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
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.gokhan.kfa.R
import com.gokhan.kfa.databinding.FragmentAntrenmanBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import model.Routine
import viewModel.RoutineViewModel

class AntrenmanFragment : Fragment() {

    private var _binding: FragmentAntrenmanBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private val routines = mutableListOf<Routine>()
    private lateinit var routineAdapter: RoutineAdapter
    private var userId: String? = null
    private lateinit var auth: FirebaseAuth

    private val routineViewModel: RoutineViewModel by activityViewModels()



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAntrenmanBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser?.uid


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()

    }


    private fun init() {
        binding.rvRoutines.layoutManager = LinearLayoutManager(context)
        routineAdapter = RoutineAdapter(routines, this::onStartRoutineClicked)
        binding.rvRoutines.adapter = routineAdapter

        binding.btnAddRoutine.setOnClickListener {
            showCreateRoutineDialog()
        }

        binding.btnDeleteRoutine.setOnClickListener {
            deleteSelectedRoutines()
        }

        fetchRoutinesFromFirestore()
    }

    private fun onStartRoutineClicked(routine: Routine) {
        if (routineViewModel.isRoutineActive()) {
            showRoutineActiveWarning(routine)
        } else {
            routineViewModel.startRoutine(routine.id)
            val elapsedTime = routineViewModel.elapsedTime.value ?: 0L
            fragmentManager?.beginTransaction()
                ?.replace(R.id.frame_layout, RutinFragment.newInstance(routine.id, elapsedTime))
                ?.addToBackStack(null)
                ?.commit()
        }
    }

    private fun showRoutineActiveWarning(routine: Routine) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_warning, null)

        // Dialog oluştur
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Dialog içindeki bileşenlere erişim sağla
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_iptal)
        val btnOk = dialogView.findViewById<Button>(R.id.btn_tamam)

        // İptal butonuna tıklama olayı ekle
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        // Tamam butonuna tıklama olayı ekle
        btnOk.setOnClickListener {
            if (routineViewModel.isRoutineActive()) {
                val activeRoutineId = routineViewModel.currentRoutineId
                if (activeRoutineId != null) {
                    val elapsedTime = routineViewModel.elapsedTime.value ?: 0L
                    val fragment = RutinFragment.newInstance(activeRoutineId, elapsedTime)
                    fragmentManager?.beginTransaction()
                        ?.replace(R.id.frame_layout, fragment)
                        ?.addToBackStack(null)
                        ?.commit()
                } else {
                    Toast.makeText(requireContext(), "Aktif bir rutin bulunamadı", Toast.LENGTH_SHORT).show()
                }
            }
            dialog.dismiss()
        }

        // Dialogu göster
        dialog.show()
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
        val routine = Routine(name = routineName, description = routineDescription)

        userId?.let { userId ->
            db.collection("users").document(userId)
                .collection("routines").add(routine)
                .addOnSuccessListener {
                    Toast.makeText(context, "Rutin başarıyla oluşturuldu", Toast.LENGTH_SHORT)
                        .show()
                    fetchRoutinesFromFirestore()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        context,
                        "Rutin oluşturulurken hata oluştu: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("AntrenmanFragment", "Error creating routine", e)
                }
        }
    }

    private fun fetchRoutinesFromFirestore() {
        userId?.let { userId ->
            db.collection("users").document(userId)
                .collection("routines").get()
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
                    Log.w("AntrenmanFragment", "Error fetching routines", e)
                }
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
            val routineRef = db.collection("users").document(userId!!)
                .collection("routines").document(routine.id)
            batch.delete(routineRef)
        }

        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(context, "Rutinler başarıyla silindi", Toast.LENGTH_SHORT).show()
                fetchRoutinesFromFirestore()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Rutinler silinirken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("AntrenmanFragment", "Error deleting routines", e)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): AntrenmanFragment {
            return AntrenmanFragment()
        }
    }
}
