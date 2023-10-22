package cz.pyramidak.kalendar

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import cz.pyramidak.kalendar.databinding.AdapterSearchBinding
import java.time.LocalDate


class SearchAdapter(val items: List<Found>, private val activity: SearchFragment? = null) : RecyclerView.Adapter<SearchAdapter.MyViewHolder>(), Filterable  {
    var itemsFiltered: MutableList<Found> = items.toMutableList()

    init { total() }

    class MyViewHolder(v: View, val binding: AdapterSearchBinding, val activity: SearchFragment?) : RecyclerView.ViewHolder(v), View.OnClickListener {
        lateinit var found: Found
        init { v.setOnClickListener(this) }
        override fun onClick(v: View) {
            activity?.activityMain?.run {
                if (found.icon == R.drawable.ic_person) {
                    kalendar.dateNow = LocalDate.of(kalendar.dateNow.year, found.date.monthValue, found.date.dayOfMonth)
                } else {
                    if (found.plan?.rocne == true) {
                        kalendar.dateNow = LocalDate.of(kalendar.dateNow.year, found.date.monthValue, found.date.dayOfMonth)
                    } else if (found.plan?.mesicne == true && found.plan?.rocne == true) {
                        kalendar.dateNow = LocalDate.of(kalendar.dateNow.year, kalendar.dateNow.monthValue, found.date.dayOfMonth)
                    } else {
                        kalendar.dateNow = found.date
                    }
                }
                binding.pager.currentItem = 2
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = AdapterSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding.root, binding, activity)
    }

    override fun onBindViewHolder(holder: MyViewHolder, index: Int) {
        holder.found = getItem(index)
        holder.apply {
            binding.ivIcon.setBackgroundResource(found.icon)
            binding.tvName.text = found.name
            binding.tvDate.text = found.date.csString()
        }
    }

    override fun getItemCount(): Int = itemsFiltered.size

    private fun getItem(index: Int): Found = itemsFiltered[index]

    var filterStartWith: Boolean = false
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString()
                getItemsFiltered(typeSet)
                if (charString.isNotEmpty()) {
                    val filteredList: MutableList<Found> = mutableListOf()
                    for (one in itemsFiltered) {
                        if (filterStartWith) {
                            if (one.name.lowercase().startsWith(charString.lowercase()) || one.name.startsWith(charSequence)) {
                                filteredList.add(one)
                            } else {
                                if (one.date.csString().startsWith(charString)) filteredList.add(one)
                            }
                        } else {
                            if (one.name.lowercase().contains(charString.lowercase()) || one.name.contains(charSequence)) {
                                filteredList.add(one)
                            } else {
                                if (one.date.csString().contains(charString)) filteredList.add(one)
                            }
                        }
                    }
                    itemsFiltered = filteredList
                }
                val filterResults = FilterResults()
                filterResults.values = itemsFiltered
                return filterResults
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
                @Suppress("UNCHECKED_CAST")
                itemsFiltered = filterResults.values as MutableList<Found>
                notifyDataSetChanged()
                total()
            }
        }
    }

    private var direction: Boolean = false
    @SuppressLint("NotifyDataSetChanged")
    fun sort(column: String): Boolean {
        if (column == "name") {
            if (direction) itemsFiltered.sortBy { it.name } else itemsFiltered.sortByDescending { it.name }
        }
        if (column == "date") {
            if (direction) itemsFiltered.sortBy { it.date } else itemsFiltered.sortByDescending { it.date }
        }
        notifyDataSetChanged()
        total()
        direction = !direction
        return direction
    }

    var typeSet: Tables = Tables.ALL
    @SuppressLint("NotifyDataSetChanged")
    fun filter(type: Tables) {
        getItemsFiltered(type)
        typeSet = type
        notifyDataSetChanged()
        total()
    }
    fun getItemsFiltered(type: Tables) {
        itemsFiltered = when (type) {
            Tables.PERSON -> items.filter { it.icon == R.drawable.ic_person }.toMutableList()
            Tables.PLAN -> items.filter { it.icon == R.drawable.ic_note }.toMutableList()
            else -> items.toMutableList()
        }
    }

    private fun total() {
        activity?.binding?.run {
            val persons = activity.getString(R.string.count_person) + ": ${itemsFiltered.filter { it.icon == R.drawable.ic_person }.size}"
            rbPersons.text = persons
            val plans = activity.getString(R.string.count_plan) + ": ${itemsFiltered.filter { it.icon == R.drawable.ic_note }.size}"
            rbPlans.text = plans
        }
    }
}