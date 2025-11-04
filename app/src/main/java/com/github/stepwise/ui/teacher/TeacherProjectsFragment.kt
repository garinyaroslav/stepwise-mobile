package com.github.stepwise.ui.teacher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.stepwise.databinding.FragmentTeacherProjectsBinding
import com.github.stepwise.network.ApiClient
import com.github.stepwise.network.models.WorkResponseDto
import com.github.stepwise.ui.work.GroupSearchDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class TeacherProjectsFragment : Fragment() {

    private var _binding: FragmentTeacherProjectsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: WorksAdapter
    private var allWorks: List<WorkResponseDto> = emptyList()
    private var selectedGroupId: Long? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTeacherProjectsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = WorksAdapter(
            onOpen = { work -> openWork(work) },
            onEdit = { work -> editWork(work) }
        )

        binding.rvWorks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvWorks.adapter = adapter

        binding.etGroupFilter.setOnClickListener {
            GroupSearchDialog().show(parentFragmentManager, "group_search")
        }

        parentFragmentManager.setFragmentResultListener("group_selected", viewLifecycleOwner) { _, bundle ->
            val gid = bundle.getLong("groupId")
            val gname = bundle.getString("groupName") ?: gid.toString()
            selectedGroupId = gid
            binding.etGroupFilter.setText(gname)
            showLoading(true)
            loadWorks()
        }

        binding.btnClearGroup.setOnClickListener {
            selectedGroupId = null
            binding.etGroupFilter.setText("")
            showLoading(true)
            loadWorks()
        }

        binding.btnRefresh.setOnClickListener {
            showLoading(true)
            loadWorks()
        }


        showLoading(true)
        loadWorks()
    }

    private fun showLoading(loading: Boolean) {
        binding.progressLoading.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRefresh.isEnabled = !loading
        binding.btnClearGroup.isEnabled = !loading
        binding.etGroupFilter.isEnabled = !loading

        if (loading) binding.tvEmpty.visibility = View.GONE
    }

    private fun loadWorks() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val profileResp = ApiClient.apiService.getMyProfile()
                if (!profileResp.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        binding.tvEmpty.visibility = View.GONE
                        Toast.makeText(requireContext(), "Ошибка получения профиля: ${profileResp.code()}", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                val me = profileResp.body()
                val teacherId = me?.id ?: run {
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        binding.tvEmpty.visibility = View.GONE
                        Toast.makeText(requireContext(), "Не удалось определить id преподавателя", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val resp = ApiClient.apiService.getWorksByTeacherId(teacherId, selectedGroupId)
                if (resp.isSuccessful) {
                    val list = resp.body() ?: emptyList()
                    allWorks = list
                    withContext(Dispatchers.Main) {
                        adapter.submitList(list)
                        showLoading(false)
                        binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        binding.tvEmpty.visibility = View.GONE
                        Toast.makeText(requireContext(), "Ошибка: ${resp.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    binding.tvEmpty.visibility = View.GONE
                    Toast.makeText(requireContext(), "Ошибка: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun filter(q: String) {
        val ql = q.trim().lowercase(Locale.getDefault())
        val filtered = if (ql.isEmpty()) {
            allWorks
        } else {
            allWorks.filter {
                (it.title?.lowercase(Locale.getDefault())?.contains(ql) == true) ||
                        (it.groupName?.lowercase(Locale.getDefault())?.contains(ql) == true)
            }
        }
        adapter.submitList(filtered)
        binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openWork(work: WorkResponseDto) {
        Toast.makeText(requireContext(), "Открыть: ${work.title}", Toast.LENGTH_SHORT).show()
    }

    private fun editWork(work: WorkResponseDto) {
        Toast.makeText(requireContext(), "Редактировать: ${work.title}", Toast.LENGTH_SHORT).show()
    }

    private fun createNewWork() {
        Toast.makeText(requireContext(), "Создать новую работу", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}