package com.yuwin.fileconverterpro

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yuwin.fileconverterpro.Util.Companion.observeOnce
import com.yuwin.fileconverterpro.databinding.FragmentDirectoryViewBinding
import com.yuwin.fileconverterpro.db.ConvertedFile
import java.io.File
import java.util.*


class DirectoryViewFragment : ActionModeBaseFragment(), FileListClickListener, ActionMode.Callback {


    private var isSelectAll = false
    private val args by navArgs<DirectoryViewFragmentArgs>()

    private var data = listOf<ConvertedFile>()

    private var _binding: FragmentDirectoryViewBinding? = null
    private val binding get() = _binding

    private var viewModel: DirectoryPreviewViewModel? = null
    private lateinit var mainViewModel: MainViewModel

    private lateinit var rootPath: String
    private lateinit var folderName: String


    private val filesListAdapter by lazy { FilesListAdapter(this) }
    private val filesGridAdapter by lazy { FilesGridAdapter(this) }
    private var isGrid = false

    override var multiSelection = false


    private var menu: Menu? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        (requireActivity() as AppCompatActivity).supportActionBar?.title = args.data.fileName

        _binding = FragmentDirectoryViewBinding.inflate(inflater, container, false)
        _binding?.lifecycleOwner = viewLifecycleOwner

        viewModel = ViewModelProvider(this).get(DirectoryPreviewViewModel::class.java)
        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

        mainViewModel.readIfGridEnabled.observeOnce(viewLifecycleOwner, {
            isGrid = it
        })

        return _binding?.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val convertedFile = args.data
        rootPath = convertedFile.filePath
        folderName = convertedFile.fileName

