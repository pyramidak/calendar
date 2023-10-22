package cz.pyramidak.kalendar

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cz.pyramidak.kalendar.databinding.ActivityReportBinding
import cz.pyramidak.kalendar.firebase.*
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

class ReportActivity: AppCompatActivity() {

    lateinit var binding: ActivityReportBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnClose2.setOnClickListener {
            finish()
        }

        val now = LocalDate.now()

        var full: Int = 0
        var allUsers: Long = 0
        var dailyActive: Int = 0
        var newUsers: Int = 0
        var oneDayTry: Int = 0
        var lastAccessOneMonth: Int = 0
        var lastAccessTwoMonth: Int = 0
        var lastAccessTooLong: Int = 0
        var anyWrite: Int = 0
        var lastWriteOneMonth: Int = 0
        var lastWriteTwoMonth: Int = 0

        val users = MyFirebase("kalendar/users")
        if (users.userSigned && users.userVerified ) {
            users.ref.get().addOnSuccessListener { ssUsers ->
                allUsers = ssUsers.childrenCount
                for (one in ssUsers.children) {
                    users.ref.child(one.key!!).child("config").get().addOnSuccessListener { ssUserConfig ->
                        val extra = ssUserConfig.getValue(Extra::class.java) ?: Extra()
//                        if (one.key == "wpc4pxNAvISR7W3FwYqpWAGNgiz1") {
//                            val pokus: Int = 0
//                        }
                        extra.full?.let { if (it) full += 1 }
                        val lastAccess: LocalDateTime? = extra.lastAccess.toDateTime()
                        val firstWrite: LocalDateTime? = extra.firstWrite.toDateTime()
                        val lastWrite: LocalDateTime? = extra.lastWrite.toDateTime()
                        if (firstWrite != null && lastAccess != null) {
                            if (firstWrite.toLocalDate() == lastAccess.toLocalDate()) {
                                if (lastAccess.toLocalDate() == now) {
                                    newUsers += 1
                                } else {
                                    if (lastAccess.toLocalDate().minusDays(3) < firstWrite.toLocalDate()) oneDayTry += 1
                                }
                            } else {
                                if (lastAccess.toLocalDate() == now || lastAccess.toLocalDate().plusDays(1) == now) dailyActive += 1
                                when (Duration.between(lastAccess, LocalDateTime.now()).toDays()) {
                                    in 0..2 -> {}
                                    in 3..30 -> lastAccessOneMonth += 1
                                    in 31..60 -> lastAccessTwoMonth += 1
                                    else -> lastAccessTooLong += 1
                                }
                            }
                            if (lastWrite != null) {
                                anyWrite += 1
                                when (Duration.between(lastWrite, LocalDateTime.now()).toDays()) {
                                    in 0..30 -> lastWriteOneMonth += 1
                                    in 31..60 -> lastWriteTwoMonth += 1
                                }
                            }
                        }

                        binding.tvUsers.text = "Total Accounts: " + allUsers
                        binding.tvFull.text = "Full version: " + full
                        binding.tvDailyActive.text = "Daily Active: " + dailyActive
                        binding.tvNewUsers.text = "New Users: " + newUsers
                        binding.tvOneDayTry.text = "One Day Try: " + oneDayTry

                        binding.tvLastAccessOneMonth.text = "Last Access One Month: " + lastAccessOneMonth
                        binding.tvlastAccessTwoMonth.text = "Last Access Two Months: " + lastAccessTwoMonth
                        binding.tvlastAccessTooLong.text = "Last Access Too Long: " + lastAccessTooLong

                        binding.tvLastWriteOneMonth.text = "Last Write One Month: " + lastWriteOneMonth
                        binding.tvLastWriteTwoMonth.text = "Last Write Two Months: " + lastWriteTwoMonth
                        //binding.tvAnyWrite.text = "Any Write: " + anyWrite

                    }
                }
            }

            var totalOdbery: Long = 0
            var connectedOdbery: Int = 0

            val odbery = MyFirebase("kalendar/odbery")
            odbery.ref.get().addOnSuccessListener { ssOdbery ->
                totalOdbery = ssOdbery.childrenCount
                for (one in ssOdbery.children) {
                    odbery.ref.child(one.key!!).child("state").get().addOnSuccessListener { ssOdberyState ->
                        val extra = ssOdberyState.getValue(Extra::class.java) ?: Extra()
                        extra.connected?.let { if (it) connectedOdbery += 1 }

                        binding.tvConnectedOdbery.text = "Family connection: " + connectedOdbery
                        binding.tvTotalOdbery.text = "Incomplete connection: " + totalOdbery
                    }
                }
            }

        }
    }
}