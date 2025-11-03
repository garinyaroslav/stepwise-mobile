package com.github.stepwise.ui.work

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.card.MaterialCardView
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
        val card: MaterialCardView = view.findViewById(R.id.cardChapter)
        val indexText: TextView = view.findViewById(R.id.textChapterIndex)
        val etTitle: TextInputEditText = view.findViewById(R.id.etChapterTitle)
        val etDescription: TextInputEditText = view.findViewById(R.id.etChapterDescription)
        val textDeadline: TextView = view.findViewById(R.id.textDeadline)
        val btnRemove: ImageButton = view.findViewById(R.id.buttonRemoveChapter)
        val btnPick: ImageButton = view.findViewById(R.id.buttonPickDate)

        var titleWatcher: TextWatcher? = null
        var descWatcher: TextWatcher? = null
    }

    private val palette = listOf(
        Color.parseColor("#FFCDD2"),
        Color.parseColor("#F8BBD0"),
        Color.parseColor("#E1BEE7"),
        Color.parseColor("#C5CAE9"),
        Color.parseColor("#BBDEFB"),
        Color.parseColor("#B2EBF2"),
        Color.parseColor("#C8E6C9"),
        Color.parseColor("#FFE0B2")
    )

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

        val color = palette[position % palette.size]
        val circle = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }

        try {
            val strokeColor = ColorUtils.blendARGB(color, Color.BLACK, 0.15f)
            holder.card.strokeColor = strokeColor
            holder.card.strokeWidth = 2
        } catch (_: Exception) {}

        holder.btnRemove.setOnClickListener { onRemove(position) }
        holder.btnPick.setOnClickListener { onPickDate(position) }
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