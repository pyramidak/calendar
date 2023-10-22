package cz.pyramidak.kalendar

import android.os.Bundle
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import cz.pyramidak.kalendar.databinding.ActivitySendBinding
import cz.pyramidak.kalendar.firebase.Message
import cz.pyramidak.kalendar.firebase.MyFirebase
import cz.pyramidak.kalendar.firebase.recordsOdber

class SendActivity : AppCompatActivity() {

    lateinit var binding: ActivitySendBinding
    private val app: MyApplication
        get() = (application as MyApplication)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var text = this.getString(R.string.person_plan)
        val person = app.messages.any { it.icon == R.drawable.ic_person }
        val note = app.messages.any { it.icon == R.drawable.ic_note }
        if (person && !note) text = this.getString(R.string.persons)
        if (!person && note) text = this.getString(R.string.plans)
        binding.tvName.text = text

        binding.rvRecipient.layoutManager = GridLayoutManager(this, this.screenOrientation() * this.screenSize())
        val eventListener = object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val odbery = dataSnapshot.recordsOdber()
                binding.rvRecipient.adapter = RecipientAdapter(odbery)
            }
            override fun onCancelled(error: DatabaseError) {
                binding.rvRecipient.adapter = RecipientAdapter(listOf())
            }
        }
        val users = MyFirebase("kalendar/users")
        users.refWithUser.child("odbery").addValueEventListener(eventListener as ValueEventListener)

        binding.rvMessage.layoutManager = GridLayoutManager(this, this.screenOrientation())
        binding.rvMessage.adapter = MessageAdapter(app.messages, this)

        binding.cbAll.setOnCheckedChangeListener{ _: CompoundButton, checked: Boolean ->
            app.messages.forEach { it.selected = checked }
            binding.rvMessage.adapter = MessageAdapter(app.messages, this)
        }

        binding.btnCancel.setOnClickListener{
            finish()
        }
        binding.btnSend.setOnClickListener{
            if (binding.rvRecipient.adapter == null) return@setOnClickListener
            val messageAdapter = binding.rvMessage.adapter as MessageAdapter
            val recipientAdapter = binding.rvRecipient.adapter as RecipientAdapter
            val founds = messageAdapter.items.filter { it.selected }.toList()
            if (founds.isEmpty()) {
                showMessage(getString(R.string.selectOneMessage))
                return@setOnClickListener
            }
            val recipients = recipientAdapter.getSelectedItems()
            if (recipients.isEmpty()) {
                showMessage(getString(R.string.selectOneRecipient))
                return@setOnClickListener
            }
            val messages = mutableListOf<Message>()
            founds.forEach { found ->
                found.person?.let { messages.add(Message(it)) }
                found.plan?.let { messages.add(Message(it)) }
            }
            val odbery = MyFirebase("kalendar/odbery")
            odbery.updateLastWrite()
            recipients.forEach { recipient ->
                messages.forEach { message ->
                    message.sender = odbery.userUid
                    odbery.ref.child(recipient.key!!).child("messages").push().setValue(message)
                }
            }
            finish()
        }
    }

    private fun showMessage(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}