package cz.pyramidak.kalendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import cz.pyramidak.kalendar.databinding.FragmentEditBinding
import cz.pyramidak.kalendar.room.RoomViewModel
import java.time.LocalDateTime

class EditFragment : Fragment() {

    private var _binding: FragmentEditBinding? = null
    private val binding
        get() = _binding!!
    private val activityMain: MainActivity
        get() = (activity as MainActivity)
    private val app: MyApplication
        get() = (activityMain.application as MyApplication)
    private val roomViewModel: RoomViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //ListViews update
        binding.rvEdit.layoutManager = GridLayoutManager(activity, activityMain.screenOrientation())
        binding.rvEdit.addOnScrollListener(CustomScrollListener(this.view))
        roomViewModel.onDayChanged = { _, newValue -> load(newValue) } //any other update
        load(roomViewModel.daySelected)

        activityMain.showDialog(activityMain.getString(R.string.help) , activityMain.getString(R.string.help_edit) , "help_edit")
    }

    override fun onPause() {
        if (binding.rvEdit.adapter != null) {
            val day = roomViewModel.daySelected
            if (day == null) {
                val adapter = binding.rvEdit.adapter as PersonAdapter
                adapter.items.forEach { roomViewModel.update( it ) }
            } else {
                if (day.edit == Tables.PERSON) {
                    val adapter = binding.rvEdit.adapter as PersonAdapter
                    adapter.items.forEach { roomViewModel.update( it ) }
                } else if (day.edit == Tables.PLAN) {
                    val adapter = binding.rvEdit.adapter as PlanAdapter
                    adapter.items.forEach { roomViewModel.update( it ) }
                }
            }
        }
        super.onPause()
    }

    private fun load(day: Day?) {
        if (day != null) {
            if (day.edit == Tables.PERSON) {
                binding.tvHeader.text = context?.getString(R.string.edit_person) ?:"Editace osob"
                if (!day.persons.any { it.jmeno == "" }) day.persons.add(Person())
                binding.rvEdit.adapter = PersonAdapter(day.persons, this)
            } else if (day.edit == Tables.PLAN) {
                val ep = context?.getString(R.string.edit_plan) ?:"Editace plánu"
                binding.tvHeader.text = context?.getString(R.string.text_join, ep, day.date.csString())
                if (!day.plans.any { it.text == "" }) day.plans.add(Plan(LocalDateTime.of(day.date.year,day.date.monthValue,day.date.dayOfMonth, 0,0)))
                binding.rvEdit.adapter = PlanAdapter(day.plans, this)
            }
        } else {
            binding.tvHeader.text = context?.getString(R.string.edit_person) ?:"Editace osob"
            binding.rvEdit.adapter = PersonAdapter(mutableListOf(Person()), this)
        }
    }
}