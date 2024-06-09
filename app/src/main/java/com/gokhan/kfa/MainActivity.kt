package com.gokhan.kfa

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.gokhan.kfa.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Base_Theme_KFA)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu -> {
                    replaceFragment(Menu.newInstance())
                    true
                }
                R.id.profile -> {
                    replaceFragment(Profile.newInstance())
                    true
                }
                R.id.antrenman -> {
                    replaceFragment(Antrenman.newInstance())
                    true
                }
                else -> false
            }
        }

        if (savedInstanceState == null) {
            binding.bottomNavigationView.selectedItemId = R.id.menu
        }

        // Excel dosyasını assets klasöründen oku
        val egzersizList = readExcelFileFromAssets("egzersizler.xlsx")

        // Verileri Firestore'a yükle
        uploadDataToFirestore(egzersizList)
    }

    private fun readExcelFileFromAssets(fileName: String): List<Egzersiz> {
        val egzersizList = mutableListOf<Egzersiz>()

        try {
            val assetManager = assets
            val inputStream: InputStream = assetManager.open(fileName)
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)

            for (row in sheet) {
                val id = String.format("%04d", row.getCell(0).numericCellValue.toInt())
                val name = row.getCell(1).stringCellValue
                val description = row.getCell(2).stringCellValue
                val equipment = row.getCell(3).stringCellValue
                val nasilYapilir = row.getCell(4).stringCellValue
                val targetMuscleGroups = row.getCell(5).stringCellValue.split(",").map { it.trim() }
                val secondaryTargetMuscleGroups = row.getCell(6).stringCellValue.split(",").map { it.trim() }
                val gifUrl = row.getCell(7).stringCellValue

                val egzersiz = Egzersiz(
                    id = id,
                    name = name,
                    description = description,
                    equipment = equipment,
                    nasilYapilir = nasilYapilir,
                    targetMuscleGroups = targetMuscleGroups,
                    secondaryTargetMuscleGroups = secondaryTargetMuscleGroups,
                    gifUrl = gifUrl
                )

                egzersizList.add(egzersiz)
                Log.d("ExcelReader", "Egzersiz eklendi: $egzersiz")
            }

            workbook.close()
            inputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("ExcelReader", "Hata oluştu: ${e.message}")
        }

        return egzersizList
    }


    private fun uploadDataToFirestore(egzersizList: List<Egzersiz>) {
        val db = FirebaseFirestore.getInstance()
        val collectionRef = db.collection("exercises")

        db.collection("exercises").get().addOnSuccessListener { querySnapshot ->
            val existingIds = querySnapshot.documents.map { it.id }.toSet()

            for (egzersiz in egzersizList) {
                val formattedId = String.format("%04d", egzersiz.id.toInt())

                if (formattedId !in existingIds) {
                    collectionRef.document(formattedId).set(egzersiz)
                        .addOnSuccessListener {
                            Log.d("Firestore", "DocumentSnapshot successfully written with ID: $formattedId")
                        }
                        .addOnFailureListener { e ->
                            Log.w("Firestore", "Error writing document", e)
                        }
                } else {
                    Log.d("Firestore", "Egzersiz zaten var, eklenmedi: $formattedId")
                }
            }
        }
    }



    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout, fragment)
        fragmentTransaction.commit()
    }
}
