package cz.pyramidak.kalendar

import android.app.*
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.Color
import android.preference.PreferenceManager
import cz.pyramidak.kalendar.firebase.Message
import cz.pyramidak.kalendar.firebase.MyFirebase
import cz.pyramidak.kalendar.firebase.records
import cz.pyramidak.kalendar.firebase.recordsOdber
import cz.pyramidak.kalendar.room.MyRoom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import java.time.LocalDateTime


class JobNotificationService : JobService() {
    private val app: MyApplication
        get() = (application as MyApplication)
    //Notification:
    private val channelId = "cz.pyramidak.kalendar.event"
    private val description = "Table calendar"
    private var noteID: Int = 546

    //*** JobIntentService ***
//    override fun onHandleWork(intent: Intent) {
//        checkNotification()
//    }
//
//    companion object {
//        const val JOB_ID = 1000
//        fun enqueueWork(context: Context, work: Intent) {
//            enqueueWork(context, JobNotificationService::class.java, JOB_ID, work)
//        }
//    }

    override fun onStartJob(params: JobParameters?): Boolean {
        checkNotification()
        return false //ukončit job; true ukončit pak ručně
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return true //přenastavit na jiný čas
    }

    //Notification job for JobService
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private fun checkNotification() {
        noteID = app.noteID
        val now = LocalDateTime.now()
        if (now.hour < 1 && now.minute < 15) { //00:00 day shift
            app.kalendar.dateNow = now.toLocalDate()
        }

        if (app.servicePlans.isEmpty() && app.servicePersons.isEmpty()) {
            loadData()

        } else {

            val minAhead: Long = 15
            val plans: MutableList<Plan> = mutableListOf()
            if (app.servicePlans.isNotEmpty()) {
                plans.addAll( app.servicePlans.filter { !it.rocne && !it.mesicne && now > it.alarm!!.minusMinutes(minAhead) && now <= it.alarm!! } )
                plans.addAll( app.servicePlans.filter { it.rocne && it.mesicne && now > LocalDateTime.of(now.year, now.monthValue, it.alarm!!.dayOfMonth, it.alarm!!.hour, it.alarm!!.minute).minusMinutes(minAhead) && now <= LocalDateTime.of(now.year, now.monthValue, it.alarm!!.dayOfMonth, it.alarm!!.hour, it.alarm!!.minute) } )
                plans.addAll( app.servicePlans.filter { it.rocne && !it.mesicne && now > LocalDateTime.of(now.year, it.alarm!!.monthValue, it.alarm!!.dayOfMonth, it.alarm!!.hour, it.alarm!!.minute).minusMinutes(minAhead) && now <= LocalDateTime.of(now.year, it.alarm!!.monthValue, it.alarm!!.dayOfMonth,it.alarm!!.hour,it.alarm!!.minute) } )
                plans.addAll( app.servicePlans.filter { !it.rocne && it.mesicne && now > LocalDateTime.of(it.alarm!!.year, now.monthValue, it.alarm!!.dayOfMonth, it.alarm!!.hour, it.alarm!!.minute).minusMinutes(minAhead) && now <= LocalDateTime.of(it.alarm!!.year,now.monthValue,it.alarm!!.dayOfMonth,it.alarm!!.hour,it.alarm!!.minute) } )
                plans.filter { !app.planAlarms.containsKey(it.alarm!!) || app.planAlarms[it.alarm!!] == false }.forEach {
                    app.planAlarms[it.alarm!!] = true
                    val time = if (it.den.hour == 0 && it.den.minute == 0) "" else createTime(it.den.hour, it.den.minute)
                    showNotification(time + " " + it.text, Tables.PLAN)
                }
            }
            val lastBirthdayAlarm = getStr("AlarmBirthday").toDate()
            if (now.hour >= 7 && now.toLocalDate() != lastBirthdayAlarm) {
                val persons = app.servicePersons.filter { it.narozeni!!.dayOfMonth == now.dayOfMonth && it.narozeni!!.monthValue == now.monthValue }
                persons.forEach {
                    // app.personDate = now.toLocalDate() app.personDate != now.toLocalDate() &&
                    showNotification(if (it.umrti == null) it.jmeno + " " + (now.year - it.narozeni!!.year) else " †" + (it.umrti!!.year - it.narozeni!!.year), Tables.PERSON)
                    setStr("AlarmBirthday", now.dbString())
                }
            }
        }
    }

