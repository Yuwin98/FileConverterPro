package com.yuwin.fileconverterpro

import androidx.constraintlayout.widget.ConstraintLayout

interface FileListClickListener {

    fun onItemClick(layout: ConstraintLayout ,position: Int)

    fun onItemLongClick(layout: ConstraintLayout,position: Int): Boolean
}