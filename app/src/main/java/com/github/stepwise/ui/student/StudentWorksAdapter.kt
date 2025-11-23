package com.github.stepwise.ui.student

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.stepwise.R
import com.github.stepwise.network.models.WorkResponseDto
import com.google.android.material.progressindicator.LinearProgressIndicator

data class WorkProgressStats(
    val approved: Int,
    val total: Int
)

class StudentWorksAdapter(
    private val onOpen: (WorkResponseDto) -> Unit
) : ListAdapter<WorkResponseDto, StudentWorksAdapter.VH>(DIFF) {

    private var progressStats: Map<Long, WorkProgressStats> = emptyMap()

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<WorkResponseDto>() {
            override fun areItemsTheSame(oldItem: WorkResponseDto, newItem: WorkResponseDto): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: WorkResponseDto, newItem: WorkResponseDto): Boolean =
                oldItem == newItem
        }
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.cardWork)
        val tvTitle: TextView = view.findViewById(R.id.tvWorkTitle)
        val tvMeta: TextView = view.findViewById(R.id.tvWorkMeta)
        val tvProgressLabel: TextView = view.findViewById(R.id.tvWorkProgressLabel)
        val progress: LinearProgressIndicator = view.findViewById(R.id.workProgress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_work, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val work = getItem(position)
        holder.tvTitle.text = work.title ?: "Работа"

        val teacherName = buildTeacherFullName(work)
        val teacherDisplay = teacherName ?: (work.teacherEmail ?: "")
        holder.tvMeta.text = "${work.groupName ?: ""} · $teacherDisplay".trim().trim('·',' ')

        val totalChapters = work.countOfChapters ?: 0
        val stats = work.id?.let { progressStats[it] }

        val approved = stats?.approved ?: 0
        val total = stats?.total ?: totalChapters

        holder.progress.max = if (total > 0) total else 1
        holder.progress.progress = approved.coerceAtMost(holder.progress.max)

        holder.tvProgressLabel.text = "$approved / $total"

        holder.card.setOnClickListener { onOpen(work) }
    }

    fun updateProgressStats(map: Map<Long, WorkProgressStats>) {
        progressStats = map
        notifyDataSetChanged()
    }

    private fun buildTeacherFullName(work: WorkResponseDto): String? {
        val last = work.teacherLastName?.trim().takeIf { !it.isNullOrBlank() }
        val first = work.teacherName?.trim().takeIf { !it.isNullOrBlank() }
        val middle = work.teacherMiddleName?.trim().takeIf { !it.isNullOrBlank() }
        return if (last != null && first != null && middle != null) {
            "$last $first $middle"
        } else null
    }
}