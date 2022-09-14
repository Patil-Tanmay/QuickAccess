package com.example.quickaccess.data

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.quickaccess.R
import com.example.quickaccess.databinding.ItemAppBinding

class AppAdapter constructor(
    val onSelect : (String) -> Unit,
    val onUninstall: (AppDetails, position:Int) -> Unit,
    val onLongPress: (AppDetails) -> Unit
) : ListAdapter<AppDetails,AppAdapter.AppViewHolder>(APP_COMPARATOR){

    private var list  = arrayListOf<AppDetails>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        return AppViewHolder(ItemAppBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
//        holder.bind(list[position])
        val currentItem = getItem(position)

        if (currentItem != null) {
            holder.bind(currentItem)
        }
    }

    fun setAppData(listApps : List<AppDetails>){
        this.list.clear()
        this.list.addAll(listApps)
    }

    inner class AppViewHolder(
        private val binding : ItemAppBinding
    ) : RecyclerView.ViewHolder(binding.root){

        fun bind(app : AppDetails){

            binding.root.animation = AnimationUtils.loadAnimation(binding.root.context, R.anim.item_scale)

            binding.name.text = app.name
            binding.packageName.text = app.packageName
            binding.imgView.setImageBitmap(app.image)

            binding.root.setOnClickListener {
                onSelect(app.packageName)
            }

            binding.btnUnInstall.setOnClickListener {
                onUninstall(app, adapterPosition)
            }

            binding.root.setOnLongClickListener {
                onLongPress(app)
                true
            }
        }

    }

    //to compare the data using diffUtil
    companion object {
        private val APP_COMPARATOR = object : DiffUtil.ItemCallback<AppDetails>() {
            override fun areItemsTheSame(oldItem: AppDetails, newItem: AppDetails): Boolean {
                return oldItem.name == newItem.name
            }

            override fun areContentsTheSame(oldItem: AppDetails, newItem: AppDetails): Boolean {
                return oldItem == newItem
            }
        }
    }

    
}