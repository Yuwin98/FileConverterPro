package com.yuwin.fileconverterpro


import android.database.Cursor
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.view.animation.Animation
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.yuwin.fileconverterpro.databinding.FragmentMainScreenBinding
import com.yuwin.fileconverterpro.db.ConvertedFile


class FileListFragment : BaseFragment() {

    private val binding by lazy{ FragmentMainScreenBinding.inflate(layoutInflater) }
    private val viewModel by lazy { FileListViewModel(requireActivity().application) }

    private val sb = StringBuilder("")

    private val filesListAdapter by lazy { FilesListAdapter() }
    private val filesGridAdapter by lazy { FilesGridAdapter() }
    private var isGrid = false

    private var data: List<ConvertedFile>? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

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
                if(!isGrid) {
                    item.setIcon(R.drawable.ic_listview)
                    item.title = "List View"
                    setupGridRecyclerView()
                    isGrid = true
                }else {
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








}