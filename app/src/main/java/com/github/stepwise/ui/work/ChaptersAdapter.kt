package com.github.stepwise.ui.work

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.github.stepwise.R
import com.github.stepwise.network.models.WorkChapterReq
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

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

    private val isoParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

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

        holder.textDeadline.text = formatDeadlineDisplay(st.deadlineIso, holder)

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
        } catch (_: Exception) {  }

        holder.btnRemove.setOnClickListener { onRemove(position) }
        holder.btnPick.setOnClickListener { onPickDate(position) }
    }

    override fun getItemCount(): Int = items.size


    private fun formatDeadlineDisplay(iso: String?, holder: VH): String {
        val ctx = holder.itemView.context
        if (iso.isNullOrBlank()) {
            holder.textDeadline.setTextColor(getDefaultTextColor(ctx))
            return ctx.getString(R.string.not_set)
        }

        return try {
            val date: Date = isoParser.parse(iso) ?: throw IllegalArgumentException("bad date")
            val millis = date.time
            val now = System.currentTimeMillis()
            val absDiff = abs(millis - now)
            val sevenDaysMs = 7 * DateUtils.DAY_IN_MILLIS

            val dateFormatter = android.text.format.DateFormat.getMediumDateFormat(ctx)
            val dateStr = dateFormatter.format(date)

            val relative = if (absDiff <= sevenDaysMs) {
                DateUtils.getRelativeTimeSpanString(
                    millis,
                    now,
                    DateUtils.DAY_IN_MILLIS,
                    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR
                ).toString()
            } else {
                null
            }

            if (millis < now) {
                val errColor = Color.parseColor("#D32F2F")
                holder.textDeadline.setTextColor(errColor)
            } else {
                holder.textDeadline.setTextColor(getDefaultTextColor(ctx))
            }

            if (relative != null) "$dateStr ($relative)" else dateStr
        } catch (e: Exception) {
            holder.textDeadline.setTextColor(getDefaultTextColor(ctx))
            iso
        }
    }

    private fun getDefaultTextColor(ctx: android.content.Context): Int {
        return resolveColor(ctx, R.color.colorOnSurface) ?: Color.BLACK
    }

    private fun resolveColor(ctx: android.content.Context, attrRes: Int): Int? {
        return try {
            val ta = ctx.theme.obtainStyledAttributes(intArrayOf(attrRes))
            val color = ta.getColor(0, Color.BLACK)
            ta.recycle()
            color
        } catch (e: Exception) {
            null
        }
    }

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