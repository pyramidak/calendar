package cz.pyramidak.kalendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import cz.pyramidak.kalendar.databinding.AdapterMessageBinding

class MessageAdapter(val items: List<Found>, private val activity: AppCompatActivity? = null) : RecyclerView.Adapter<MessageAdapter.MyViewHolder>() {

    class MyViewHolder(v: View, val binding: AdapterMessageBinding, val activity: AppCompatActivity?) : RecyclerView.ViewHolder(v), View.OnClickListener{
        lateinit var found: Found
        init { v.setOnClickListener(this) }
        override fun onClick(v: View) {
            found.selected = !found.selected
            binding.cbSelected.isChecked = found.selected
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = AdapterMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding.root, binding, activity)
    }

    override fun onBindViewHolder(holder: MyViewHolder, index: Int) {
        holder.found = getItem(index)
        holder.apply {
            binding.cbSelected.isChecked = found.selected
            binding.ivIcon.setBackgroundResource(found.icon)
            binding.tvName.text = found.name
            binding.tvDate.text = if (found.sender != null) found.sender else found.date.csString()
            binding.cbSelected.setOnCheckedChangeListener{ _: CompoundButton, checked: Boolean ->
                found.selected = checked
                binding.cbSelected.isChecked = checked
            }
        }
    }

    override fun getItemCount(): Int = items.size

    private fun getItem(index: Int): Found = items[index]
}