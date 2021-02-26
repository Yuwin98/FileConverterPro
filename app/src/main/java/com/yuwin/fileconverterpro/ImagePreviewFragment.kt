package com.yuwin.fileconverterpro

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.yuwin.fileconverterpro.databinding.FragmentImageViewBinding
import com.yuwin.fileconverterpro.db.ConvertedFile
import java.io.File


class ImagePreviewFragment : BaseFragment() {

    private val args by navArgs<ImagePreviewFragmentArgs>()
    private val binding by lazy {FragmentImageViewBinding.inflate(layoutInflater)}

    override var bottomNavigationVisibility = View.GONE

    private lateinit var imagePreviewViewModel: ImagePreviewViewModel

    private lateinit var deleteDialog: AlertDialog.Builder
    private lateinit var renameDialog: AlertDialog.Builder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imagePreviewViewModel = ViewModelProvider(
            this,
            ImagePreviewViewModelFactory(requireActivity().application, args.convertedFile.id)
        ).get(ImagePreviewViewModel::class.java)

        deleteDialog = AlertDialog.Builder(requireContext())
        renameDialog = AlertDialog.Builder(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding.lifecycleOwner = viewLifecycleOwner
        binding.convertedFile = args.convertedFile
        binding.viewModel = imagePreviewViewModel

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val file = args.convertedFile

        binding.favoriteOutlineImageView.setOnClickListener {
            if(imagePreviewViewModel.isFavorite.value == true) {
                imagePreviewViewModel.setFavorite(file, false)
            }else {
                imagePreviewViewModel.setFavorite(file, true)
            }
        }

        binding.shareImageView.setOnClickListener {

            val imageUri: Uri = FileProvider.getUriForFile(
                requireActivity(),
                "com.yuwin.fileconverterpro.fileprovider",
                File(file.filePath)
            )

            val typeString = getSendingType(file)
            val shareIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, imageUri)
                type = typeString
            }
            startActivity(Intent.createChooser(shareIntent, "Send To"))
        }

        binding.deleteImageView.setOnClickListener {
            deleteDialog.setTitle("Delete")
                .setMessage("Are you sure you want to delete this file?")
                .setPositiveButton("Delete") { dialog, id ->
                    dialog.dismiss()
                    imagePreviewViewModel.deleteFile(file)
                    findNavController().navigate(R.id.action_imageViewFragment_to_home)
                }
                .setNegativeButton("Cancel") {dialog, id ->
                    dialog.dismiss()
                }.show()
        }

        binding.editImageView.setOnClickListener {
            renameDialog.setTitle("Rename File")

            val newName = EditText(requireContext())
            newName.inputType = InputType.TYPE_CLASS_TEXT
            renameDialog.setView(newName)

            renameDialog.setPositiveButton("Rename"){dialog, id ->
                val name = newName.text.toString()
                if(name.isNotEmpty()) {
                    imagePreviewViewModel.rename(file, name)
                    dialog.dismiss()
                }else {
                    dialog.dismiss()
                }
            }
            renameDialog.setNegativeButton("Cancel"){dialog, id ->
                dialog.dismiss()
            }.show()
        }

    }

    private fun getSendingType(file: ConvertedFile): String {

        when(Util.getMimeType(requireContext(), file.uri)) {
            "jpg" -> {
                return "image/jpg"
            }
            "jpeg" -> {
                return "image/jpeg"
            }
            "png" -> {
                return "image/png"
            }
            "pdf" -> {
                return "application/pdf"
            }
            "webp" -> {
                return "image/webp"
            }
        }
        return "*/*"
    }


}