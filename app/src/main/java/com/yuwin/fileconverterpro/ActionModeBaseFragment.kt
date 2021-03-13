package com.yuwin.fileconverterpro

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ActionMode
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yuwin.fileconverterpro.db.ConvertedFile
import kotlinx.coroutines.launch
import java.io.File

abstract class ActionModeBaseFragment : Fragment() {

    protected open var bottomNavigationVisibility = View.VISIBLE


    private lateinit var navHostFragment: FragmentContainerView

    private var isCopyOperation = false

    private lateinit var commonActionBar: MotionLayout
    private lateinit var moveCopyActionBar: MotionLayout

    private lateinit var moveCopyItemCount: TextView

    private lateinit var moveFilesButton: ConstraintLayout
    private lateinit var copyFilesButton: ConstraintLayout
    private lateinit var fileDetailsButton: ConstraintLayout
    private lateinit var fileShareButton: ConstraintLayout
    private lateinit var fileDeleteButton: ConstraintLayout
    private lateinit var moveCopyFilesCancelButton: ConstraintLayout
    private lateinit var moveCopyFilesActionButton: ConstraintLayout

    private lateinit var moveCopyCancelButton: MaterialButton
    private lateinit var moveCopyActionButton: MaterialButton

    private val mainViewModel: MainViewModel by viewModels()

