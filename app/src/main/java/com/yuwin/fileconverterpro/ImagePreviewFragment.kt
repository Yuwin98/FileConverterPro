package com.yuwin.fileconverterpro

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yuwin.fileconverterpro.databinding.FragmentImageViewBinding
import java.io.File


class ImagePreviewFragment : BaseFragment() {

    private val args by navArgs<ImagePreviewFragmentArgs>()
    private val binding by lazy { FragmentImageViewBinding.inflate(layoutInflater) }

    override var bottomNavigationVisibility = View.GONE

    private lateinit var filePreviewViewModel: FilePreviewViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        filePreviewViewModel = ViewModelProvider(
            this,
            FilePreviewViewModelFactory(requireActivity().application, args.convertedFile.id)
        ).get(FilePreviewViewModel::class.java)


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding.lifecycleOwner = viewLifecycleOwner
        binding.convertedFile = args.convertedFile
        binding.viewModel = filePreviewViewModel

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val file = args.convertedFile

        binding.favoriteOutlineImageView.setOnClickListener {
            if (filePreviewViewModel.isFavorite.value == true) {
                filePreviewViewModel.setFavorite(file, false)
            } else {
                filePreviewViewModel.setFavorite(file, true)
            }
        }

        binding.shareImageView.setOnClickListener {
            val typeString = Util.getSendingType(requireContext(), file)
            val shareIntent =
                Util.startShareSheetSingle(requireActivity(), File(file.filePath), typeString)
            startActivity(Intent.createChooser(shareIntent, "Send To"))
        }

        binding.deleteImageView.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete")
                .setMessage("This will delete this file")
                .setPositiveButton("Delete") { dialog, _ ->
                    dialog.dismiss()
                    filePreviewViewModel.deleteFile(file)
                    findNavController().navigate(R.id.action_imageViewFragment_to_home)
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }.show()

        }

        binding.editImageView.setOnClickListener {
            val editTextView = layoutInflater.inflate(R.layout.edittext_layout, null)
            MaterialAlertDialogBuilder(requireContext())
                .setView(editTextView)
                .setTitle("Rename File")
                .setPositiveButton("Rename") { dialog, _ ->
                    val editText = editTextView.findViewById<EditText>(R.id.renameFileEditText)
                    val name = editText.text.toString()
                    if (name.isNotBlank()) {
                        dialog.dismiss()
                        filePreviewViewModel.rename(file, name)
                    } else {
                        Toast.makeText(requireContext(), "Name empty", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()

        }

    }


}