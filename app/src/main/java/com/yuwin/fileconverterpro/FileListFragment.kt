package com.yuwin.fileconverterpro


import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import com.yuwin.fileconverterpro.databinding.FragmentMainScreenBinding
import com.yuwin.fileconverterpro.db.AppDatabase
import com.yuwin.fileconverterpro.db.ConvertedFile
import com.yuwin.fileconverterpro.db.ConvertedFileDao
import java.lang.StringBuilder


class FileListFragment : BaseFragment() {

    private val binding by lazy{ FragmentMainScreenBinding.inflate(layoutInflater) }
    private val viewModel by lazy { FileListViewModel(requireActivity().application) }

    private val sb = StringBuilder("")

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
            for (item in items) {
                convertDatabaseData(item)
            }
        })

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.home_action_menu, menu)
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

        sb.append("File Uri: ${item.fileUri}")
        sb.append("\n")

        sb.append("Date Created: ${item.date}")
        sb.append("\n")

        Log.d("convertedFiles", sb.toString())
    }








}