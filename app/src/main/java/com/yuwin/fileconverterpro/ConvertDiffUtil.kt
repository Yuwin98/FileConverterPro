package com.yuwin.fileconverterpro

import androidx.recyclerview.widget.DiffUtil

class ConvertDiffUtil(
    private val oldList: List<ConvertInfo>,
    private val newList: List<ConvertInfo>
): DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
       return oldList[oldItemPosition].uri == newList[newItemPosition].uri
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}