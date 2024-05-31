package com.gokhan.kfa

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
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
import com.bumptech.glide.Glide
import com.google.firebase.firestore.SetOptions


class Profile : Fragment() {

    private lateinit var profileImageView: ImageView
    private lateinit var changePhotoButton: Button
    private lateinit var aboutEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var ageSpinner: Spinner
    private lateinit var weightSpinner: Spinner
    private lateinit var heightSpinner: Spinner
    private lateinit var nameEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var genderSpinner: Spinner
    private lateinit var emailEditText: EditText
    private lateinit var changePasswordButton: Button

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var currentUser: FirebaseUser? = null

    private var selectedImageUri: Uri? = null
    private lateinit var genderAdapter: ArrayAdapter<CharSequence>

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
        aboutEditText = view.findViewById(R.id.aboutEditText)
        saveButton = view.findViewById(R.id.saveButton)
        ageSpinner = view.findViewById(R.id.ageSpinner)
        weightSpinner = view.findViewById(R.id.weightSpinner)
        heightSpinner = view.findViewById(R.id.heightSpinner)
        nameEditText = view.findViewById(R.id.nameEditText)
        usernameEditText = view.findViewById(R.id.usernameEditText)
        genderSpinner = view.findViewById(R.id.genderSpinner)
        emailEditText = view.findViewById(R.id.emailEditText)
        changePasswordButton = view.findViewById(R.id.changePasswordButton)

        setupSpinners()
        populateUserData()
        setupButtonListeners()

