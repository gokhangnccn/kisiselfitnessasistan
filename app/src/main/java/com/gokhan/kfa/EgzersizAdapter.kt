package com.gokhan.kfa

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.gokhan.kfa.databinding.ItemEgzersizSecimBinding

class EgzersizAdapter(
    private val exercises: MutableList<Egzersiz>,
    private val onExerciseClicked: (Egzersiz) -> Unit
) : RecyclerView.Adapter<EgzersizAdapter.EgzersizViewHolder>() {

    private val selectedExercises = mutableSetOf<Egzersiz>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EgzersizViewHolder {
        val binding = ItemEgzersizSecimBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EgzersizViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EgzersizViewHolder, position: Int) {
        val exercise = exercises[position]
        holder.bind(exercise)
        holder.itemView.setOnClickListener {
            onExerciseClicked(exercise)
        }
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedExercises.add(exercise)
            } else {
                selectedExercises.remove(exercise)
            }
        }
    }

    override fun getItemCount() = exercises.size

    fun getSelectedExercises(): List<Egzersiz> {
        return selectedExercises.toList()
    }

    fun updateExercises(newExercises: List<Egzersiz>) {
        exercises.clear()
        exercises.addAll(newExercises)
        notifyDataSetChanged()
    }


    class EgzersizViewHolder(private val binding: ItemEgzersizSecimBinding) : RecyclerView.ViewHolder(binding.root) {
        val checkBox: CheckBox = binding.cbSelectExercise

        fun bind(exercise: Egzersiz) {
            binding.tvExerciseName.text = exercise.name
            binding.tvExerciseDescription.text = exercise.description
            binding.tvTargetMuscles.text = "Hedef Kaslar: ${exercise.targetMuscleGroups?.joinToString(", ")}"

            val width = 300
            val height = 300

            Glide.with(binding.root)
                .asGif()
                .load(exercise.gifUrl)
                .apply(
                    RequestOptions()
                        .override(width, height)
                        .fitCenter()
                )
                .into(binding.ivExerciseIcon)

        }

    }
}



