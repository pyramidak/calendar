package cz.pyramidak.kalendar.room

import android.content.Context
import androidx.lifecycle.*
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import cz.pyramidak.kalendar.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.properties.Delegates

// Table
@Entity(tableName = "osoby")
data class Osoba(
    var jmeno: String?, //všechny var a null
    var rodne: String?,
    var narozeni: String?,
    var umrti: String?,
    var zmena: String?,
    var smazane: Boolean?,
    @PrimaryKey(autoGenerate = true) var uid: Int? = null) //výchozí null

@Entity(tableName = "upominky")
data class Upominka(
    var vznik: String?, //všechny var a null
    var den: String?,
    var text: String?,
    var mesicne: Boolean?,
    var rocne: Boolean?,
    var zmena: String?,
    var alarm: String? = null,
    @PrimaryKey(autoGenerate = true) var uid: Int? = null) //výchozí null

// DAO
@Dao
interface DAOosoby {
    @Query("SELECT * FROM osoby")
    fun getAllflow(): Flow<List<Osoba>>

    @Query("SELECT * FROM osoby")
    fun getAll(): List<Osoba>

    @Query("SELECT * FROM osoby WHERE smazane = 0")
    fun getAllvalid(): List<Osoba>

    @Query("SELECT * FROM osoby WHERE jmeno IN (:name) LIMIT 1")
    fun findByName(name: String): Osoba?

    @Query("SELECT * FROM osoby WHERE uid LIKE :id LIMIT 1")
    fun findByUid(id: Int): Osoba?

    @Query("DELETE FROM osoby")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: Osoba): Long

    @Update
    suspend fun update(record: Osoba)

    @Delete
    suspend fun delete(record: Osoba)

    @RawQuery
    suspend fun checkpoint(supportSQLiteQuery: SupportSQLiteQuery): Int
}
@Dao
interface DAOupominky {
    @Query("SELECT * FROM upominky")
    fun getAllflow(): Flow<List<Upominka>>

    @Query("SELECT * FROM upominky")
    fun getAll(): List<Upominka>

    @Query("SELECT * FROM upominky WHERE text != '' AND alarm != ''")
    fun getAllvalid(): List<Upominka>

    //Exact date:
    //@Query("SELECT * FROM upominky WHERE text != '' AND alarm IS NOT NULL AND alarm LIKE :datum")
    //fun getAllvalid(datum: String = LocalDate.now().toString() + "%"): List<Upominka>

    @Query("SELECT * FROM upominky WHERE vznik IN (:datum) LIMIT 1")
    fun findByDay(datum: String): Upominka?

    @Query("SELECT * FROM upominky WHERE uid LIKE :id LIMIT 1")
    fun findByUid(id: Int): Upominka?

    @Query("DELETE FROM upominky")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: Upominka): Long

    @Update
    suspend fun update(record: Upominka)

    @Delete
    suspend fun delete(record: Upominka)

    @RawQuery
    suspend fun checkpoint(supportSQLiteQuery: SupportSQLiteQuery): Int
}
// Aktualizace databáze
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE upominky ADD COLUMN alarm TEXT")
    }
}
// Database
@Database(entities = [Osoba::class, Upominka::class], version = 2, exportSchema = false)
abstract class MyRoom : RoomDatabase(){
    abstract fun daoOsoby(): DAOosoby
    abstract fun daoUpominky(): DAOupominky

    companion object {
        @Volatile var instance: MyRoom? = null
        private val LOCK = Any()
        private val scope = CoroutineScope(SupervisorJob())
        operator fun invoke(context: Context) = instance ?: synchronized(LOCK){
            instance ?: buildDatabase(context).also { instance = it}
        }
        private fun buildDatabase(context: Context) = Room.databaseBuilder(context, MyRoom::class.java, "kalendar.db" )
                .addMigrations(MIGRATION_1_2).build()
    }
    override fun close() {
        super.close()
        instance = null
    }
}
// Database pro synchronizaci
@Database(entities = [Osoba::class, Upominka::class], version = 2, exportSchema = false)
abstract class MySync : RoomDatabase(){
    abstract fun daoOsoby(): DAOosoby
    abstract fun daoUpominky(): DAOupominky

