package cz.pyramidak.kalendar

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import cz.pyramidak.kalendar.databinding.FragmentDatabaseBinding
import cz.pyramidak.kalendar.drive.MyFiles
import cz.pyramidak.kalendar.firebase.Extra
import cz.pyramidak.kalendar.firebase.MyFirebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.system.exitProcess


class DatabaseFragment : Fragment() {

    private var _binding: FragmentDatabaseBinding? = null
    private val binding
        get() = _binding!!
    private val activityMain: MainActivity
        get() = (activity as MainActivity)
    private val app: MyApplication
        get() = (activityMain.application as MyApplication)

    //private val myFile by lazy { MyFiles(activityMain) }
    private var resultUri = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let{ uri ->
                val myFile = MyFiles(activityMain)
                val sPath = app.getDatabasePath("kalendar.db").absolutePath
                if (myFile.Save(uri, sPath.substring(0, sPath.length - 3) + "_sync.db", ".db")) {
                    activityMain.isBackup = true
                    activityMain.showMessage(activityMain.getString(R.string.backup_restored))
                    app.scope.launch(Dispatchers.Main) { activityMain.doSync() }
                    //activityMain.binding.pager.currentItem = 2
                } else {
                    activityMain.isBackup = false
                    activityMain.showMessage(activityMain.getString(R.string.recovery_failed))
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDatabaseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.layoutRoom.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            resultUri.launch(intent)
        }
        binding.layoutDisk.setOnClickListener {
            val intent = Intent(context, AuthActivity::class.java)
            intent.putExtra("FRAGMENT", "drive")
            startActivity(intent)
        }
        binding.layoutFirebase.setOnClickListener {
            val intent = Intent(context, AuthActivity::class.java)
            intent.putExtra("FRAGMENT", "firebase")
            startActivity(intent)
        }
        // About program
        binding.tvCopyright?.text = activityMain.getString(R.string.copyright, "2023", "Zdeněk Jantač")
        binding.tvProgram?.text = activityMain.getString(R.string.program, "pyramidak", "", BuildConfig.VERSION_NAME)

        binding.ibHttp?.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://app.jantac.net/kalendar/"))
            startActivity(browserIntent)
        }
        binding.ibEmail?.setOnClickListener { //packageName
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data =
                Uri.parse("mailto:zdenek@jantac.net?subject=Feedback Android " + activityMain.getString(R.string.app_name) + " v." + BuildConfig.VERSION_NAME)
            startActivity(intent)
        }

