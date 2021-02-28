package com.yuwin.fileconverterpro

import android.view.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.yuwin.fileconverterpro.databinding.FileListItemBinding
import com.yuwin.fileconverterpro.db.ConvertedFile

class FilesListAdapter(private val fileItemClick: FileListClickListener): RecyclerView.Adapter<FilesListAdapter.ViewHolder>(){

    private var data  = mutableListOf<ConvertedFile>()

    class ViewHolder(private val binding: FileListItemBinding, private val onItemClickListener: FileListClickListener): RecyclerView.ViewHolder(binding.root) , View.OnClickListener, View.OnLongClickListener {


        fun bind(convertedFile: ConvertedFile) {
            binding.convertedFile = convertedFile
            binding.fileListRootLayout.setOnClickListener(this)
            binding.fileListRootLayout.setOnLongClickListener(this)
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup, onItemClickListener: FileListClickListener): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = FileListItemBinding.inflate(layoutInflater,parent, false)
                return ViewHolder(binding, onItemClickListener)
            }
        }

        override fun onClick(v: View?) {
            onItemClickListener.onItemClick(binding.fileListRootLayout,adapterPosition)

        }

        override fun onLongClick(v: View?): Boolean {
           return onItemClickListener.onItemLongClick(binding.fileListRootLayout,adapterPosition)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent, fileItemClick)
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