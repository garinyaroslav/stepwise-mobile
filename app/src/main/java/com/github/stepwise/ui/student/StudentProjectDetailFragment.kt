package com.github.stepwise.ui.student

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.stepwise.databinding.FragmentStudentProjectDetailBinding
import com.github.stepwise.network.ApiClient
import com.github.stepwise.network.models.ExplanatoryNoteItemResponseDto
import com.github.stepwise.network.models.ProjectResponseDto
import com.github.stepwise.network.models.UpdateProjectDto
import com.github.stepwise.network.models.WorkChapterDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import java.io.File
import java.io.InputStream

class StudentProjectDetailFragment : Fragment() {
    private var _binding: FragmentStudentProjectDetailBinding? = null
    private val binding get() = _binding!!
    private var workId: Long = -1L
    private var project: ProjectResponseDto? = null
    private var chapters: List<WorkChapterDto> = emptyList()
    private var items: List<ExplanatoryNoteItemResponseDto> = emptyList()
    private lateinit var chaptersAdapter: ProjectChaptersAdapter
    private var pendingChapterIndex: Int = -1

    private val pickPdf = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val uri: Uri? = result.data?.data
        if (uri != null) uploadFileForChapter(uri, pendingChapterIndex)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workId = arguments?.getLong("workId", -1L) ?: -1L
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStudentProjectDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chaptersAdapter = ProjectChaptersAdapter(
            onAttach = { chapterIndex -> onAttachClicked(chapterIndex) },
            onView = { item -> openPdfForItem(item) },
            onSubmit = { item -> submitExplanatoryItem(item) }
        )
        binding.rvChapters.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChapters.adapter = chaptersAdapter

        binding.swipeRefresh.setOnRefreshListener { loadData() }
        binding.tvProjectTitle.setOnClickListener { showEditProjectDialog() }

