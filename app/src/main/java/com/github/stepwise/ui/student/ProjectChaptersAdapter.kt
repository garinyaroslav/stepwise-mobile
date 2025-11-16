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
    private val onView: (item: ExplanatoryNoteItemResponseDto?) -> Unit,
    private val onSubmit: (item: ExplanatoryNoteItemResponseDto) -> Unit
) : ListAdapter<Pair<WorkChapterDto, ExplanatoryNoteItemResponseDto?>, ProjectChaptersAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Pair<WorkChapterDto, ExplanatoryNoteItemResponseDto?>>() {
            override fun areItemsTheSame(
                oldItem: Pair<WorkChapterDto, ExplanatoryNoteItemResponseDto?>,
                newItem: Pair<WorkChapterDto, ExplanatoryNoteItemResponseDto?>
            ): Boolean = oldItem.first.index == newItem.first.index

            override fun areContentsTheSame(
                oldItem: Pair<WorkChapterDto, ExplanatoryNoteItemResponseDto?>,
                newItem: Pair<WorkChapterDto, ExplanatoryNoteItemResponseDto?>
            ): Boolean = oldItem == newItem
        }
    }

    private var firstAttachablePosition: Int? = null

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.cardChapter)
        val tvTitle: TextView = view.findViewById(R.id.tvChapterTitle)
        val tvDeadline: TextView = view.findViewById(R.id.tvChapterDeadline)
        val tvStatus: TextView = view.findViewById(R.id.tvChapterStatus)
        val btnAttach: Button = view.findViewById(R.id.btnAttach)
        val btnView: Button = view.findViewById(R.id.btnView)
        val btnSubmit: Button = view.findViewById(R.id.btnSubmit)
    }

    override fun submitList(list: List<Pair<WorkChapterDto, ExplanatoryNoteItemResponseDto?>>?) {
        firstAttachablePosition = list
            ?.withIndex()
            ?.firstOrNull { (_, pair) ->
                val item = pair.second
                item == null || item.status?.name == "DRAFT" || item.status?.name == "REJECTED"
            }?.index
        super.submitList(list)
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
            if (deadlineStr.isNotBlank()) {
                val dt = LocalDateTime.parse(deadlineStr, DateTimeFormatter.ISO_DATE_TIME)
                if (dt.isBefore(LocalDateTime.now())) overdue = true
            }
        } catch (_: Exception) { }

        val status = item?.status?.name
        val approved = status == "APPROVED"
        holder.tvDeadline.setTextColor(if (overdue && !approved) Color.RED else Color.DKGRAY)
        holder.tvStatus.text = status ?: "Не прикреплён"

        holder.btnAttach.visibility = View.GONE
        holder.btnView.visibility = View.GONE
        holder.btnSubmit.visibility = View.GONE
        holder.btnAttach.isEnabled = true
        holder.btnSubmit.isEnabled = true

        val isFirstAttachable = firstAttachablePosition == position
        val isLastChapterOverall = position == itemCount - 1
        val hasFile = item?.fileName?.isNotBlank() == true

        when {
            item == null -> {
                if (isFirstAttachable) {
                    holder.btnAttach.visibility = View.VISIBLE
                    holder.btnAttach.text = "Прикрепить"
                }
            }

            status == "DRAFT" -> {
                if (isFirstAttachable) {
                    holder.btnAttach.visibility = View.VISIBLE
                    holder.btnAttach.text = if (hasFile) "Заменить" else "Загрузить"
                }
                if (hasFile) holder.btnView.visibility = View.VISIBLE
                if (isFirstAttachable && isLastChapterOverall && hasFile) {
                    holder.btnSubmit.visibility = View.VISIBLE
                }
            }

            status == "SUBMITTED" -> {
                if (hasFile) holder.btnView.visibility = View.VISIBLE
            }

            status == "APPROVED" -> {
                if (hasFile) holder.btnView.visibility = View.VISIBLE
            }

            status == "REJECTED" -> {
                if (isFirstAttachable) {
                    holder.btnAttach.visibility = View.VISIBLE
                    holder.btnAttach.text = "Заменить"
                }
                if (hasFile) holder.btnView.visibility = View.VISIBLE
            }
        }

        holder.btnAttach.setOnClickListener { onAttach(chapter.index) }
        holder.btnView.setOnClickListener { onView(item) }
        holder.btnSubmit.setOnClickListener {
            holder.btnSubmit.isEnabled = false
            item?.let { onSubmit(it) }
        }
    }
}