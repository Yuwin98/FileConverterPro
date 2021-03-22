package com.yuwin.fileconverterpro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.yuwin.fileconverterpro.databinding.PdfPreviewItemBinding


class PdfPreviewGridAdapter(private val fileItemClick: FileListClickListener): RecyclerView.Adapter<PdfPreviewGridAdapter.ViewHolder>() {

    private var data = mutableListOf<PdfPreviewModel>()

    class ViewHolder(private val binding: PdfPreviewItemBinding, private val onItemClickListener: FileListClickListener): RecyclerView.ViewHolder(binding.root), View.OnClickListener, View.OnLongClickListener {


        fun bind(item: PdfPreviewModel, pos: Int) {
            binding.pdfPreviewModel = item
            binding.position = pos + 1
            binding.pdfPreviewRadioButton.setOnClickListener(this)
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup, onItemClickListener: FileListClickListener): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = PdfPreviewItemBinding.inflate(layoutInflater)
                return ViewHolder(binding, onItemClickListener)
            }
        }

        override fun onClick(v: View?) {
            onItemClickListener.onItemClick(bindingAdapterPosition)
        }

        override fun onLongClick(v: View?): Boolean {
            return false
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent, fileItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(data[position], position)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setData(newData: MutableList<PdfPreviewModel>) {
        val filesDiffUtil = PdfPreviewDiffUtil(data, newData)
        val diffUtilResults = DiffUtil.calculateDiff(filesDiffUtil)
        data = newData.toMutableList()
        diffUtilResults.dispatchUpdatesTo(this)
    }


}