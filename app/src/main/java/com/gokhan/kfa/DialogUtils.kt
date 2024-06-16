package com.gokhan.kfa

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView

object DialogUtils {
    fun showOverlayDialog(context: Context, onContinue: () -> Unit, onFinish: () -> Unit) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.overlay_dialog, null)
        val continueButton = dialogView.findViewById<Button>(R.id.btn_continue)
        val finishButton = dialogView.findViewById<Button>(R.id.btn_finish)

        val builder = AlertDialog.Builder(context)
        builder.setView(dialogView)
            .setCancelable(false)

        val dialog = builder.create()

        continueButton.setOnClickListener {
            onContinue()
            dialog.dismiss()
        }

        finishButton.setOnClickListener {
            onFinish()
            dialog.dismiss()
        }

        dialog.show()
    }

    fun showExerciseDetailsDialog(context: Context, exercise: Egzersiz) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_egzersiz_detay, null)
        val exerciseName = dialogView.findViewById<TextView>(R.id.tv_exercise_name)
        val exerciseDescription = dialogView.findViewById<TextView>(R.id.tv_exercise_description)

        exerciseName.text = exercise.name
        exerciseDescription.text = exercise.description

        val builder = AlertDialog.Builder(context)
        builder.setView(dialogView)
            .setTitle("Egzersiz Detayları")
            .setPositiveButton("Kapat") { dialog, _ ->
                dialog.dismiss()
            }

        val dialog = builder.create()
        dialog.show()
    }
}

