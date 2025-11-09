package com.github.stepwise.ui.teacher

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.stepwise.R
import com.github.stepwise.network.models.WorkChapterDto
import formatIsoToDdMmYyyy

class ChaptersAdapter : ListAdapter<WorkChapterDto, ChaptersAdapter.VH>(DIFF) {

    private val expanded = HashSet<Int>()

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<WorkChapterDto>() {
            override fun areItemsTheSame(oldItem: WorkChapterDto, newItem: WorkChapterDto): Boolean =
                oldItem.index == newItem.index

            override fun areContentsTheSame(oldItem: WorkChapterDto, newItem: WorkChapterDto): Boolean =
                oldItem == newItem
        }
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.cardChapter)
        val tvTitle: TextView = view.findViewById(R.id.tvChapterTitle)
        val tvDeadline: TextView = view.findViewById(R.id.tvChapterDeadline)
        val tvDescription: TextView = view.findViewById(R.id.tvChapterDescription)
        val ivToggle: ImageView = view.findViewById(R.id.ivChapterToggle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_chapter, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val chapter = getItem(position)
        holder.tvTitle.text = chapter.title ?: "Пункт ${chapter.index}"
        holder.tvDeadline.text = formatIsoToDdMmYyyy(chapter.deadline) ?: ""
        holder.tvDescription.text = chapter.description ?: ""

        val isExpanded = expanded.contains(chapter.index)
        holder.tvDescription.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.ivToggle.rotation = if (isExpanded) 180f else 0f

        holder.card.setOnClickListener {
            toggleExpanded(chapter.index, holder)
        }
        holder.ivToggle.setOnClickListener {
            toggleExpanded(chapter.index, holder)
        }
    }

    private fun toggleExpanded(index: Int, holder: VH) {
        val currently = expanded.contains(index)
        if (currently) expanded.remove(index) else expanded.add(index)

        val from = if (currently) 180f else 0f
        val to = if (currently) 0f else 180f
        ObjectAnimator.ofFloat(holder.ivToggle, "rotation", from, to).apply {
            duration = 220
            interpolator = DecelerateInterpolator()
            start()
        }

        holder.tvDescription.visibility = if (currently) View.GONE else View.VISIBLE
    }
}