// EgzersizSecimAdapter.kt
package com.gokhan.kfa

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class EgzersizSecimAdapter(
    private val exercises: List<Egzersiz>,
    private val onExerciseSelected: (Egzersiz, Boolean) -> Unit
) : RecyclerView.Adapter<EgzersizSecimAdapter.EgzersizSecimViewHolder>() {

    private val selectedExercises = mutableSetOf<Egzersiz>()

    class EgzersizSecimViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val exerciseName: TextView = itemView.findViewById(R.id.tv_exercise_name)
        val exerciseDescription: TextView = itemView.findViewById(R.id.tv_exercise_description)
        val exerciseTargetMuscles: TextView = itemView.findViewById(R.id.tv_target_muscles)
        val checkBox: CheckBox = itemView.findViewById(R.id.cb_select_exercise)
        val exerciseGif: ImageView = itemView.findViewById(R.id.iv_gif)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EgzersizSecimViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_egzersiz_secim, parent, false)
        return EgzersizSecimViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: EgzersizSecimViewHolder, position: Int) {
        val currentExercise = exercises[position]
        holder.exerciseName.text = currentExercise.name
        holder.exerciseDescription.text = currentExercise.description
        holder.exerciseTargetMuscles.text = "Hedef Kaslar: " + currentExercise.targetMuscleGroups?.joinToString(", ")

        Glide.with(holder.itemView.context)
            .load(currentExercise.gifUrl)
            .placeholder(R.drawable.gymicon)
            .into(holder.exerciseGif)

        holder.checkBox.isChecked = selectedExercises.contains(currentExercise)

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedExercises.add(currentExercise)
            } else {
                selectedExercises.remove(currentExercise)
            }
            onExerciseSelected(currentExercise, isChecked)
        }
    }

    override fun getItemCount() = exercises.size
}
