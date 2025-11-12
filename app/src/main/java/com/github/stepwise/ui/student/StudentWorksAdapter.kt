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

class StudentWorksAdapter(private val onOpen: (WorkResponseDto) -> Unit) :
    ListAdapter<WorkResponseDto, StudentWorksAdapter.VH>(DIFF) {

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
        val progress: LinearProgressIndicator = view.findViewById(R.id.workProgress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_student_work, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val work = getItem(position)
        holder.tvTitle.text = work.title ?: "Работа"
        holder.tvMeta.text = "${work.groupName ?: ""} · ${work.teacherName ?: work.teacherEmail ?: ""}"

        val total = work.countOfChapters ?: 0
        holder.progress.max = if (total > 0) total else 100
        holder.progress.progress = 0

        holder.card.setOnClickListener { onOpen(work) }
    }
}