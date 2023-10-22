package cz.pyramidak.kalendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import cz.pyramidak.kalendar.databinding.AdapterPlanBinding
import java.time.LocalDateTime


class PlanAdapter(val items: MutableList<Plan>, private val activity: EditFragment?) : RecyclerView.Adapter<PlanAdapter.MyViewHolder>() {
    class MyViewHolder(v: View, val binding: AdapterPlanBinding) : RecyclerView.ViewHolder(v) {
        var typePicker: Int = 0
        fun showPicker(visibility: Int) {
            binding.timePicker.visibility = visibility
            binding.btnPicker.visibility = visibility
            binding.tvPicker.visibility = visibility
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = AdapterPlanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding.root, binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, index: Int) {
        val item = getItem(index)
        holder.showPicker(View.GONE)
        holder.binding.apply {
            tvTime.text = createTime(item.den.hour, item.den.minute)
            item.alarm?.let { tvAlarm.text = createTime(it.hour, it.minute) }
            etNote.setText(item.text)
            cbMonthly.isChecked = item.mesicne
            cbYearly.isChecked = item.rocne
            timePicker.setIs24HourView(true)
            timePicker.hour = item.den.hour
            timePicker.minute = item.den.minute

            btnPicker.setOnClickListener {
                holder.showPicker(View.GONE)
                holder.typePicker = 0
            }
            timePicker.setOnTimeChangedListener { _, hour, minute ->
                val time = LocalDateTime.of(item.den.year, item.den.monthValue, item.den.dayOfMonth, hour, minute)
                when (holder.typePicker) {
                    1 -> {  tvTime.text = createTime(hour, minute)
                            item.den = time
                            item.alarm = time //.minusMinutes(15)
                            item.alarm?.let { tvAlarm.text = createTime(it.hour, it.minute) } }
                    2 -> {  tvAlarm.text = createTime(hour, minute)
                            item.alarm = time }
                }
            }
            tvTime.setOnClickListener{
                holder.typePicker = 1
                holder.showPicker(View.VISIBLE)
                tvPicker.text = activity?.resources?.getString(R.string.time_event)
            }
            tvAlarm.setOnClickListener{
                holder.typePicker = 2
                holder.showPicker(View.VISIBLE)
                tvPicker.text = activity?.resources?.getString(R.string.time_alarm)
            }
            etNote.doOnTextChanged{ text, _, _, _ ->
                item.text = text.toString()
                if (!items.any { it.text == "" }) {
                    items.add(Plan(LocalDateTime.of(items[0].vznik.year,items[0].vznik.monthValue,items[0].vznik.dayOfMonth,0,0)))
                    notifyItemInserted(items.size - 1)
                }
                etNote.requestFocus()
            }
            cbMonthly.setOnCheckedChangeListener{ _, isChecked ->
                item.mesicne = isChecked
            }
            cbYearly.setOnCheckedChangeListener{ _, isChecked ->
                item.rocne = isChecked
            }


        }


    }

    private fun createTime(hour: Int, minute: Int): String {
        val hodina = if (hour < 10) "0$hour" else hour
        val minuta = if (minute < 10) "0$minute" else minute
        return "$hodina:$minuta"
    }

    override fun getItemCount(): Int = items.size

    fun getItem(index: Int): Plan = items[index]

    fun getPosition(note: Plan): Int = items.indexOf(note)
}
