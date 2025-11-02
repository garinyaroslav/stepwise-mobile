package com.github.stepwise.ui.work

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.github.stepwise.R

class TypeSpinnerAdapter(
    context: Context,
    private val items: List<String>,
    private val icons: IntArray
) : ArrayAdapter<String>(context, R.layout.spinner_selected_item, items) {

    private val inflater = LayoutInflater.from(context)

    override fun getCount(): Int = items.size

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.spinner_selected_item, parent, false)
        bindSelectedView(view, position)
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.spinner_dropdown_item, parent, false)
        bindDropdownView(view, position)
        return view
    }

    private fun bindSelectedView(view: View, position: Int) {
        val icon = view.findViewById<ImageView>(R.id.ivSelectedIcon)
        val title = view.findViewById<TextView>(R.id.tvSelectedTitle)

        title.text = items[position]
        if (position in icons.indices) icon.setImageResource(icons[position]) else icon.setImageDrawable(null)
    }

    private fun bindDropdownView(view: View, position: Int) {
        val icon = view.findViewById<ImageView>(R.id.ivDropdownIcon)
        val title = view.findViewById<TextView>(R.id.tvDropdownTitle)

        title.text = items[position]
        if (position in icons.indices) icon.setImageResource(icons[position]) else icon.setImageDrawable(null)
    }
}