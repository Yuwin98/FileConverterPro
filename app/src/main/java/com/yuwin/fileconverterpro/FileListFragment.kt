package com.yuwin.fileconverterpro


import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yuwin.fileconverterpro.Util.Companion.observeOnce
import com.yuwin.fileconverterpro.databinding.FragmentMainScreenBinding
import com.yuwin.fileconverterpro.db.ConvertedFile
import java.io.File
import java.util.*


class FileListFragment : FileListClickListener, SearchView.OnQueryTextListener, ActionMode.Callback,
    ActionModeBaseFragment() {

    private var _binding: FragmentMainScreenBinding? = null
    private val binding get() = _binding
    private var viewModel: FileListViewModel? = null
    private lateinit var mainViewModel: MainViewModel

    private lateinit var rootPath: String

    override var multiSelection = false
    override var selectedFiles = arrayListOf<ConvertedFile>()
    override var actionMode: ActionMode? = null

    private var menu: Menu? = null

    private val filesListAdapter by lazy { FilesListAdapter(this) }
    private val filesGridAdapter by lazy { FilesGridAdapter(this) }
    private var isGrid = false
    private var isSelectAll = false

    private var data: List<ConvertedFile>? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        _binding = FragmentMainScreenBinding.inflate(inflater, container, false)
        binding?.lifecycleOwner = viewLifecycleOwner
        viewModel = ViewModelProvider(this).get(FileListViewModel::class.java)
        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        mainViewModel.readIfGridEnabled.observeOnce(viewLifecycleOwner, {
            isGrid = it
            val item = menu?.findItem(R.id.viewChange)
            if (isGrid) {
                item?.setIcon(R.drawable.ic_listview)
                item?.title = "List View"
            } else {
                item?.setIcon(R.drawable.ic_gridview)
                item?.title = "Grid View"
            }
        })

        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rootPath = Util.getExternalDir(requireContext())

        viewModel?.readFilesInRoot?.observeOnce(viewLifecycleOwner, { items ->
            if (items.isNullOrEmpty()) {
                binding?.let {
                    it.noFilesImageView.visibility = View.VISIBLE
                    it.noFilesTextView.visibility = View.VISIBLE
                }
                data = items
                setupRecyclerView(isGrid)

            } else {
                items.map { it.isSelected = false }

                data = items
                data = mainViewModel.deepFilter(FILTER.DATE, data!!)

                if (data!!.isNullOrEmpty()) {
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


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.home_action_menu, menu)
        this.menu = menu

        val search = menu.findItem(R.id.menu_search)
        val searchView = search.actionView as SearchView
        searchView.setOnQueryTextListener(this)

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
                        dialog.dismiss()
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
                            mainViewModel.checkNewFolderNameUnique(
                                File(
                                    Util.getExternalDir(
                                        requireContext()
                                    )
                                ), name
                            ) -> {
                                Toast.makeText(
                                    requireContext(),
                                    "Folder already exists",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            !mainViewModel.checkIfFileNameValid(name) -> {
                                Toast.makeText(
                                    requireContext(),
                                    "Filename can only consist of (a-zA-z0-9_-)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            else -> {
                                val currentDir = (activity as MainActivity).currentUserDirectory
                                mainViewModel.createFileDirectory(
                                    name,
                                    selectedFiles.size,
                                    currentDir,
                                    false
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
                mainViewModel.getAllDirectoryFilesWithPath(rootPath)
                    .observe(viewLifecycleOwner, { items ->
                        setTitleBar("My Files (SIZE)")
                        val directoryFiles = Util.filterItemsInDirectory(File(rootPath), items)
                        val filteredItems = mainViewModel.deepFilter(FILTER.SIZE, directoryFiles)
                        filterSortAndUpdateData(filteredItems)
                    })
            }
            R.id.sortFileName -> {
                mainViewModel.getAllDirectoryFilesWithPath(rootPath)
                    .observe(viewLifecycleOwner, { items ->
                        setTitleBar("My Files (NAME)")
                        val directoryFiles = Util.filterItemsInDirectory(File(rootPath), items)
                        val filteredItems = mainViewModel.deepFilter(FILTER.NAME, directoryFiles)
                        filterSortAndUpdateData(filteredItems)
                    })
            }
            R.id.sortFileDate -> {
                mainViewModel.getAllDirectoryFilesWithPath(rootPath)
                    .observe(viewLifecycleOwner, { items ->
                        setTitleBar("My Files (DATE)")
                        val directoryFiles = Util.filterItemsInDirectory(File(rootPath), items)
                        val filteredItems = mainViewModel.deepFilter(FILTER.DATE, directoryFiles)
                        filterSortAndUpdateData(filteredItems)
                    })
            }
            R.id.sortFileType -> {
                mainViewModel.getAllDirectoryFilesWithPath(rootPath)
                    .observe(viewLifecycleOwner, { items ->
                        setTitleBar("My Files (TYPE)")
                        val directoryFiles = Util.filterItemsInDirectory(File(rootPath), items)
                        val filteredItems = mainViewModel.deepFilter(FILTER.TYPE, directoryFiles)
                        filterSortAndUpdateData(filteredItems)
                    })
            }
            R.id.filterJpg -> {
                mainViewModel.getAllDirectoryFilesWithPath(rootPath)
                    .observe(viewLifecycleOwner, { items ->
                        setTitleBar("My Files (JPG/JPEG)")
                        val directoryFiles = Util.filterItemsInDirectory(File(rootPath), items)
                        val filteredItems = mainViewModel.deepFilter(FILTER.JPG, directoryFiles)
                        filterSortAndUpdateData(filteredItems)
                    })
            }
            R.id.filterPdf -> {
                mainViewModel.getAllDirectoryFilesWithPath(rootPath)
                    .observe(viewLifecycleOwner, { items ->
                        setTitleBar("My Files (PDF)")
                        val directoryFiles = Util.filterItemsInDirectory(File(rootPath), items)
                        val filteredItems =
                            mainViewModel.deepFilter(FILTER.PDF_FILE, directoryFiles)
                        filterSortAndUpdateData(filteredItems)
                    })
            }
            R.id.filterPng -> {
                mainViewModel.getAllDirectoryFilesWithPath(rootPath)
                    .observe(viewLifecycleOwner, { items ->
                        setTitleBar("My Files (PNG)")
                        val directoryFiles = Util.filterItemsInDirectory(File(rootPath), items)
                        val filteredItems = mainViewModel.deepFilter(FILTER.PNG, directoryFiles)
                        filterSortAndUpdateData(filteredItems)
                    })
            }
            R.id.filterWebp -> {
                mainViewModel.getAllDirectoryFilesWithPath(rootPath)
                    .observe(viewLifecycleOwner, { items ->
                        setTitleBar("My Files (WEBP)")
                        val directoryFiles = Util.filterItemsInDirectory(File(rootPath), items)
                        val filteredItems = mainViewModel.deepFilter(FILTER.WEBP, directoryFiles)
                        filterSortAndUpdateData(filteredItems)
                    })
            }
        }
        return true
    }

    override fun onPause() {
        super.onPause()
        this.actionMode?.finish()
        commonTransitionToEnd()
        detailsLayoutTransitionEnd()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        (activity as MainActivity).currentUserDirectory = ContextCompat.getExternalFilesDirs(
            requireContext().applicationContext,
            null
        )[0].absolutePath
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
            updateData(data!!.toMutableList())
        } else {
            data = items
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

        if (selectedFiles.size > 1) {
            changeRenameButtonVisibility(View.GONE)
        } else {
            changeRenameButtonVisibility(View.VISIBLE)
        }

    }

    private fun selectAllFiles(selectAll: Boolean) {
        if (selectAll) {
            selectedFiles.clear()
        }

        for (i in 0 until data?.size!!) {
            applySelection(i)
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
            R.id.selectAll -> {
                if (!isSelectAll) {
                    item.setIcon(R.drawable.ic_select_all)
                    isSelectAll = true
                    selectAllFiles(isSelectAll)
                } else {
                    item.setIcon(R.drawable.ic_done_all_white)
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
                                    "Minimum folder name length is 2",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            mainViewModel.checkNewFolderNameUnique(
                                File(
                                    Util.getExternalDir(
                                        requireContext()
                                    )
                                ), name
                            ) -> {
                                Toast.makeText(
                                    requireContext(),
                                    "Folder already exists",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            !mainViewModel.checkIfFileNameValid(name) -> {
                                Toast.makeText(
                                    requireContext(),
                                    "Filename can only consist of (a-zA-z0-9_)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            else -> {
                                val currentDir = (activity as MainActivity).currentUserDirectory
                                val folder = mainViewModel.createFileDirectory(
                                    name,
                                    selectedFiles.size,
                                    currentDir,
                                    false
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
            mainViewModel.updateFile(newFile)
            data?.let { updateData(it.toMutableList()) }

        }
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

    override fun onQueryTextSubmit(query: String?): Boolean {
        return true
    }

    override fun onQueryTextChange(query: String?): Boolean {
        if(query != null) {
            searchDatabase(query)
        }
        return true
    }

    private fun searchDatabase(query: String) {
        val searchQuery = "%$query%"
        mainViewModel.searchDatabaseInRoot(searchQuery).observeOnce(viewLifecycleOwner, {list ->
            data = list.toMutableList()
            updateData(list.toMutableList())
        })
    }


}