    companion object {
        @Volatile private var instance: MySync? = null
        private val LOCK = Any()
        operator fun invoke(context: Context) = instance ?: synchronized(LOCK){
            instance ?: buildDatabase(context).also { instance = it}
        }
        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context, MySync::class.java, "kalendar_sync.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
    override fun close() {
        super.close()
        instance = null
    }
}

// Repository
class RoomRepository(private val db: MyRoom) {
    val osobyFlow: Flow<List<Osoba>> = db.daoOsoby().getAllflow()
    val upominkyFlow: Flow<List<Upominka>> = db.daoUpominky().getAllflow()
    suspend fun insert(record: Osoba) = db.daoOsoby().insert(record)
    suspend fun update(record: Osoba) = db.daoOsoby().update(record)
    suspend fun delete(record: Osoba) = db.daoOsoby().delete(record)
    suspend fun insert(record: Upominka) = db.daoUpominky().insert(record)
    suspend fun update(record: Upominka) = db.daoUpominky().update(record)
    suspend fun delete(record: Upominka) = db.daoUpominky().delete(record)
}

// ViewModel
class RoomViewModel(private val repository: RoomRepository) : ViewModel() {
    private val osobyLive: LiveData<List<Osoba>> = repository.osobyFlow.asLiveData()
    val personsLive: LiveData<List<Person>> = osobyLive.map() { osoby ->
        val persons = mutableListOf<Person>()
        for (one in osoby) {
            val person = Person().apply {
                jmeno = one.jmeno ?:""
                rodne = one.rodne
                narozeni = one.narozeni.toDate()!!
                umrti = one.umrti.toDate()
                zmena = one.zmena.toDateTime()!!
                smazane = one.smazane ?:false
                uid = one.uid ?:0
            }
            persons.add(person)
        }
        persons
    }

    private val upominkyLive: LiveData<List<Upominka>> = repository.upominkyFlow.asLiveData()
    val plansLive: LiveData<List<Plan>> = upominkyLive.map() { upominky ->
        val plans = mutableListOf<Plan>()
        for (one in upominky) {
            val plan = Plan().apply {
                vznik = one.vznik.toDateTime()!!
                den = one.den.toDateTime()!!
                text = one.text ?:""
                mesicne = one.mesicne ?:false
                rocne = one.rocne ?:false
                zmena = one.zmena.toDateTime()!!
                alarm = one.alarm.toDateTime()
                uid = one.uid ?:0
            }
            plans.add(plan)
        }
        plans
    }

    var daySelected: Day? by Delegates.observable(null) { _, oldValue, newValue ->
        onDayChanged?.invoke(oldValue, newValue)
    }
    var onDayChanged: ((Day?, Day?) -> Unit)? = null

    //upload je proveden je při změně dat
    var dataChanged: Boolean = false

    fun delete(record: Person) = viewModelScope.launch {
        osobyLive.value?.find { it.uid == record.uid }?.let { repository.delete(it) }
    }
    fun update(record: Person) = viewModelScope.launch {
        var osoba = osobyLive.value?.find { it.uid == record.uid || it.jmeno == record.jmeno }
        if (osoba != null) {
            osoba.let {
                if (it.jmeno != record.jmeno || it.rodne != record.rodne || it.narozeni != record.narozeni.dbString() || it.umrti != record.umrti.dbString() || it.smazane != record.smazane) {
                    if (record.jmeno != "") it.jmeno = record.jmeno
                    it.rodne = record.rodne
                    it.narozeni = record.narozeni.dbString()
                    it.umrti = record.umrti.dbString()
                    it.zmena = LocalDateTime.now().dbString()
                    it.smazane = false
                    if (record.jmeno == "") it.smazane = true
                    dataChanged = true
                    repository.update(it)
                }
            }
        } else {
            if (record.jmeno != "" && record.narozeni != null) {
                dataChanged = true
                osoba = Osoba(record.jmeno, record.rodne, record.narozeni.dbString(), record.umrti.dbString(), LocalDateTime.now().dbString(), record.smazane)
                osoba.uid = repository.insert(osoba).toInt()
            }
        }
    }

    fun delete(record: Plan) = viewModelScope.launch {
        upominkyLive.value?.find { it.uid == record.uid }?.let { repository.delete(it) }
    }
    fun update(record: Plan) = viewModelScope.launch {
        var upominka = upominkyLive.value?.find { it.uid == record.uid }
        if (upominka == null) upominka = upominkyLive.value?.find { it.text == record.text && it.den == record.den.dbString() }
        if (upominka != null) {
            upominka.let {
                if (it.den != record.den.dbString() || it.alarm != record.alarm.dbString() || it.text != record.text || it.mesicne != record.mesicne || it.rocne != record.rocne) {
                    it.vznik = record.vznik.dbString()
                    it.den = record.den.dbString()
                    it.alarm = record.alarm.dbString()
                    it.text = record.text
                    it.mesicne = record.mesicne
                    it.rocne = record.rocne
                    it.zmena = LocalDateTime.now().dbString()
                    dataChanged = true
                    repository.update(it)
                }
            }
        } else {
            if (record.text != "") {
                dataChanged = true
                upominka = Upominka(record.vznik.dbString(), record.den.dbString(), record.text, record.mesicne, record.rocne, LocalDateTime.now().dbString(), record.alarm.dbString())
                upominka.uid = repository.insert(upominka).toInt()
            }
        }
    }
}

// Factory
class RoomViewModelFactory(private val repository: RoomRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoomViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RoomViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
