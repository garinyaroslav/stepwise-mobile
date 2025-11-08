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
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_student_project, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val project = getItem(position)
        holder.tvTitle.text = project.title ?: "Проект"
        val owner = project.owner
        holder.tvStudent.text = if (owner != null) {
            listOfNotNull(owner.firstName, owner.lastName).joinToString(" ")
        } else {
            project.owner?.email ?: "Студент"
        }
        holder.tvDesc.text = project.description ?: ""

        val items: List<ExplanatoryNoteItemResponseDto> = project.items ?: emptyList()
        holder.tvCounts.text = "${items.size} пункт(ов)"

        holder.chipGroup.removeAllViews()

        if (items.isNotEmpty()) {
            val last = items.maxByOrNull { it.submittedAt ?: it.draftedAt ?: "" }
            val statusChip = Chip(holder.itemView.context)
            statusChip.isClickable = false
            statusChip.text = last?.status?.name ?: "—"
            holder.chipGroup.addView(statusChip)
        } else {
            val chip = Chip(holder.itemView.context)
            chip.isClickable = false
            chip.text = "Нет прикреплённых пунктов"
            holder.chipGroup.addView(chip)
        }

        val approvedChip = Chip(holder.itemView.context)
        approvedChip.isClickable = false
        approvedChip.text = if (project.isApprovedForDefense) "Допущен к защите" else "Не допущен"
        holder.chipGroup.addView(approvedChip)

        holder.card.setOnClickListener {
            onOpenProject(project)
        }
    }
}