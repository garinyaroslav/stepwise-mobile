package com.github.stepwise.ui.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.stepwise.R
import com.github.stepwise.databinding.FragmentStudentProjectsBinding
import com.github.stepwise.network.ApiClient
import com.github.stepwise.network.models.WorkResponseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudentProjectsFragment : Fragment() {

    private var _binding: FragmentStudentProjectsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: StudentWorksAdapter
    private var works: List<WorkResponseDto> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStudentProjectsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = StudentWorksAdapter { work ->
            val bundle = Bundle().apply { putLong("workId", work.id ?: -1L) }
            findNavController().navigate(R.id.student_project_detail_fragment, bundle)

        }
        binding.rvWorks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvWorks.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { loadWorks() }

        loadWorks()
    }

    private fun loadWorks() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val resp = ApiClient.apiService.getStudentWorks()
                withContext(Dispatchers.Main) { binding.swipeRefresh.isRefreshing = false }
                if (!resp.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Ошибка: ${resp.code()}", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                val list = resp.body() ?: emptyList()
                works = list
                withContext(Dispatchers.Main) {
                    adapter.submitList(list)
                    binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.swipeRefresh.isRefreshing = false
                    Toast.makeText(requireContext(), "Ошибка сети: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}