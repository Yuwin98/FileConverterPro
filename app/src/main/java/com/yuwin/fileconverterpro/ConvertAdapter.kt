package com.yuwin.fileconverterpro

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.yuwin.fileconverterpro.databinding.QueueItemBinding
import java.util.*

class ConvertAdapter: RecyclerView.Adapter<ConvertAdapter.ViewHolder>(), ItemTouchInterface{

    private var data = mutableListOf<ConvertInfo>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = data[position]
        holder.bind(currentItem)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    class ViewHolder(private val binding: QueueItemBinding): RecyclerView.ViewHolder(binding.root) {

        fun bind(convertInfo: ConvertInfo) {
            binding.item = convertInfo
            binding.executePendingBindings()
        }


        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding =  QueueItemBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }
    }

    fun setData(newData: List<ConvertInfo>) {
        val convertDiffUtil = ConvertDiffUtil(data, newData)
        val diffUtilResult = DiffUtil.calculateDiff(convertDiffUtil)
        data = newData.toMutableList()
        diffUtilResult.dispatchUpdatesTo(this)
    }

    override fun onItemMove(from: Int, to: Int): Boolean {
        if (from < to) {
            for (i in from until to) {
                Collections.swap(data, i, i + 1)
            }
        } else {
            for (i in from downTo to + 1) {
                Collections.swap(data, i, i - 1)
            }
        }
        notifyItemMoved(from, to)
        return true
    }

    override fun onItemSwipe(position: Int) {
        data.removeAt(position)
        notifyItemRemoved(position)
    }

    fun getAdapterData(): MutableList<ConvertInfo> {
        return data
    }

}