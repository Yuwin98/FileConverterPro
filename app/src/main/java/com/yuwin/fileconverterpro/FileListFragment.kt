package com.yuwin.fileconverterpro


import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.yuwin.fileconverterpro.databinding.FragmentMainScreenBinding
import com.yuwin.fileconverterpro.db.ConvertedFile
import java.io.File


class FileListFragment : BaseFragment(), FileListClickListener, ActionMode.Callback {

    private val binding by lazy{ FragmentMainScreenBinding.inflate(layoutInflater) }
    private val viewModel by lazy { FileListViewModel(requireActivity().application) }

    private val sb = StringBuilder("")

    private var multiSelection = false
    private var selectedFiles = arrayListOf<ConvertedFile>()
    private var selectedLayouts = arrayListOf<ConstraintLayout>()
    private lateinit var actionMode: ActionMode
    private lateinit var rootView: View

    private val filesListAdapter by lazy { FilesListAdapter(this) }
    private val filesGridAdapter by lazy { FilesGridAdapter(this) }
    private var isGrid = false

    private var data: List<ConvertedFile>? = null



    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)

        binding.lifecycleOwner = viewLifecycleOwner


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rootView = binding.filesRecyclerView

        viewModel.readFiles.observe(viewLifecycleOwner, { items ->
            if (items.isNullOrEmpty()) {
                binding.noFilesImageView.visibility = View.VISIBLE
                binding.noFilesTextView.visibility = View.VISIBLE
                data = items
                setupRecyclerView(isGrid)
            } else {
                data = items
                setupRecyclerView(isGrid)
            }
        })

        setupRecyclerView(isGrid)

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.home_action_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.viewChange -> {
                if (!isGrid) {
                    item.setIcon(R.drawable.ic_listview)
                    item.title = "List View"
                    setupGridRecyclerView()
                    isGrid = true
                } else {
                    item.setIcon(R.drawable.ic_gridview)
                    item.title = "Grid View"
                    setupListRecyclerView()
                    isGrid = false
                }
            }
            R.id.deleteAll -> {
                viewModel.clearDatabase()
            }
        }
        return true
    }

    override fun onPause() {
        super.onPause()
        Log.d("heart", "Pause called")
        if(this::actionMode.isInitialized) {
            actionMode.finish()
        }
    }

    private fun setupListRecyclerView() {
        binding.filesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.filesRecyclerView.adapter = filesListAdapter
        data?.let { filesListAdapter.setData(it.toMutableList()) }
    }

    private fun setupGridRecyclerView() {
        binding.filesRecyclerView.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.filesRecyclerView.adapter = filesGridAdapter
        data?.let { filesGridAdapter.setData(it.toMutableList()) }
    }

    private fun setupRecyclerView(isGrid: Boolean) {
        if(isGrid) {
            setupGridRecyclerView()
        }else {
            setupListRecyclerView()
        }
    }

    private fun updateData(newData: List<ConvertedFile>) {
        filesGridAdapter.setData(newData.toMutableList())
        filesListAdapter.setData(newData.toMutableList())
        filesGridAdapter.notifyDataSetChanged()
        filesListAdapter.notifyDataSetChanged()
    }

    private fun convertDatabaseData(item: ConvertedFile) {
        sb.clear()

        sb.append("\n")
        sb.append("ID: ${item.id}")
        sb.append("\n")

        sb.append("File Name: ${item.fileName}")
        sb.append("\n")

        sb.append("File Type: ${item.fileType}")
        sb.append("\n")

        sb.append("File Size: ${item.fileSize}")
        sb.append("\n")

        sb.append("File Path: ${item.filePath}")
        sb.append("\n")

        sb.append("File Uri: ${item.uri}")
        sb.append("\n")

        sb.append("Date Created: ${item.date}")
        sb.append("\n")

        Log.d("convertedFiles", sb.toString())
    }


    override fun onItemClick(layout: ConstraintLayout, position: Int) {
        if(multiSelection) {
            applySelection(layout, position)
        }else {
            val data = data?.get(position)
            openMyFile(data)
        }

    }

    private fun openMyFile(data: ConvertedFile?) {
        if(data?.fileType == "pdf") {
            startPdfOpenActivity(data.filePath)
        }else {
            val action = data?.let { FileListFragmentDirections.actionHomeToImageViewFragment(it) }
            if (action != null) {
                findNavController().navigate(action)
            }
        }

    }

    private fun startPdfOpenActivity(filePath: String) {
        val file = File(filePath)
        val uri = Util.getFileUri(requireActivity(), file)

        val target = Intent()
        target.action = Intent.ACTION_VIEW
        target.setDataAndType(uri, "application/pdf")
        target.flags = FLAG_GRANT_READ_URI_PERMISSION or FLAG_GRANT_WRITE_URI_PERMISSION
        val intent = Intent.createChooser(target, "Open PDF")
        try {
            startActivity(intent)
        }catch (e: ActivityNotFoundException) {
            showSnackBar("There is no app to view PDF files")
        }
    }


    override fun onItemLongClick(layout: ConstraintLayout, position: Int): Boolean {
        return if(!multiSelection) {
            multiSelection = true
            requireActivity().startActionMode(this)
            applySelection(layout, position)
            true
        }else {
            applySelection(layout, position)
            true
        }

    }


    private fun applySelection(layout: ConstraintLayout, position: Int) {
        val currentFile = data?.get(position)
        if(selectedFiles.contains(currentFile)) {
            selectedFiles.remove(currentFile)
            selectedLayouts.remove(layout)
            data?.get(position)?.isSelected = false
            data?.let { updateData(it) }
            setActionModeTitle()
        }else {
            if (currentFile != null) {
                data?.get(position)?.isSelected = true
                data?.let { updateData(it) }
                selectedFiles.add(currentFile)
                selectedLayouts.add(layout)
                setActionModeTitle()
            }
        }
    }

    private fun changeCardStyles(layout: ConstraintLayout, color: Int) {
        if(!isGrid) {
            layout.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), color)
            )
        }else {
            layout.foreground = ColorDrawable(
                    ContextCompat.getColor(requireContext(), color)
            )
        }
    }

    private fun setActionModeTitle() {
        when(selectedFiles.size) {
            0 -> {
                multiSelection = false
                actionMode.finish()
            }
            1 -> {
                actionMode.title = "1 item selected"
            }
            else -> {
                actionMode.title = "${selectedFiles.size} items selected"
            }
        }
    }

    override fun onCreateActionMode(actionMode: ActionMode, menu: Menu?): Boolean {
        actionMode.menuInflater?.inflate(R.menu.action_menu, menu)
        applyStatusBarColor(R.color.contextualStatusBarColor)
        this.actionMode = actionMode
        return true
    }

    override fun onPrepareActionMode(actionMode: ActionMode?, menu: Menu?): Boolean {
        return true
    }

    override fun onActionItemClicked(actionMode: ActionMode?, item: MenuItem?): Boolean {
        when(item?.itemId) {
            R.id.actionDelete -> {
                selectedFiles.forEach {
                    viewModel.deleteSelectedFiles(it)
                }
                showSnackBar("${selectedFiles.size} File(s) deleted")
                multiSelection = false
                selectedFiles.clear()
                actionMode?.finish()
            }
            R.id.actionShare -> {
                val shareIntent = Util.startShareSheetMultiple(requireActivity(), selectedFiles)
                startActivity(Intent.createChooser(shareIntent, "Send File(s) To"))


            }
        }
        return true
    }

    override fun onDestroyActionMode(actionMode: ActionMode?) {
        selectedLayouts.forEach {
            changeCardStyles(it, R.color.colorBackground)
        }
        data?.forEach {
            it.isSelected = false
        }
        data?.let { updateData(it) }
        applyStatusBarColor(R.color.statusBarColor)
        selectedFiles.clear()
        multiSelection = false
    }

    private fun applyStatusBarColor(color: Int) {
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireActivity(), color)
    }


    private fun showSnackBar(message: String) {
        Snackbar.make(
                binding.root,
                message,
                Snackbar.LENGTH_SHORT
        ).setAction("Ok"){}
                .show()
    }
}