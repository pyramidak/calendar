package cz.pyramidak.kalendar

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import cz.pyramidak.kalendar.firebase.Message
import cz.pyramidak.kalendar.firebase.MyFirebase
import cz.pyramidak.kalendar.firebase.records
import cz.pyramidak.kalendar.firebase.recordsOdber
import java.time.LocalDate
import java.time.LocalDateTime
import android.service.notification.StatusBarNotification

import android.app.NotificationManager





class NotificationService : Service() {
    private lateinit var handler: Handler
    private val app: MyApplication
        get() = (application as MyApplication)
    //Notification:
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationChannel: NotificationChannel
    private lateinit var builder: Notification.Builder
    private val channelId = "cz.pyramidak.kalendar.event"
    private val description = "Table calendar"
    private var connectedToFirebase: Boolean = false
    val mReceiver = AlarmReceiver()
    var countMsgLast: Int = 0

    override fun onBind(intent: Intent?): IBinder? { return null }

    override fun onCreate() {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        handler = Handler(Looper.getMainLooper())
        handler.post(checkConnection)

        //Morning job
        //val now = LocalDateTime.now()
        //setAlarm(this, LocalDateTime.of(now.year, now.monthValue, now.dayOfMonth, 7,0,0), true)

        //BroadcastReciever registration
        //val intentFilter = IntentFilter("action_name")
        //registerReceiver(mReceiver, intentFilter)

        //Another activity finished
        app.onReceiveFinished = { _, newValue -> if (newValue) startMessagesChecking() }
    }

    //Messages checking
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private var connectionChangeListener: ValueEventListener? = null
    private fun startOdberyChecking() {
        val users = MyFirebase("kalendar/users")
        if (users.userSigned && users.userVerified ) {
            connectionChangeListener?.let { users.refWithUser.child("odbery").removeEventListener(it) }
            connectionChangeListener = object: ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    startMessagesChecking()
                }
                override fun onCancelled(error: DatabaseError) {
                    clearMessagesListeners()
                }
            }
            users.refWithUser.child("odbery").addValueEventListener (connectionChangeListener as ValueEventListener)
        }
    }

    private val mapEvents = HashMap<String, ValueEventListener>()
    private fun clearMessagesListeners() {
        if (mapEvents.isNotEmpty()) {
            val users = MyFirebase("kalendar/users")
            if (users.userSigned && users.userVerified) {
                users.refWithUser.child("odbery").get().addOnSuccessListener { snapshotOdber ->
                    snapshotOdber.recordsOdber().forEach { odber ->
                        val odbery = MyFirebase("kalendar/odbery")
                        if (mapEvents.isNotEmpty()) {
                            mapEvents[odber.key!!]?.let {
                                odbery.ref.child(odber.key!!).child("messages").removeEventListener(it)
                            }
                        }
                    }
                    mapEvents.clear()
                }
            }
        }
    }
    private fun startMessagesChecking() {
        clearMessagesListeners()
        val users = MyFirebase("kalendar/users")
        if (users.userSigned && users.userVerified ) {
            users.refWithUser.child("odbery").get().addOnSuccessListener { snapshotOdber ->
                snapshotOdber.recordsOdber().forEach { odber ->
                    val eventListener = object: ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val messages = dataSnapshot.records<Message>()
                            val countMsg = messages.filter { it.sender != users.userUid }.size
                            if (countMsg > countMsgLast) {
                                showNotification( getString(R.string.dataRecieved) + " " + odber.name)
                            }
                            countMsgLast = countMsg
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    }
                    mapEvents[odber.key!!] = eventListener
                    val odbery = MyFirebase("kalendar/odbery")
                    odbery.ref.child(odber.key!!).child("messages").addValueEventListener(eventListener as ValueEventListener)
                }
            }
        }
    }

    //Timer for checking connection
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private val checkConnection = object : Runnable {
        override fun run() {
            //checking firebase connection change
            val users = MyFirebase("kalendar/users")
            val connected = (users.userSigned && users.userVerified)
            if (connected && !connectedToFirebase) { startOdberyChecking() }
            connectedToFirebase = connected

            handler.postDelayed(this, 300000)
        }
    }

    //Notification show
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private fun showNotification(msg: String) {
        //var notificationExist: Boolean = notificationManager.activeNotifications.any { it.id == 546 }
        val title = getString(R.string.event_message)
        val intent = Intent(this, ReceiveActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent,PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        notificationChannel = NotificationChannel(channelId, description, NotificationManager.IMPORTANCE_HIGH)
        notificationChannel.enableLights(true)
        notificationChannel.lightColor = Color.GREEN
        notificationChannel.enableVibration(true)
        notificationManager.createNotificationChannel(notificationChannel)
//        val soundUri: Uri = Uri.parse("android.resource://" + this.applicationContext.packageName.toString() + "/" + R.raw.pristine)
//        if (soundUri != null) { //vlastní zvuk
//            val audioAttributes = AudioAttributes.Builder()
//                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
//                .setUsage(AudioAttributes.USAGE_ALARM)
//                .build()
//            notificationChannel.setSound(soundUri, audioAttributes)
//        }
        builder = Notification.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(msg)
            .setSmallIcon(R.drawable.ic_note)
            .setLargeIcon(BitmapFactory.decodeResource(this.resources, R.drawable.calender))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        notificationManager.notify(546, builder.build())
    }
}