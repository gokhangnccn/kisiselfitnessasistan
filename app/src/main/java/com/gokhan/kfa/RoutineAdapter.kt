package com.gokhan.kfa

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RoutineAdapter(
    private val routines: List<Routine>,
    private val onRoutineSelected: (Routine) -> Unit,
    private val onStartRoutine: (Routine) -> Unit
) : RecyclerView.Adapter<RoutineAdapter.RoutineViewHolder>() {

    class RoutineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val routineName: TextView = itemView.findViewById(R.id.tv_routine_name)
        val btnStartRoutine: Button = itemView.findViewById(R.id.btn_start_routine)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoutineViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_routine, parent, false)
        return RoutineViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: RoutineViewHolder, position: Int) {
        val currentRoutine = routines[position]
        holder.routineName.text = currentRoutine.name
        holder.itemView.setOnClickListener {
            onRoutineSelected(currentRoutine)
        }
        holder.btnStartRoutine.setOnClickListener {
            onStartRoutine(currentRoutine)
        }
    }

    override fun getItemCount() = routines.size
}
