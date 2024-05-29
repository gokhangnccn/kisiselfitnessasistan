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
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class Profile : Fragment() {

    private lateinit var profileImageView: ImageView
    private lateinit var changePhotoButton: Button
    private lateinit var aboutEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var ageSpinner: Spinner
    private lateinit var weightSpinner: Spinner
    private lateinit var heightSpinner: Spinner

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var currentUser: FirebaseUser? = null

    val MAX_AGE = 200
    val MAX_WEIGHT = 300
    val MAX_HEIGHT = 250
    private val PICK_IMAGE_REQUEST = 71

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        currentUser = firebaseAuth.currentUser

        if (currentUser == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
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
        heightSpinner = view.findViewById(R.id.heightSpinner)
        aboutEditText = view.findViewById(R.id.aboutEditText)
        saveButton = view.findViewById(R.id.saveButton)

        // Yaş spinner'ı için adaptör oluşturma
        val ageList = (1..MAX_AGE).map { it.toString() }.toMutableList()
        ageList.add(0, "Yaş")
        val ageAdapter = object : ArrayAdapter<String>(requireContext(), R.layout.spinner_item, ageList) {
            override fun isEnabled(position: Int): Boolean {
                return position != 0
            }
        }
        ageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ageSpinner.adapter = ageAdapter

        // Kilo spinner'ı için adaptör oluşturma
        val weightList = (1..MAX_WEIGHT).map { it.toString() }.toMutableList()
        weightList.add(0, "Kilo") // Hint olarak görünen öğe
        val weightAdapter = object : ArrayAdapter<String>(requireContext(), R.layout.spinner_item, weightList) {
            override fun isEnabled(position: Int): Boolean {
                return position != 0
            }
        }
        weightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        weightSpinner.adapter = weightAdapter


        // Boy spinner'ı için adaptör oluşturma
        val heightList = (1..MAX_WEIGHT).map { it.toString() }.toMutableList()
        heightList.add(0, "Boy") // Hint olarak görünen öğe
        val heightAdapter = object : ArrayAdapter<String>(requireContext(), R.layout.spinner_item, heightList) {
            override fun isEnabled(position: Int): Boolean {
                return position != 0
            }
        }
        heightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        heightSpinner.adapter = heightAdapter


        changePhotoButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        saveButton.setOnClickListener {
            saveUserInfo()
        }

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
                    val age = document.getString("age")?.toInt() ?: 0
                    val weight = document.getString("weight")?.toInt() ?: 0
                    val height = document.getString("height")?.toInt() ?: 0

                    ageSpinner.setSelection((1..MAX_AGE).indexOf(age) + 1)
                    weightSpinner.setSelection((1..MAX_WEIGHT).indexOf(weight) + 1)
                    heightSpinner.setSelection((1..MAX_HEIGHT).indexOf(height) + 1)

                    aboutEditText.setText(document.getString("about"))

                    val profileImageUrl = document.getString("profileImageUrl")
                    if (!profileImageUrl.isNullOrEmpty()) {
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
        val height = heightSpinner.selectedItem.toString()
        val about = aboutEditText.text.toString()

        if (age == "Yaş" || weight == "Kilo" || height =="Boy" || about.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill out all fields", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("users").document(currentUser!!.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val currentProfileImageUrl = document.getString("profileImageUrl")

                    val userMap = hashMapOf(
                        "age" to age,
                        "weight" to weight,
                        "height" to height,
                        "about" to about,
                        "profileImageUrl" to currentProfileImageUrl
                    )

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
                startCropActivity(selectedImageUri)
            }
        } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == Activity.RESULT_OK) {
                val resultUri: Uri = result.uri
                profileImageView.setImageURI(resultUri)
                uploadImageToFirebase(resultUri)
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                val error = result.error
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startCropActivity(imageUri: Uri) {
        val intent = CropImage.activity(imageUri)
            .setAspectRatio(1, 1)
            .setGuidelines(CropImageView.Guidelines.ON)
            .getIntent(requireContext())
        startActivityForResult(intent, CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE)
    }

    private fun uploadImageToFirebase(imageUri: Uri) {
        val ref = storage.reference.child("profileImages/${currentUser!!.uid}")
        ref.putFile(imageUri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    firestore.collection("users").document(currentUser!!.uid)
                        .update("profileImageUrl", uri.toString())
                        .addOnSuccessListener {
                            Toast.makeText(context, "Profile photo updated", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(context, "Failed to update profile photo: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Failed to upload profile photo: ${exception.message}", Toast.LENGTH_SHORT).show()
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
