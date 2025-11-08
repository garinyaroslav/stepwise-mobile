package com.github.stepwise.ui.teacher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.stepwise.databinding.FragmentWorkDetailBinding
import com.github.stepwise.network.ApiClient
import com.github.stepwise.network.models.ProjectResponseDto
import com.github.stepwise.network.models.WorkResponseDto
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorkDetailFragment : Fragment() {

    private var _binding: FragmentWorkDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var projectsAdapter: StudentsProjectsAdapter
    private var currentWork: WorkResponseDto? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWorkDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        projectsAdapter = StudentsProjectsAdapter { project ->
            val bs = ProjectDetailBottomSheet.newInstance(project.id ?: -1L)
            bs.show(parentFragmentManager, "project_detail")
        }
        binding.rvProjects.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProjects.adapter = projectsAdapter

        binding.fabRefresh.setOnClickListener {
            loadWork()
        }

        val workId = arguments?.getLong("workId", -1L) ?: -1L
        if (workId <= 0) {
            Toast.makeText(requireContext(), "Неверный id работы", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)
        loadWork()
    }

    private fun showLoading(loading: Boolean) {
        binding.progressLoading.visibility = if (loading) View.VISIBLE else View.GONE
        binding.fabRefresh.isEnabled = !loading
    }

    private fun loadWork() {
        val workId = arguments?.getLong("workId", -1L) ?: -1L
        if (workId <= 0) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val wResp = ApiClient.apiService.getWorkById(workId)
                if (!wResp.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        Toast.makeText(requireContext(), "Ошибка получения работы: ${wResp.code()}", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                val work = wResp.body()

                val pResp = ApiClient.apiService.getProjectsByWorkForTeacher(workId)
                val projects: List<ProjectResponseDto> = if (pResp.isSuccessful) pResp.body() ?: emptyList() else emptyList()

                withContext(Dispatchers.Main) {
                    showLoading(false)
                    currentWork = work
                    renderWork(work)
                    projectsAdapter.submitList(projects)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Ошибка: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun renderWork(work: WorkResponseDto?) {
        if (work == null) return
        binding.tvTitle.text = work.title ?: ""
        binding.tvDescription.text = work.description ?: ""
        binding.tvMeta.text = "${work.groupName ?: ""} · ${work.teacherName ?: work.teacherEmail ?: ""} · ${work.countOfChapters ?: 0} частей"

        binding.chaptersContainer.removeAllViews()
        val chapters = work.academicWorkChapters
        if (chapters.isNullOrEmpty()) {
            val chip = Chip(requireContext())
            chip.text = "Нет настроенных пунктов"
            chip.isClickable = false
            binding.chaptersContainer.addView(chip)
        } else {
            for (ch in chapters) {
                val chip = Chip(requireContext())
                chip.text = ch.title ?: "Пункт"
                chip.isClickable = false
                binding.chaptersContainer.addView(chip)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(workId: Long): WorkDetailFragment {
            val f = WorkDetailFragment()
            val args = Bundle()
            args.putLong("workId", workId)
            f.arguments = args
            return f
        }
    }
}