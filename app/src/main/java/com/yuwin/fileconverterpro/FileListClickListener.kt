package com.yuwin.fileconverterpro


interface FileListClickListener {

    fun onItemClick(position: Int)

    fun onItemLongClick(position: Int): Boolean
}