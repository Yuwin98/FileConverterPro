package com.yuwin.fileconverterpro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.yuwin.fileconverterpro.databinding.FragmentFavoriteBinding
import com.yuwin.fileconverterpro.db.ConvertedFile


class FavoriteFragment : BaseFragment(), FileListClickListener {

    private var _binding: FragmentFavoriteBinding? = null
    private val binding get() = _binding

    private var viewModel: FavoriteViewModel? = null

    private var data: List<ConvertedFile>? = null

    private val filesListAdapter by lazy { FilesListAdapter(this) }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity?.let {
            val moveCopy = it.findViewById<MotionLayout>(R.id.moveCopyLayout)
            moveCopy.visibility = View.GONE
        }
        _binding = FragmentFavoriteBinding.inflate(inflater, container, false)

        binding?.lifecycleOwner = viewLifecycleOwner

        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(FavoriteViewModel::class.java)
        viewModel?.readFavoriteFiles?.observe(viewLifecycleOwner, { items ->
            if(items.isNullOrEmpty()) {
                data = items
                binding?.let {
                    it.noFavoriteFilesImageView.visibility = View.VISIBLE
                    it.noFavoriteFilesTextView.visibility = View.VISIBLE
                }
                setupRecyclerView()
            }else {
                data = items
                setupRecyclerView()
            }
        })
    }


    override fun onItemClick(position: Int) {
        val data = data?.get(position)

        val fileType = data?.fileType
        if(fileType == "pdf"){
            val action = FavoriteFragmentDirections.actionFavoriteToPdfViewerFragment(data)
            findNavController().navigate(action)
        }else {
            val action =
                data?.let { FavoriteFragmentDirections.actionFavoriteToImageViewFragment(it) }
            if (action != null) {
                findNavController().navigate(action)
            }
        }
    }

    override fun onItemLongClick(position: Int): Boolean {
        return false
    }

    private fun setupRecyclerView() {
        binding?.let {
            it.favoriteRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            it.favoriteRecyclerView.adapter = filesListAdapter

        }
        data?.let { filesListAdapter.setData(it.toMutableList()) }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.favoriteRecyclerView?.adapter = null
        _binding = null
        viewModel = null

    }


}