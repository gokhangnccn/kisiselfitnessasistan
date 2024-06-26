package com.gokhan.kfa

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.gokhan.kfa.databinding.DialogEgzersizDetayBinding
import model.Egzersiz

object DialogUtils {
    fun showExerciseDetailsDialog(context: Context, exercise: Egzersiz) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_egzersiz_detay, null)
        val dialogBinding = DialogEgzersizDetayBinding.bind(dialogView)

        dialogBinding.tvExerciseName.text = exercise.name
        dialogBinding.tvExerciseDescription.text = exercise.description
        dialogBinding.tvTargetMuscles.text = "Hedef Kaslar: ${exercise.targetMuscleGroups?.joinToString(", ")}"
        dialogBinding.tvSecondaryMuscles.text = "Yardımcı Kaslar: ${exercise.secondaryTargetMuscleGroups?.joinToString(", ")}"
        dialogBinding.tvEquipment.text = "Ekipman: ${exercise.equipment}"
        dialogBinding.tvHowTo.text = "Nasıl Yapılır: ${exercise.nasilYapilir}"

        val width = 300
        val height = 300

        Glide.with(context)
            .asGif()
            .load(exercise.gifUrl)
            .apply(
                RequestOptions()
                    .override(width, height)
                    .fitCenter()
                    .placeholder(R.drawable.gymicon)
                    .error(R.drawable.baseline_image_not_supported_24)
            )
            .into(dialogBinding.ivExerciseGif)

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton("Tamam") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.setOnShowListener {
            val positiveButton = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setTextColor(ContextCompat.getColor(context, R.color.c4))
        }

        dialog.show()

    }
}

