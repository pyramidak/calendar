package cz.pyramidak.kalendar

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import cz.pyramidak.kalendar.databinding.ActivityMainBinding
import cz.pyramidak.kalendar.drive.MyDrive
import cz.pyramidak.kalendar.room.Osoba
import cz.pyramidak.kalendar.room.RoomViewModel
import cz.pyramidak.kalendar.room.RoomViewModelFactory
import cz.pyramidak.kalendar.room.Upominka
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class MainActivity: AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private val app: MyApplication
        get() = (application as MyApplication)
    val kalendar: Kalendar
        get() = app.kalendar
    private val drive by lazy { MyDrive(this,"kalendar.db") }
    private val roomViewModel: RoomViewModel by viewModels { RoomViewModelFactory(app.repository) }
    var isBackup: Boolean = false

    override fun onStop() {
        if (drive.isOnline && drive.isUserSigned) { drive.download() }
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        if (drive.isOnline && drive.isUserSigned) { drive.download() }
    }

    //onCreate
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //přepnutí jazyka při změně jazyka systému
        app.kalendar.jazyk = app.language

        // Data connection
        app.databaseOpen()
        roomViewModel.personsLive.observe(this) { personsOrNull ->
            app.servicePersons.clear()
            personsOrNull?.let { persons ->
                persons.filter { !it.smazane }.forEach { person ->
                    app.servicePersons.add(person)
                }
            }
        }
        roomViewModel.plansLive.observe(this) { plansOrNull ->
            app.servicePlans.clear()
            plansOrNull?.let { plans ->
                plans.filter { it.text != "" && it.alarm != null }.filter { (it.alarm!!.hour != 0 || it.alarm!!.minute != 0) && it.alarm!! > LocalDateTime.now() }.forEach { plan ->
                    app.servicePlans.add(plan)
                }
            }
        }

        // Horizon ViewPager2 and a PagerAdapter
        binding.pager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.pager.adapter = PagerAdapter(this)
        binding.pager.currentItem = 2

        drive.setOnEventListener(object : MyDrive.OnEventListener {
            override fun onDownloaded(successfully: Boolean) {
                if (successfully) {
                    isBackup = true
                    app.scope.launch(Dispatchers.Main) { doSync() }
                } else {
                    if (roomViewModel.dataChanged) drive.upload()
                }
            }

            override fun onUploaded(successfully: Boolean) {
                if (successfully) roomViewModel.dataChanged = false
            }
        })

        //NotificationService
        //Upozornění na obdržená data narozenin a událostí
        startService(Intent(this, NotificationService::class.java))

        //JobNotificationService
        //Upozornění na narozeniny nebo události
        val component = ComponentName(this, JobNotificationService::class.java)
        val jobInfo = JobInfo.Builder(546, component).setPeriodic(15*60*1000).setPersisted(true).build() //.setPersisted(true) + RECEIVE_BOOT_COMPLETED permission.
        val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.schedule(jobInfo)
    }

    //Button back
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private var allowBack: Boolean = false
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (allowBack) {
            super.onBackPressed()
        } else {
            binding.pager.currentItem = 2
        }
    }

    //Sync databases
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    suspend fun doSync() {
        val base = app.database
        val sync = app.datasyncOpen()
        //scope běží na jiném vláknu
        val value = app.scope.async {
            sync.daoOsoby().getAll().size
            for (one in sync.daoOsoby().getAll()) {
                val record = base.daoOsoby().findByName(one.jmeno!!)
                if (record == null) {
                    base.daoOsoby().insert(Osoba(one.jmeno, one.rodne, one.narozeni, one.umrti, one.zmena, one.smazane))
                } else {
                    val zmenaLocal = record.zmena.toDateTime()
                    val zmenaCloud = one.zmena.toDateTime()
                    if (zmenaLocal != null) {
                        if (zmenaLocal < zmenaCloud) {
                            record.jmeno = one.jmeno
                            record.rodne = one.rodne
                            record.narozeni = one.narozeni
                            record.umrti = one.umrti
                            record.zmena = one.zmena
                            record.smazane = one.smazane
                            base.daoOsoby().update(record)
                        }
                    }
                }
            }
            sync.daoUpominky().getAll().size
            for (one in sync.daoUpominky().getAll()) {
                val record = base.daoUpominky().findByDay(one.vznik!!)
                if (record == null) {
                    base.daoUpominky().insert(Upominka(one.vznik, one.den, one.text, one.mesicne, one.rocne, one.zmena, one.alarm))
                } else {
                    val zmenaLocal = record.zmena.toDateTime()
                    val zmenaCloud = one.zmena.toDateTime()
                    if (zmenaLocal != null) {
                        if (zmenaLocal < zmenaCloud) {
                            record.den = one.den
                            record.text = one.text
                            record.mesicne = one.mesicne
                            record.rocne = one.rocne
                            record.zmena = one.zmena
                            record.alarm = one.alarm
                            base.daoUpominky().update(record)
                        }
                    }
                }
            }
        }
        //čeká se na dokončení jiného vlákna
        value.await()
        app.datasync.close()
        if (roomViewModel.dataChanged) drive.upload()
        //showMessage(getString(R.string.db_synced))
    }


    fun showMessage(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    //Alert Help
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    fun showDialog(title: String, message: String, setting: String) {
        if (!app.getBool(setting, false)) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(title)
            builder.setMessage(message)
            builder.setPositiveButton(getString(R.string.ok)){_, _ ->
                app.setBool(setting, true)
            }
            builder.show()
        }
    }

    //Permissions
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    fun checkPermissions() {
        checkPermission(android.Manifest.permission.INTERNET)
        checkPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
        checkPermission(android.Manifest.permission.WAKE_LOCK)
        checkPermission(android.Manifest.permission.RECEIVE_BOOT_COMPLETED)
        checkPermission(android.Manifest.permission.VIBRATE)
    }

    private fun checkPermission(permission: String) {
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 0)
        } else {
            showMessage("Granted: $permission")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showMessage("New granted: ${permissions[0]}")
            } else {
                showMessage("Denied: ${permissions[0]}")
            }
        }
    }
}