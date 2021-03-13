package com.yuwin.fileconverterpro

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yuwin.fileconverterpro.databinding.FragmentDirectoryViewBinding
import com.yuwin.fileconverterpro.db.ConvertedFile
import kotlinx.coroutines.launch
import java.io.File
import java.util.*


class DirectoryViewFragment : ActionModeBaseFragment(), FileListClickListener, ActionMode.Callback {

    override var bottomNavigationVisibility: Int = View.GONE

    private val args by navArgs<DirectoryViewFragmentArgs>()

    private var data = listOf<ConvertedFile>()

    private var _binding: FragmentDirectoryViewBinding? = null
    private val binding get() = _binding

    private var viewModel: DirectoryPreviewViewModel? = null
    private val mainViewModel: MainViewModel by viewModels()


    private val filesListAdapter by lazy { FilesListAdapter(this) }

    override var multiSelection = false
    override var selectedFiles = arrayListOf<ConvertedFile>()


    private var menu: Menu? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        (requireActivity() as AppCompatActivity).supportActionBar?.title =
            args.data.fileName.toUpperCase(
                Locale.ROOT
            )

        _binding = FragmentDirectoryViewBinding.inflate(inflater, container, false)

        _binding?.lifecycleOwner = viewLifecycleOwner

        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(DirectoryPreviewViewModel::class.java)
        val convertedFile = args.data

        viewModel?.allDirectoryFiles
            ?.observe(viewLifecycleOwner, { items ->
                if (items.isNullOrEmpty()) {
                    data = items
                    setupRecyclerView()
                } else {
                    data = Util.filterItemsInDirectory(File(convertedFile.filePath), items)
                    setupRecyclerView()
                }
            })
        setupRecyclerView()
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
        if (fileType == "pdf") {
            val action =
                DirectoryViewFragmentDirections.actionDirectoryViewFragmentToPdfViewerFragment(data)
            findNavController().navigate(action)
        } else {
            val action =
                DirectoryViewFragmentDirections.actionDirectoryViewFragmentToImageViewFragment(data)
            findNavController().navigate(action)
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

    private fun setupRecyclerView() {
        binding?.let {
            it.directoryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            it.directoryRecyclerView.adapter = filesListAdapter

        }
        data.let { filesListAdapter.setData(it.toMutableList()) }

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.directory_home_action_menu, menu)
        this.menu = menu
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

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
        return super.onOptionsItemSelected(item)
    }

    private fun filterSortAndUpdateData(items: List<ConvertedFile>) {
        if (items.isNullOrEmpty()) {
            data = items
            updateData(data.toMutableList())
        } else {
            val convertedFile = args.data
            data = Util.filterItemsInDirectory(File(convertedFile.filePath), items)
            updateData(data.toMutableList())
        }
    }

    private fun updateData(newData: List<ConvertedFile>) {
        filesListAdapter.setData(newData.toMutableList())
        filesListAdapter.notifyDataSetChanged()
    }

    override fun onPause() {
        super.onPause()
        Log.d("heart", "Pause called")
        actionMode?.finish()
        commonTransitionToEnd()
    }

    override fun onCreateActionMode(actionMode: ActionMode, menu: Menu?): Boolean {
//        actionMode.menuInflater?.inflate(R.menu.directory_action_menu, menu)
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
        moveCopyTransitionStart()

    }

    private fun applyStatusBarColor(color: Int) {
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), color)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding?.directoryRecyclerView?.adapter = null
        _binding = null
        viewModel = null
    }


}