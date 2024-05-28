package com.gokhan.kfa

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import android.widget.Spinner


class Profile : Fragment() {

    private lateinit var profileImageView: ImageView
    private lateinit var changePhotoButton: Button
    private lateinit var aboutEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var ageSpinner: Spinner
    private lateinit var weightSpinner: Spinner


    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var currentUser: FirebaseUser? = null



    val MAX_AGE = 200
    val MAX_WEIGHT = 300
    val weightArray = Array(MAX_WEIGHT) { (it + 1).toString() }
    val ageArray = Array(MAX_AGE) { (it + 1).toString() }
    private val PICK_IMAGE_REQUEST = 71

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        currentUser = firebaseAuth.currentUser

        if (currentUser == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
            // Optionally, redirect to login screen or handle the case as needed
        } else {
            loadUserData()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        profileImageView = view.findViewById(R.id.profileImageView)
        changePhotoButton = view.findViewById(R.id.changePhotoButton)
        ageSpinner = view.findViewById(R.id.ageSpinner)
        weightSpinner = view.findViewById(R.id.weightSpinner)
        aboutEditText = view.findViewById(R.id.aboutEditText)
        saveButton = view.findViewById(R.id.saveButton)

        // Adapter'ları oluştur
        val ageAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, (1..MAX_AGE).map { it.toString() })
        ageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        val weightAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, (1..MAX_WEIGHT).map { it.toString() })
        weightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        ageSpinner.adapter = ageAdapter
        weightSpinner.adapter = weightAdapter


        aboutEditText = view.findViewById(R.id.aboutEditText)
        saveButton = view.findViewById(R.id.saveButton)

        changePhotoButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        saveButton.setOnClickListener {
            saveUserInfo()
        }

        // loadUserData() fonksiyonunu kaldır
        // loadUserData()

        return view
    }



    private fun loadUserData() {
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("users").document(currentUser!!.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val ageIndex = ageArray.indexOf(document.getString("age"))
                    val weightIndex = weightArray.indexOf(document.getString("weight"))

                    // Age ve weight değerlerini uygun spinner öğelerine ayarla
                    ageSpinner.setSelection(if (ageIndex != -1) ageIndex else 0)
                    weightSpinner.setSelection(if (weightIndex != -1) weightIndex else 0)

                    aboutEditText.setText(document.getString("about"))

                    val profileImageUrl = document.getString("profileImageUrl")
                    if (!profileImageUrl.isNullOrEmpty()) {
                        // AsyncTask'i kullanarak profil resmini yükle
                        ImageLoaderTask(profileImageView).execute(profileImageUrl)
                    }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Failed to load user data: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }






    private fun saveUserInfo() {
        val age = ageSpinner.selectedItem.toString()
        val weight = weightSpinner.selectedItem.toString()
        val about = aboutEditText.text.toString()

        if (age.isEmpty() || weight.isEmpty() || about.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill out all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Get the current user document
        firestore.collection("users").document(currentUser!!.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val currentProfileImageUrl = document.getString("profileImageUrl")

                    // Create a map of the user information to update
                    val userMap = hashMapOf(
                        "age" to age,
                        "weight" to weight,
                        "about" to about,
                        "profileImageUrl" to currentProfileImageUrl  // Retain the existing profile image URL
                    )

                    // Update the user document
                    firestore.collection("users").document(currentUser!!.uid).set(userMap)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(requireContext(), "Failed to update profile: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Failed to load current user data: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }




    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            val selectedImageUri: Uri? = data?.data
            if (selectedImageUri != null) {
                profileImageView.setImageURI(selectedImageUri)
                uploadImageToFirebase(selectedImageUri)
            }
        }
    }

    private fun uploadImageToFirebase(imageUri: Uri) {
        val ref = storage.reference.child("profileImages/${currentUser!!.uid}")
        ref.putFile(imageUri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    firestore.collection("users").document(currentUser!!.uid)
                        .update("profileImageUrl", uri.toString())
                        .addOnSuccessListener {
                            // Context null kontrolü ekleniyor
                            if (context != null) {
                                Toast.makeText(context, "Profile photo updated", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { exception ->
                            // Context null kontrolü ekleniyor
                            if (context != null) {
                                Toast.makeText(context, "Failed to update profile photo: ${exception.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }
            .addOnFailureListener { exception ->
                // Context null kontrolü ekleniyor
                if (context != null) {
                    Toast.makeText(context, "Failed to upload profile photo: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }


    companion object {
        @JvmStatic
        fun newInstance() = Profile()
    }

    private class ImageLoaderTask(private val imageView: ImageView) : AsyncTask<String, Void, Bitmap?>() {
        override fun doInBackground(vararg params: String?): Bitmap? {
            val url = params[0]
            return try {
                val connection: HttpURLConnection = URL(url).openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input: InputStream = connection.inputStream
                BitmapFactory.decodeStream(input)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        override fun onPostExecute(result: Bitmap?) {
            if (result != null) {
                imageView.setImageBitmap(result)
            }
        }
    }
}
