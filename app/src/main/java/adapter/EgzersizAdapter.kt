package adapter

import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import model.Egzersiz
import com.gokhan.kfa.R
import com.gokhan.kfa.databinding.ItemEgzersizSecimBinding
import com.gokhan.kfa.databinding.ItemRutinEgzersiziBinding


class EgzersizAdapter(
    private val exercises: MutableList<Egzersiz>,
    private val onExerciseClicked: (Egzersiz) -> Unit,
    private val onInfoClicked: (Egzersiz) -> Unit,
    private val onDeleteClicked: (Egzersiz) -> Unit,
    private val onAddSetClicked: (Egzersiz) -> Unit,
    private val isRoutineExercise: Boolean
) : RecyclerView.Adapter<EgzersizAdapter.EgzersizViewHolder>() {

    private val selectedExercises = mutableSetOf<Egzersiz>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EgzersizViewHolder {
        val binding = if (isRoutineExercise) {
            ItemRutinEgzersiziBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        } else {
            ItemEgzersizSecimBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        }
        return EgzersizViewHolder(binding, onInfoClicked, onAddSetClicked)
    }

    override fun onBindViewHolder(holder: EgzersizViewHolder, position: Int) {
        val exercise = exercises[position]
        holder.bind(exercise)

        holder.removeButton?.setOnClickListener {
            onDeleteClicked(exercise)
            notifyDataSetChanged()
            Log.d("EgzersizAdapter", "Exercise removed from routine: ${exercise.name}")
        }

        holder.itemView.setOnClickListener {
            onExerciseClicked(exercise)
        }

        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = selectedExercises.contains(exercise)
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

    inner class EgzersizViewHolder(
        private val binding: ViewBinding,
        private val onInfoClicked: (Egzersiz) -> Unit,
        private val onAddSetClicked: (Egzersiz) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        val checkBox: CheckBox = if (binding is ItemEgzersizSecimBinding) binding.cbSelectExercise else (binding as ItemRutinEgzersiziBinding).cbSetCompleted
        val removeButton: ImageButton? = when (binding) {
            is ItemRutinEgzersiziBinding -> binding.btnRemoveExerciseFromRoutine
            else -> null
        }
        val addSetButton: ImageButton? = if (isRoutineExercise) {
            (binding as ItemRutinEgzersiziBinding).btnAddNewSet
        } else null


        fun bind(exercise: Egzersiz) {
            when (binding) {
                is ItemEgzersizSecimBinding -> {
                    binding.tvExerciseName.text = exercise.name
                    binding.tvExerciseDescription.text = getTruncatedDescription(exercise.description)
                    binding.tvExerciseDescription.setOnClickListener {
                        onInfoClicked(exercise)
                    }
                    binding.tvTargetMuscles.text = "Hedef Kaslar: ${exercise.targetMuscleGroups?.joinToString(", ")}"
                    binding.tvSecondaryMuscles.text = "Yardımcı Kaslar: ${exercise.secondaryTargetMuscleGroups?.joinToString(", ")}"
                    val width = 300
                    val height = 300

                    Glide.with(binding.root)
                        .asGif()
                        .load(exercise.gifUrl)
                        .apply(
                            RequestOptions()
                                .override(width, height)
                                .fitCenter()
                                .placeholder(R.drawable.gymicon)
                                .error(R.drawable.baseline_image_not_supported_24)
                        )
                        .listener(object : RequestListener<GifDrawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<GifDrawable>?,
                                isFirstResource: Boolean
                            ): Boolean {
                                e?.logRootCauses("Glide")
                                return false
                            }

                            override fun onResourceReady(
                                resource: GifDrawable?,
                                model: Any?,
                                target: Target<GifDrawable>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ): Boolean {
                                resource?.setLoopCount(GifDrawable.LOOP_INTRINSIC)
                                return false
                            }
                        })
                        .into(binding.ivExerciseIcon)
                    binding.infoLayout.setOnClickListener {
                        onInfoClicked(exercise)
                    }
                    binding.ivInfo.setOnClickListener {
                        onInfoClicked(exercise)
                    }
                    binding.ivInfoText.setOnClickListener {
                        onInfoClicked(exercise)
                    }
                }
                is ItemRutinEgzersiziBinding -> {
                    binding.tvExerciseName.text = exercise.name
                    binding.tvExerciseDescription.text = getTruncatedDescription(exercise.description)
                    binding.tvExerciseDescription.setOnClickListener {
                        onInfoClicked(exercise)
                    }

                    val width = 300
                    val height = 300

                    Glide.with(binding.root)
                        .asGif()
                        .load(exercise.gifUrl)
                        .apply(
                            RequestOptions()
                                .override(width, height)
                                .fitCenter()
                                .placeholder(R.drawable.gymicon)
                                .error(R.drawable.baseline_image_not_supported_24)
                        )
                        .listener(object : RequestListener<GifDrawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<GifDrawable>?,
                                isFirstResource: Boolean
                            ): Boolean {
                                e?.logRootCauses("Glide")
                                return false
                            }

                            override fun onResourceReady(
                                resource: GifDrawable?,
                                model: Any?,
                                target: Target<GifDrawable>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ): Boolean {
                                resource?.setLoopCount(GifDrawable.LOOP_INTRINSIC)
                                return false
                            }
                        })
                        .into(binding.ivExerciseIcon)
                    binding.infoLayout.setOnClickListener {
                        onInfoClicked(exercise)
                    }
                    binding.ivInfo.setOnClickListener {
                        onInfoClicked(exercise)
                    }
                    binding.ivInfoText.setOnClickListener {
                        onInfoClicked(exercise)
                    }
                    addSetButton?.setOnClickListener {
                        onAddSetClicked(exercise)
                    }
                }
            }
        }

        private fun getTruncatedDescription(description: String): SpannableString {
            val maxLength = 110
            val readMoreText = " ... Devamını Oku"

            return if (description.length > maxLength) {
                val truncatedText = "${description.substring(0, maxLength)}$readMoreText"
                val spannableString = SpannableString(truncatedText)

                val clickableSpan = object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        // Tam açıklamayı göstermek için gerekli işlemler
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        val readMoreColor = ContextCompat.getColor(itemView.context, R.color.c3)
                        val alphaColor = readMoreColor and 0x00ffffff or (128 shl 24)
                        ds.color = alphaColor
                        ds.isUnderlineText = false
                    }
                }

                val startIndex = truncatedText.indexOf(readMoreText)
                spannableString.setSpan(clickableSpan, startIndex, startIndex + readMoreText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                spannableString
            } else {
                SpannableString(description)
            }
        }
    }
}
