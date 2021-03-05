package com.yuwin.fileconverterpro


import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yuwin.fileconverterpro.databinding.FragmentMainScreenBinding
import com.yuwin.fileconverterpro.db.ConvertedFile
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.util.*


class FileListFragment : BaseFragment(), FileListClickListener, ActionMode.Callback {

    private var _binding: FragmentMainScreenBinding? = null
    private val binding get() = _binding
    private var viewModel: FileListViewModel? = null

    private val sb = StringBuilder("")

    private var multiSelection = false
    private var selectedFiles = arrayListOf<ConvertedFile>()
    private lateinit var actionMode: ActionMode

    private val filesListAdapter by lazy { FilesListAdapter(this) }
    private val filesGridAdapter by lazy { FilesGridAdapter(this) }
    private var isGrid = false

    private var data: List<ConvertedFile>? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        _binding = FragmentMainScreenBinding.inflate(inflater, container, false)
        binding?.lifecycleOwner = viewLifecycleOwner
        viewModel = ViewModelProvider(this).get(FileListViewModel::class.java)

        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        viewModel?.readFiles?.observe(viewLifecycleOwner, { items ->
            if (items.isNullOrEmpty()) {
                binding?.let {
                    it.noFilesImageView.visibility = View.VISIBLE
                    it.noFilesTextView.visibility = View.VISIBLE
                }
                data = items
                setupRecyclerView(isGrid)

            } else {
                data = items
                data = items.filter { file -> !file.inDirectory }
                setupRecyclerView(isGrid)

            }
        })

