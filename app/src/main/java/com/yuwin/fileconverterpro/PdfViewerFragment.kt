package com.yuwin.fileconverterpro

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yuwin.fileconverterpro.databinding.FragmentPdfViewerBinding
import java.io.File
import java.io.IOException


class PdfViewerFragment : BaseFragment() {

    private val args by navArgs<PdfViewerFragmentArgs>()

    override var bottomNavigationVisibility = View.GONE

    private val CURRENTPAGEINDEX = "current_page_index"

    private var pageNumber: Int = 0

    private lateinit var pdfRenderer: PdfRenderer

    private lateinit var fileDescriptor: ParcelFileDescriptor

    private var currentPage: PdfRenderer.Page? = null

    private lateinit var _binding: FragmentPdfViewerBinding
    private val binding get() = _binding

    private lateinit var viewModel: FilePreviewViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(
            this,
            FilePreviewViewModelFactory(requireActivity().application, args.pdfFile.id))
            .get(FilePreviewViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentPdfViewerBinding.inflate(layoutInflater, container, false)

        _binding.lifecycleOwner = viewLifecycleOwner
        _binding.viewModel = viewModel
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pageNumber = 0

        if (savedInstanceState != null) {
            pageNumber = savedInstanceState.getInt(CURRENTPAGEINDEX, 0)
        }

        binding.nextButton.setOnClickListener {
            currentPage?.index?.plus(1)?.let { it1 -> showCurrentPage(it1) }
        }

        binding.previousButton.setOnClickListener {
            currentPage?.index?.minus(1)?.let { it1 -> showCurrentPage(it1) }
        }


        val file = args.pdfFile

        binding.favoriteOutlineImageView.setOnClickListener {
            if (viewModel.isFavorite.value == true) {
                viewModel.setFavorite(file, false)
            } else {
                viewModel.setFavorite(file, true)
            }
        }

        binding.shareImageView.setOnClickListener {
            val typeString = Util.getSendingType(requireContext(), file)
            val shareIntent =
                Util.startShareSheetSingle(requireActivity(), File(file.filePath), typeString)
            startActivity(Intent.createChooser(shareIntent, "Send To"))
        }

        binding.deleteImageView.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete File?")
                .setMessage("This will delete this file")
                .setPositiveButton("Delete") { dialog, id ->
                    dialog.dismiss()
                    viewModel.deleteFile(file)
                    findNavController().navigate(R.id.action_pdfViewerFragment_to_home)
                }
                .setNegativeButton("Cancel") { dialog, id ->
                    dialog.dismiss()
                }.show()

        }

        binding.editImageView.setOnClickListener {
            val editTextView = layoutInflater.inflate(R.layout.edittext_layout, null)
            MaterialAlertDialogBuilder(requireContext())
                .setView(editTextView)
                .setTitle("Rename File")
                .setPositiveButton("Rename") { dialog, id ->
                    val editText = editTextView.findViewById<EditText>(R.id.renameFileEditText)
                    val name = editText.text.toString()
                    if (name.isNotBlank()) {
                        dialog.dismiss()
                        viewModel.rename(file, name)
                    } else {
                        Toast.makeText(requireContext(), "Name empty", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel") { dialog, id ->
                    dialog.dismiss()
                }
                .show()

        }

    }

    override fun onStart() {
        super.onStart()
        try {
            openPdfRenderer()
            showCurrentPage(pageNumber)
        } catch (exception: IOException) {
            exception.printStackTrace()
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            closePdfRenderer()
        } catch (exception: IOException) {
            exception.printStackTrace()
        }
    }

    private fun closePdfRenderer() {
        currentPage?.close()
        pdfRenderer.close()
        fileDescriptor.close()
    }

    private fun showCurrentPage(index: Int) {
        if (pdfRenderer.pageCount <= index || index < 0) {
            return
        }
        Log.d("pdfpages", "${pdfRenderer.pageCount}: $index")
        currentPage?.close()
        currentPage = pdfRenderer.openPage(index)
        val bitmap = currentPage?.let {
            Bitmap.createBitmap(
                it.width, it.height,
                Bitmap.Config.ARGB_8888
            )
        }

        if (bitmap != null) {
            currentPage?.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        }
        binding.pdfPage.setImageBitmap(bitmap)

    }

    private fun openPdfRenderer() {
        val file = File(args.pdfFile.filePath)
        fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE)
        pdfRenderer = PdfRenderer(fileDescriptor)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(CURRENTPAGEINDEX, pageNumber)
    }


}