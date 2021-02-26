package com.yuwin.fileconverterpro

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.yuwin.fileconverterpro.databinding.FileListItemBinding
import com.yuwin.fileconverterpro.db.ConvertedFile

class FilesListAdapter: RecyclerView.Adapter<FilesListAdapter.ViewHolder>() {

    private var data  = mutableListOf<ConvertedFile>()

    class ViewHolder(private val binding: FileListItemBinding): RecyclerView.ViewHolder(binding.root) {


        fun bind(convertedFile: ConvertedFile) {
            binding.convertedFile = convertedFile
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = FileListItemBinding.inflate(layoutInflater,parent, false)
                return ViewHolder(binding)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setData(newData: MutableList<ConvertedFile>) {
        val filesDiffUtil = FilesDiffUtil(data, newData)
        val diffUtilResults = DiffUtil.calculateDiff(filesDiffUtil)
        data = newData.toMutableList()
        diffUtilResults.dispatchUpdatesTo(this)
    }
}