    private fun createTime(hour: Int, minute: Int): String {
        val hodina = if (hour < 10) "0$hour" else hour
        val minuta = if (minute < 10) "0$minute" else minute
        return "$hodina:$minuta"
    }

    //Load new data without application activity
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    val scope = CoroutineScope(SupervisorJob())
    private fun loadData() {
        val database = MyRoom(this)
        scope.async {
            val osoby = database.daoOsoby().getAllvalid()
            val persons: MutableList<Person> = mutableListOf()
            osoby.forEach { persons.add(Person(it)) }

            val upominky = database.daoUpominky().getAllvalid()
            val plans: MutableList<Plan> = mutableListOf()
            upominky.forEach { plans.add(Plan(it)) }

            val now = LocalDateTime.now()

            val minAhead: Long = 15
            val plansAlarm: MutableList<Plan> = mutableListOf()
            if (plans.isNotEmpty()) {
                plansAlarm.addAll( plans.filter { !it.rocne && !it.mesicne && now > it.alarm!!.minusMinutes(minAhead) && now <= it.alarm!! } )
                plansAlarm.addAll( plans.filter { it.rocne && it.mesicne && now > LocalDateTime.of(now.year, now.monthValue, it.alarm!!.dayOfMonth, it.alarm!!.hour, it.alarm!!.minute).minusMinutes(minAhead) && now <= LocalDateTime.of(now.year, now.monthValue, it.alarm!!.dayOfMonth, it.alarm!!.hour, it.alarm!!.minute) } )
                plansAlarm.addAll( plans.filter { it.rocne && !it.mesicne && now > LocalDateTime.of(now.year, it.alarm!!.monthValue, it.alarm!!.dayOfMonth, it.alarm!!.hour, it.alarm!!.minute).minusMinutes(minAhead) && now <= LocalDateTime.of(now.year, it.alarm!!.monthValue, it.alarm!!.dayOfMonth,it.alarm!!.hour,it.alarm!!.minute) } )
                plansAlarm.addAll( plans.filter { !it.rocne && it.mesicne && now > LocalDateTime.of(it.alarm!!.year, now.monthValue, it.alarm!!.dayOfMonth, it.alarm!!.hour, it.alarm!!.minute).minusMinutes(minAhead) && now <= LocalDateTime.of(it.alarm!!.year,now.monthValue,it.alarm!!.dayOfMonth,it.alarm!!.hour,it.alarm!!.minute) } )
                plansAlarm.forEach {
                    val time = if (it.den.hour == 0 && it.den.minute == 0) "" else createTime(it.den.hour, it.den.minute)
                    showNotification( time + " " + it.text, Tables.PLAN)
                }
            }

            val lastBirthdayAlarm = getStr("AlarmBirthday").toDate()
            if (now.hour >= 7 && now.toLocalDate() != lastBirthdayAlarm ) {
                val personsAlarm = persons.filter { it.narozeni!!.dayOfMonth == now.dayOfMonth && it.narozeni!!.monthValue == now.monthValue }
                personsAlarm.forEach {
                    showNotification( if (it.umrti == null) it.jmeno + " " + (now.year - it.narozeni!!.year) else " †" + (it.umrti!!.year - it.narozeni!!.year), Tables.PERSON)
                }
                setStr("AlarmBirthday", now.dbString())
            }
        }
        //value.await()
        val users = MyFirebase("kalendar/users")
        if (users.userSigned && users.userVerified ) {
            users.refWithUser.child("odbery").get().addOnSuccessListener { snapshotOdber ->
                val odbery = MyFirebase("kalendar/odbery")
                snapshotOdber.recordsOdber().forEach { odber ->
                    odbery.ref.child(odber.key!!).child("messages").get().addOnSuccessListener { snapshotMsg  ->
                        val messages = snapshotMsg.records<Message>()
                        if (messages.filter { it.sender != users.userUid }.isNotEmpty()) {
                            showNotification( getString(R.string.dataRecieved) + " " + odber.name, Tables.MESSAGE)
                        }
                    }
                }
            }
        }
    }