    protected open var actionMode: ActionMode? = null
    protected open var multiSelection = false
    protected open var selectedFiles = arrayListOf<ConvertedFile>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (activity is MainActivity) {
            val mainActivity = activity as MainActivity
            mainActivity.setBottomNavigationViewVisibility(bottomNavigationVisibility)
        }
    }

    override fun onResume() {
        super.onResume()
        if (activity is MainActivity) {
            val mainActivity = activity as MainActivity
            mainActivity.setBottomNavigationViewVisibility(bottomNavigationVisibility)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        activity?.let {
            navHostFragment = it.findViewById(R.id.navHostFragment)

            commonActionBar = it.findViewById(R.id.commonActionModeBar)
            moveCopyActionBar = it.findViewById(R.id.moveCopyLayout)

            moveFilesButton = it.findViewById(R.id.moveFileConstraintLayout)
            copyFilesButton = it.findViewById(R.id.copyFileConstraintLayout)
            fileDetailsButton = it.findViewById(R.id.fileDetailsConstraintLayout)
            fileShareButton = it.findViewById(R.id.fileShareConstraintLayout)
            fileDeleteButton = it.findViewById(R.id.fileDeleteConstraintLayout)

            moveCopyFilesCancelButton = it.findViewById(R.id.moveCopyCancelConstraintLayout)
            moveCopyFilesActionButton = it.findViewById(R.id.moveCopyActionConstraintLayout)

            moveCopyItemCount = it.findViewById(R.id.moveCopyItemCount)

            moveCopyCancelButton = it.findViewById(R.id.moveCopyCancelButton)
            moveCopyActionButton = it.findViewById(R.id.moveCopyActionButton)
        }

        commonActionBar.setTransitionListener(object : MotionLayout.TransitionListener {
            override fun onTransitionStarted(motionLayout: MotionLayout?, p1: Int, p2: Int) {

            }

            override fun onTransitionChange(
                motionLayout: MotionLayout?,
                p1: Int,
                p2: Int,
                p3: Float
            ) {

            }

            override fun onTransitionCompleted(motionLayout: MotionLayout?, p1: Int) {
                if (!multiSelection) {
                    if (p1 == R.id.commonActionBarStart) {
                        motionLayout?.visibility = View.GONE
                    }
                }
            }

            override fun onTransitionTrigger(
                motionLayout: MotionLayout?,
                p1: Int,
                p2: Boolean,
                p3: Float
            ) {

            }

        })

        moveCopyActionBar.setTransitionListener(object : MotionLayout.TransitionListener {
            override fun onTransitionStarted(p0: MotionLayout?, p1: Int, p2: Int) {

            }

            override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, p3: Float) {
            }

            override fun onTransitionCompleted(motionLayout: MotionLayout?, p1: Int) {
                if (!multiSelection) {
                    if (p1 == R.id.moveCopyEnd) {
                        motionLayout?.visibility = View.GONE
                    }
                }
            }

            override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {
            }

        })


        moveFilesButton.setOnClickListener {
            (activity as MainActivity).filesToModify = selectedFiles.toMutableList()
            moveCopyItemCount.text = Util.getContentSize(selectedFiles.size)
            (activity as MainActivity).isCopyOperation = false
            attachHostToMoveCopyLayout()
            moveCopyActionButton.text = getString(R.string.move_here_text)
            moveCopyBarVisibility(View.VISIBLE)
            moveCopyTransitionStart()
            commonTransitionToEnd()
            actionMode?.finish()
        }

        copyFilesButton.setOnClickListener {
            (activity as MainActivity).filesToModify = selectedFiles.toMutableList()
            (activity as MainActivity).isCopyOperation = true
            attachHostToMoveCopyLayout()
            moveCopyActionButton.text = getString(R.string.copy_here_text)
            moveCopyBarVisibility(View.VISIBLE)
            moveCopyTransitionStart()
            commonTransitionToEnd()
            actionMode?.finish()
        }

        fileDetailsButton.setOnClickListener {

        }

        fileShareButton.setOnClickListener {
            var count = 0
            selectedFiles.forEach {
                if (it.isDirectory)
                    count++
            }
            when (count) {
                0 -> {
                    val shareIntent =
                        Util.startShareSheetMultiple(requireActivity(), selectedFiles)
                    startActivity(Intent.createChooser(shareIntent, "Send File(s) To"))
                }
                1 -> {
                    if (selectedFiles.size > 1) {
                        Toast.makeText(
                            requireContext(),
                            "Files and folders cannot mix when sharing",
                            Toast.LENGTH_SHORT
                        ).show()

                    } else {
                        val files = File(selectedFiles[0].filePath).listFiles()
                        if (files?.size!! < 100) {
                            val shareIntent =
                                Util.shareSheetMultipleDirectory(requireActivity(), files)
                            startActivity(Intent.createChooser(shareIntent, "Send File(s) To"))
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Maximum number of files that can be shared is 100",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                }
                else -> {
                    Toast.makeText(
                        requireContext(),
                        "Only 1 folder can be shared once",
                        Toast.LENGTH_SHORT
                    ).show()

                }
            }

        }

        fileDeleteButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete")
                .setMessage("This will delete selected files")
                .setPositiveButton("Delete") { dialog, _ ->
                    selectedFiles.forEach {
                        if (!it.isDirectory) {
                            lifecycleScope.launch {
                                mainViewModel.deleteSelectedFiles(it)
                            }
                        } else {
                            deleteDirectoryAndFiles(it)
                        }
                    }

                    multiSelection = false
                    Toast.makeText(
                        requireContext(),
                        "${selectedFiles.size} file(s) deleted",
                        Toast.LENGTH_SHORT
                    ).show()
                    selectedFiles.clear()
                    actionMode?.finish()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()

        }

        moveCopyCancelButton.setOnClickListener {
            attachHostToCommonActionBar()
            moveCopyTransitionEnd()
        }

        moveCopyActionButton.setOnClickListener {
            val filesToModify = (activity as MainActivity).filesToModify
            var newLocation = (activity as MainActivity).currentUserDirectory
            var currentLocation: String? = File(filesToModify[0].filePath).parent
            if (newLocation.last() == '/') {
                newLocation = newLocation.substring(0, newLocation.lastIndex)
            }
            if (currentLocation?.last() == '/') {
                currentLocation = currentLocation.substring(0, currentLocation.lastIndex)
            }
            Log.d("movecopydetails", newLocation)
            Log.d("movecopydetails", currentLocation!!)

            attachHostToCommonActionBar()
            moveCopyTransitionEnd()

            val isCopy = (activity as MainActivity).isCopyOperation
            val operation = if (isCopy) "copy" else "move"

            if (currentLocation != newLocation) {
                if (isCopy) {
                    mainViewModel.copyFileOrDirectory(filesToModify, newLocation)
                } else {
                    mainViewModel.moveFileOrDirectory(filesToModify, newLocation)
                }

            } else {
                Toast.makeText(
                    requireContext(),
                    "Cannot $operation to same directory",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }

    }


    fun commonActionBarVisibility(visibility: Int) {
        commonActionBar.visibility = visibility
    }

    private fun moveCopyBarVisibility(visibility: Int) {
        moveCopyActionBar.visibility = visibility
    }

    fun commonTransitionToEnd() {
        commonActionBar.transitionToEnd()
    }

    fun commonTransitionToStart() {
        commonActionBar.transitionToStart()
    }

    fun moveCopyTransitionStart() {
        moveCopyActionBar.transitionToStart()
    }

    private fun moveCopyTransitionEnd() {
        moveCopyActionBar.transitionToEnd()
    }


    fun attachHostToCommonActionBar() {
        val params = navHostFragment.layoutParams as ConstraintLayout.LayoutParams
        params.bottomToTop = commonActionBar.id
        navHostFragment.requestLayout()
    }

    private fun attachHostToMoveCopyLayout() {
        val params = navHostFragment.layoutParams as ConstraintLayout.LayoutParams
        params.bottomToTop = moveCopyActionBar.id
        navHostFragment.requestLayout()
    }

    private fun deleteDirectoryAndFiles(file: ConvertedFile) {
        if (file.isDirectory) {
            val filePath = file.filePath
            lifecycleScope.launch {
                mainViewModel.deleteSelectedFiles(file)
            }
            mainViewModel.readFiles.observe(viewLifecycleOwner, {
                val databaseFiles = Util.filterItemsInDirectory(File(filePath), it)
                databaseFiles.forEach {
                    lifecycleScope.launch {
                        mainViewModel.deleteSelectedFiles(it)
                    }
                }

            })
            val dir = File(file.filePath)
            dir.delete()
        }
    }
}