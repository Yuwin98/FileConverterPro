package com.yuwin.fileconverterpro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.yuwin.fileconverterpro.databinding.FragmentDirectoryViewBinding
import com.yuwin.fileconverterpro.db.ConvertedFile
import java.io.File
import java.util.*


class DirectoryViewFragment : BaseFragment(), FileListClickListener {
    override var bottomNavigationVisibility: Int = View.GONE

    private val args by navArgs<DirectoryViewFragmentArgs>()

    private var data = listOf<ConvertedFile>()
    private var databaseData: List<ConvertedFile>? = null

    private var _binding: FragmentDirectoryViewBinding? = null
    private val binding get() = _binding

    private var viewModel: DirectoryPreviewViewModel? = null

    private val filesListAdapter by lazy { FilesListAdapter(this) }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

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


        viewModel?.allDirectoryFiles?.observe(viewLifecycleOwner, { items ->
            if (items.isNullOrEmpty()) {
                data = items
                setupRecyclerView()
            } else {
                val convertedFile = args.data
                data = Util.filterItemsIn(File(convertedFile.filePath), items)
                setupRecyclerView()
            }
        })

        setupRecyclerView()
    }


    override fun onItemClick(position: Int) {
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
        return false
    }


    private fun setupRecyclerView() {
        binding?.let {
            it.directoryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            it.directoryRecyclerView.adapter = filesListAdapter

        }
        data.let { filesListAdapter.setData(it.toMutableList()) }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding?.directoryRecyclerView?.adapter = null
        _binding = null
        viewModel = null
    }


}