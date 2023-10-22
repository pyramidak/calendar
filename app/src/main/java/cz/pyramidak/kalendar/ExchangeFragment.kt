package cz.pyramidak.kalendar

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import cz.pyramidak.kalendar.databinding.FragmentExchangeBinding
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.progressindicator.BaseProgressIndicator
import cz.pyramidak.kalendar.firebase.*
import kotlinx.coroutines.*


class ExchangeFragment : Fragment() {

    private var _binding: FragmentExchangeBinding? = null
    private val binding
        get() = _binding!!
    private val activityMain: MainActivity
        get() = (activity as MainActivity)
    private var eventListener: ValueEventListener? = null
    private val app: MyApplication
        get() = (activityMain.application as MyApplication)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExchangeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnGenerate.setOnClickListener {
            newOdber = Odber()
            newOdber.own = true
            inputBox(activityMain.getString(R.string.personNameToConnect))
        }
        binding.btnReceived.setOnClickListener {
            newOdber = Odber()
            inputBox(activityMain.getString(R.string.receivedKeyToConnect))
        }
        binding.rvOdber.layoutManager = GridLayoutManager(activity, activityMain.screenOrientation())

        activityMain.showDialog(activityMain.getString(R.string.help), activityMain.getString(R.string.help_link), "help_link")

        app.onScannerFinished = { _, newValue ->
            if (newValue && app.codeQR != "" ) {
                val database = MyFirebase("kalendar/users")
                if (database.checkCharacters(app.codeQR)) {
                    newOdber.key = app.codeQR
                    inputBox(activityMain.getString(R.string.personNameOfConnection))
                }
            }
        }

    }

    override fun onResume() {
        super.onResume()
        checkUserSignIn()
    }

    override fun onPause() {
        super.onPause()
        eventListener?.let {  MyFirebase("kalendar/users").refWithUser.child("odbery").removeEventListener(it) }
    }

    private fun checkUserSignIn() {
        val database = MyFirebase("kalendar/users")
        if (!database.userSigned || !database.userVerified ) {
            eventListener?.let { database.refWithUser.child("odbery").removeEventListener(it) }
            binding.btnGenerate.isEnabled = false
            binding.btnReceived.isEnabled = false
            binding.rvOdber.isEnabled = false
            database.askToSignIn(this)
        } else {
            binding.btnGenerate.isEnabled = true
            binding.btnReceived.isEnabled = true
            binding.rvOdber.isEnabled = true
            eventListener = object: ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val odbery = dataSnapshot.recordsOdber()
                    binding.rvOdber.adapter = OdberAdapter(odbery, activityMain)
                }
                override fun onCancelled(error: DatabaseError) {
                    binding.rvOdber.adapter = OdberAdapter(listOf(), activityMain)
                    eventListener?.let { database.refWithUser.child("odbery").removeEventListener(it) }
                }
            }
            database.refWithUser.child("odbery").addValueEventListener(eventListener as ValueEventListener)
        }
    }

    private var newOdber = Odber()
    private fun inputBox(header: String) {
        val builder = AlertDialog.Builder(activityMain)
        builder.setTitle(header)
        val input = EditText(activityMain)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.textAlignment = View.TEXT_ALIGNMENT_CENTER
        input.requestFocus()
        builder.setView(input)
        builder.setPositiveButton(getString(R.string.ok)) { _, _ ->
            if (getString(R.string.personNameToConnect) == header) {
                newOdber.name = input.text.toString()
                if (newOdber.name == "") return@setPositiveButton

                val users = MyFirebase("kalendar/users")
                users.updateLastWrite()
                val key = users.refWithUser.child("odbery").push().key
                if (key != null) {
                    users.refWithUser.child("odbery").child(key).setValue(newOdber) //nový záznam odběru v uživateli
                    val extra = Extra()
                    extra.connected = false
                    MyFirebase("kalendar/odbery").ref.child(key).child("state").setValue(extra) //nový odběr vytvořen pro protějšek se stavem, že ještě nejsou propojeni
                }

            } else if (getString(R.string.receivedKeyToConnect) == header) {
                newOdber.key = input.text.toString()
                if (newOdber.key == "") return@setPositiveButton
                inputBox(getString(R.string.personNameOfConnection))

            } else if (getString(R.string.personNameOfConnection) == header) {
                newOdber.name = input.text.toString()
                if (newOdber.name == "") return@setPositiveButton

                val users = MyFirebase("kalendar/users")
                users.updateLastWrite()
                val usersOdber = users.refWithUser.child("odbery").child(newOdber.key!!).ref
                usersOdber.get().addOnSuccessListener { userOdberSnapshot ->
                    if (userOdberSnapshot.value == null) {
                        usersOdber.setValue(newOdber) //nový záznam odběru v uživateli, aby získal přístup do zadaného odběru

                        val odbery = MyFirebase("kalendar/odbery").ref.child(newOdber.key!!).child("state")
                        odbery.get().addOnSuccessListener {
                            if (it.value == null) {
                                activityMain.showMessage(getString(R.string.keyDoesNotExist))
                                usersOdber.removeValue() //záznam odběru v uživateli smazán
                            } else {
                                val extra = it.getValue(Extra::class.java) ?: Extra()
                                if (extra.connected == true) {
                                    showDialog(activityMain.getString(R.string.connection_completed_no_add))
                                } else {
                                    extra.connected = true
                                    odbery.setValue(extra)  //změna stavu na propojeni získaného odběru
                                }
                            }
                        }.addOnFailureListener{
                            activityMain.showMessage(activityMain.getString(R.string.keyDoesNotExist))
                            usersOdber.removeValue() //záznam odběru v uživateli smazán
                        }
                    } else {
                        //val userOdber = userOdberSnapshot.getValue(Odber::class.java)
                        showMessage(getString(R.string.connection_exist))
                    }
                }
            }
        }
        if (getString(R.string.receivedKeyToConnect) == header) {
            builder.setNeutralButton(getString(R.string.qrcode)) { dialog, _ ->
                dialog.cancel()
                val intent = Intent(context, ScannerActivity::class.java)
                startActivity(intent)
            }
        }
        builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    //Alert Help
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    fun showDialog(message: String) {
        val builder = AlertDialog.Builder(activityMain)
        builder.setTitle(activityMain.getString(R.string.connection))
        builder.setMessage(message)
        builder.setPositiveButton(activityMain.getString(R.string.ok)){_, _ -> }
        builder.show()
    }

    fun showMessage(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    }
}