        setupRecyclerView(isGrid)

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.home_action_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.viewChange -> {
                if (!isGrid) {
                    item.setIcon(R.drawable.ic_listview)
                    item.title = "List View"
                    isGrid = true
                    setupRecyclerView(isGrid)
                } else {
                    item.setIcon(R.drawable.ic_gridview)
                    item.title = "Grid View"
                    isGrid = false
                    setupRecyclerView(isGrid)
                }
            }
            R.id.deleteAll -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Clear Database")
                    .setMessage("This will delete all converted files")
                    .setPositiveButton("Clear") { dialog, _ ->
                        viewModel?.clearDatabase()
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
            R.id.sortFileSize -> {
                viewModel?.readFilesBySize?.observe(viewLifecycleOwner, { items ->
                    filterSortAndUpdateData(items)
                })
            }
            R.id.sortFileName -> {
                viewModel?.readFilesByName?.observe(viewLifecycleOwner, { items ->
                    filterSortAndUpdateData(items)
                })
            }
            R.id.sortFileDate -> {
                viewModel?.readFilesByDate?.observe(viewLifecycleOwner, { items ->
                    filterSortAndUpdateData(items)
                })
            }
            R.id.sortFileType -> {
                viewModel?.readFilesByType?.observe(viewLifecycleOwner, { items ->
                    filterSortAndUpdateData(items)
                })
            }
            R.id.filterJpg -> {
                viewModel?.filterFilesByJpgJpeg?.observe(viewLifecycleOwner, { items ->
                    filterSortAndUpdateData(items)
                })
            }
            R.id.filterPdf -> {
                viewModel?.filterFilesByPdf?.observe(viewLifecycleOwner, { items ->
                    filterSortAndUpdateData(items)
                })
            }
            R.id.filterPng -> {
                viewModel?.filterFilesByPng?.observe(viewLifecycleOwner, { items ->
                    filterSortAndUpdateData(items)
                })
            }
            R.id.filterWebp -> {
                viewModel?.filterFilesByWebp?.observe(viewLifecycleOwner, { items ->
                    filterSortAndUpdateData(items)
                })
            }
        }
        return true
    }

    override fun onPause() {
        super.onPause()
        Log.d("heart", "Pause called")
        if (this::actionMode.isInitialized) {
            actionMode.finish()
        }
    }

    private fun filterSortAndUpdateData(items: List<ConvertedFile>) {
        if (items.isNullOrEmpty()) {
            data = items
            binding?.let {
                it.noFilesImageView.visibility = View.VISIBLE
                it.noFilesTextView.visibility = View.VISIBLE
            }
            updateData(data!!.toMutableList())
        } else {
            data = items
            data = items.filter { file -> !file.inDirectory }
            binding?.let {
                it.noFilesImageView.visibility = View.GONE
                it.noFilesTextView.visibility = View.GONE
            }
            updateData(data!!.toMutableList())
        }
    }

    private fun setupListRecyclerView() {
        binding?.let {
            it.filesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            it.filesRecyclerView.adapter = filesListAdapter

        }
        data?.let { filesListAdapter.setData(it.toMutableList()) }
    }

    private fun setupGridRecyclerView() {
        binding?.let {
            it.filesRecyclerView.layoutManager = GridLayoutManager(requireContext(), 4)
            it.filesRecyclerView.adapter = filesGridAdapter
        }
        data?.let { filesGridAdapter.setData(it.toMutableList()) }
    }

    private fun setupRecyclerView(isGrid: Boolean) {
        if (isGrid) {
            setupGridRecyclerView()
        } else {
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


    override fun onItemClick(position: Int) {
        if (multiSelection) {
            applySelection(position)
        } else {
            val data = data?.get(position)
            data?.let {
                if (it.isDirectory) {
                    val action = FileListFragmentDirections.actionHomeToDirectoryViewFragment(data)
                    findNavController().navigate(action)
                } else {
                    openMyFile(data)

                }
            }
        }

    }


    private fun openMyFile(data: ConvertedFile?) {
        if (data?.fileType == "pdf") {
            val action = FileListFragmentDirections.actionHomeToPdfViewerFragment(data)
            findNavController().navigate(action)
        } else {
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
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "No app to view pdf files", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onItemLongClick(position: Int): Boolean {
        return if (!multiSelection) {
            multiSelection = true
            requireActivity().startActionMode(this)
            applySelection(position)
            true
        } else {
            applySelection(position)
            true
        }

    }


    private fun applySelection(position: Int) {
        val currentFile = data?.get(position)
        if (selectedFiles.contains(currentFile)) {
            selectedFiles.remove(currentFile)
            data?.get(position)?.isSelected = false
            data?.let { updateData(it) }
            setActionModeTitle()
        } else {
            if (currentFile != null) {
                data?.get(position)?.isSelected = true
                data?.let { updateData(it) }
                selectedFiles.add(currentFile)
                setActionModeTitle()
            }
        }
    }


    private fun setActionModeTitle() {
        when (selectedFiles.size) {
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
        when (item?.itemId) {
            R.id.actionMoveToFolder -> {
                val editTextView = layoutInflater.inflate(R.layout.edittext_layout, null)
                MaterialAlertDialogBuilder(requireContext())
                    .setView(editTextView)
                    .setTitle("Create Folder")
                    .setMessage("This will create a folder and move selected files into it")
                    .setPositiveButton("Create") { dialog, _ ->
                        val editText = editTextView.findViewById<EditText>(R.id.renameFileEditText)
                        val name = editText.text.toString()
                        if (name.isNotBlank()) {
                            val folderPath = createFileDirectory(name)
                            if (folderPath.isNotBlank()) {
                                moveSelectedFilesToDirectory(selectedFiles, folderPath)
                            }
                            multiSelection = false
                            selectedFiles.clear()
                            actionMode?.finish()
                            dialog.dismiss()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Folder Name empty",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
            R.id.actionDelete -> {

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete")
                    .setMessage("This will delete selected files")
                    .setPositiveButton("Delete") { dialog, _ ->
                        selectedFiles.forEach {
                            lifecycleScope.launch {
                                viewModel?.deleteSelectedFiles(it)
                            }
                        }

                        multiSelection = false
                        Toast.makeText(
                            requireContext(),
                            "${selectedFiles.size} file(s) deleted",
                            Toast.LENGTH_SHORT
                        ).show()
                        selectedFiles.clear()
                        actionMode?.finish()
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()


            }
            R.id.actionShare -> {
                var count = 0
                selectedFiles.forEach {
                    if (it.isDirectory)
                        count++
                }
                when (count) {
                    0 -> {
                        val shareIntent =
                            Util.startShareSheetMultiple(requireActivity(), selectedFiles)
                        startActivity(Intent.createChooser(shareIntent, "Send File(s) To"))
                    }
                    1 -> {
                        if(selectedFiles.size > 1) {
                            Toast.makeText(
                                requireContext(),
                                "Files and folders cannot mix when sharing",
                                Toast.LENGTH_SHORT
                            ).show()
                            return true
                        }else {
                            val files = File(selectedFiles[0].filePath).listFiles()
                            val shareIntent = Util.shareSheetMultipleDirectory(requireActivity(), files)
                            startActivity(Intent.createChooser(shareIntent, "Send File(s) To"))
                        }

                    }
                    else -> {
                        Toast.makeText(
                            requireContext(),
                            "Only 1 folder can be shared once",
                            Toast.LENGTH_SHORT
                        ).show()
                        return true
                    }
                }

            }
        }
        return true
    }

    private fun moveSelectedFilesToDirectory(
        selectedFiles: ArrayList<ConvertedFile>,
        folderPath: String
    ) {
        selectedFiles.forEach { file ->
            val newFilePath = "${folderPath}/${file.fileName}"
            File(file.filePath).renameTo(File(newFilePath))

            val newFile = file.apply {
                filePath = File(newFilePath).path
                uri = File(newFilePath).toUri()
                inDirectory = true
                isSelected = false
            }
            viewModel?.updateNewFile(newFile)
            data?.let { updateData(it.toMutableList()) }

        }


    }

    private fun createFileDirectory(name: String): String {
        try {

            val dirPath = Util.getExternalDir(requireContext())
            val folderPath = Util.getStorageFolder(dirPath, name)
            val contentSize = Util.getContentSize(selectedFiles.size)
            val directoryColor = (0..24).random()
            val date = Date(Util.getCurrentTimeMillis().toLong())
            val file = File(folderPath)
            if (!file.exists()) {
                file.mkdir()
            }

            val folder = ConvertedFile(
                0,
                name,
                contentSize,
                0,
                file.path,
                "Directory",
                file.toUri(),
                null,
                isFavorite = false,
                isSelected = false,
                isDirectory = true,
                inDirectory = false,
                directoryColor,
                date
            )
            viewModel?.insertFolder(folder)

            return if (file.exists()) folderPath else ""
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    override fun onDestroyActionMode(actionMode: ActionMode?) {
        data?.forEach {
            it.isSelected = false
        }
        data?.let { updateData(it) }
        applyStatusBarColor(R.color.statusBarColor)
        selectedFiles.clear()
        multiSelection = false
    }

    private fun applyStatusBarColor(color: Int) {
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), color)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("FileListFragment", "On Destroy View called")
        binding?.filesRecyclerView?.adapter = null
        viewModel = null
        _binding = null
    }


}