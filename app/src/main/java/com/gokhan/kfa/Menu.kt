package com.gokhan.kfa

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class Menu : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_menu, container, false)

        // Find the button and set an OnClickListener
        val buttonCikis: Button = view.findViewById(R.id.button_cikis)
        buttonCikis.setOnClickListener {
            // Log out from Firebase
            FirebaseAuth.getInstance().signOut()

            // Navigate to the login screen
            val intent = Intent(activity, GirisEkrani::class.java)
            startActivity(intent)
            activity?.finish() // Finish the current activity to remove it from the back stack
        }

        val db = Firebase.firestore

        val editTextName = view.findViewById<EditText>(R.id.editTextName)
        val editTextDescription = view.findViewById<EditText>(R.id.editTextDescription)
        val editTextEquipment = view.findViewById<EditText>(R.id.editTextEquipment)
        val editTextNasilYapilir = view.findViewById<EditText>(R.id.editTextNasilYapilir)
        val editTextTargetMuscleGroups = view.findViewById<EditText>(R.id.editTextTargetMuscleGroups)
        val editTextSecondaryTargetMuscleGroups = view.findViewById<EditText>(R.id.editTextSecondaryTargetMuscleGroups)
        val editTextGifUrl = view.findViewById<EditText>(R.id.editTextGifUrl)
        val buttonEkle = view.findViewById<Button>(R.id.buttonEkle)

        buttonEkle.setOnClickListener {
            val name = editTextName.text.toString()
            val description = editTextDescription.text.toString()
            val equipment = editTextEquipment.text.toString()
            val nasilYapilir = editTextNasilYapilir.text.toString()
            val targetMuscleGroups = editTextTargetMuscleGroups.text.toString().split(",").map { it.trim() }
            val secondaryTargetMuscleGroups = editTextSecondaryTargetMuscleGroups.text.toString().split(",").map { it.trim() }
            val gifUrl = editTextGifUrl.text.toString()

            db.collection("exercises").orderBy("id").get().addOnSuccessListener { querySnapshot ->
                val lastId = querySnapshot.documents.lastOrNull()?.id?.toIntOrNull() ?: 0
                val newId = String.format("%04d", lastId + 1)

                val egzersiz = Egzersiz(
                    id = newId,
                    name = name,
                    description = description,
                    equipment = equipment,
                    nasilYapilir = nasilYapilir,
                    targetMuscleGroups = targetMuscleGroups,
                    secondaryTargetMuscleGroups = secondaryTargetMuscleGroups,
                    gifUrl = gifUrl
                )

                db.collection("exercises")
                    .document(egzersiz.id)
                    .set(egzersiz)
                    .addOnSuccessListener {
                        println("Egzersiz başarıyla eklendi. Document ID: ${egzersiz.id}")
                        // Ekledikten sonra alanları temizle
                        editTextName.setText("")
                        editTextDescription.setText("")
                        editTextEquipment.setText("")
                        editTextNasilYapilir.setText("")
                        editTextTargetMuscleGroups.setText("")
                        editTextSecondaryTargetMuscleGroups.setText("")
                        editTextGifUrl.setText("")
                    }
                    .addOnFailureListener { e ->
                        println("Egzersiz eklenirken bir hata oluştu: $e")
                    }
            }
        }

        return view
    }

    companion object {
        @JvmStatic
        fun newInstance() = Menu()
    }
}
