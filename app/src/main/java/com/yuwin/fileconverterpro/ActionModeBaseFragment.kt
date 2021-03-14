package com.yuwin.fileconverterpro

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ActionMode
import android.view.View
import android.view.WindowManager
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
import com.yuwin.fileconverterpro.Util.Companion.observeOnce
import com.yuwin.fileconverterpro.db.ConvertedFile
import kotlinx.coroutines.*
import java.io.File


abstract class ActionModeBaseFragment : Fragment() {

    protected open var bottomNavigationVisibility = View.VISIBLE

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)


    // Details Card Items-------------------------------------------------

    private lateinit var fileNameTitle: TextView
    private lateinit var fileNameText: TextView

    private lateinit var fileSizeText: TextView

    private lateinit var lastModifiedTitle: TextView
    private lateinit var lastModifiedText: TextView

    private lateinit var containsTitle: TextView
    private lateinit var containsText: TextView

    private lateinit var filePathTitle: TextView
    private lateinit var filePathText: TextView


    //---------------------------------------------------------------------


    private lateinit var navHostFragment: FragmentContainerView


    private lateinit var commonActionBar: MotionLayout
    private lateinit var moveCopyActionBar: MotionLayout
    private lateinit var detailsLayout: MotionLayout

    private lateinit var moveCopyItemCount: TextView

    private lateinit var moveFilesButton: ConstraintLayout
    private lateinit var copyFilesButton: ConstraintLayout
    private lateinit var fileDetailsButton: ConstraintLayout
    private lateinit var fileShareButton: ConstraintLayout
    private lateinit var fileDeleteButton: ConstraintLayout
    private lateinit var moveCopyFilesCancelButton: ConstraintLayout
    private lateinit var moveCopyFilesActionButton: ConstraintLayout
    private lateinit var detailsCardOkButton: ConstraintLayout
    private lateinit var progressBarLayout: ConstraintLayout

    private lateinit var moveCopyCancelButton: MaterialButton
    private lateinit var moveCopyActionButton: MaterialButton
    private lateinit var detailsCardButton: MaterialButton

    private val mainViewModel: MainViewModel by viewModels()

    private lateinit var commonActionBarTransitionListener: MotionLayout.TransitionListener
    private lateinit var moveCopyActionModeTransitionListener: MotionLayout.TransitionListener
    private lateinit var detailsLayoutTransitionListener: MotionLayout.TransitionListener

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
            fileNameTitle = it.findViewById(R.id.detailsFileNameTitle)
            fileNameText = it.findViewById(R.id.detailsFileNameText)

            fileSizeText = it.findViewById(R.id.detailsFileSizeText)

            lastModifiedTitle = it.findViewById(R.id.detailsFileLastModifiedTitle)
            lastModifiedText = it.findViewById(R.id.detailsFileLastModifiedText)

            containsTitle = it.findViewById(R.id.detailsFileContainsTitle)
            containsText = it.findViewById(R.id.detailsFileContainsText)

            filePathTitle = it.findViewById(R.id.detailsFilePathTitle)
            filePathText = it.findViewById(R.id.detailsFilePathText)
        }


        activity?.let {
            navHostFragment = it.findViewById(R.id.navHostFragment)
            progressBarLayout = it.findViewById(R.id.progressBarLayout)

            commonActionBar = it.findViewById(R.id.commonActionModeBar)
            moveCopyActionBar = it.findViewById(R.id.moveCopyLayout)
            detailsLayout = it.findViewById(R.id.fileDetailsViewConstraintLayout)

            moveFilesButton = it.findViewById(R.id.moveFileConstraintLayout)
            copyFilesButton = it.findViewById(R.id.copyFileConstraintLayout)
            fileDetailsButton = it.findViewById(R.id.fileDetailsConstraintLayout)
            fileShareButton = it.findViewById(R.id.fileShareConstraintLayout)
            fileDeleteButton = it.findViewById(R.id.fileDeleteConstraintLayout)
            detailsCardOkButton = it.findViewById(R.id.detailsCardButtonConstraintLayout)

            moveCopyFilesCancelButton = it.findViewById(R.id.moveCopyCancelConstraintLayout)
            moveCopyFilesActionButton = it.findViewById(R.id.moveCopyActionConstraintLayout)

            moveCopyItemCount = it.findViewById(R.id.moveCopyItemCount)

            moveCopyCancelButton = it.findViewById(R.id.moveCopyCancelButton)
            moveCopyActionButton = it.findViewById(R.id.moveCopyActionButton)
            detailsCardButton = it.findViewById(R.id.detailsCardButton)
        }
        commonActionBarTransitionListener = object : MotionLayout.TransitionListener {
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

        }
        moveCopyActionModeTransitionListener = object : MotionLayout.TransitionListener {
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

        }
        detailsLayoutTransitionListener = object : MotionLayout.TransitionListener {
            override fun onTransitionStarted(p0: MotionLayout?, p1: Int, p2: Int) {

            }

            override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, p3: Float) {
            }

            override fun onTransitionCompleted(motionLayout: MotionLayout?, p1: Int) {
                if (p1 == R.id.detailsCardStart) {
                    motionLayout?.visibility = View.GONE
                }
            }

            override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {
            }

        }

        commonActionBar.setTransitionListener(commonActionBarTransitionListener)

        moveCopyActionBar.setTransitionListener(moveCopyActionModeTransitionListener)

        detailsLayout.setTransitionListener(detailsLayoutTransitionListener)


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
            detailsLayout.visibility = View.VISIBLE
            setupDetailsCard(selectedFiles.size, selectedFiles)
            detailsLayout.transitionToEnd()
        }

        detailsCardButton.setOnClickListener {
            detailsLayout.transitionToStart()

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
                        when {
                            files?.isEmpty() == true -> {
                                Toast.makeText(
                                    requireContext(),
                                    "Folder is empty",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            files?.size!! > 100 -> {
                                Toast.makeText(
                                    requireContext(),
                                    "Maximum number of files that can be shared is 100",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            else -> {
                                val shareIntent =
                                    Util.shareSheetMultipleDirectory(requireActivity(), files)
                                startActivity(Intent.createChooser(shareIntent, "Send File(s) To"))
                            }
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
            activity?.runOnUiThread {
                progressBarLayout.visibility = View.VISIBLE
                activity?.window?.setFlags(
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                )
            }

            copyMoveOperation()

        }

    }

    private fun copyMoveOperation() {
        val filesToModify = (activity as MainActivity).filesToModify
        var newLocation = (activity as MainActivity).currentUserDirectory
        var currentLocation: String? = File(filesToModify[0].filePath).parent
        if (newLocation.last() == '/') {
            newLocation = newLocation.substring(0, newLocation.lastIndex)
        }
        if (currentLocation?.last() == '/') {
            currentLocation = currentLocation.substring(0, currentLocation.lastIndex)
        }
//        Log.d("movecopydetails", newLocation)
//        Log.d("movecopydetails", currentLocation!!)

        attachHostToCommonActionBar()
        moveCopyTransitionEnd()

        val isCopy = (activity as MainActivity).isCopyOperation
        val operation = if (isCopy) "copy" else "move"

        val setOfPaths = filesToModify.map { it.filePath }
        Log.d("invalidop", setOfPaths.toString())
        Log.d("invalidop", newLocation)

        if (newLocation in setOfPaths) {
            Toast.makeText(
                requireContext(),
                "Can't Perform operation, Destination folder is a subfolder of a folder being moved",
                Toast.LENGTH_SHORT
            ).show()
            activity?.runOnUiThread {
                progressBarLayout.visibility = View.GONE
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            }
            return
        }

        if (currentLocation != newLocation) {

            if (isCopy) {
                mainViewModel.readAllDirectoryFiles.observeOnce(viewLifecycleOwner, {

                    scope.launch {
                        val job = async {
                            mainViewModel.copyFileOrDirectory(
                                filesToModify,
                                it,
                                newLocation
                            )
                        }
                        job.join()
                    }.invokeOnCompletion { throwable ->
                        if (throwable is CancellationException) {
                            activity?.runOnUiThread {
                                progressBarLayout.visibility = View.GONE
                                Toast.makeText(
                                    requireContext(),
                                    "Operation failed",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                            }
                        } else {
                            activity?.runOnUiThread {
                                progressBarLayout.visibility = View.GONE
                                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                            }
                        }
                    }


                })

            } else {
                mainViewModel.readAllDirectoryFiles.observeOnce(viewLifecycleOwner, {

                    scope.launch {
                        val job = async {
                            mainViewModel.moveFileOrDirectory(filesToModify, it, newLocation)
                        }
                        job.join()
                    }.invokeOnCompletion { throwable ->
                        if (throwable is CancellationException) {
                            activity?.runOnUiThread {
                                progressBarLayout.visibility = View.GONE
                                Toast.makeText(
                                    requireContext(),
                                    "Operation failed",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                            }
                        } else {
                            activity?.runOnUiThread {
                                progressBarLayout.visibility = View.GONE
                                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                            }
                        }
                    }
                })
            }


        } else {
            Toast.makeText(
                requireContext(),
                "Cannot $operation to same directory",
                Toast.LENGTH_SHORT
            ).show()
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

    fun detailsLayoutTransitionEnd() {
        detailsLayout.transitionToEnd()
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

        val filePath = file.filePath

        mainViewModel.readFiles.observeOnce(viewLifecycleOwner, { files ->
            val databaseFiles = Util.filterItemsInDirectory(File(filePath), files)
            databaseFiles.forEach {
                if (it.isDirectory) {
                    deleteDirectoryAndFiles(it)

                } else {
                    lifecycleScope.launch {
                        mainViewModel.deleteSelectedFiles(it)
                    }
                }
            }
        })

        File(filePath).delete()

        lifecycleScope.launch {
            mainViewModel.deleteSelectedFiles(file)
        }

    }


    private fun setupDetailsCard(selectedFilesSize: Int, selectedFiles: List<ConvertedFile>) {
        showHideCardDetails(selectedFilesSize)
        showRelevantCardDetails(selectedFiles)
    }

    private fun showHideCardDetails(size: Int) {

        when (size) {
            1 -> {
                fileNameTitle.visibility = View.VISIBLE
                fileNameText.visibility = View.VISIBLE

                containsText.visibility = View.VISIBLE
                containsTitle.visibility = View.VISIBLE

                lastModifiedTitle.visibility = View.VISIBLE
                lastModifiedText.visibility = View.VISIBLE

                filePathTitle.visibility = View.VISIBLE
                filePathText.visibility = View.VISIBLE
            }
            else -> {

                containsTitle.visibility = View.VISIBLE
                containsText.visibility = View.VISIBLE

                fileNameTitle.visibility = View.GONE
                fileNameText.visibility = View.GONE

                lastModifiedTitle.visibility = View.GONE
                lastModifiedText.visibility = View.GONE

                filePathTitle.visibility = View.GONE
                filePathText.visibility = View.GONE

            }
        }
    }

    private fun showRelevantCardDetails(selectedFiles: List<ConvertedFile>) {
        when (selectedFiles.size) {
            1 -> {
                val file = selectedFiles[0]
                val fileSize = Util.retrieveFileSize(File(file.filePath))
                fileNameText.text = file.fileName
                fileSizeText.text = Util.convertBytes(fileSize)
                filePathText.text = File(file.filePath).absolutePath
                lastModifiedText.text = Util.getDataString(File(file.filePath).lastModified())
                retrieveAndSetFilesAndFolderCount(selectedFiles)
            }
            else -> {
                var fileSize = 0L
                selectedFiles.forEach { currentFile ->
                    val currentFileSize = Util.retrieveFileSize(File(currentFile.filePath))
                    fileSize += currentFileSize
                }
                fileSizeText.text = Util.convertBytes(fileSize)
                retrieveAndSetFilesAndFolderCount(selectedFiles)
            }
        }
    }

    private fun retrieveAndSetFilesAndFolderCount(selectedFiles: List<ConvertedFile>) {
        when (selectedFiles.size) {
            1 -> {
                val file = selectedFiles[0]
                if (file.isDirectory) {
                    val (fileCount, folderCount) = countFilesAndFolders(File(file.filePath))
                    containsText.text = getContentText(fileCount, folderCount)
                } else {
                    containsTitle.visibility = View.GONE
                    containsText.visibility = View.GONE
                }

            }
            else -> {
                var files = 0
                var folders = 0
                selectedFiles.forEach { file ->
                    val (fileCount, folderCount) = countFilesAndFolders(File(file.filePath))
                    files += fileCount
                    folders += folderCount
                }

                containsText.text = getContentText(files, folders)
            }
        }
    }


    private fun getContentText(files: Int, folders: Int): String {

        if (files == 0 && folders > 0) {
            return when (folders) {
                1 -> {
                    "$folders folder"
                }
                else -> {
                    "$folders folders"
                }
            }
        }

        if (folders == 0 && files > 1) {
            return "$files files"
        }

        return if (files == 1 && folders == 1) {
            "$files file, $folders folder"
        } else if (files == 1 && folders > 1) {
            "$files file, $folders folders"
        } else if (files > 1 && folders == 1) {
            "$files files, $folders folder"
        } else {
            "$files files, $folders folders"
        }
    }

    private fun countFilesAndFolders(file: File): Pair<Int, Int> {
        var files = 0
        var folders = 0

        if (file.isDirectory) {
            folders++
            val fileList = file.listFiles()
            fileList?.forEach { currentFile ->
                val (fileCount, folderCount) = countFilesAndFolders(currentFile)
                files += fileCount
                folders += folderCount
            }

        } else {
            files += 1
        }

        return Pair(files, folders)

    }

    override fun onDestroy() {
        super.onDestroy()
        commonActionBar.removeTransitionListener(commonActionBarTransitionListener)
        moveCopyActionBar.removeTransitionListener(moveCopyActionModeTransitionListener)
        detailsLayout.removeTransitionListener(detailsLayoutTransitionListener)
    }
}