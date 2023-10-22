package cz.pyramidak.kalendar

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        //MediaPlayer.create(context, R.raw.pristine).start()
        //JobNotificationService.enqueueWork(context,  Intent(context, JobNotificationService::class.java))
    }
}

class WakeUpAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            val now = LocalDateTime.now()
            setAlarm(context, LocalDateTime.of(now.year, now.monthValue, now.dayOfMonth, 7,0,0), true)
        }
    }
}

private var alarmID: Int = 5460
fun setAlarm(context: Context, time: LocalDateTime, daily: Boolean = false) {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, time.hour)
    calendar.set(Calendar.MINUTE, time.minute)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    if (daily && time < LocalDateTime.now()) {
        calendar.add(Calendar.DATE, 1) //snad o den později
    }
    if (!daily && time.toLocalDate() == LocalDate.now().plusDays(1)) {
        calendar.add(Calendar.DATE, 1) //snad o den později
    }

    val myIntent = Intent(context, AlarmReceiver::class.java)
    alarmID += 1
    val pendingIntent = PendingIntent.getBroadcast(context, alarmID, myIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    if (daily) {
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    } else {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }
    //alarmManager.cancel(pendingIntent)
}