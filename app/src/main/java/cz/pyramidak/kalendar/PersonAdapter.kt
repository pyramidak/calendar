package cz.pyramidak.kalendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import cz.pyramidak.kalendar.databinding.AdapterPersonBinding
import java.time.LocalDate

class PersonAdapter(val items: MutableList<Person>, private val activity: EditFragment?) : RecyclerView.Adapter<PersonAdapter.MyViewHolder>() {
    class MyViewHolder(v: View, val binding: AdapterPersonBinding) : RecyclerView.ViewHolder(v) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = AdapterPersonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding.root, binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, index: Int) {
        val item = getItem(index)
        holder.binding.apply {

            etName.setText(item.jmeno)
            etPerNum.setText(item.rodne)
            item.narozeni?.let { born ->
                etDay.setText(born.dayOfMonth.toString())
                etMonth.setText(born.monthValue.toString())
                etYear.setText(born.year.toString())
                etYears.setText((LocalDate.now().year - born.year).toString())
            }
            item.umrti?.let { dead ->
                etDayD.setText(dead.dayOfMonth.toString())
                etMonthD.setText(dead.monthValue.toString())
                etYearD.setText(dead.year.toString())
                item.narozeni?.let { born -> etYearsD.setText((dead.year - born.year).toString()) }
            }
            etName.doOnTextChanged{ text, _, _, _ ->
                item.jmeno = text.toString()
                item.smazane = (item.jmeno == "")
                if (!items.any { it.jmeno == "" }) {
                    items.add(Person())
                    notifyItemInserted(items.size - 1)
                }
                etName.requestFocus()
            }

            etPerNum.doOnTextChanged{ text, _, _, _ ->
                item.rodne = text.toString()
                val str = text.toString().replace("/", "")
                val num = str.toIntOrNull()
                if (num != null) {
                    if (str.length >= 2) {
                        val yearEnds = str.substring(0,2).toInt()
                        val yearStarts = if (yearEnds >= 54) 19 else 20
                        val year = "$yearStarts" + str.substring(0,2)
                        etYear.setText(year)
                    }
                    if (str.length >= 4) {
                        var month = str.substring(2,4).toInt()
                        if (month > 50) month -= 50
                        etMonth.setText("$month")
                    }
                    if (str.length >= 6) {
                        val day = str.substring(4,6).toInt()
                        etDay.setText("$day")
                    }
                }
                etPerNum.requestFocus()
            }

            etDay.doOnTextChanged{ _, _, _, _ ->
                val date = completeDate(etDay, etMonth, etYear)
                if (date != null) item.narozeni = date
            }
            etMonth.doOnTextChanged{ _, _, _, _ ->
                val date = completeDate(etDay, etMonth, etYear)
                if (date != null) item.narozeni = date
            }
            etYear.doOnTextChanged{ _, _, _, _ ->
                val date = completeDate(etDay, etMonth, etYear)
                if (date != null) item.narozeni = date
            }
            etYears.doOnTextChanged{ text, _, _, _ ->
                val years = text.toString().toIntOrNull()
                if (years != null) etYear.setText((LocalDate.now().year - years).toString())
                val date = completeDate(etDay, etMonth, etYear)
                if (date != null) item.narozeni = date
            }

            etDayD.doOnTextChanged{ _, _, _, _ ->
                val date = completeDate(etDayD, etMonthD, etYearD)
                if (deadBeforeBorn(item.narozeni, date, etYearD)) item.umrti = date
            }
            etMonthD.doOnTextChanged{ _, _, _, _ ->
                val date = completeDate(etDayD, etMonthD, etYearD)
                if (deadBeforeBorn(item.narozeni, date, etYearD)) item.umrti = date
            }
            etYearD.doOnTextChanged{ _, _, _, _ ->
                val date = completeDate(etDayD, etMonthD, etYearD)
                if (deadBeforeBorn(item.narozeni, date, etYearD)) item.umrti = date
            }
            etYearsD.doOnTextChanged{ text, _, _, _ ->
                val years = text.toString().toIntOrNull()
                item.narozeni?.let { born ->
                    if (years != null) {
                        val bornPLUSyears = born.year + (years ?:0)
                        etYearD.setText((bornPLUSyears).toString())
                    }
                }
                val date = completeDate(etDayD, etMonthD, etYearD)
                try {
                    if (deadBeforeBorn(item.narozeni, date, etYearD)) item.umrti = date
                } catch (e: Exception) {
                    //chyba BugSnag
                }
            }
        }
    }

    private fun deadBeforeBorn(narozeni: LocalDate?, umrti: LocalDate?, etYear: EditText): Boolean {
        return if (umrti != null && narozeni != null && umrti < narozeni) {
            etYear.error = activity?.resources?.getString(R.string.err_dead)
            false
        } else {
            etYear.error = null
            true
        }
    }

    private fun completeDate(etDay: EditText, etMonth: EditText, etYear: EditText ): LocalDate? {
        val day = etDay.text.toString().toIntOrNull()
        val month = etMonth.text.toString().toIntOrNull()
        val year = etYear.text.toString().toIntOrNull()
        if (day != null && month != null && year != null) {
            if (day in 1..31) etDay.error = null else etDay.error =  activity?.resources?.getString(R.string.err_day)
            if (month in 1..12) etMonth.error = null else etMonth.error =  activity?.resources?.getString(R.string.err_month)
            if (year in 1111..LocalDate.now().year) etYear.error = null else etYear.error =  activity?.resources?.getString(R.string.err_year)
            if (etDay.error != null || etMonth.error != null || etYear.error != null) return null
            try {
                etDay.error = null
                return LocalDate.of(year, month, day)
            } catch (e: Exception) {
                etDay.error =  activity?.resources?.getString(R.string.err_date)
            }
        }
        return null
    }

    override fun getItemCount(): Int = items.size

    private fun getItem(index: Int): Person = items[index]
}
