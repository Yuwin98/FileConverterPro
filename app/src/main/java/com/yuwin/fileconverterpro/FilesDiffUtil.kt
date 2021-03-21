package com.yuwin.fileconverterpro

import androidx.recyclerview.widget.DiffUtil
import com.yuwin.fileconverterpro.db.ConvertedFile

class FilesDiffUtil(
       private val oldList: List<ConvertedFile>,
       private val  newList: List<ConvertedFile>
): DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}