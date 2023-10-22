package cz.pyramidak.kalendar

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cz.pyramidak.kalendar.databinding.FragmentSearchBinding
import cz.pyramidak.kalendar.firebase.MyFirebase
import cz.pyramidak.kalendar.room.RoomViewModel
import java.time.LocalDate

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    val binding
        get() = _binding!!
    val activityMain: MainActivity
        get() = (activity as MainActivity)
    private val app: MyApplication
        get() = (activityMain.application as MyApplication)
    private val roomViewModel: RoomViewModel by activityViewModels()
    private  var adapter = SearchAdapter( listOf() )
    private var updown: Boolean = false
    private var foundPersons: MutableList<Found> = mutableListOf()
    private var foundPlans: MutableList<Found> = mutableListOf()
    var columnName: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        roomViewModel.personsLive.observe(viewLifecycleOwner) { records ->
            records?.let { persons ->
                foundPersons.clear()
                persons.filter { !it.smazane }.forEach { foundPersons.add(Found(it)) }
                createAdapter()
            }
        }
        roomViewModel.plansLive.observe(viewLifecycleOwner) { records ->
            records?.let { plans ->
                foundPlans.clear()
                plans.filter { it.text != "" }.forEach { foundPlans.add(Found(it)) }
                createAdapter()
            }
        }
        binding.rvSearch.addOnScrollListener(CustomScrollListener(this.view))
        //Filter
        binding.searchHra.findViewById<ImageView>(R.id.search_mag_icon).setColorFilter(Color.WHITE)
        binding.searchHra.findViewById<ImageView>(R.id.search_close_btn).setColorFilter(Color.WHITE)
        binding.searchHra.findViewById<TextView>(R.id.search_src_text).setTextColor(Color.WHITE)
        binding.searchHra.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                view.keyboardHide()
                return false
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter.filter(newText)
                return false
            }
        })
        binding.searchHra.findViewById<ImageView>(R.id.search_mag_icon).setOnClickListener{
            adapter.filterStartWith = !adapter.filterStartWith
            binding.searchHra.findViewById<ImageView>(R.id.search_mag_icon)
                .setImageResource(if (adapter.filterStartWith) R.drawable.ic_lupa_2 else R.drawable.ic_lupa_1)
        }

        columnName = context?.getString(R.string.person_plan) ?:"Osoby a plány"
        //Sort
        binding.tvName.setOnClickListener {
            val direction = adapter.sort("name")
            val nameTV = (if (direction) ">" else "<") + columnName
            binding.tvName.text = nameTV
            binding.tvDate.text = context?.getString(R.string.date) ?:"Datum"
        }
        binding.tvDate.setOnClickListener {
            val direction = adapter.sort("date")
            binding.tvDate.text = (if (direction) ">" else "<") + context?.getString(R.string.date) ?:"Datum"
            binding.tvName.text = columnName
        }

        //Filter
        binding.ivUp.setOnClickListener {
            binding.rvSearch.scrollToPosition(if (updown) adapter.itemCount - 1 else 0)
            binding.ivUp.setImageResource(if (updown) R.drawable.ic_up else R.drawable.ic_down)
            updown = !updown
        }

        //Up and down
        binding.rbPersons.setOnClickListener {
            columnName = context?.getString(R.string.persons) ?:"Narozeniny"
            binding.tvName.text = columnName
            binding.rbPersons.isChecked = true
            binding.rbPlans.isChecked = false
            adapter.filter(Tables.PERSON)
        }
        binding.rbPlans.setOnClickListener {
            columnName = context?.getString(R.string.plans) ?:"PLány"
            binding.tvName.text = columnName
            binding.rbPersons.isChecked = false
            binding.rbPlans.isChecked = true
            adapter.filter(Tables.PLAN)
        }

        //Send messages
        binding.ivSend.setOnClickListener{
            if (adapter.itemsFiltered.size != 0) {
                val database = MyFirebase("kalendar/users")
                if (!database.userSigned || !database.userVerified) {
                    database.askToSignIn(this)
                } else {
                    app.messages = adapter.itemsFiltered
                    val intent = Intent(context, SendActivity::class.java)
                    startActivity(intent)
                }
            }
        }
    }

    fun createAdapter() {
        val founds = mutableListOf<Found>()
        founds.addAll(foundPersons)
        founds.addAll(foundPlans)
        adapter = SearchAdapter(founds,this)
        val layoutManager = LinearLayoutManager(activity)
        layoutManager.stackFromEnd = true
        binding.rvSearch.layoutManager = GridLayoutManager(activity, if (activityMain.screenSize() == 3 && activityMain.screenOrientation() == 2) 2 else 1)
        binding.rvSearch.adapter = adapter
        binding.searchHra.findViewById<ImageView>(R.id.search_mag_icon).setImageResource(R.drawable.ic_lupa_1)
    }

    override fun onResume() {
        super.onResume()
        binding.searchHra.requestFocus()
        if (adapter.items.isNotEmpty()) {
            activityMain.showDialog(activityMain.getString(R.string.help), activityMain.getString(R.string.help_find), "help_find")
        }
    }

    override fun onStop() {
        super.onStop()
        binding.rbPersons.isChecked = true
        binding.rbPlans.isChecked = true
    }
}