        val database = MyFirebase("kalendar")
        if (database.userUid == "Zh2a20oyy0hKuz845fiyT39xAMb2" && database.userEmail =="zdenek.jantac@gmail.com" && database.userSigned && database.userVerified) {
            binding.ivPyramid?.setOnClickListener {
                //report activity
                val intent = Intent(context, ReportActivity::class.java)
                startActivity(intent)
            }
        }
        updateScreen()
        //Another activity finished
        app.onAuthFinished = { _, _ ->
            if (activity != null) { activityMain.binding.pager.currentItem = 0 }
        }
    }

    override fun onResume() {
        super.onResume()
        updateScreen()
        if (app.version == 4) showTrialEnded()
    }

    override fun onPause() {
        super.onPause()
        if (app.version == 4) activityMain.binding.pager.currentItem = 0
    }

    private fun updateScreen() {
        val account = GoogleSignIn.getLastSignedInAccount(activityMain)
        val scopeDrive: Boolean = account?.grantedScopes?.any { it.scopeUri.contains("auth/drive.file") } ?:false
        binding.ivPlusDisk.setImageResource( if (scopeDrive) R.drawable.ic_plus else R.drawable.ic_plus_gray)
        binding.ivPlusRoom.setImageResource( if (activityMain.isBackup) R.drawable.ic_plus else R.drawable.ic_plus_gray)

        //Full, trial, signed
        if (app.version == 1) { //první den neotravovat s Trial verzí
            val firstRun = app.getStr("firstRun")
            if (firstRun == "") {
                app.setStr("firstRun", LocalDate.now().dbString())
            } else {
                if (firstRun.toDate() != LocalDate.now()) app.version = 2
            }
        }
        val database = MyFirebase("kalendar")
        if (database.userSigned && database.userVerified) {
            database.updateLastAccess()
            binding.ivPlusFirebase.setImageResource(R.drawable.ic_plus)
            if (app.version != 5) app.version = 3
            if (app.version == 3) { //od druhého dne kontrolovat vypršení Trial verze
                database.refConfig.get().addOnSuccessListener { configSnapshot ->
                    val config = configSnapshot.getValue(Extra::class.java) ?: Extra()
                    if (config.full != null && config.full!!) {
                        app.version = 5
                        app.setBool("full", true)
                    } else {
                        app.version = 3
                        app.setBool("full", false)
                        //refConfig uživatelská configurace vs. refAppConfig administrátorská configurace
                        database.refAppConfig.get().addOnSuccessListener { appConfigSnapshot ->
                            val appConfig = appConfigSnapshot.getValue(Extra::class.java) ?: Extra()
                            if (appConfig.full!!) { //existuje už plná verze k zakoupení
                                val firstWrite = config.firstWrite.toDateTime()
                                if (firstWrite!!.plusMonths(1) < LocalDateTime.now()) { //Trial verze skončila
                                    app.version = 4
                                    showTrialEnded()
                                    activityMain.binding.pager.currentItem = 0
                                }
                            }
                        }
                    }
                    updateVersion()
                }
                updateVersion()
            }
        } else {
            if (app.getBool("full", false)) app.version = 5
            updateVersion()
            binding.ivPlusFirebase.setImageResource(R.drawable.ic_plus_gray)
            if (app.version == 2) {
                val intent = Intent(context, AuthActivity::class.java)
                intent.putExtra("FRAGMENT", "firebase")
                startActivity(intent)
            }
        }
        //v play storu pak vypnout aktualizaci
        //checkNewVersion()
    }

    private fun updateVersion() {
        binding.tvProgram?.text = activityMain.getString(R.string.program, "pyramidak", if (app.version == 5) "Pro" else "Trial", BuildConfig.VERSION_NAME)
    }

    private fun showTrialEnded() {
        val builder = AlertDialog.Builder(activityMain)
        builder.setTitle(activityMain.getString(R.string.trial_title))
        builder.setMessage(activityMain.getString(R.string.trial_end))
        builder.setPositiveButton(activityMain.getString(R.string.open)) { _, _ ->
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${activityMain.packageName}")))
            } catch (e: ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${activityMain.packageName}")))
            }
            //Google Drive installation:
//            val browserIntent = Intent(Intent.ACTION_VIEW,
//                Uri.parse("https://drive.google.com/file/d/12k3hM0W86PGXUwiRIfGNY9_8zDyMjbwW/view?usp=sharing")
//            )
//            startActivity(browserIntent)
        }
        builder.setNegativeButton(activityMain.getString(R.string.close)) { _, _ ->
            activityMain.moveTaskToBack(true)
            activityMain.finishAffinity()
            exitProcess(-1)
        }
        builder.show()
    }


    fun checkNewVersion() {
        Thread {
            val aplikace = ArrayList<String>()
            try {
                val url = URL("https://vb.jantac.net/apps/version.ini")
                val conn: HttpURLConnection = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000 // timing out in a minute
                val stream = BufferedReader(InputStreamReader(conn.inputStream))
                var str: String
                while (stream.readLine().also { str = it } != null) {
                    aplikace.add(str)
                }
            } catch (e: Exception) {}

            this@DatabaseFragment.activityMain.runOnUiThread(Runnable {
                aplikace.forEach { one ->
                    if (one.contains("=")) {
                        val apka = one.split("=")
                        if (apka[0] == "kalendar_release") {
                            if (apka[1].toInt() > BuildConfig.VERSION_CODE) {
                                val builder = AlertDialog.Builder(activityMain)
                                builder.setTitle(getString(R.string.new_title))
                                builder.setMessage(getString(R.string.new_text, apka[1]))
                                builder.setPositiveButton(getString(R.string.open)) { _, _ ->
                                    val browserIntent = Intent(Intent.ACTION_VIEW,
                                        Uri.parse("https://drive.google.com/file/d/12k3hM0W86PGXUwiRIfGNY9_8zDyMjbwW/view?usp=sharing")
                                    )
                                    startActivity(browserIntent)
                                }
                                builder.setNegativeButton(getString(R.string.close)) { _, _ -> }
                                builder.show()
                            }
                        }
                    }
                }
            })
        }.start()
    }

    //otevřít aplikace v google storu
//    try {
//        val appStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${app.packageName}"))
//        appStoreIntent.setPackage("com.android.vending")
//        startActivity(appStoreIntent)
//    } catch (exception: ActivityNotFoundException) {
//        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${app.packageName}")))
//    }

}