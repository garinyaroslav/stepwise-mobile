package com.github.stepwise.ui.teacher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.stepwise.R
import com.github.stepwise.network.models.WorkResponseDto
import com.google.android.material.card.MaterialCardView

class WorksAdapter(
    private val onOpen: (WorkResponseDto) -> Unit,
) : ListAdapter<WorkResponseDto, WorksAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<WorkResponseDto>() {
            override fun areItemsTheSame(oldItem: WorkResponseDto, newItem: WorkResponseDto): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: WorkResponseDto, newItem: WorkResponseDto): Boolean =
                oldItem == newItem
        }
    }

    private val expanded = HashSet<Long>()

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.cardWork)
        val tvTitle: TextView = itemView.findViewById(R.id.tvWorkTitle)
        val tvMeta: TextView = itemView.findViewById(R.id.tvWorkMeta)
        val ivExpand: ImageView = itemView.findViewById(R.id.ivExpand)
        val expandedArea: View = itemView.findViewById(R.id.expandedArea)
        val tvDescription: TextView = itemView.findViewById(R.id.tvWorkDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_teacher_work, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val work = getItem(position)

        holder.tvTitle.text = work.title
        val typeOfWork = if (work.type == "COURSEWORK") "Курсовая работа" else "Дипломная работа"
        val meta = "${typeOfWork} у группы ${work.groupName}. Пунктов: ${work.countOfChapters}"
        holder.tvMeta.text = meta

        holder.tvDescription.text = work.description ?: ""

        val isExpanded = expanded.contains(work.id)
        holder.expandedArea.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.ivExpand.rotation = if (isExpanded) 180f else 0f

        holder.card.setOnClickListener { onOpen(work) }

        holder.ivExpand.setOnClickListener {
            toggleExpansion(work.id, holder)
        }
    }

    private fun toggleExpansion(id: Long, holder: VH) {
        if (expanded.contains(id)) {
            expanded.remove(id)
            animateExpandIcon(holder.ivExpand, false)
            holder.expandedArea.visibility = View.GONE
        } else {
            expanded.add(id)
            animateExpandIcon(holder.ivExpand, true)
            holder.expandedArea.visibility = View.VISIBLE
        }
    }

    private fun animateExpandIcon(iv: ImageView, expand: Boolean) {
        val from = if (expand) 0f else 180f
        val to = if (expand) 180f else 0f
        val anim = RotateAnimation(from, to, RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f)
        anim.duration = 180
        anim.fillAfter = true
        iv.startAnimation(anim)
        iv.rotation = to
    }
}