        loadData()
    }

    private fun loadData() {
        binding.swipeRefresh.isRefreshing = true
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val workResp = ApiClient.apiService.getWorkById(workId)
                if (!workResp.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        binding.swipeRefresh.isRefreshing = false
                        Toast.makeText(requireContext(), "Ошибка загрузки работы: ${workResp.code()}", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                val work = workResp.body()

                val pResp = ApiClient.apiService.getProjectsByWork(workId)
                if (!pResp.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        binding.swipeRefresh.isRefreshing = false
                        Toast.makeText(requireContext(), "Ошибка загрузки проекта: ${pResp.code()}", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                val projects = pResp.body() ?: emptyList()
                val projectDto = projects.firstOrNull()

                withContext(Dispatchers.Main) {
                    binding.swipeRefresh.isRefreshing = false
                    if (work == null) {
                        Toast.makeText(requireContext(), "Работа не найдена", Toast.LENGTH_SHORT).show()
                        return@withContext
                    }
                    chapters = (work.academicWorkChapters ?: emptyList()).sortedBy { it.index }
                    project = projectDto
                    items = projectDto?.items ?: emptyList()

                    renderHeader(work.title ?: "", projectDto)
                    val display = chapters.map { ch ->
                        val item = items.find { it.orderNumber == ch.index }
                        ch to item
                    }
                    chaptersAdapter.submitList(display)
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

    private fun renderHeader(title: String, projectDto: ProjectResponseDto?) {
        binding.tvTitle.text = title
        val total = chapters.size
        val approved = items.count { it.status?.name == "APPROVED" }
        binding.tvProgress.text = "$approved / $total"
        binding.progressIndicator.max = if (total > 0) total else 100
        binding.progressIndicator.progress = approved
        binding.tvProjectTitle.text = projectDto?.title ?: "Мой проект"
        binding.tvProjectStatus.text = if (projectDto?.isApprovedForDefense == true) "Допущен к защите" else "Не допущён"
        setupDescription(projectDto?.description)
    }

    private fun setupDescription(description: String?) {
        val desc = description ?: ""
        if (desc.isBlank()) {
            binding.tvProjectDescription.visibility = View.GONE
            binding.tvToggleDescription.visibility = View.GONE
            return
        }
        binding.tvProjectDescription.visibility = View.VISIBLE
        binding.tvProjectDescription.text = desc
        binding.tvProjectDescription.maxLines = 3
        binding.tvProjectDescription.ellipsize = TextUtils.TruncateAt.END
        binding.tvToggleDescription.visibility = View.GONE
        binding.tvProjectDescription.post {
            if (binding.tvProjectDescription.lineCount > 3) {
                binding.tvToggleDescription.visibility = View.VISIBLE
                binding.tvToggleDescription.text = "Читать полностью"
                binding.tvToggleDescription.setOnClickListener {
                    val expanded = binding.tvProjectDescription.maxLines == Int.MAX_VALUE
                    if (expanded) {
                        binding.tvProjectDescription.maxLines = 3
                        binding.tvProjectDescription.ellipsize = TextUtils.TruncateAt.END
                        binding.tvToggleDescription.text = "Читать полностью"
                    } else {
                        binding.tvProjectDescription.maxLines = Int.MAX_VALUE
                        binding.tvProjectDescription.ellipsize = null
                        binding.tvToggleDescription.text = "Свернуть"
                    }
                }
            } else {
                binding.tvToggleDescription.visibility = View.GONE
            }
        }
    }

    private fun onAttachClicked(chapterIndex: Int) {
        pendingChapterIndex = chapterIndex
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }
        pickPdf.launch(intent)
    }

    private fun uploadFileForChapter(uri: Uri, chapterIndex: Int) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fname = queryFileName(uri) ?: "file.pdf"
                val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Невозможно прочитать файл", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val tmp = File(requireContext().cacheDir, "upload_${System.currentTimeMillis()}_${fname}")
                inputStream.use { input -> tmp.outputStream().use { out -> input.copyTo(out) } }

                val reqFile = okhttp3.RequestBody.create("application/pdf".toMediaTypeOrNull(), tmp)
                val part = MultipartBody.Part.createFormData("file", tmp.name, reqFile)
                val projectId = project?.id ?: run {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Проект не найден", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val projectIdBody = okhttp3.RequestBody.create("text/plain".toMediaTypeOrNull(), projectId.toString())

                withContext(Dispatchers.Main) { binding.progressBar.visibility = View.VISIBLE }

                val resp = ApiClient.apiService.createExplanatoryNoteItem(projectIdBody, part)

                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    if (resp.isSuccessful) {
                        Toast.makeText(requireContext(), "Файл отправлен (черновик)", Toast.LENGTH_SHORT).show()
                        loadData()
                    } else {
                        Toast.makeText(requireContext(), "Ошибка загрузки: ${resp.code()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Ошибка: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun queryFileName(uri: Uri): String? {
        var name: String? = null
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = it.getString(idx)
            }
        }
        return name
    }

    private fun openPdfForItem(item: ExplanatoryNoteItemResponseDto?) {
        if (item == null || item.id == null) {
            Toast.makeText(requireContext(), "Файл не найден", Toast.LENGTH_SHORT).show()
            return
        }
        val ownerId = project?.owner?.id
        val projectId = project?.id
        val itemId = item.id
        if (ownerId == null || projectId == null) {
            Toast.makeText(requireContext(), "Не удалось определить владельца проекта", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val resp = ApiClient.apiService.downloadItemFile(ownerId, projectId, itemId)
                if (!resp.isSuccessful || resp.body() == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Ошибка загрузки файла: ${resp.code()}", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                val inputStream = resp.body()!!.byteStream()
                val file = File(requireContext().cacheDir, "project_${projectId}_item_${itemId}.pdf")
                file.outputStream().use { out -> inputStream.copyTo(out) }

                withContext(Dispatchers.Main) {
                    val intent = android.content.Intent(requireContext(), com.github.stepwise.ui.viewer.PdfViewerActivity::class.java)
                    intent.putExtra("pdf_path", file.absolutePath)
                    startActivity(intent)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Ошибка: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun submitExplanatoryItem(item: ExplanatoryNoteItemResponseDto) {
        val itemId = item.id ?: run {
            Toast.makeText(requireContext(), "Неверный id пункта", Toast.LENGTH_SHORT).show()
            return
        }
        binding.swipeRefresh.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val resp = ApiClient.apiService.submitExplanatoryNoteItem(itemId)
                withContext(Dispatchers.Main) {
                    binding.swipeRefresh.isEnabled = true
                    if (resp.isSuccessful) {
                        Toast.makeText(requireContext(), "Пункт отправлен на проверку", Toast.LENGTH_SHORT).show()
                        loadData()
                    } else {
                        val msg = try { resp.errorBody()?.string() } catch (_: Throwable) { null }
                        Toast.makeText(requireContext(), msg ?: "Ошибка отправки: ${resp.code()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.swipeRefresh.isEnabled = true
                    Toast.makeText(requireContext(), "Ошибка сети: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showEditProjectDialog() {
        val proj = project ?: run {
            Toast.makeText(requireContext(), "Проект не загружен", Toast.LENGTH_SHORT).show()
            return
        }

        val ctx = requireContext()
        val lp = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 12, 20, 0)
        }
        val etTitle = EditText(ctx).apply {
            hint = "Название проекта"
            setText(proj.title ?: "")
        }
        val etDesc = EditText(ctx).apply {
            hint = "Описание проекта"
            setText(proj.description ?: "")
            minLines = 3
        }
        lp.addView(etTitle)
        lp.addView(etDesc)

        val dlg = AlertDialog.Builder(ctx)
            .setTitle("Редактировать проект")
            .setView(lp)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Сохранить", null)
            .create()

        dlg.setOnShowListener {
            val positive = dlg.getButton(AlertDialog.BUTTON_POSITIVE)
            positive.setOnClickListener {
                val newTitle = etTitle.text?.toString()?.trim().orEmpty()
                val newDesc = etDesc.text?.toString()?.trim().orEmpty()
                if (newTitle.length < 3) {
                    Toast.makeText(ctx, "Название должно быть не менее 3 символов", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                positive.isEnabled = false
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val dto = UpdateProjectDto(id = proj.id ?: -1L, title = newTitle, description = newDesc)
                        val resp = ApiClient.apiService.updateProject(dto)
                        withContext(Dispatchers.Main) {
                            positive.isEnabled = true
                            if (resp.isSuccessful) {
                                Toast.makeText(ctx, "Проект успешно обновлён", Toast.LENGTH_SHORT).show()
                                dlg.dismiss()
                                loadData()
                            } else {
                                val msg = try { resp.errorBody()?.string() } catch (_: Throwable) { null }
                                Toast.makeText(ctx, msg ?: "Ошибка обновления: ${resp.code()}", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            positive.isEnabled = true
                            Toast.makeText(ctx, "Ошибка сети: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        dlg.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}