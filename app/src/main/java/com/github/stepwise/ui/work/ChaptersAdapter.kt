package com.github.stepwise.ui.work

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.github.stepwise.R
import com.github.stepwise.network.models.WorkChapterReq

class ChaptersAdapter(
    private val items: MutableList<ChapterState>,
    private val onRemove: (position: Int) -> Unit,
    private val onPickDate: (position: Int) -> Unit
) : RecyclerView.Adapter<ChaptersAdapter.VH>() {

    data class ChapterState(
        var title: String = "",
        var description: String = "",
        var deadlineIso: String? = null
    )

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val indexText: TextView = view.findViewById(R.id.textChapterIndex)
        val etTitle: TextInputEditText = view.findViewById(R.id.etChapterTitle)
        val etDescription: TextInputEditText = view.findViewById(R.id.etChapterDescription)
        val textDeadline: TextView = view.findViewById(R.id.textDeadline)
        val btnRemove: ImageButton = view.findViewById(R.id.buttonRemoveChapter)
        val btnPick: ImageButton = view.findViewById(R.id.buttonPickDate)

        var titleWatcher: TextWatcher? = null
        var descWatcher: TextWatcher? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_work_chapter, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val st = items[position]

        holder.indexText.text = "${position + 1}."

        holder.titleWatcher?.let { holder.etTitle.removeTextChangedListener(it) }
        holder.descWatcher?.let { holder.etDescription.removeTextChangedListener(it) }

        holder.etTitle.setText(st.title)
        holder.etDescription.setText(st.description)
        holder.textDeadline.text = st.deadlineIso ?: "Не задан"

        val newTitleWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                st.title = s?.toString() ?: ""
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        holder.etTitle.addTextChangedListener(newTitleWatcher)
        holder.titleWatcher = newTitleWatcher

        val newDescWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                st.description = s?.toString() ?: ""
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        holder.etDescription.addTextChangedListener(newDescWatcher)
        holder.descWatcher = newDescWatcher

        holder.btnRemove.setOnClickListener {
            onRemove(position)
        }

        holder.btnPick.setOnClickListener {
            onPickDate(position)
        }
    }

    override fun getItemCount(): Int = items.size

    fun toRequestChapters(): List<WorkChapterReq> {
        return items.mapIndexed { index, s ->
            WorkChapterReq(
                index = index + 1,
                title = s.title.ifBlank { null },
                description = s.description.ifBlank { null },
                deadline = s.deadlineIso
            )
        }
    }
}