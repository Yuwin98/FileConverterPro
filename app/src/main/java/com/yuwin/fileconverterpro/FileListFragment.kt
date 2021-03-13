package com.yuwin.fileconverterpro


import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yuwin.fileconverterpro.databinding.FragmentMainScreenBinding
import com.yuwin.fileconverterpro.db.ConvertedFile
import java.io.File
import java.io.IOException
import java.util.*


class FileListFragment : FileListClickListener, ActionMode.Callback,
    ActionModeBaseFragment() {

    private var _binding: FragmentMainScreenBinding? = null
    private val binding get() = _binding
    private var viewModel: FileListViewModel? = null
    private val mainViewModel: MainViewModel by viewModels()


    override var multiSelection = false
    override var selectedFiles = arrayListOf<ConvertedFile>()
    override var actionMode: ActionMode? = null

    private var menu: Menu? = null

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

        mainViewModel.readIfGridEnabled.observe(viewLifecycleOwner, {
            Log.d("isGrid", it.toString())
            isGrid = it
        })

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
                val rootPath = Util.getExternalDir(requireContext())
                data = Util.filterItemsInDirectory(File(rootPath), items)
               // data = data!!.filter { file -> !file.inDirectory }

                if (items.isEmpty()) {
                    binding?.let {
                        it.noFilesImageView.visibility = View.VISIBLE
                        it.noFilesTextView.visibility = View.VISIBLE
                    }
                } else {
                    binding?.let {
                        it.noFilesImageView.visibility = View.GONE
                        it.noFilesTextView.visibility = View.GONE
                    }
                }

                setupRecyclerView(isGrid)
            }
        })

    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.home_action_menu, menu)
        this.menu = menu
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.viewChange -> {
                if (!isGrid) {
                    item.setIcon(R.drawable.ic_listview)
                    item.title = "List View"
                    isGrid = true
                    mainViewModel.setIsGrid(true)
                    setupRecyclerView(isGrid)
                } else {
                    item.setIcon(R.drawable.ic_gridview)
                    item.title = "Grid View"
                    isGrid = false
                    mainViewModel.setIsGrid(false)
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
            R.id.newFolder -> {
                val editTextView = layoutInflater.inflate(R.layout.edittext_layout, null)
                MaterialAlertDialogBuilder(requireContext())
                    .setView(editTextView)
                    .setTitle("Create Folder")
                    .setMessage("This will create an empty folder")
                    .setPositiveButton("Create") { dialog, _ ->
                        val editText = editTextView.findViewById<EditText>(R.id.renameFileEditText)
                        val name = editText.text.toString()
                        if (name.isNotBlank()) {
                            createFileDirectory(name)
                            dialog.dismiss()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Folder name empty",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
                editTextView.requestFocus()

            }
            R.id.sortFileSize -> {
                mainViewModel.readFilesBySize.observe(viewLifecycleOwner, { items ->
                    filterSortAndUpdateData(items)
                })
            }
            R.id.sortFileName -> {
                mainViewModel.readFilesByName.observe(viewLifecycleOwner, { items ->
                    filterSortAndUpdateData(items)
                })
            }
            R.id.sortFileDate -> {
                mainViewModel.readFilesByDate.observe(viewLifecycleOwner, { items ->
                    filterSortAndUpdateData(items)
                })
            }
            R.id.sortFileType -> {
                mainViewModel.readFilesByType.observe(viewLifecycleOwner, { items ->
                    filterSortAndUpdateData(items)
                })
            }
            R.id.filterJpg -> {
                mainViewModel.filterFilesByJpgJpeg.observe(viewLifecycleOwner, { items ->
                    filterSortAndUpdateData(items)
                })
            }
            R.id.filterPdf -> {
                mainViewModel.filterFilesByPdf.observe(viewLifecycleOwner, { items ->
                    filterSortAndUpdateData(items)
                })
            }
            R.id.filterPng -> {
                mainViewModel.filterFilesByPng.observe(viewLifecycleOwner, { items ->
                    filterSortAndUpdateData(items)
                })
            }
            R.id.filterWebp -> {
                mainViewModel.filterFilesByWebp.observe(viewLifecycleOwner, { items ->
                    filterSortAndUpdateData(items)
                })
            }
        }
        return true
    }

    override fun onPause() {
        super.onPause()
        this.actionMode?.finish()
        commonTransitionToEnd()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        (activity as MainActivity).currentUserDirectory = ContextCompat.getExternalFilesDirs(
            requireContext().applicationContext,
            null
        )[0].absolutePath
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

    override fun onItemClick(position: Int) {
        if (multiSelection) {
            applySelection(position)
        } else {
            val data = data?.get(position)
            data?.let {
                if (it.isDirectory) {
                    (activity as MainActivity).currentUserDirectory = it.filePath
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
                actionMode?.finish()
            }
            1 -> {
                actionMode?.title = "1 item selected"
            }
            else -> {
                actionMode?.title = "${selectedFiles.size} items selected"
            }
        }
    }

    override fun onCreateActionMode(actionMode: ActionMode, menu: Menu?): Boolean {
        actionMode.menuInflater?.inflate(R.menu.action_menu, menu)
        applyStatusBarColor(R.color.contextualStatusBarColor)
        this.actionMode = actionMode
        attachHostToCommonActionBar()
        commonActionBarVisibility(View.VISIBLE)
        commonTransitionToEnd()
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
                            val folder = createFileDirectory(name)
                            if (folder?.filePath?.isNotBlank() == true) {
                                moveSelectedFilesToDirectory(selectedFiles, folder)
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
        }
        return true
    }


    private fun moveSelectedFilesToDirectory(
        selectedFiles: ArrayList<ConvertedFile>,
        folderPath: ConvertedFile?
    ) {
        selectedFiles.forEach { file ->
            val newFilePath = "${folderPath?.filePath}/${file.fileName}"
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

    private fun createFileDirectory(name: String): ConvertedFile? {
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

            return if (file.exists()) folder else null
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun onDestroyActionMode(actionMode: ActionMode?) {
        data?.forEach {
            it.isSelected = false
        }
        data?.let { updateData(it) }
        applyStatusBarColor(R.color.statusBarColor)
        selectedFiles.clear()
        multiSelection = false
        commonTransitionToStart()
        moveCopyTransitionStart()

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