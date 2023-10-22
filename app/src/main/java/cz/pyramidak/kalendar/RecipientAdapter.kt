package cz.pyramidak.kalendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cz.pyramidak.kalendar.databinding.AdapterRecipientBinding
import cz.pyramidak.kalendar.firebase.Odber

class RecipientAdapter(val items: List<Odber>) : RecyclerView.Adapter<RecipientAdapter.MyViewHolder>() {
    private val viewHolders: MutableList<MyViewHolder> = mutableListOf()

    class MyViewHolder(v: View, val binding: AdapterRecipientBinding) : RecyclerView.ViewHolder(v), View.OnClickListener {
        init { v.setOnClickListener(this) }
        lateinit var item: Odber
        override fun onClick(v: View) {
            v.isSelected = !v.isSelected
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = AdapterRecipientBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding.root, binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, index: Int) {
        val item = getItem(index)
        holder.item = item
        holder.binding.tvBest.text = item.name
        viewHolders.add(holder)
    }

    override fun getItemCount(): Int = items.size

    private fun getItem(index: Int): Odber = items[index]

    fun getSelectedItems(): List<Odber> {
        return viewHolders.filter { it.itemView.isSelected }.map { it.item }
    }
}
