package com.github.stepwise.ui.teacher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.stepwise.databinding.FragmentProjectDetailBottomSheetBinding
import com.github.stepwise.network.ApiClient
import com.github.stepwise.network.models.ExplanatoryNoteItemResponseDto
import com.github.stepwise.network.models.RejectItemDto
import com.github.stepwise.network.models.WorkChapterDto
import com.github.stepwise.network.models.WorkResponseDto
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProjectDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentProjectDetailBottomSheetBinding? = null
    private val binding get() = _binding!!

    private var projectId: Long = -1L
    private var workId: Long = -1L

    private lateinit var itemsAdapter: ExplanatoryItemsAdapter
    private var chapterTitles: Map<Int, String> = emptyMap()
    private var isApprovedForDefense: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        projectId = arguments?.getLong("projectId", -1L) ?: -1L
        workId = arguments?.getLong("workId", -1L) ?: -1L
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProjectDetailBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        itemsAdapter = ExplanatoryItemsAdapter(
            chapterTitles = chapterTitles,
            scope = viewLifecycleOwner.lifecycleScope,
            onApprove = { item -> approveItem(item) },
            onReject = { item, comment -> rejectItem(item, comment) }
        )
        binding.rvItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvItems.adapter = itemsAdapter

        binding.btnClose.setOnClickListener { dismiss() }
        binding.btnApproveProject.setOnClickListener { approveProject() }

        loadProjectItems()
    }

    private fun approveItem(item: ExplanatoryNoteItemResponseDto) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val resp = ApiClient.apiService.approveExplanatoryNoteItem(item.id ?: -1L)
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful) {
                        Toast.makeText(requireContext(), "Пункт подтверждён", Toast.LENGTH_SHORT).show()
                        loadProjectItems()
                    } else {
                        Toast.makeText(requireContext(), "Ошибка: ${resp.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Ошибка: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun rejectItem(item: ExplanatoryNoteItemResponseDto, comment: String) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val dto = RejectItemDto(teacherComment = comment)
                val resp = ApiClient.apiService.rejectExplanatoryNoteItem(item.id ?: -1L, dto)
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful) {
                        Toast.makeText(requireContext(), "Пункт отклонён", Toast.LENGTH_SHORT).show()
                        loadProjectItems()
                    } else {
                        Toast.makeText(requireContext(), "Ошибка: ${resp.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Ошибка: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun approveProject() {
        binding.btnApproveProject.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val resp = ApiClient.apiService.approveProject(projectId)
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful) {
                        Toast.makeText(requireContext(), "Проект допущен к защите", Toast.LENGTH_SHORT).show()
                        isApprovedForDefense = true
                        binding.btnApproveProject.visibility = View.GONE
                        loadProjectItems()
                    } else {
                        binding.btnApproveProject.isEnabled = true
                        val msg = try { resp.errorBody()?.string() } catch (_: Exception) { null }
                        Toast.makeText(requireContext(), msg ?: "Ошибка допуска: ${resp.code()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnApproveProject.isEnabled = true
                    Toast.makeText(requireContext(), "Ошибка: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun loadChapterTitlesIfNeeded() {
        if (workId <= 0) {
            chapterTitles = emptyMap()
            return
        }
        try {
            val resp = ApiClient.apiService.getWorkById(workId)
            if (resp.isSuccessful) {
                val work: WorkResponseDto? = resp.body()
                val chapters: List<WorkChapterDto> = work?.academicWorkChapters ?: emptyList()
                chapterTitles = chapters.associate { (it.index ?: 0) to (it.title ?: "") }
            }
        } catch (_: Exception) { }
    }

    private fun loadProjectItems() {
        if (projectId <= 0L) {
            Toast.makeText(requireContext(), "Некорректный id проекта", Toast.LENGTH_SHORT).show()
            return
        }
        binding.progressLoading.visibility = View.VISIBLE
        binding.btnApproveProject.visibility = View.GONE
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val chaptersDeferred = async { loadChapterTitlesIfNeeded() }
                val projectRespDeferred = async { ApiClient.apiService.getProjectByIdForTeacher(projectId) }

                chaptersDeferred.await()
                val resp = projectRespDeferred.await()

                if (!resp.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        binding.progressLoading.visibility = View.GONE
                        Toast.makeText(requireContext(), "Ошибка получения проекта: ${resp.code()}", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val project = resp.body()
                if (project == null) {
                    withContext(Dispatchers.Main) {
                        binding.progressLoading.visibility = View.GONE
                        Toast.makeText(requireContext(), "Проект не найден", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                isApprovedForDefense = project.isApprovedForDefense
                val ownerId = project.owner?.id

                val items: List<ExplanatoryNoteItemResponseDto> =
                    (project.items ?: emptyList()).sortedBy { it.orderNumber ?: Int.MAX_VALUE }

                val allApproved = items.isNotEmpty() && items.all { it.status?.name == "APPROVED" }
                val canShowApproveProject = !isApprovedForDefense && allApproved

                withContext(Dispatchers.Main) {
                    binding.progressLoading.visibility = View.GONE

                    itemsAdapter.updateChapterTitles(chapterTitles)
                    itemsAdapter.updateContext(ownerId, projectId)
                    itemsAdapter.submitList(items)

                    binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE

                    if (canShowApproveProject) {
                        binding.btnApproveProject.visibility = View.VISIBLE
                        binding.btnApproveProject.isEnabled = true
                    } else {
                        binding.btnApproveProject.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.progressLoading.visibility = View.GONE
                    Toast.makeText(requireContext(), "Ошибка: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(projectId: Long, workId: Long): ProjectDetailBottomSheet {
            val b = ProjectDetailBottomSheet()
            val args = Bundle()
            args.putLong("projectId", projectId)
            args.putLong("workId", workId)
            b.arguments = args
            return b
        }
    }
}