package cz.pyramidak.kalendar

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import cz.pyramidak.kalendar.databinding.FragmentMainBinding
import cz.pyramidak.kalendar.room.RoomViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields

class MainFragment: Fragment() {
    private var _binding: FragmentMainBinding? = null
    private val binding
        get() = _binding!!
    private val activityMain: MainActivity
        get() = (activity as MainActivity) ?: throw IllegalStateException("Fragment $this not attached to a context.")
    private val app: MyApplication
        get() = (activityMain.application as MyApplication)
    val kalendar: Kalendar
        get() = app.kalendar
    private val roomViewModel: RoomViewModel by activityViewModels()
    private val days: List<Day> = listOf(Day(), Day(), Day(), Day(), Day(), Day(), Day(), Day())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        view?.keyboardHide()
    }

    //ViewCreated
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (activityMain.screenOrientation() == 2 && activityMain.screenSize() != 3) {
            binding.layoutHeader.visibility = View.GONE
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0 , 1f)
            binding.layoutFooter.layoutParams = lp
        }

        //Zobrazit aktuální týden
        weekCalculation(kalendar.dateNow)
        kalendar.onDayChanged = { _, newValue ->
            weekCalculation(newValue)
        }
        //Data connection
        roomViewModel.personsLive.observe(viewLifecycleOwner) {
            liveBirthday(it)
        }
        roomViewModel.plansLive.observe(viewLifecycleOwner) {
            liveNotes(it)
        }

        //Touch
        binding.btnRok.setOnClickListener {
            kalendar.dateNow = kalendar.dateNow.plusYears(1)
        }
        binding.btnRok.setOnLongClickListener {
            kalendar.dateNow = kalendar.dateNow.minusYears(1)
            true
        }
        binding.btnTyden.setOnClickListener {
            kalendar.dateNow = kalendar.dateNow.plusWeeks(1)
        }
        binding.btnTyden.setOnLongClickListener {
            kalendar.dateNow = kalendar.dateNow.minusWeeks(1)
            true
        }
        binding.btnMesic.setOnClickListener {
            kalendar.dateNow = kalendar.dateNow.plusMonths(1)
        }
        binding.btnMesic.setOnLongClickListener {
            kalendar.dateNow = kalendar.dateNow.minusMonths(1)
            true
        }
        binding.btnDnes.setOnClickListener {
            kalendar.dateNow = LocalDate.now()
            //activityMain.checkPermissions()
        }

        //Click
        for (d in 1..7) {
            weekDayClick(d)
            weekNamedayClick(d)
            weekNoteClick(d)
            weekNoteLongClick(d)
            weekPersonClick(d)
            weekPersonLongClick(d)
        }

        activityMain.showDialog(activityMain.getString(R.string.help), activityMain.getString(R.string.help_main), "help_main")
    }

    @SuppressLint("SetTextI18n")
    fun weekCalculation(dateNow: LocalDate) {
        var dateFirst: LocalDate = dateNow
        var dateLast: LocalDate = dateNow
        for (i in 0..6) {
            dateFirst =  dateNow.minusDays(i.toLong())
            if (dateFirst.dayOfWeek.value == 1) break
        }
        for (i in 0..6) {
            dateLast =  dateNow.plusDays(i.toLong())
            if (dateLast.dayOfWeek.value == 7) break
        }

        //rok, x. týden, měsíc
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        val weekNumber: Int = dateNow.get(WeekFields.ISO.weekOfWeekBasedYear())
        binding.btnTyden.text = "$weekNumber. " + context?.getString(R.string.week) ?:"týden"

        if (dateFirst.monthValue != dateLast.monthValue) {
            binding.btnMesic.text = "${kalendar.getMesic(dateFirst.monthValue)} / ${kalendar.getMesic(dateLast.monthValue)}"
        } else {
            binding.btnMesic.text = kalendar.getMesic(dateFirst.monthValue)
        }

        if (dateFirst.year != dateLast.year) {
            binding.btnRok.text = "${dateFirst.year} / ${dateLast.year}"
        } else {
            binding.btnRok.text = "${dateNow.year}"
        }

        for (d in 1..7) {
            dateLast = dateFirst.plusDays(d.toLong()-1)
            days[d].date = dateLast
            //Dny
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            weekDay(d, dateLast.dayOfMonth.toString())
            weekDayColor(d, if (dateLast == dateNow) Color.YELLOW else Color.WHITE)
            weekDayName(d, kalendar.getDenJmeno(d))
            //Jmeniny
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            weekNameday(d, kalendar.getJmenaJoin(dateLast))
            weekNamedayColor(d, Color.BLACK)
            //Svátky
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            weekHoliday(d, kalendar.getSvatek(dateLast))
            weekImage(d, kalendar.getZavreno(dateLast))
        }
        //Narozeniny a Upomínky
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        liveBirthday(roomViewModel.personsLive.value)
        liveNotes(roomViewModel.plansLive.value)
    }
    //Narozeniny
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private fun liveBirthday(persons: List<Person>?) {
        if (persons == null) return
        for (d in 1..7) {
            days[d].personsOfBirthday.clear()
            days[d].personsOfBirthday.addAll(persons.filter { !it.smazane &&
                it.narozeni!!.monthValue == days[d].date.monthValue && it.narozeni!!.dayOfMonth == days[d].date.dayOfMonth })
            val life = days[d].personsOfBirthday.joinToString(separator = "\n") {
                "${it.jmeno} ${if (it.umrti == null) "" else "†${it.umrti!!.year - it.narozeni!!.year} ("}" +
                "${days[d].date.year - it.narozeni!!.year}${if (it.umrti == null) "" else ")"}" }
            val deads = persons.filter { !it.smazane && it.umrti != null }.filter {
                it.umrti!!.monthValue == days[d].date.monthValue && it.umrti!!.dayOfMonth == days[d].date.dayOfMonth }
            val dead = deads.joinToString(separator = "\n") { "† ${it.jmeno} †${it.umrti!!.year - it.narozeni!!.year}" }
            days[d].personsOfBirthday.addAll(deads)
            weekBirthday(d, life + (if (life == "") "" else "\n") + dead)

            //Stejné jméno v narozeninách podbarvit
            val jmena = kalendar.getJmena(days[d].date)
            for (one in jmena) {
                val osoby = persons.filter { it.jmeno.contains("$one ") }
                if (osoby.isNotEmpty()) {
                    days[d].personsOfNameday.clear()
                    days[d].personsOfNameday.addAll(osoby)
                    weekNamedayColor(d, Color.BLUE )
                }
            }
        }
    }
    //Upominky
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private fun liveNotes(plans: List<Plan>?) {
        if (plans == null) return
        for (d in 1..7) {
            days[d].plans.clear()
            days[d].plans.addAll( plans.filter { it.text != "" && !it.mesicne && !it.rocne && it.den.toLocalDate() == days[d].date } )
            days[d].plans.addAll( plans.filter { it.text != "" && it.mesicne && it.den.dayOfMonth == days[d].date.dayOfMonth && it.den.year == days[d].date.year } )
            days[d].plans.addAll( plans.filter { it.text != "" && it.rocne && it.den.dayOfMonth == days[d].date.dayOfMonth && it.den.monthValue == days[d].date.monthValue } )
            days[d].plans.addAll( plans.filter { it.text != "" && it.mesicne && it.rocne && it.den.dayOfMonth == days[d].date.dayOfMonth } )
            days[d].plans.sortBy { it.den }
            weekNote(d, days[d].plans.joinToString(separator = "; ") {
                "${if (it.den.hour == 0 && it.den.minute == 0) "" else it.den.format(DateTimeFormatter.ofPattern("HH:mm "))}${it.text}" })
        }
    }
    //Binding
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private fun weekDay(day: Int, text: String) {
        when (day) {
            1 -> binding.tvDen1.text = text
            2 -> binding.tvDen2.text = text
            3 -> binding.tvDen3.text = text
            4 -> binding.tvDen4.text = text
            5 -> binding.tvDen5.text = text
            6 -> binding.tvDen6.text = text
            else -> binding.tvDen7.text = text
        }
    }
    private fun weekDayColor(day: Int, color: Int) {
        when (day) {
            1 -> binding.tvDen1.setTextColor(color)
            2 -> binding.tvDen2.setTextColor(color)
            3 -> binding.tvDen3.setTextColor(color)
            4 -> binding.tvDen4.setTextColor(color)
            5 -> binding.tvDen5.setTextColor(color)
            6 -> binding.tvDen6.setTextColor(color)
            else -> binding.tvDen7.setTextColor(color)
        }
    }
    private fun weekDayName(day: Int, text: String) {
        when (day) {
            1 -> binding.tvDenTydne1.text = text
            2 -> binding.tvDenTydne2.text = text
            3 -> binding.tvDenTydne3.text = text
            4 -> binding.tvDenTydne4.text = text
            5 -> binding.tvDenTydne5.text = text
            6 -> binding.tvDenTydne6.text = text
            else -> binding.tvDenTydne7.text = text
        }
    }
    private fun weekNameday(day: Int, text: String) {
        when (day) {
            1 -> binding.tvJmeniny1.text = text
            2 -> binding.tvJmeniny2.text = text
            3 -> binding.tvJmeniny3.text = text
            4 -> binding.tvJmeniny4.text = text
            5 -> binding.tvJmeniny5.text = text
            6 -> binding.tvJmeniny6.text = text
            else -> binding.tvJmeniny7.text = text
        }
    }
    private fun weekNamedayColor(day: Int, color: Int) {
        when (day) {
            1 -> binding.tvJmeniny1.setTextColor(color)
            2 -> binding.tvJmeniny2.setTextColor(color)
            3 -> binding.tvJmeniny3.setTextColor(color)
            4 -> binding.tvJmeniny4.setTextColor(color)
            5 -> binding.tvJmeniny5.setTextColor(color)
            6 -> binding.tvJmeniny6.setTextColor(color)
            else -> binding.tvJmeniny7.setTextColor(color)
        }
    }
    private fun weekBirthday(day: Int, text: String) {
        when (day) {
            1 -> binding.tvNarozeniny1.text = text
            2 -> binding.tvNarozeniny2.text = text
            3 -> binding.tvNarozeniny3.text = text
            4 -> binding.tvNarozeniny4.text = text
            5 -> binding.tvNarozeniny5.text = text
            6 -> binding.tvNarozeniny6.text = text
            else -> binding.tvNarozeniny7.text = text
        }
    }
    private fun weekNote(day: Int, text: String) {
        when (day) {
            1 -> binding.tvUpominky1.text = text
            2 -> binding.tvUpominky2.text = text
            3 -> binding.tvUpominky3.text = text
            4 -> binding.tvUpominky4.text = text
            5 -> binding.tvUpominky5.text = text
            6 -> binding.tvUpominky6.text = text
            else -> binding.tvUpominky7.text = text
        }
    }
    private fun weekHoliday(day: Int, text: String) {
        when (day) {
            1 -> binding.tvSvatky1.text = text
            2 -> binding.tvSvatky2.text = text
            3 -> binding.tvSvatky3.text = text
            4 -> binding.tvSvatky4.text = text
            5 -> binding.tvSvatky5.text = text
            6 -> binding.tvSvatky6.text = text
            else -> binding.tvSvatky7.text = text
        }
    }
    private fun weekImage(day: Int, visibility: Int) {
        when (day) {
            1 -> binding.ivClose1.visibility = visibility
            2 -> binding.ivClose2.visibility = visibility
            3 -> binding.ivClose3.visibility = visibility
            4 -> binding.ivClose4.visibility = visibility
            5 -> binding.ivClose5.visibility = visibility
            6 -> binding.ivClose6.visibility = visibility
            else -> binding.ivClose7.visibility = visibility
        }
    }
    //Listeners
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private fun weekDayClick(day: Int) {
        when (day) {
            1 -> binding.tvDen1.setOnClickListener { weekDayScript(day) }
            2 -> binding.tvDen2.setOnClickListener { weekDayScript(day) }
            3 -> binding.tvDen3.setOnClickListener { weekDayScript(day) }
            4 -> binding.tvDen4.setOnClickListener { weekDayScript(day) }
            5 -> binding.tvDen5.setOnClickListener { weekDayScript(day) }
            6 -> binding.tvDen6.setOnClickListener { weekDayScript(day) }
            else -> binding.tvDen7.setOnClickListener { weekDayScript(day) }
        }
    }
    private fun weekDayScript(day: Int) {
        kalendar.dateNow = days[day].date
    }
    private fun weekNamedayClick(day: Int) {
        when (day) {
            1 -> binding.tvJmeniny1.setOnClickListener { weekNamedayScript(day) }
            2 -> binding.tvJmeniny2.setOnClickListener { weekNamedayScript(day) }
            3 -> binding.tvJmeniny3.setOnClickListener { weekNamedayScript(day) }
            4 -> binding.tvJmeniny4.setOnClickListener { weekNamedayScript(day) }
            5 -> binding.tvJmeniny5.setOnClickListener { weekNamedayScript(day) }
            6 -> binding.tvJmeniny6.setOnClickListener { weekNamedayScript(day) }
            else -> binding.tvJmeniny7.setOnClickListener { weekNamedayScript(day) }
        }
    }
    private fun weekNamedayScript(day: Int) {
        if (days[day].personsOfNameday.isNotEmpty()) {
            days[day].edit = Tables.PERSON
            days[day].persons = days[day].personsOfNameday
            roomViewModel.daySelected = days[day]
            activityMain.binding.pager.currentItem = 3
        }
    }
    private fun weekPersonClick(day: Int) {
        when (day) {
            1 -> binding.tvNarozeniny1.setOnClickListener { weekPersonScript(day) }
            2 -> binding.tvNarozeniny2.setOnClickListener { weekPersonScript(day) }
            3 -> binding.tvNarozeniny3.setOnClickListener { weekPersonScript(day) }
            4 -> binding.tvNarozeniny4.setOnClickListener { weekPersonScript(day) }
            5 -> binding.tvNarozeniny5.setOnClickListener { weekPersonScript(day) }
            6 -> binding.tvNarozeniny6.setOnClickListener { weekPersonScript(day) }
            else -> binding.tvNarozeniny7.setOnClickListener { weekPersonScript(day) }
        }
    }
    private fun weekPersonScript(day: Int) {
        days[day].edit = Tables.PERSON
        days[day].persons = days[day].personsOfBirthday
        roomViewModel.daySelected = days[day]
        activityMain.binding.pager.currentItem = 3
    }
    private fun weekPersonLongClick(day: Int) {
        when (day) {
            1 -> binding.tvNarozeniny1.setOnLongClickListener { weekMessageScript(day, Tables.PERSON)
                false }
            2 -> binding.tvNarozeniny2.setOnLongClickListener { weekMessageScript(day, Tables.PERSON)
                false }
            3 -> binding.tvNarozeniny3.setOnLongClickListener { weekMessageScript(day, Tables.PERSON)
                false }
            4 -> binding.tvNarozeniny4.setOnLongClickListener { weekMessageScript(day, Tables.PERSON)
                false }
            5 -> binding.tvNarozeniny5.setOnLongClickListener { weekMessageScript(day, Tables.PERSON)
                false }
            6 -> binding.tvNarozeniny6.setOnLongClickListener { weekMessageScript(day, Tables.PERSON)
                false }
            else -> binding.tvNarozeniny7.setOnLongClickListener { weekMessageScript(day, Tables.PERSON)
                false }
        }
    }
    private fun weekNoteClick(day: Int) {
        when (day) {
            1 -> binding.tvUpominky1.setOnClickListener { weekNoteScript(day) }
            2 -> binding.tvUpominky2.setOnClickListener { weekNoteScript(day) }
            3 -> binding.tvUpominky3.setOnClickListener { weekNoteScript(day) }
            4 -> binding.tvUpominky4.setOnClickListener { weekNoteScript(day) }
            5 -> binding.tvUpominky5.setOnClickListener { weekNoteScript(day) }
            6 -> binding.tvUpominky6.setOnClickListener { weekNoteScript(day) }
            else -> binding.tvUpominky7.setOnClickListener { weekNoteScript(day) }
        }
    }
    private fun weekNoteScript(day: Int) {
        days[day].edit = Tables.PLAN
        roomViewModel.daySelected = days[day]
        activityMain.binding.pager.currentItem = 3
    }
    private fun weekNoteLongClick(day: Int) {
        when (day) {
            1 -> binding.tvUpominky1.setOnLongClickListener { weekMessageScript(day, Tables.PLAN)
                false }
            2 -> binding.tvUpominky2.setOnLongClickListener { weekMessageScript(day, Tables.PLAN)
                false }
            3 -> binding.tvUpominky3.setOnLongClickListener { weekMessageScript(day, Tables.PLAN)
                false }
            4 -> binding.tvUpominky4.setOnLongClickListener { weekMessageScript(day, Tables.PLAN)
                false }
            5 -> binding.tvUpominky5.setOnLongClickListener { weekMessageScript(day, Tables.PLAN)
                false }
            6 -> binding.tvUpominky6.setOnLongClickListener { weekMessageScript(day, Tables.PLAN)
                false }
            else -> binding.tvUpominky7.setOnLongClickListener { weekMessageScript(day, Tables.PLAN)
                false}
        }
    }
    private fun weekMessageScript(day: Int, table: Tables) {
        app.messages.clear()
        if (table == Tables.PERSON) {
            if (days[day].personsOfBirthday.isEmpty()) return
            for (one in days[day].personsOfBirthday.filter { it.jmeno != "" && it.narozeni != null }) {
                app.messages.add(Found(one))
            }
        } else {
            if (days[day].plans.isEmpty()) return
            for (one in days[day].plans.filter { it.text != "" }) {
                app.messages.add(Found(one))
            }
        }
        val intent = Intent(context, SendActivity::class.java)
        startActivity(intent)
    }
}

