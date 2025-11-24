package com.github.stepwise.ui.teacher

import android.widget.EditText
import android.widget.Toast
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.stepwise.R
import com.github.stepwise.network.ApiClient
import com.github.stepwise.network.models.ExplanatoryNoteItemResponseDto
import com.github.stepwise.network.models.ItemStatus
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.*
import okhttp3.ResponseBody
import java.io.File

class ExplanatoryItemsAdapter(
    private var chapterTitles: Map<Int, String> = emptyMap(),
    private val scope: CoroutineScope,
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

    private var ownerId: Long? = null
    private var projectId: Long? = null

    fun updateChapterTitles(map: Map<Int, String>) {
        chapterTitles = map
        notifyDataSetChanged()
    }

    fun updateContext(ownerId: Long?, projectId: Long?) {
        this.ownerId = ownerId
        this.projectId = projectId
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.cardItem)
        val tvOrder: TextView = view.findViewById(R.id.tvItemOrder)
        val tvTitle: TextView = view.findViewById(R.id.tvItemTitle)
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
        val idx = item.orderNumber ?: position
        val displayNumber = idx + 1
        val rawTitle = chapterTitles[idx].orEmpty().ifBlank { "Без названия" }

        holder.tvOrder.text = "$displayNumber. $rawTitle"
        holder.tvTitle.visibility = View.GONE

        val status = item.status ?: ItemStatus.DRAFT
        holder.tvStatus.text = status.russian()
        holder.tvComment.text = item.teacherComment ?: ""

        val hasFile = !item.fileName.isNullOrBlank()
        holder.btnView.isEnabled = hasFile
        holder.btnView.alpha = if (hasFile) 1f else 0.4f
        holder.btnView.setOnClickListener {
            if (!hasFile) return@setOnClickListener
            val ctx = holder.itemView.context
            val oId = ownerId
            val pId = projectId
            val iId = item.id
            if (oId == null || pId == null || iId == null) {
                Toast.makeText(ctx, "Недостаточно данных для скачивания файла", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Toast.makeText(ctx, "Скачиваю файл...", Toast.LENGTH_SHORT).show()
            holder.btnView.isEnabled = false
            scope.launch(Dispatchers.IO) {
                try {
                    val resp = ApiClient.apiService.downloadItemFile(oId, pId, iId)
                    if (!resp.isSuccessful || resp.body() == null) {
                        withContext(Dispatchers.Main) {
                            holder.btnView.isEnabled = true
                            Toast.makeText(ctx, "Ошибка загрузки: ${resp.code()}", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                    val body: ResponseBody = resp.body()!!
                    val file = File(ctx.cacheDir, "project_${pId}_item_${iId}.pdf")
                    body.byteStream().use { input -> file.outputStream().use { out -> input.copyTo(out) } }
                    withContext(Dispatchers.Main) {
                        holder.btnView.isEnabled = true
                        if (ctx is android.app.Activity && ctx.isDestroyed) return@withContext
                        val intent = android.content.Intent(ctx, com.github.stepwise.ui.viewer.PdfViewerActivity::class.java)
                        intent.putExtra("pdf_path", file.absolutePath)
                        ctx.startActivity(intent)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        holder.btnView.isEnabled = true
                        Toast.makeText(ctx, "Ошибка: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        val allowActions = status == ItemStatus.SUBMITTED
        holder.btnApprove.isEnabled = allowActions
        holder.btnReject.isEnabled = allowActions
        holder.btnApprove.alpha = if (allowActions) 1f else 0.4f
        holder.btnReject.alpha = if (allowActions) 1f else 0.4f

        holder.btnApprove.setOnClickListener {
            if (!allowActions) return@setOnClickListener
            holder.btnApprove.isEnabled = false
            onApprove(item)
        }

        holder.btnReject.setOnClickListener {
            if (!allowActions) return@setOnClickListener
            val ctx = holder.itemView.context
            val input = EditText(ctx).apply { hint = "Комментарий преподавателя (обязательно)" }
            android.app.AlertDialog.Builder(ctx)
                .setTitle("Отклонить пункт")
                .setView(input)
                .setPositiveButton("Отклонить") { _, _ ->
                    val comment = input.text?.toString()?.trim().orEmpty()
                    if (comment.isBlank()) {
                        Toast.makeText(ctx, "Комментарий обязателен", Toast.LENGTH_SHORT).show()
                    } else {
                        holder.btnReject.isEnabled = false
                        onReject(item, comment)
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }
}