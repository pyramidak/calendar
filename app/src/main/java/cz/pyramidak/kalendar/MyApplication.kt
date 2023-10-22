package cz.pyramidak.kalendar

import android.app.Application
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.sqlite.db.SimpleSQLiteQuery
import com.bugsnag.android.Bugsnag
import cz.pyramidak.kalendar.firebase.MyFirebase
import cz.pyramidak.kalendar.room.MyRoom
import cz.pyramidak.kalendar.room.MySync
import cz.pyramidak.kalendar.room.RoomRepository
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.HashMap
import kotlin.properties.Delegates

class MyApplication : Application() {
    var version = 1 //1=trial, 5=full
    val scope = CoroutineScope(SupervisorJob())
    val firebase by lazy { MyFirebase("kalendar") }
    lateinit var database: MyRoom
    lateinit var datasync: MySync
    val repository by lazy { RoomRepository(database) }
    val kalendar = Kalendar(language)
    var messages = mutableListOf<Found>()
    //service
    var servicePersons: MutableList<Person> = mutableListOf()
    var servicePlans: MutableList<Plan> = mutableListOf()
    var planAlarms = HashMap<LocalDateTime, Boolean>()
    var personDate: LocalDate = LocalDate.now().minusDays(1)
    var noteID: Int = 546
    var codeQR: String = ""

    //get event for finished activities:
    var finishedReceiveActivity: Boolean by Delegates.observable(false) { _, oldValue, newValue ->
        onReceiveFinished?.invoke(oldValue, newValue)
    }
    var onReceiveFinished: ((Boolean, Boolean) -> Unit)? = null

    var finishedAuthActivity: Boolean by Delegates.observable(false) { _, oldValue, newValue ->
        onAuthFinished?.invoke(oldValue, newValue)
    }
    var onAuthFinished: ((Boolean, Boolean) -> Unit)? = null

    var finishedScannerActivity: Boolean by Delegates.observable(false) { _, oldValue, newValue ->
        onScannerFinished?.invoke(oldValue, newValue)
    }
    var onScannerFinished: ((Boolean, Boolean) -> Unit)? = null

    //get language
    val language: String
        get() {
            var lge = Locale.getDefault().language
            if (lge == "cs") lge = "cz"
            if (lge != "cz" && lge != "sk") lge = "en"
            return lge.uppercase(Locale.getDefault())
        }

    //BugSnag
    override fun onCreate() {
        super.onCreate()
        Bugsnag.start(this)
        //Bugsnag.notify(RuntimeException("Test error"))
    }

    // settings
    private val prefs: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this.applicationContext) }
    fun getBool(name: String, defValue: Boolean = false): Boolean {
        return prefs.getBoolean(name, defValue)
    }
    fun setBool(name: String, value: Boolean) {
        prefs.edit().putBoolean(name, value).apply()
    }
    fun getStr(name: String, defValue: String = ""): String {
        return prefs.getString(name, defValue)!!
    }
    fun setStr(name: String, value: String) {
        prefs.edit().putString(name, value).apply()
    }

    fun databaseOpen() : MyRoom {
        database = MyRoom(this)
        return database
    }
    fun datasyncOpen() : MySync {
        datasync = MySync(this)
        return datasync
    }
    fun databaseSave() {
        scope.launch(Dispatchers.Main) { databaseCheckpoint() }
    }
    fun datasyncSave() {
        scope.launch(Dispatchers.Main) { datasyncCheckpoint() }
    }
    private suspend fun databaseCheckpoint() {
        val value = scope.async {
            database.daoOsoby().checkpoint(SimpleSQLiteQuery("pragma wal_checkpoint(full)"))
        }
        value.await()
    }
    private suspend fun datasyncCheckpoint() {
        val value = scope.async {
            datasync.daoOsoby().checkpoint(SimpleSQLiteQuery("pragma wal_checkpoint(full)"))
        }
        value.await()
    }
}