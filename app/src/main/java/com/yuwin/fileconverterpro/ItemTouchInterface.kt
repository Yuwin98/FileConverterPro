package com.yuwin.fileconverterpro

interface ItemTouchInterface {

    fun onItemMove(from: Int, to: Int): Boolean

    fun onItemSwipe(position: Int)

}