package com.github.stepwise.ui.teacher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.stepwise.R
import com.github.stepwise.network.models.ExplanatoryNoteItemResponseDto
import com.github.stepwise.network.models.ItemStatus
import com.google.android.material.card.MaterialCardView

class ExplanatoryItemsAdapter(
    private val onViewPdf: (ExplanatoryNoteItemResponseDto) -> Unit,
    private val onApprove: (ExplanatoryNoteItemResponseDto) -> Unit,
    private val onReject: (ExplanatoryNoteItemResponseDto, String) -> Unit
) : ListAdapter<ExplanatoryNoteItemResponseDto, ExplanatoryItemsAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ExplanatoryNoteItemResponseDto>() {
            override fun areItemsTheSame(oldItem: ExplanatoryNoteItemResponseDto, newItem: ExplanatoryNoteItemResponseDto): Boolean =
                oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: ExplanatoryNoteItemResponseDto, newItem: ExplanatoryNoteItemResponseDto): Boolean =
                oldItem == newItem
        }
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.cardItem)
        val tvOrder: TextView = view.findViewById(R.id.tvItemOrder)
        val tvStatus: TextView = view.findViewById(R.id.tvItemStatus)
        val tvComment: TextView = view.findViewById(R.id.tvItemComment)
        val btnView: ImageButton = view.findViewById(R.id.btnViewPdf)
        val btnApprove: ImageButton = view.findViewById(R.id.btnApprove)
        val btnReject: ImageButton = view.findViewById(R.id.btnReject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_explanatory_item, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.tvOrder.text = "Пункт ${item.orderNumber ?: position + 1}"
        holder.tvStatus.text = item.status?.name ?: "—"
        holder.tvComment.text = item.teacherComment ?: ""

        holder.btnView.setOnClickListener { onViewPdf(item) }

        val status = item.status ?: ItemStatus.DRAFT
        val allowActions = status != ItemStatus.DRAFT
        holder.btnApprove.isEnabled = allowActions
        holder.btnReject.isEnabled = allowActions

        holder.btnApprove.setOnClickListener { onApprove(item) }
        holder.btnReject.setOnClickListener {
            val ctx = holder.itemView.context
            val input = android.widget.EditText(ctx)
            input.hint = "Комментарий преподавателя (обязательно)"
            android.app.AlertDialog.Builder(ctx)
                .setTitle("Отклонить пункт")
                .setView(input)
                .setPositiveButton("Отклонить") { _, _ ->
                    val comment = input.text?.toString()?.trim().orEmpty()
                    if (comment.isBlank()) {
                        android.widget.Toast.makeText(ctx, "Комментарий обязателен", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        onReject(item, comment)
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }
}