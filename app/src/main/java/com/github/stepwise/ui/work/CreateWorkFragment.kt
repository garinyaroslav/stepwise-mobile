package com.github.stepwise.ui.work

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatSpinner
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.stepwise.databinding.FragmentCreateWorkBinding
import com.github.stepwise.network.ApiClient
import com.github.stepwise.network.models.CreateWorkReq
import com.github.stepwise.network.models.ProjectType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import java.text.SimpleDateFormat

class CreateWorkFragment : Fragment() {
    private var _binding: FragmentCreateWorkBinding? = null
    private val binding get() = _binding!!
    private lateinit var chaptersAdapter: ChaptersAdapter
    private val chapterStates = mutableListOf<ChaptersAdapter.ChapterState>()
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    private val typeDisplay = listOf("Курсовая", "Дипломная работа")
    private val typeValues = listOf(ProjectType.COURSEWORK, ProjectType.THESIS)
    private var selectedTypeIndex: Int = 0

    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreateWorkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTypeSpinner()

        chaptersAdapter = ChaptersAdapter(
            chapterStates,
            onRemove = { pos ->
                if (pos >= 0 && pos < chapterStates.size) {
                    chapterStates.removeAt(pos)
                    chaptersAdapter.notifyDataSetChanged()
                }
            },
            onPickDate = { pos -> showDatePickerFor(pos) }
        )

        binding.rvChapters.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChapters.adapter = chaptersAdapter

        binding.buttonAddChapter.setOnClickListener {
            chapterStates.add(ChaptersAdapter.ChapterState())
            chaptersAdapter.notifyItemInserted(chapterStates.size - 1)
        }

        binding.buttonCancelCreate.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.buttonSaveWork.setOnClickListener {
            submitWork()
        }
        binding.etGroupId.apply {
            isFocusable = false
            isClickable = true
            isLongClickable = false
            setOnClickListener {
                val dlg = GroupSearchDialog()
                dlg.show(parentFragmentManager, "group_search")
            }
        }

        parentFragmentManager.setFragmentResultListener("group_selected", viewLifecycleOwner) { _, bundle ->
            val gid = bundle.getLong("groupId")
            val gname = bundle.getString("groupName") ?: gid.toString()
            binding.etGroupId.setText(gname)
            binding.etGroupId.tag = gid
        }
    }

    private fun setupTypeSpinner() {
        val spinner = binding.spinnerType as AppCompatSpinner
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, typeDisplay)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        val icons = intArrayOf(
            android.R.drawable.ic_menu_edit,
            android.R.drawable.ic_menu_agenda
        )
        val typeAdapter = TypeSpinnerAdapter(requireContext(), typeDisplay, icons)
        spinner.adapter = typeAdapter

        spinner.setSelection(selectedTypeIndex, false)

        try {
            spinner.setPopupBackgroundResource(android.R.color.white)
        } catch (e: Exception) {
        }

        spinner.post {
            try {
                spinner.dropDownWidth = spinner.width
            } catch (_: Exception) {}
        }

        spinner.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                spinner.performClick()
                return@setOnTouchListener true
            }
            false
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedTypeIndex = position
            }

            override fun onNothingSelected(parent: AdapterView<*>?) { /* no-op */ }
        }
    }

    private fun showDatePickerFor(position: Int) {
        val cal = Calendar.getInstance()
        val dp = DatePickerDialog(requireContext(), { _, y, m, d ->
            val c = Calendar.getInstance()
            c.set(y, m, d, 23, 59, 59)
            val iso = isoFormat.format(c.time)
            if (position in chapterStates.indices) {
                chapterStates[position].deadlineIso = iso
                chaptersAdapter.notifyItemChanged(position)
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
        dp.show()
    }

    private fun submitWork() {
        val title = binding.etWorkTitle.text?.toString()?.trim().orEmpty()
        val description = binding.etWorkDescription.text?.toString()?.trim().orEmpty()

        val typeEnum = typeValues.getOrNull(selectedTypeIndex) ?: ProjectType.COURSEWORK

        val groupId = (binding.etGroupId.tag as? Long) ?: run {
            binding.tilGroupId.error = "Выберите группу"
            return
        }

        if (title.length < 3) {
            binding.tilWorkTitle.error = "Введите название (мин. 3 символа)"
            return
        } else {
            binding.tilWorkTitle.error = null
        }

        if (chapterStates.isEmpty()) {
            Toast.makeText(requireContext(), "Добавьте хотя бы одну часть", Toast.LENGTH_SHORT).show()
            return
        }

        for ((index, chapter) in chapterStates.withIndex()) {
            if (chapter.title.isBlank()) {
                Toast.makeText(requireContext(), "Часть ${index + 1}: укажите название", Toast.LENGTH_SHORT).show()
                return
            }
            if (chapter.deadlineIso.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Часть ${index + 1}: укажите дедлайн", Toast.LENGTH_SHORT).show()
                return
            }
        }

        binding.buttonSaveWork.isEnabled = false
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val profileResp = ApiClient.apiService.getMyProfile()
                if (!profileResp.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        binding.buttonSaveWork.isEnabled = true
                        Toast.makeText(requireContext(), "Не удалось получить профиль: ${profileResp.code()}", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                val profile = profileResp.body()
                val teacherId = profile?.id
                if (teacherId == null) {
                    withContext(Dispatchers.Main) {
                        binding.buttonSaveWork.isEnabled = true
                        Toast.makeText(requireContext(), "Не удалось получить id преподавателя", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val chaptersReq = chaptersAdapter.toRequestChapters()
                val req = CreateWorkReq(
                    title = title,
                    description = description.ifBlank { null },
                    type = typeEnum,
                    groupId = groupId,
                    teacherId = teacherId,
                    chapters = chaptersReq
                )

                val resp = ApiClient.apiService.createWork(req)
                withContext(Dispatchers.Main) {
                    binding.buttonSaveWork.isEnabled = true
                    if (resp.isSuccessful) {
                        Toast.makeText(requireContext(), "Работа успешно создана", Toast.LENGTH_SHORT).show()
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    } else {
                        Toast.makeText(requireContext(), "Ошибка создания: ${resp.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.buttonSaveWork.isEnabled = true
                    Toast.makeText(requireContext(), "Ошибка: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}