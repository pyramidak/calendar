package cz.pyramidak.kalendar.firebase

import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import cz.pyramidak.kalendar.*
import cz.pyramidak.kalendar.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import java.time.LocalDateTime

// DataSnapshot - do not change:
inline fun <reified T> DataSnapshot.records(): List<T> {
    if (!this.exists()) return listOf()
    val records = mutableListOf<T>()
    if (this.children.count() == 0) {
        records.add(this.getValue(T::class.java)!!)
    } else {
        for (one in this.children) {
            records.add(one.getValue(T::class.java)!!)
        }
    }
    return records
}
inline fun <reified T> DataSnapshot.record(): T? {
    return this.getValue(T::class.java)
}
// DataSnapshot
fun DataSnapshot.recordsOdber(): List<Odber> {
    if (!this.exists()) return listOf()
    val records = mutableListOf<Odber>()
    if (this.children.count() == 0) {
        records.add(this.getValue(Odber::class.java)!!)
    } else {
        for (one in this.children) {
            val record = one.getValue(Odber::class.java)!!
            record.key = one.key
            records.add(record)
        }
    }
    return records
}
// DataSnapshot
fun DataSnapshot.recordsMessage(): List<Message> {
    if (!this.exists()) return listOf()
    val records = mutableListOf<Message>()
    if (this.children.count() == 0) {
        records.add(this.getValue(Message::class.java)!!)
    } else {
        for (one in this.children) {
            val record = one.getValue(Message::class.java)!!
            record.key = one.key
            records.add(record)
        }
    }
    return records
}

// Table
data class Extra(
    var firstWrite: String? = null,
    var lastWrite: String? = null,
    var lastAccess: String? = null,
    var fullWrite: String? = null,
    var full: Boolean? = null,
    var email: String? = null,
    var createdBy: String? = null,
    var connected: Boolean? = null)
{
//  Firebase musí mít druhý constructor níže nebo v prvním výše všechny hodnoty výchozí hodnotu
//  constructor() : this(0F, null,0)
}

// Table
@IgnoreExtraProperties
data class Odber(
    var name: String = "",
    var own: Boolean = false,)
{
    @get:Exclude var key: String? = null
}

// Table
@IgnoreExtraProperties
data class Message(
    var jmeno: String? = null,
    var narozeni: String? = null,
    var umrti: String? = null,
    var den: String? = null,
    var text: String? = null,
    var mesicne: Boolean? = null,
    var rocne: Boolean? = null,
    var sender: String? = null)
{
    @get:Exclude var key: String? = null

    constructor(person: Person): this() {
        jmeno = person.jmeno
        narozeni = person.narozeni.dbString()
        umrti = if (person.umrti == null) null else person.umrti.dbString()
    }
    constructor(plan: Plan): this() {
        den = plan.den.dbString()
        text = plan.text
        mesicne = plan.mesicne
        rocne = plan.rocne
    }
}

// Database
class MyFirebase(val table: String) {
    private val user
        get() = Firebase.auth.currentUser
    val userEmail
        get() = user?.email ?: ""
    val userVerified
        get() = user?.isEmailVerified ?: false
    val userSigned
        get() = user != null
    private val refBase =
        Firebase.database("https://stolni-kalendar-default-rtdb.europe-west1.firebasedatabase.app").reference
    val ref
        get() = refBase.child(table).ref
    val refWithUser
        get() = refBase.child(table).child(userUid).ref
    val userUid
        get() = if (userSigned) user!!.uid else ""
    val refConfig = refBase.child("kalendar/users/${userUid}/config").ref
    val refAppConfig = refBase.child("kalendar/config").ref

    fun checkCharacters(path: String): Boolean {
        return (!path.contains(".") && !path.contains("$") && !path.contains("#") && !path.contains("[") && !path.contains("]"))
    }

    fun updateLastAccess() {
        refConfig.get().addOnSuccessListener {
            val extra = it.getValue(Extra::class.java) ?: Extra()
            user?.email?.let { email -> if (extra.email == null) extra.email = email }
            if (extra.firstWrite == null) extra.firstWrite = LocalDateTime.now().dbString()
            if (extra.full == null) extra.full = false
            if (extra.fullWrite == null && extra.full!!) extra.fullWrite = LocalDateTime.now().dbString()
            extra.lastAccess = LocalDateTime.now().dbString()
            refConfig.setValue(extra) //datum prvního zápisu a posledního přístupu
        }
    }

    fun updateLastWrite() {
        refConfig.get().addOnSuccessListener {
            val extra = it.getValue(Extra::class.java) ?: Extra()
            extra.lastWrite = LocalDateTime.now().dbString()
            refConfig.setValue(extra) //datum posledního zápisu
        }
    }

    fun askToSignIn(frag: Fragment) {
        val database = MyFirebase("kalendar")
        val userSigned = database.userSigned //app.database.userSigned
        val builder = AlertDialog.Builder(frag.requireContext())
        builder.setTitle(
            frag.getString(R.string.alert_sign_00) + " " + (if (!userSigned) frag.getString(R.string.alert_sign_01) else frag.getString(
                R.string.alert_sign_02
            )) + " " + frag.getString(R.string.alert_sign_03)
        )
        builder.setMessage(
            frag.getString(R.string.alert_sign_10) + " " + (if (!userSigned) frag.getString(R.string.alert_sign_11) else frag.getString(
                R.string.alert_sign_12
            )) + " " + frag.getString(R.string.alert_sign_20)
        )
        builder.setPositiveButton(frag.getString(R.string.yes)) { _, _ ->
            val intent = Intent(frag.context, AuthActivity::class.java)
            intent.putExtra("FRAGMENT", "firebase")
            frag.startActivity(intent)
        }
        builder.setNegativeButton(frag.getString(R.string.no)) { _, _ -> }
        builder.show()
    }
}

// ViewModel
class ZaznamViewModel(private val database: MyFirebase) : ViewModel()
{
    private var _recordsLive = MutableLiveData<List<Odber>>()
    val recordsLive: LiveData<List<Odber>>
        get() = _recordsLive
    private var eventListener: ValueEventListener? = null

    fun updateConnection() {
        eventListener?.let { database.ref.removeEventListener(it) }
        if (database.userSigned && database.userVerified) {
            eventListener = object: ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    Log.d("GET", "Got table: ${database.table}")
                    _recordsLive.value = dataSnapshot.recordsOdber()
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.w("GET", "Got table failed.", error.toException())
                }
            }
            database.ref.addValueEventListener(eventListener as ValueEventListener)
        }
    }

    // Flow to List
    suspend fun <T> Flow<List<T>>.flattenToList() = flatMapConcat { it.asFlow() }.toList()

    fun insert(record: Odber) = viewModelScope.launch {
        database.ref.push().setValue(record)
    }
    fun delete(record: Odber) = viewModelScope.launch {
        record.key?.let { database.ref.child(it).removeValue() }
    }
    fun delete(key: String) = viewModelScope.launch {
        database.ref.child(key).removeValue()
    }
}

// Factory
class ZaznamViewModelFactory(private val database: MyFirebase) : ViewModelProvider.Factory
{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ZaznamViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ZaznamViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

