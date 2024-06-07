package com.gokhan.kfa

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EgzersizAdapter(
    private val exercises: MutableList<Egzersiz>,
    private val onExerciseClick: (Egzersiz) -> Unit
) : RecyclerView.Adapter<EgzersizAdapter.ExerciseViewHolder>() {

    private val selectedExercises = mutableSetOf<Egzersiz>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_egzersiz, parent, false)
        return ExerciseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        val exercise = exercises[position]
        holder.bind(exercise)
        holder.itemView.setOnClickListener {
            onExerciseClick(exercise)
            toggleSelection(exercise)
            notifyItemChanged(position)
        }
    }

    override fun getItemCount() = exercises.size

    private fun toggleSelection(exercise: Egzersiz) {
        if (selectedExercises.contains(exercise)) {
            selectedExercises.remove(exercise)
        } else {
            selectedExercises.add(exercise)
        }
    }

    fun getSelectedExercises(): List<Egzersiz> {
        return selectedExercises.toList()
    }

    fun updateExercises(newExercises: List<Egzersiz>) {
        exercises.clear()
        exercises.addAll(newExercises)
        notifyDataSetChanged()
    }

    class ExerciseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvExerciseName: TextView = itemView.findViewById(R.id.tv_exercise_name)
        private val tvExerciseDescription: TextView = itemView.findViewById(R.id.tv_exercise_description)

        fun bind(exercise: Egzersiz) {
            tvExerciseName.text = exercise.name
            tvExerciseDescription.text = exercise.description
        }
    }
}
