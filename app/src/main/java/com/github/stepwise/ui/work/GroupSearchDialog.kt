package com.github.stepwise.ui.work

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.stepwise.R
import com.github.stepwise.network.ApiClient
import kotlinx.coroutines.*

class GroupSearchDialog : DialogFragment() {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var searchJob: Job? = null
    private lateinit var adapter: GroupSearchAdapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_group_search, null)

        val etSearch = view.findViewById<EditText>(R.id.etSearchGroup)
        val rv = view.findViewById<RecyclerView>(R.id.rvGroups)
        val progress = view.findViewById<ProgressBar>(R.id.progressGroups)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)

        adapter = GroupSearchAdapter(emptyList()) { group ->
            // return selection to parent via FragmentResult API
            setFragmentResult("group_selected", bundleOf("groupId" to group.id, "groupName" to group.name))
            dismiss()
        }

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        fun setLoading(loading: Boolean) {
            progress.visibility = if (loading) View.VISIBLE else View.GONE
        }

        fun setEmpty(empty: Boolean, text: String? = null) {
            tvEmpty.visibility = if (empty) View.VISIBLE else View.GONE
            tvEmpty.text = text ?: getString(R.string.no_groups_found)
        }

        setLoading(false)
        setEmpty(true, getString(R.string.enter_search_query))

        val searchWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchJob?.cancel()
                val query = s?.toString()?.trim().orEmpty()
                if (query.isEmpty()) {
                    adapter.submitList(emptyList())
                    setEmpty(true, getString(R.string.enter_search_query))
                    return
                }
                searchJob = coroutineScope.launch {
                    delay(300) // debounce
                    setLoading(true)
                    setEmpty(false)
                    try {
                        val resp = withContext(Dispatchers.IO) {
                            ApiClient.apiService.getAllGroups(query)
                        }
                        if (resp.isSuccessful) {
                            val list = resp.body() ?: emptyList()
                            adapter.submitList(list)
                            setEmpty(list.isEmpty(), getString(R.string.no_groups_found))
                        } else {
                            adapter.submitList(emptyList())
                            setEmpty(true, getString(R.string.error_loading_groups))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        adapter.submitList(emptyList())
                        setEmpty(true, getString(R.string.error_loading_groups))
                    } finally {
                        setLoading(false)
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        }

        etSearch.addTextChangedListener(searchWatcher)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.select_group))
            .setView(view)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            etSearch.requestFocus()
        }

        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        coroutineScope.cancel()
    }
}