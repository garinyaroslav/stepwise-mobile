package com.github.stepwise.ui.work

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.stepwise.R
import com.github.stepwise.network.models.GroupResponseDto

class GroupSearchAdapter(
    private var items: List<GroupResponseDto> = emptyList(),
    private val onClick: (GroupResponseDto) -> Unit
) : RecyclerView.Adapter<GroupSearchAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvGroupName)
        val tvMeta: TextView = view.findViewById(R.id.tvGroupMeta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_group_search, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val g = items[position]
        holder.tvName.text = g.name
        holder.tvMeta.text = "${g.studentsCount} студентов"
        holder.itemView.setOnClickListener { onClick(g) }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(new: List<GroupResponseDto>) {
        items = new
        notifyDataSetChanged()
    }
}