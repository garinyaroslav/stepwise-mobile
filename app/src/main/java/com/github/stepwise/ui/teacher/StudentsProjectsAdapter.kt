package com.github.stepwise.ui.teacher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.stepwise.R
import com.github.stepwise.network.models.ExplanatoryNoteItemResponseDto
import com.github.stepwise.network.models.ProjectResponseDto
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class StudentsProjectsAdapter(
    private val onOpenProject: (ProjectResponseDto) -> Unit
) : ListAdapter<ProjectResponseDto, StudentsProjectsAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ProjectResponseDto>() {
            override fun areItemsTheSame(oldItem: ProjectResponseDto, newItem: ProjectResponseDto): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: ProjectResponseDto, newItem: ProjectResponseDto): Boolean =
                oldItem == newItem
        }

        private fun pluralizePunkt(count: Int): String {
            val mod100 = count % 100
            val mod10 = count % 10
            return when {
                mod100 in 11..14 -> "пунктов"
                mod10 == 1 -> "пункт"
                mod10 in 2..4 -> "пункта"
                else -> "пунктов"
            }
        }
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.cardProject)
        val tvTitle: TextView = view.findViewById(R.id.tvProjectTitle)
        val tvStudent: TextView = view.findViewById(R.id.tvProjectStudent)
        val tvDesc: TextView = view.findViewById(R.id.tvProjectDesc)
        val tvCounts: TextView = view.findViewById(R.id.tvProjectCounts)
        val chipGroup: ChipGroup = view.findViewById(R.id.chipStatusGroup)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_project, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val project = getItem(position)

        holder.tvTitle.text = project.title ?: "Проект"
        holder.tvDesc.text = project.description.orEmpty()

        val owner = project.owner
        holder.tvStudent.text = when {
            owner != null -> listOfNotNull(owner.firstName, owner.lastName).joinToString(" ").ifBlank {
                owner.email ?: "Студент"
            }
            else -> "Студент"
        }

        val items: List<ExplanatoryNoteItemResponseDto> = project.items ?: emptyList()
        val count = items.size
        holder.tvCounts.text = "Прикреплено $count ${pluralizePunkt(count)}"

        holder.chipGroup.removeAllViews()

        if (items.isNotEmpty()) {
            val last = items.maxByOrNull { it.submittedAt ?: it.draftedAt ?: "" }
            val statusChip = Chip(holder.itemView.context).apply {
                isClickable = false
                text = last?.status?.russian() ?: "—"
            }
            holder.chipGroup.addView(statusChip)        }

        val approvedChip = Chip(holder.itemView.context).apply {
            isClickable = false
            text = if (project.isApprovedForDefense) "Допущен к защите" else "Не допущен"
        }
        holder.chipGroup.addView(approvedChip)

        if (items.isEmpty()) {
            val emptyChip = Chip(holder.itemView.context).apply {
                isClickable = false
                text = "Нет пунктов"
            }
            holder.chipGroup.addView(emptyChip, 0)
        }

        holder.card.setOnClickListener { onOpenProject(project) }
    }
}