package com.github.stepwise.ui.student

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.stepwise.R
import com.github.stepwise.network.models.ExplanatoryNoteItemResponseDto
import com.github.stepwise.network.models.WorkChapterDto
import formatIsoToDdMmYyyy
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ProjectChaptersAdapter(
    private val onAttach: (chapterIndex: Int) -> Unit,
    private val onView: (item: ExplanatoryNoteItemResponseDto?) -> Unit
) : ListAdapter<Pair<WorkChapterDto, ExplanatoryNoteItemResponseDto?>, ProjectChaptersAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Pair<WorkChapterDto, ExplanatoryNoteItemResponseDto?>>() {
            override fun areItemsTheSame(oldItem: Pair<WorkChapterDto, ExplanatoryNoteItemResponseDto?>, newItem: Pair<WorkChapterDto, ExplanatoryNoteItemResponseDto?>): Boolean =
                oldItem.first.index == newItem.first.index

            override fun areContentsTheSame(oldItem: Pair<WorkChapterDto, ExplanatoryNoteItemResponseDto?>, newItem: Pair<WorkChapterDto, ExplanatoryNoteItemResponseDto?>): Boolean =
                oldItem == newItem
        }
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.cardChapter)
        val tvTitle: TextView = view.findViewById(R.id.tvChapterTitle)
        val tvDeadline: TextView = view.findViewById(R.id.tvChapterDeadline)
        val tvStatus: TextView = view.findViewById(R.id.tvChapterStatus)
        val btnAttach: Button = view.findViewById(R.id.btnAttach)
        val btnView: Button = view.findViewById(R.id.btnView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_project_chapter, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val (chapter, item) = getItem(position)
        holder.tvTitle.text = chapter.title ?: "Пункт ${chapter.index}"
        val deadlineStr = chapter.deadline ?: ""
        holder.tvDeadline.text = formatIsoToDdMmYyyy(deadlineStr)

        var overdue = false
        try {
            if (!deadlineStr.isNullOrBlank()) {
                val dt = LocalDateTime.parse(deadlineStr, DateTimeFormatter.ISO_DATE_TIME)
                if (dt.isBefore(LocalDateTime.now())) overdue = true
            }
        } catch (_: Exception) {  }

        val approved = item?.status?.name == "APPROVED"
        if (overdue && !approved) {
            holder.tvDeadline.setTextColor(Color.RED)
        } else {
            holder.tvDeadline.setTextColor(Color.DKGRAY)
        }

        holder.tvStatus.text = item?.status?.name ?: "Не прикреплён"

        holder.btnView.visibility = if (item?.fileName != null) View.VISIBLE else View.GONE

        val canAttach = item == null || item.status?.name == "DRAFT" || item.status?.name == "REJECTED"
        holder.btnAttach.isEnabled = canAttach
        holder.btnAttach.text = if (item == null) "Прикрепить" else if (item.status?.name == "REJECTED") "Заменить" else "Загрузить"

        holder.btnAttach.setOnClickListener {
            onAttach(chapter.index)
        }
        holder.btnView.setOnClickListener {
            onView(item)
        }
    }
}