        mainViewModel.getAllDirectoryFilesWithPath(convertedFile.filePath)
            .observe(viewLifecycleOwner, { items ->
                if (items.isNullOrEmpty()) {
                    data = items
                    setupRecyclerView(isGrid)
                    binding?.let {
                        it.noFilesImageView.visibility = View.VISIBLE
                        it.noFilesTextView.visibility = View.VISIBLE
                    }
                } else {

                    data = Util.filterItemsInDirectory(File(convertedFile.filePath), items)
                    data = data.sortedBy { it.fileType }

                    if (data.isEmpty()) {
                        binding?.let {
                            it.noFilesImageView.visibility = View.VISIBLE
                            it.noFilesTextView.visibility = View.VISIBLE
                        }
                    } else {
                        setupRecyclerView(isGrid)
                        binding?.let {
                            it.noFilesImageView.visibility = View.GONE
                            it.noFilesTextView.visibility = View.GONE
                        }
                    }
                }
            })
    }



    override fun onItemClick(position: Int) {
        if (multiSelection) {
            applySelection(position)
        } else {
            openMyFile(position)
        }
    }

    private fun openMyFile(position: Int) {
        val data = data[position]

        val fileType = data.fileType
        when {
            fileType == "pdf" -> {
                val action =
                    DirectoryViewFragmentDirections.actionDirectoryViewFragmentToPdfViewerFragment(
                        data
                    )
                findNavController().navigate(action)
            }
            data.isDirectory -> {
                (activity as MainActivity).currentUserDirectory = data.filePath
                val action = DirectoryViewFragmentDirections.actionDirectoryViewFragmentSelf(data)
                findNavController().navigate(action)
            }
            else -> {
                val action =
                    DirectoryViewFragmentDirections.actionDirectoryViewFragmentToImageViewFragment(
                        data
                    )
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
        val currentFile = data[position]
        if (selectedFiles.contains(currentFile)) {
            selectedFiles.remove(currentFile)
            data[position].isSelected = false
            updateData(data)
            setActionModeTitle()
        } else {
            data[position].isSelected = true
            updateData(data)
            selectedFiles.add(currentFile)
            setActionModeTitle()
        }

        if (selectedFiles.size > 1) {
            changeRenameButtonVisibility(View.GONE)
        } else {
            changeRenameButtonVisibility(View.VISIBLE)
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

    private fun setupListRecyclerView() {
        binding?.let {
            it.directoryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            it.directoryRecyclerView.adapter = filesListAdapter
        }
        data.let { filesListAdapter.setData(it.toMutableList()) }


    }

    private fun setupGridRecyclerView() {
        binding?.let {
            it.directoryRecyclerView.layoutManager = GridLayoutManager(requireContext(), 4)
            it.directoryRecyclerView.adapter = filesGridAdapter
        }
        data.let { filesGridAdapter.setData(it.toMutableList()) }

    }

    private fun setupRecyclerView(isGrid: Boolean) {
        if (isGrid) {
            setupGridRecyclerView()
        } else {
            setupListRecyclerView()
        }
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.directory_home_action_menu, menu)
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
                    .setTitle("Clear Directory")
                    .setMessage("This will delete all converted files in this directory")
                    .setPositiveButton("Clear") { dialog, _ ->
                        viewModel?.clearDirectory(data)
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

                        dialog.dismiss()

                        val fileParent = File(args.data.filePath).parent
                        val file = File(fileParent)
                        when {
                            name.isBlank() -> {
                                Toast.makeText(
                                    requireContext(),
                                    "Name cannot be empty",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            mainViewModel.checkFileNameTooLong(name) -> {
                                Toast.makeText(
                                    requireContext(),
                                    "Maximum filename length is 30",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            mainViewModel.checkFileNameTooShort(name) -> {
                                Toast.makeText(
                                    requireContext(),
                                    "Minimum filename length is 2",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            mainViewModel.checkNewFolderNameUnique(file, name) -> {
                                Toast.makeText(
                                    requireContext(),
                                    "Filename already exists",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            !mainViewModel.checkIfFileNameValid(name) -> {
                                Toast.makeText(
                                    requireContext(),
                                    "Filename can only consist of (a-zA-z0-9!_)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            else -> {
                                val currentDir = (activity as MainActivity).currentUserDirectory
                                mainViewModel.createFileDirectory(
                                    name,
                                    selectedFiles.size,
                                    currentDir,
                                    true
                                )
                            }
                        }

                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
                editTextView.requestFocus()

            }
            R.id.sortFileSize -> {
                mainViewModel.getAllDirectoryFilesWithPath(rootPath).observe(viewLifecycleOwner, { items ->
                    setTitleBar("(SIZE) - $folderName")
                    val directoryFiles = Util.filterItemsInDirectory(File(rootPath), items)
                    val filteredItems = mainViewModel.deepFilter(FILTER.SIZE, directoryFiles)
                    filterSortAndUpdateData(filteredItems)
                })
            }
            R.id.sortFileName -> {
                mainViewModel.getAllDirectoryFilesWithPath(rootPath).observe(viewLifecycleOwner, { items ->
                    setTitleBar("(NAME) - $folderName")
                    val directoryFiles = Util.filterItemsInDirectory(File(rootPath), items)
                    val filteredItems = mainViewModel.deepFilter(FILTER.NAME, directoryFiles)
                    filterSortAndUpdateData(filteredItems)
                })
            }
            R.id.sortFileDate -> {
                mainViewModel.getAllDirectoryFilesWithPath(rootPath).observe(viewLifecycleOwner, { items ->
                    setTitleBar("(DATE) - $folderName")
                    val directoryFiles = Util.filterItemsInDirectory(File(rootPath), items)
                    val filteredItems = mainViewModel.deepFilter(FILTER.DATE, directoryFiles)
                    filterSortAndUpdateData(filteredItems)
                })
            }
            R.id.sortFileType -> {
                mainViewModel.getAllDirectoryFilesWithPath(rootPath).observe(viewLifecycleOwner, { items ->
                    setTitleBar("(Type) - $folderName")
                    val directoryFiles = Util.filterItemsInDirectory(File(rootPath), items)
                    val filteredItems = mainViewModel.deepFilter(FILTER.TYPE, directoryFiles)
                    filterSortAndUpdateData(filteredItems)
                })
            }
            R.id.filterJpg -> {
                mainViewModel.getAllDirectoryFilesWithPath(rootPath).observe(viewLifecycleOwner, { items ->
                    setTitleBar("(JPG/JPEG) - $folderName")
                    val directoryFiles = Util.filterItemsInDirectory(File(rootPath), items)
                    val filteredItems = mainViewModel.deepFilter(FILTER.JPG, directoryFiles)
                    filterSortAndUpdateData(filteredItems)
                })
            }
            R.id.filterPdf -> {
                mainViewModel.getAllDirectoryFilesWithPath(rootPath).observe(viewLifecycleOwner, { items ->
                    setTitleBar("(PDF) - $folderName")
                    val directoryFiles = Util.filterItemsInDirectory(File(rootPath), items)
                    val filteredItems = mainViewModel.deepFilter(FILTER.PDF_FILE, directoryFiles)
                    filterSortAndUpdateData(filteredItems)
                })
            }
            R.id.filterPng -> {
                mainViewModel.getAllDirectoryFilesWithPath(rootPath).observe(viewLifecycleOwner, { items ->
                    setTitleBar("(PNG) - $folderName ")
                    val directoryFiles = Util.filterItemsInDirectory(File(rootPath), items)
                    val filteredItems = mainViewModel.deepFilter(FILTER.PNG, directoryFiles)
                    filterSortAndUpdateData(filteredItems)
                })
            }
            R.id.filterWebp -> {
                mainViewModel.getAllDirectoryFilesWithPath(rootPath).observe(viewLifecycleOwner, { items ->
                    setTitleBar("(WEBP) - $folderName ")
                    val directoryFiles = Util.filterItemsInDirectory(File(rootPath), items)
                    val filteredItems = mainViewModel.deepFilter(FILTER.WEBP, directoryFiles)
                    filterSortAndUpdateData(filteredItems)
                })
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setTitleBar(title: String) {
        (requireActivity() as AppCompatActivity).supportActionBar?.title =
            title
    }

    private fun filterSortAndUpdateData(items: List<ConvertedFile>) {
        if (items.isNullOrEmpty()) {
            data = items
            binding?.let {
                it.noFilesImageView.visibility = View.VISIBLE
                it.noFilesTextView.visibility = View.VISIBLE
            }
            updateData(data.toMutableList())
        } else {
            data = items
            binding?.let {
                it.noFilesImageView.visibility = View.GONE
                it.noFilesTextView.visibility = View.GONE
            }
            updateData(data.toMutableList())
        }
    }

    private fun updateData(newData: List<ConvertedFile>) {
        filesGridAdapter.setData(newData.toMutableList())
        filesListAdapter.setData(newData.toMutableList())
        filesGridAdapter.notifyDataSetChanged()
        filesListAdapter.notifyDataSetChanged()
    }

    override fun onPause() {
        super.onPause()
        actionMode?.finish()
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

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return true
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.selectAll -> {
                if (!isSelectAll) {
                    item.setIcon(R.drawable.ic_select_all)
                    isSelectAll = true
                    selectAllFiles(isSelectAll)
                } else {
                    item.setIcon(R.drawable.ic_select_all_black)
                    isSelectAll = false
                    selectAllFiles(isSelectAll)
                }
            }
            R.id.actionMoveToFolder -> {
                val editTextView = layoutInflater.inflate(R.layout.edittext_layout, null)
                MaterialAlertDialogBuilder(requireContext())
                    .setView(editTextView)
                    .setTitle("Create Folder")
                    .setMessage("This will create a folder and move selected files into it")
                    .setPositiveButton("Create") { dialog, _ ->
                        val editText = editTextView.findViewById<EditText>(R.id.renameFileEditText)
                        val name = editText.text.toString()
                        dialog.dismiss()
                        val file = File(args.data.filePath)

                        when {
                            name.isBlank() -> {
                                Toast.makeText(
                                    requireContext(),
                                    "Name cannot be empty",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            mainViewModel.checkFileNameTooLong(name) -> {
                                Toast.makeText(
                                    requireContext(),
                                    "Maximum folder name length is 30",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            mainViewModel.checkFileNameTooShort(name) -> {
                                Toast.makeText(
                                    requireContext(),
                                    "Minimum filename length is 2",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            mainViewModel.checkNewFolderNameUnique(file, name) -> {
                                Toast.makeText(
                                    requireContext(),
                                    "Folder already exists",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            !mainViewModel.checkIfFileNameValid(name) -> {
                                Toast.makeText(
                                    requireContext(),
                                    "Folder name can only consist of (a-zA-z0-9!_)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            else -> {
                                val currentDir = (activity as MainActivity).currentUserDirectory
                                val folder = mainViewModel.createFileDirectory(
                                    name,
                                    selectedFiles.size,
                                    currentDir,
                                    true
                                )
                                if (folder != null) {
                                    if (folder.filePath.isNotBlank()) {
                                        moveSelectedFilesToDirectory(selectedFiles, folder)
                                    }
                                }
                                multiSelection = false
                                selectedFiles.clear()
                                actionMode?.finish()

                            }
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

    override fun onDestroyActionMode(mode: ActionMode?) {
        data.forEach {
            it.isSelected = false
        }
        updateData(data)
        applyStatusBarColor(R.color.statusBarColor)
        selectedFiles.clear()
        multiSelection = false
        commonTransitionToStart()

    }

    private fun applyStatusBarColor(color: Int) {
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), color)
    }

    private fun selectAllFiles(selectAll: Boolean) {
        if (selectAll) {
            selectedFiles.clear()
        }

        for (i in data.indices) {
            applySelection(i)
        }
    }

    private fun moveSelectedFilesToDirectory(
        selectedFiles: ArrayList<ConvertedFile>,
        folderPath: ConvertedFile?,
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
            mainViewModel.updateFile(newFile)
            updateData(data.toMutableList())

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("dirFragState", "OnDestroyView called")
        _binding?.directoryRecyclerView?.adapter = null
        _binding = null
        viewModel = null
    }


}