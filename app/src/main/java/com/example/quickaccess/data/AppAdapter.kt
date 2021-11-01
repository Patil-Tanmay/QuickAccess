package com.example.quickaccess.data

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.quickaccess.databinding.ItemAppBinding
import android.graphics.drawable.Drawable

class AppAdapter constructor(
    val onSelect : (String) -> Unit,
    val onUninstall: (String) -> Unit
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>(){

    private lateinit var list : List<AppDetails>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        return AppViewHolder(ItemAppBinding.inflate(LayoutInflater.from(parent.context),parent,false),
                onSelect0 = {
                    onSelect(it)
                },
            onUninstall = {
                onUninstall(it)
            }
            )
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {

        holder.bind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun setAppData(listApps : List<AppDetails>){
        this.list = listApps
    }

    class AppViewHolder(
        private val binding : ItemAppBinding,
        val onSelect0: (String) -> Unit,
        val onUninstall : (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root){

        fun bind(app : AppDetails){
            binding.name.text = app.name
            binding.imgView.setImageDrawable(app.image)

            binding.root.setOnClickListener {
                onSelect0(app.packageName)
            }

            binding.btnUnInstall.setOnClickListener {
                onUninstall(app.packageName)
            }
        }

    }

    
}