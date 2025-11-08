package com.github.stepwise.ui.teacher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.github.stepwise.databinding.FragmentProjectDetailBottomSheetBinding
import com.github.stepwise.network.ApiClient
import com.github.stepwise.network.models.RejectItemDto
import com.github.stepwise.network.models.ExplanatoryNoteItemResponseDto
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProjectDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentProjectDetailBottomSheetBinding? = null
    private val binding get() = _binding!!

    private var projectId: Long = -1L
    private lateinit var itemsAdapter: ExplanatoryItemsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        projectId = arguments?.getLong("projectId", -1L) ?: -1L
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProjectDetailBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        itemsAdapter = ExplanatoryItemsAdapter(
            onViewPdf = { item ->
                // TODO: call backend to download file and open it
                Toast.makeText(requireContext(), "Open PDF for item ${item.id}", Toast.LENGTH_SHORT).show()
            },
            onApprove = { item ->
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
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Ошибка: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            onReject = { item, comment ->
                // reject item with comment via API
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
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Ошибка: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )

        binding.rvItems.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.rvItems.adapter = itemsAdapter

        binding.btnClose.setOnClickListener { dismiss() }

        loadProjectItems()
    }

    private fun loadProjectItems() {
        binding.progressLoading.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val resp = ApiClient.apiService.getProjectById(projectId)
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

                val items: List<ExplanatoryNoteItemResponseDto> = project.items ?: emptyList()
                withContext(Dispatchers.Main) {
                    binding.progressLoading.visibility = View.GONE
                    itemsAdapter.submitList(items)
                    binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.progressLoading.visibility = View.GONE
                    Toast.makeText(requireContext(), "Ошибка: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(projectId: Long): ProjectDetailBottomSheet {
            val b = ProjectDetailBottomSheet()
            val args = Bundle()
            args.putLong("projectId", projectId)
            b.arguments = args
            return b
        }
    }
}