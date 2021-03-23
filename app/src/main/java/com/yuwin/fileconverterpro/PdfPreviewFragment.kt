package com.yuwin.fileconverterpro


import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.yuwin.fileconverterpro.Util.Companion.observeOnce
import com.yuwin.fileconverterpro.databinding.FragmentPdfReviewBinding
import kotlinx.coroutines.*
import java.lang.Exception


class PdfPreviewFragment : BaseFragment(), FileListClickListener {

    override var bottomNavigationVisibility: Int = View.GONE

    private val args by navArgs<PdfPreviewFragmentArgs>()


    private var _binding: FragmentPdfReviewBinding? = null
    private val binding get() = _binding

    private lateinit var pdfUris: List<Uri>

    private var data = mutableListOf<PdfPreviewModel>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)


    private var selectedPages = mutableListOf<Int>()

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
            pdfPreviewViewModel.data.observeOnce(viewLifecycleOwner, { data ->
                if (data.isNullOrEmpty()) {
                    activity?.window?.setFlags(
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    )
                    pdfPreviewViewModel.openPdfRenderer(args.UriList.items[args.currentPdfUri])
                    binding?.pdfPreviewProgress?.max = pdfPreviewViewModel.pageCount
                    scope.launch {
                        val job = async {
                            pdfPreviewViewModel.createPageBitmapList()
                        }
                        job.join()
                    }.invokeOnCompletion {
                        activity?.runOnUiThread {
                            (requireActivity() as AppCompatActivity).supportActionBar?.title =
                                "Preview (${pdfPreviewViewModel.pageCount})"
                            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                            if (data != null) {
                                this.data = data
                            }
                            setupRecyclerView()
                            pdfPreviewViewModel.selectedPage.observeOnce(
                                viewLifecycleOwner,
                                { selectedPageList ->
                                    if (!selectedPageList.isNullOrEmpty()) {
                                        this.selectedPages = selectedPageList
                                        (requireActivity() as AppCompatActivity).supportActionBar?.title =
                                            "Preview (${selectedPages.size}/${pdfPreviewViewModel.pageCount})"
                                    }
                                })

                        }

                    }
                } else {
                    this.data = data
                    setupRecyclerView()
                    pdfPreviewViewModel.selectedPage.observeOnce(
                        viewLifecycleOwner,
                        { selectedPageList ->
                            if (!selectedPageList.isNullOrEmpty()) {
                                this.selectedPages = selectedPageList
                                (requireActivity() as AppCompatActivity).supportActionBar?.title =
                                    "Preview (${selectedPages.size}/${pdfPreviewViewModel.pageCount})"
                            }else {
                                (requireActivity() as AppCompatActivity).supportActionBar?.title =
                                    "Preview (${pdfPreviewViewModel.pageCount})"
                            }
                        })

                }
            })

        } catch (e: Exception) {
            e.printStackTrace()
        }


    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.pdf_preview_menu, menu)
        if(pdfPreviewViewModel.isAllSelected) {
            menu.findItem(R.id.selectAll).setIcon(R.drawable.ic_select_all)
        }else {
            menu.findItem(R.id.selectAll).setIcon(R.drawable.ic_done_all_white)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.selectAll -> {
                if (pdfPreviewViewModel.isAllSelected) {
                    item.setIcon(R.drawable.ic_done_all_white)
                    pdfPreviewViewModel.isAllSelected = false
                    selectAll(pdfPreviewViewModel.isAllSelected)
                } else {
                    item.setIcon(R.drawable.ic_select_all)
                    pdfPreviewViewModel.isAllSelected = true
                    selectAll(pdfPreviewViewModel.isAllSelected)
                }
            }
            R.id.nextButton -> {
                val selectedPageInfo = SelectedPageInfo(
                    args.currentPdfUri,
                    pdfPreviewViewModel.pageCount,
                    pdfPreviewViewModel.selectedPage.value!!.toList()
                )
                val pageInfoList: SelectedPageInfoList = if (args.pageInfoList == null) {
                    SelectedPageInfoList(
                        listOf(selectedPageInfo)
                    )
                } else {
                    val pdfInfoList = args.pageInfoList!!.items.toMutableList()
                    pdfInfoList.add(selectedPageInfo)
                    SelectedPageInfoList(
                        pdfInfoList.toList()
                    )
                }

                val action: NavDirections

                if (isLastPdf()) {
                    action = PdfPreviewFragmentDirections.actionPdfReviewFragmentToConvert(
                        args.UriList,
                        args.convertAll,
                        args.convertInto,
                        args.isPdfMerge,
                        args.mergeImagesIntoPdf,
                        args.singleImageToPdf,
                        args.pdfIntoImages,
                        pageInfoList
                    )
                } else {
                    action = PdfPreviewFragmentDirections.actionPdfReviewFragmentSelf(
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
        return if (scope.isActive) {
            scope.cancel()
            super.onOptionsItemSelected(item)
        } else {
            super.onOptionsItemSelected(item)
        }

    }

    private fun isLastPdf(): Boolean {
        return (args.UriList.items.size - 1) == args.currentPdfUri
    }

    private fun setupRecyclerView() {
        binding?.pdfPreviewRecyclerView?.layoutManager = GridLayoutManager(requireContext(), 4)
        binding?.pdfPreviewRecyclerView?.adapter = pdfPreviewGridAdapter
        pdfPreviewGridAdapter.setData(data)
        binding?.pdfPreviewProgress?.visibility = View.GONE
        binding?.pdfPreviewProgressTextView?.visibility = View.GONE
    }


    private fun selectAll(isSelected: Boolean) {
        data.forEach { it.isSelected = isSelected }

        selectedPages.clear()
        if (isSelected) {
            selectedPages.addAll(0 until pdfPreviewViewModel.pageCount)
            (requireActivity() as AppCompatActivity).supportActionBar?.title =
                "Preview (${selectedPages.size}/${pdfPreviewViewModel.pageCount})"
        } else {
            (requireActivity() as AppCompatActivity).supportActionBar?.title =
                "Preview (${pdfPreviewViewModel.pageCount})"
        }
        pdfPreviewViewModel.updateSelectedPages(selectedPages)
        pdfPreviewViewModel.updateDataList(data)
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
        if (data[position].isSelected) {
            selectedPages.add(position)
            (requireActivity() as AppCompatActivity).supportActionBar?.title =
                "Preview (${selectedPages.size}/${pdfPreviewViewModel.pageCount})"
        } else {
            selectedPages.remove(position)
            if (selectedPages.isEmpty()) {
                (requireActivity() as AppCompatActivity).supportActionBar?.title =
                    "Preview (${pdfPreviewViewModel.pageCount})"
            } else {
                (requireActivity() as AppCompatActivity).supportActionBar?.title =
                    "Preview (${selectedPages.size}/${pdfPreviewViewModel.pageCount})"
            }

        }
        pdfPreviewViewModel.updateSelectedPages(selectedPages)
        pdfPreviewViewModel.updateDataList(data)
        updateDataItem(data, position)
    }

    override fun onItemLongClick(position: Int): Boolean {
        return false
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding?.pdfPreviewRecyclerView?.adapter = null
        _binding = null
    }


}