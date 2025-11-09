package com.github.stepwise.ui.teacher

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.stepwise.R
import com.github.stepwise.databinding.FragmentTeacherProjectsBinding
import com.github.stepwise.network.ApiClient
import com.github.stepwise.network.models.WorkResponseDto
import com.github.stepwise.ui.work.GroupSearchDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    private fun openWork(work: WorkResponseDto) {
        val wid = work.id ?: run {
            Toast.makeText(requireContext(), "У работы нет id", Toast.LENGTH_SHORT).show()
            return
        }

        val nav = findNavController()

        val bundle = bundleOf("workId" to wid)
        nav.navigate(R.id.work_detail_fragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}