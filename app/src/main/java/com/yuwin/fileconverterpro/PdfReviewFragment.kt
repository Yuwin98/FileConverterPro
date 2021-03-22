package com.yuwin.fileconverterpro

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.*
import androidx.fragment.app.viewModels
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.yuwin.fileconverterpro.databinding.FragmentPdfReviewBinding
import kotlinx.coroutines.*
import java.io.IOException
import java.lang.Exception


class PdfReviewFragment : BaseFragment(), FileListClickListener {

    override var bottomNavigationVisibility: Int = View.GONE

    private val args by navArgs<PdfReviewFragmentArgs>()


    private var pageCount: Int = 0

    private var currentPage: PdfRenderer.Page? = null

    private lateinit var pdfRenderer: PdfRenderer

    private lateinit var fileDescriptor: ParcelFileDescriptor

    private var _binding: FragmentPdfReviewBinding? = null
    private val binding get() = _binding

    private lateinit var pdfUris: List<Uri>

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val data = mutableListOf<PdfPreviewModel>()

    private val selectedPages = mutableListOf<Int>()

    private var isAllSelected = false

    private val pdfPreviewGridAdapter by lazy { PdfPreviewGridAdapter(this) }

    private val pdfPreviewViewModel: PdfPreviewViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPdfReviewBinding.inflate(inflater, container, false)
        _binding?.lifecycleOwner = viewLifecycleOwner
        _binding?.viewModel = pdfPreviewViewModel
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pdfUris = args.UriList.items
        setHasOptionsMenu(true)
        try {
            openPdfRenderer()
            binding?.pdfPreviewProgress?.max = pageCount
            scope.launch {
                val job = async {
                    createPageBitmapList()
                }
                job.join()
            }.invokeOnCompletion {
                activity?.runOnUiThread {
                    setupRecyclerView()
                }
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }


    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.pdf_preview_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.selectAll -> {
                if(isAllSelected) {
                    item.setIcon(R.drawable.ic_done_all_white)
                    isAllSelected = false
                    selectAll(isAllSelected)
                }else {
                    item.setIcon(R.drawable.ic_select_all)
                    isAllSelected = true
                    selectAll(isAllSelected)
                }
            }
            R.id.nextButton -> {
                val selectedPageInfo = SelectedPageInfo(
                    args.currentPdfUri,
                    selectedPages
                )
                val pageInfoList: SelectedPageInfoList = if(args.pageInfoList == null) {
                    SelectedPageInfoList(
                        listOf(selectedPageInfo)
                    )
                }else {
                    val pdfInfoList = args.pageInfoList!!.items.toMutableList()
                    pdfInfoList.add(selectedPageInfo)
                    SelectedPageInfoList(
                        pdfInfoList.toList()
                    )
                }

                var action: NavDirections

                if(isLastPdf()) {
                    action = PdfReviewFragmentDirections.actionPdfReviewFragmentToConvert(
                        args.UriList,
                        args.convertAll,
                        args.convertInto,
                        args.isPdfMerge,
                        args.mergeImagesIntoPdf,
                        args.singleImageToPdf,
                        args.pdfIntoImages,
                        pageInfoList
                    )
                }else {
                    action = PdfReviewFragmentDirections.actionPdfReviewFragmentSelf(
                        args.UriList,
                        args.convertAll,
                        args.convertInto,
                        args.isPdfMerge,
                        args.mergeImagesIntoPdf,
                        args.singleImageToPdf,
                        args.pdfIntoImages,
                        args.currentPdfUri + 1,
                        pageInfoList
                    )
                }
                
                findNavController().navigate(action)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun isLastPdf(): Boolean {
        return  (args.UriList.items.size - 1) == args.currentPdfUri
    }

    private fun setupRecyclerView() {
        binding?.pdfPreviewRecyclerView?.layoutManager = GridLayoutManager(requireContext(), 4)
        binding?.pdfPreviewRecyclerView?.adapter = pdfPreviewGridAdapter
        pdfPreviewGridAdapter.setData(data)
        binding?.pdfPreviewProgress?.visibility = View.GONE
        binding?.pdfPreviewProgressTextView?.visibility = View.GONE
    }

    private fun openPdfRenderer() {
        fileDescriptor = requireContext().contentResolver.openFileDescriptor(pdfUris[args.currentPdfUri], "r")!!
        pdfRenderer = PdfRenderer(fileDescriptor)
        pageCount = pdfRenderer.pageCount
    }

    private fun closePdfRenderer() {
        currentPage?.close()
        pdfRenderer.close()
        fileDescriptor.close()
    }

    private fun createPageBitmapList() {

        for (i in 0 until pageCount) {
            currentPage?.close()
            currentPage = pdfRenderer.openPage(i)
            val bitmap = currentPage?.let {
                Bitmap.createBitmap(
                    it.width, it.height,
                    Bitmap.Config.ARGB_8888
                )
            }

            if (bitmap != null) {
                bitmap.eraseColor(Color.WHITE)
                Canvas(bitmap).drawBitmap(bitmap, 0f, 0f, null)
                currentPage?.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                activity?.runOnUiThread {
                    pdfPreviewViewModel.incrementProgressValue()
                    pdfPreviewViewModel.changeProgressText(pageCount)
                }
                val dataItem = PdfPreviewModel(bitmap, false)
                data.add(dataItem)
            }

        }


    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            closePdfRenderer()
        } catch (exception: IOException) {
            exception.printStackTrace()
        }
    }

    private fun selectAll(isSelected: Boolean) {
        data.forEach { it.isSelected = isSelected }
        updateData(data)
    }


    private fun updateDataItem(newData: List<PdfPreviewModel>, pos: Int) {
        pdfPreviewGridAdapter.setData(newData.toMutableList())
        pdfPreviewGridAdapter.notifyItemChanged(pos)
    }

    private fun updateData(newData: List<PdfPreviewModel>) {
        pdfPreviewGridAdapter.setData(newData.toMutableList())
        pdfPreviewGridAdapter.notifyDataSetChanged()
    }

    override fun onItemClick(position: Int) {
        data[position].isSelected = !data[position].isSelected
        if(data[position].isSelected) {
            selectedPages.add(position)
        }else {
            selectedPages.remove(position)
        }
        Log.d("selectedPages", selectedPages.toString())
        updateDataItem(data, position)
    }

    override fun onItemLongClick(position: Int): Boolean {
        return false
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }



}