    // settings
    private val prefs: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this.applicationContext) }
    fun getStr(name: String, defValue: String = ""): String {
        return prefs.getString(name, defValue)!!
    }
    fun setStr(name: String, value: String) {
        prefs.edit().putString(name, value).apply()
    }

    //Notification show
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private fun showNotification(msg: String, type: Tables) {
        val title = when (type) {
            Tables.PERSON -> getString(R.string.event_birthday)
            Tables.PLAN ->  getString(R.string.event_note)
            Tables.MESSAGE -> getString(R.string.event_message)
            else -> ""
        }
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notificationChannel = NotificationChannel(channelId, description, NotificationManager.IMPORTANCE_HIGH)
        notificationChannel.enableLights(true)
        notificationChannel.lightColor = Color.GREEN
        notificationChannel.enableVibration(true)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)
        val builder = Notification.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(msg)
            .setSmallIcon(R.drawable.ic_note)
            .setLargeIcon(BitmapFactory.decodeResource(this.resources, R.drawable.calender))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        noteID += 1
        app.noteID = noteID
        notificationManager.notify(noteID, builder.build())
    }

    //Notification job for JobIntentService
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//    private fun checkNotification() {
//        val now = LocalDateTime.now()
//
//        val minAhead: Long = 0
//        val plans: MutableList<Plan> = mutableListOf()
//        if (app.servicePlans.isNotEmpty()) {
//            plans.addAll( app.servicePlans.filter { !it.rocne && !it.mesicne && it.den == LocalDateTime.of(now.year,now.monthValue,now.dayOfMonth,now.hour,now.minute).plusMinutes(minAhead) } )
//            plans.addAll( app.servicePlans.filter { it.rocne && it.mesicne && LocalDateTime.of(now.year, now.monthValue, it.den.dayOfMonth, it.den.hour, it.den.minute).minusMinutes(minAhead) == LocalDateTime.of(now.year,now.monthValue,now.dayOfMonth,now.hour,now.minute) } )
//            plans.addAll( app.servicePlans.filter { it.rocne && !it.mesicne && LocalDateTime.of(now.year, it.den.monthValue, it.den.dayOfMonth, it.den.hour, it.den.minute).minusMinutes(minAhead) == LocalDateTime.of(now.year,now.monthValue,now.dayOfMonth,now.hour,now.minute) } )
//            plans.addAll( app.servicePlans.filter { !it.rocne && it.mesicne && LocalDateTime.of(it.den.year, now.monthValue, it.den.dayOfMonth, it.den.hour, it.den.minute).minusMinutes(minAhead) == LocalDateTime.of(now.year,now.monthValue,now.dayOfMonth,now.hour,now.minute) } )
//            plans.filter { app.alarms[it.den] == true }.forEach {
//                app.alarms[it.den] = false
//                showNotification("${it.den.hour}:${it.den.minute} ${it.text}", Tables.PLAN)
//            }
//        }
//
//        if (app.servicePlans.isNotEmpty()) {
//            plans.clear()
//            plans.addAll( app.servicePlans.filter { !it.rocne && !it.mesicne && it.den.toLocalDate() == now.toLocalDate() && it.den > now } )
//            plans.addAll( app.servicePlans.filter { it.rocne && it.mesicne && LocalDate.of(now.year, now.monthValue, it.den.dayOfMonth) == now.toLocalDate() && LocalDateTime.of(now.year, now.monthValue, it.den.dayOfMonth, it.den.hour, it.den.minute) > now } )
//            plans.addAll( app.servicePlans.filter { it.rocne && !it.mesicne && LocalDate.of(now.year, it.den.monthValue, it.den.dayOfMonth) == now.toLocalDate() && LocalDateTime.of(now.year, it.den.monthValue, it.den.dayOfMonth, it.den.hour, it.den.minute) > now } )
//            plans.addAll( app.servicePlans.filter { !it.rocne && it.mesicne && LocalDate.of(it.den.year, now.monthValue, it.den.dayOfMonth) == now.toLocalDate() && LocalDateTime.of(it.den.year, now.monthValue, it.den.dayOfMonth, it.den.hour, it.den.minute) > now } )
//            plans.filter { !app.alarms.containsKey(it.den) || app.alarms[it.den] == false }.forEach {
//                app.alarms[it.den] = true
//                setAlarm(this, it.den)
//            }
//        }
//
//        val datum = app.getStr("LastBirthdayNotificationDate","")
//        if (datum.toDate() != LocalDate.now()) {
//            val persons = app.servicePersons.filter { it.narozeni!!.dayOfMonth == now.dayOfMonth && it.narozeni!!.monthValue == now.monthValue }
//            persons.forEach {
//                app.setStr("LastBirthdayNotificationDate", now.toLocalDate().dbString())
//                showNotification(if (it.umrti == null) it.jmeno + " " + (now.year - it.narozeni!!.year) else " †" + (it.umrti!!.year - it.narozeni!!.year), Tables.PERSON)
//            }
//        }
//    }
}

