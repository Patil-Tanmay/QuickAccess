package com.example.quickaccess.data

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.quickaccess.databinding.ItemAppBinding

class AppAdapter constructor(
    val onSelect : (String) -> Unit,
    val onUninstall: (String) -> Unit,
    val onLongPress: (AppDetails) -> Unit
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>(){

    private var list  = arrayListOf<AppDetails>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        return AppViewHolder(ItemAppBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun setAppData(listApps : List<AppDetails>){
        this.list.clear()
        this.list.addAll(listApps)
    }

    inner class AppViewHolder(
        private val binding : ItemAppBinding
    ) : RecyclerView.ViewHolder(binding.root){

        fun bind(app : AppDetails){
            binding.name.text = app.name
            binding.imgView.setImageDrawable(app.image)

            binding.root.setOnClickListener {
                onSelect(app.packageName)
            }

            binding.btnUnInstall.setOnClickListener {
                onUninstall(app.packageName)
            }

            binding.root.setOnLongClickListener {
                onLongPress(app)
                true
            }
        }

    }

    
}