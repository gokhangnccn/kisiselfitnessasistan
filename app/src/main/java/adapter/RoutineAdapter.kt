package adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gokhan.kfa.R
import model.Routine

class RoutineAdapter(
    private val routines: MutableList<Routine>,
    private val onStartRoutineClicked: (Routine) -> Unit
) : RecyclerView.Adapter<RoutineAdapter.RoutineViewHolder>() {

    private val selectedRoutines = mutableSetOf<Routine>()

    inner class RoutineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val routineName: TextView = itemView.findViewById(R.id.tv_routine_name)
        val routineDescription: TextView = itemView.findViewById(R.id.tv_routine_description)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkbox_select_routine)
        val startRoutineButton: Button = itemView.findViewById(R.id.btn_start_routine)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoutineViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_routine, parent, false)
        return RoutineViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: RoutineViewHolder, position: Int) {
        val currentRoutine = routines[position]
        holder.routineName.text = currentRoutine.name
        holder.routineDescription.text = currentRoutine.description
        holder.checkBox.isChecked = selectedRoutines.contains(currentRoutine)
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedRoutines.add(currentRoutine)
            } else {
                selectedRoutines.remove(currentRoutine)
            }
        }
        holder.startRoutineButton.setOnClickListener {
            onStartRoutineClicked(currentRoutine)

        }
    }

    override fun getItemCount() = routines.size

    fun getSelectedRoutines(): List<Routine> = selectedRoutines.toList()
}
