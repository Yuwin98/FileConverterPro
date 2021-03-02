package com.yuwin.fileconverterpro

import androidx.constraintlayout.widget.ConstraintLayout

interface FileListClickListener {

    fun onItemClick(position: Int)

    fun onItemLongClick(position: Int): Boolean
}