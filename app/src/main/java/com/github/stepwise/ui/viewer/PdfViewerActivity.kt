package com.github.stepwise.ui.viewer

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.stepwise.R
import com.github.stepwise.databinding.ActivityPdfViewerBinding
import java.io.File

class PdfViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Просмотр PDF"

        val path = intent.getStringExtra("pdf_path") ?: return finish()
        val file = File(path)
        if (!file.exists()) {
            finish()
            return
        }

        binding.pdfView.fromFile(file)
            .enableSwipe(true)
            .swipeHorizontal(false)
            .enableDoubletap(true)
            .defaultPage(0)
            .load()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}