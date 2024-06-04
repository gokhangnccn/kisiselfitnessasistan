package com.gokhan.kfa

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EgzersizAdapter(private val exercises: MutableList<Egzersiz>) : RecyclerView.Adapter<EgzersizAdapter.ExerciseViewHolder>() {

    private val selectedExercises = mutableSetOf<Egzersiz>()

    class ExerciseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val exerciseName: TextView = itemView.findViewById(R.id.tv_exercise_name)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkbox_select)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_egzersiz, parent, false)
        return ExerciseViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        val currentExercise = exercises[position]
        holder.exerciseName.text = currentExercise.name
        holder.checkBox.isChecked = selectedExercises.contains(currentExercise)
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedExercises.add(currentExercise)
            } else {
                selectedExercises.remove(currentExercise)
            }
        }
    }

    override fun getItemCount() = exercises.size

    fun getSelectedExercises(): List<Egzersiz> = selectedExercises.toList()

    fun setExercises(exercises: List<Egzersiz>) {
        this.exercises.clear()
        this.exercises.addAll(exercises)
        notifyDataSetChanged()
    }
}