        return view
    }

    private fun setupSpinners() {
        // Age Spinner
        val ageList = ArrayList<Int>()
        for (i in 15..80) {
            ageList.add(i)
        }
        val ageAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, ageList)
        ageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ageSpinner.adapter = ageAdapter

        // Weight Spinner
        val weightList = ArrayList<Int>()
        for (i in 30..150) {
            weightList.add(i)
        }
        val weightAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, weightList)
        weightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        weightSpinner.adapter = weightAdapter

        // Height Spinner
        val heightList = ArrayList<Int>()
        for (i in 100..250) {
            heightList.add(i)
        }
        val heightAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, heightList)
        heightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        heightSpinner.adapter = heightAdapter

        // Gender Spinner
        genderAdapter = ArrayAdapter.createFromResource(requireContext(), R.array.gender_array, android.R.layout.simple_spinner_item)
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        genderSpinner.adapter = genderAdapter
    }


    private fun populateUserData() {
        currentUser?.let { user ->
            emailEditText.setText(user.email)

            firestore.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val about = document.getString("about") ?: ""
                        val age = (document.get("age") as? Number)?.toInt() ?: 15
                        val weight = (document.get("weight") as? Number)?.toInt() ?: 30
                        val height = (document.get("height") as? Number)?.toInt() ?: 100
                        val name = document.getString("name") ?: ""
                        val username = document.getString("username") ?: ""
                        val gender = document.getString("gender") ?: ""

                        aboutEditText.setText(about)
                        ageSpinner.setSelection(age - 15)
                        weightSpinner.setSelection(weight - 30)
                        heightSpinner.setSelection(height - 100)
                        nameEditText.setText(name)
                        usernameEditText.setText(username)
                        genderSpinner.setSelection((genderAdapter.getPosition(gender)))

                        val profileImageUrl = document.getString("profileImageUrl")
                        if (!profileImageUrl.isNullOrEmpty()) {
                            Glide.with(requireContext()).load(profileImageUrl).into(profileImageView)
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Failed to load user data: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }


    private fun setupButtonListeners() {
        changePhotoButton.setOnClickListener {
            openGallery()
        }

        changePasswordButton.setOnClickListener {
            // Handle password change logic
            // This can involve sending a password reset email or opening a new fragment/dialog for password change
        }

        saveButton.setOnClickListener {
            saveUserData()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 1000)
    }

    private fun loadUserData() {
        currentUser?.let { user ->
            firestore.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val age = document.getLong("age")?.toString()?.toIntOrNull() ?: 0
                        val weight = document.getLong("weight")?.toString()?.toIntOrNull() ?: 0
                        val height = document.getLong("height")?.toString()?.toIntOrNull() ?: 0

                        aboutEditText.setText(document.getString("about") ?: "")

                        ageSpinner.setSelection((1..MAX_AGE).indexOf(age) + 1)
                        weightSpinner.setSelection((1..MAX_WEIGHT).indexOf(weight) + 1)
                        heightSpinner.setSelection((1..MAX_HEIGHT).indexOf(height) + 1)

                        val profileImageUrl = document.getString("profileImageUrl")
                        if (!profileImageUrl.isNullOrEmpty()) {
                            Glide.with(requireContext()).load(profileImageUrl).into(profileImageView)
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Failed to load user data: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }



    private fun saveUserInfo() {
        val age = ageSpinner.selectedItem.toString()
        val weight = weightSpinner.selectedItem.toString()
        val height = heightSpinner.selectedItem.toString()
        val about = aboutEditText.text.toString()

        if (age == "Yaş" || weight == "Kilo" || height == "Boy" || about.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill out all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val userInfo = mapOf(
            "age" to age,
            "weight" to weight,
            "height" to height,
            "about" to about
        )

        firestore.collection("users").document(currentUser!!.uid)
            .set(userInfo)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Failed to update profile: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveUserData() {
        // EditText alanlarındaki metinleri al
        val about = aboutEditText.text.toString().trim()
        val name = nameEditText.text.toString().trim()
        val username = usernameEditText.text.toString().trim()

        // Tüm gerekli alanların doldurulup doldurulmadığını kontrol et
        var allFieldsFilled = true
        if (about.isEmpty()) {
            aboutEditText.background = getDrawableWithBorder(Color.RED)
            aboutEditText.setHintTextColor(Color.RED)
            allFieldsFilled = false
        } else {
            aboutEditText.background = getDrawableWithBorder(Color.TRANSPARENT)
            aboutEditText.setHintTextColor(null)
        }

        if (name.isEmpty()) {
            nameEditText.background = getDrawableWithBorder(Color.RED)
            nameEditText.setHintTextColor(Color.RED)
            allFieldsFilled = false
        } else {
            nameEditText.background = getDrawableWithBorder(Color.TRANSPARENT)
            nameEditText.setHintTextColor(null)
        }

        if (username.isEmpty()) {
            usernameEditText.background = getDrawableWithBorder(Color.RED)
            usernameEditText.setHintTextColor(Color.RED)
            allFieldsFilled = false
        } else {
            usernameEditText.background = getDrawableWithBorder(Color.TRANSPARENT)
            usernameEditText.setHintTextColor(null)
        }

        // Tüm gerekli alanlar doldurulmuşsa kullanıcı bilgilerini kaydet
        if (allFieldsFilled) {
            val age = ageSpinner.selectedItem.toString().toInt()
            val weight = weightSpinner.selectedItem.toString().toInt()
            val height = heightSpinner.selectedItem.toString().toInt()
            val gender = genderSpinner.selectedItem.toString()

            currentUser?.let { user ->
                val userData = hashMapOf(
                    "about" to about,
                    "age" to age,
                    "weight" to weight,
                    "height" to height,
                    "name" to name,
                    "username" to username,
                    "gender" to gender
                )

                firestore.collection("users").document(user.uid).set(userData, SetOptions.merge())
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Veriler kaydedildi", Toast.LENGTH_SHORT).show()
                        uploadProfileImage() // Kullanıcı verileri başarıyla kaydedildikten sonra profil resmini yükle
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Veriler kaydedilemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        } else {
            Toast.makeText(requireContext(), "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getDrawableWithBorder(color: Int): Drawable {
        val border = GradientDrawable()
        border.setColor(Color.TRANSPARENT) // Arka plan rengi
        border.setStroke(1, color) // Çerçeve rengi ve kalınlığı
        return border
    }



    private fun uploadProfileImage() {
        selectedImageUri?.let { uri ->
            currentUser?.let { user ->
                val storageRef = storage.reference.child("profileImages/${user.uid}")
                val uploadTask = storageRef.putFile(uri)

                uploadTask.continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let { throw it }
                    }
                    storageRef.downloadUrl
                }.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val downloadUri = task.result
                        firestore.collection("users").document(user.uid)
                            .update("profileImageUrl", downloadUri.toString())
                            .addOnSuccessListener {
                                if (isAdded) { // Check if the fragment is attached to its context
                                    Toast.makeText(context, "Profile photo updated", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener { exception ->
                                if (isAdded) { // Check if the fragment is attached to its context
                                    Toast.makeText(context, "Failed to update profile photo: ${exception.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    } else {
                        if (isAdded) { // Check if the fragment is attached to its context
                            Toast.makeText(context, "Failed to upload profile photo", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1000 && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            profileImageView.setImageURI(selectedImageUri)
            startCropActivity(selectedImageUri!!)
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
