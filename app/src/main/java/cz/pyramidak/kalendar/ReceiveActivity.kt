package cz.pyramidak.kalendar

import android.os.Bundle
import android.widget.CompoundButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import cz.pyramidak.kalendar.databinding.ActivityReceiveBinding
import cz.pyramidak.kalendar.firebase.*
import cz.pyramidak.kalendar.room.RoomViewModel
import cz.pyramidak.kalendar.room.RoomViewModelFactory

class ReceiveActivity : AppCompatActivity() {

    lateinit var binding: ActivityReceiveBinding
    private val app: MyApplication
        get() = (application as MyApplication)
    private val roomViewModel: RoomViewModel by viewModels { RoomViewModelFactory(app.repository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceiveBinding.inflate(layoutInflater)
        setContentView(binding.root)

        app.finishedReceiveActivity = false
        // Data connection
        app.databaseOpen()
        roomViewModel.personsLive.observe(this ,  {})
        roomViewModel.plansLive.observe(this ,  {})

        binding.rvMessage.layoutManager = GridLayoutManager(this, this.screenOrientation())
        val users = MyFirebase("kalendar/users")
        users.refWithUser.child("odbery").get().addOnSuccessListener { snapshotOdber ->
            val founds = mutableListOf<Found>()
            snapshotOdber.recordsOdber().forEach { odber ->
                val odbery = MyFirebase("kalendar/odbery")
                odbery.ref.child(odber.key!!).child("messages").get().addOnSuccessListener { snapshotMessage ->
                    val messages = snapshotMessage.recordsMessage()
                    messages.filter { it.sender != odbery.userUid }.forEach { msg ->
                        msg.sender = odber.name
                        founds.add(Found(msg))
                    }
                    binding.rvMessage.adapter = MessageAdapter(founds)
                    var text = getString(R.string.person_plan)
                    val person = founds.any { it.icon == R.drawable.ic_person }
                    val note = founds.any { it.icon == R.drawable.ic_note }
                    if (person && !note) text = getString(R.string.persons)
                    if (!person && note) text = getString(R.string.plans)
                    binding.tvName.text = text
                }
            }
        }

        binding.cbAll.setOnCheckedChangeListener{ _: CompoundButton, checked: Boolean ->
            if (binding.rvMessage.adapter != null) {
                val adapter = binding.rvMessage.adapter as MessageAdapter
                adapter.items.forEach { it.selected = checked }
                binding.rvMessage.adapter = MessageAdapter(adapter.items, this)
            }
        }

        binding.btnReject.setOnClickListener{
            deleteMessages()
        }
        binding.btnReceive.setOnClickListener{
            if (binding.rvMessage.adapter == null) return@setOnClickListener
            val adapter = binding.rvMessage.adapter as MessageAdapter
            var founds = adapter.items.filter { it.selected }.toList()
            if (founds.isEmpty()) {
                showMessage(getString(R.string.selectOneMessage))
                return@setOnClickListener
            }
            founds.forEach {
                if (it.icon == R.drawable.ic_person) {
                    roomViewModel.update(it.person!!)
                } else {
                    roomViewModel.update(it.plan!!)
                }
            }
            if (founds.isNotEmpty()) {
                app.kalendar.dateNow = founds[0].date
            }
            founds = adapter.items.filter { !it.selected }.toList()
            binding.rvMessage.adapter = MessageAdapter(founds, this)
            if (founds.isEmpty()) { deleteMessages() }
        }
    }

    private fun deleteMessages() {
        val users = MyFirebase("kalendar/users")
        users.updateLastWrite()
        users.refWithUser.child("odbery").get().addOnSuccessListener { snapshotOdber ->
            snapshotOdber.recordsOdber().forEach { odber ->
                val odbery = MyFirebase("kalendar/odbery")
                odbery.ref.child(odber.key!!).child("messages").get().addOnSuccessListener { snapshotMessage ->
                    val messages = snapshotMessage.recordsMessage()
                    messages.filter { it.sender != odbery.userUid }.forEach { msg ->
                        //delete loaded message
                        odbery.ref.child(odber.key!!).child("messages").child(msg.key!!).removeValue()
                    }
                    app.finishedReceiveActivity = true
                    finish()
                }
            }
        }
    }

    private fun showMessage(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}