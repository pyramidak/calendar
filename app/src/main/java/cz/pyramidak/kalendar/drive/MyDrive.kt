package cz.pyramidak.kalendar.drive

import android.accounts.Account
import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import cz.pyramidak.kalendar.MyApplication
import cz.pyramidak.kalendar.R
import java.io.File
import java.util.*


class MyDrive(private val activity: AppCompatActivity, private val dbName: String) {

    private var dbFolder = "pyramidak"
    private var dbMain: File
    private var dbSync: File
    private var isRestore = false
    private var mGoogleApiClient: GoogleSignInClient? = null
    private var mDriveServiceHelper: DriveServiceHelper? = null
    private var dbFiles: List<GoogleDriveFileHolder> = listOf()
    private var dbFolderId: String? = null
    val isOnline
        get() = internetConnection()
    val user: GoogleSignInAccount?
        get() = GoogleSignIn.getLastSignedInAccount(activity.applicationContext)
    val isUserSigned
        get() = user?.grantedScopes?.any { it.scopeUri.contains("auth/drive.file") } ?:false
    private val app: MyApplication
        get() = (activity.application as MyApplication)
    var silentMode: Boolean = false
    var isBusy: Boolean = false

    interface OnEventListener {
        fun onDownloaded(successfully: Boolean)
        fun onUploaded(successfully: Boolean) {}
    }
    private var myEventListener: OnEventListener? = null
    fun setOnEventListener(listener: OnEventListener?) {
        myEventListener = listener
    }

    val resultAuthLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            task.addOnSuccessListener {
                createDriveService(it.account!!)
                mDriveServiceHelper?.let { getIDs() }
            }
        }
    }

    init {
        val sPath = app.getDatabasePath(dbName).absolutePath
        dbMain = File(sPath)
        dbSync = File(sPath.substring(0, sPath.length - 3) + "_sync.db")
    }

    fun upload() {
        if (isBusy) return
        isBusy = true
        isRestore = false
        if (mDriveServiceHelper == null) {
            if (!isUserSigned) googleAuth() else user?.account?.let { createDriveService(it) }
        }
        app.databaseSave()
        getID()
    }

    fun download() {
        if (isBusy) return
        isBusy = true
        isRestore = true
        if (mDriveServiceHelper == null) {
            if (!isUserSigned) googleAuth() else user?.account?.let { createDriveService(it) }
        }
        getID()
    }

    private fun showMessage(msg: String) {
        if (!silentMode) Toast.makeText(app, msg, Toast.LENGTH_SHORT).show()
    }

    private fun googleAuth() {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope(Scopes.PROFILE), Scope(DriveScopes.DRIVE_FILE))
            .build()
        mGoogleApiClient = GoogleSignIn.getClient(app, signInOptions)
        resultAuthLauncher.launch(mGoogleApiClient!!.signInIntent)
    }

    private fun createDriveService(account: Account) {
        val credential = GoogleAccountCredential.usingOAuth2(app, Collections.singleton(DriveScopes.DRIVE_FILE))
        credential.selectedAccount = account

        val googleDriveService = Drive.Builder(NetHttpTransport(), GsonFactory(), credential)
            .setApplicationName(activity.getString(R.string.app_name)).build()
        mDriveServiceHelper = DriveServiceHelper(googleDriveService)
    }

    private fun getIDs() {
        //Get Folder
        val task = mDriveServiceHelper?.searchFolder(dbFolder)
        task?.addOnCompleteListener {
            try {
                it.result?.id
            } catch (err: Exception) {
                showMessage(activity.resources.getString(R.string.auth_disk_failed))
                myEventListener?.run {
                    isBusy = false
                    if (isRestore) onDownloaded(false) else onUploaded(false)
                }
                return@addOnCompleteListener
            }
            if (it.result?.id != null) {
                dbFolderId = it.result?.id
                //Get File
                val task2 = mDriveServiceHelper?.searchFile(dbMain.name, dbFolderId)
                task2?.addOnCompleteListener{ task ->
                    dbFiles = if (task.result == null) listOf() else task.result!!
                    if (isRestore && dbFiles.isEmpty()) {
                        showMessage(activity.resources.getString(R.string.backup_missing))
                        isBusy = false
                        myEventListener?.run { onDownloaded(false) }
                    } else {
                        if (isRestore) downloadFile() else uploadFile()
                    }
                }
            } else {
                //Create Folder
                val task2 = mDriveServiceHelper?.createFolder(dbFolder)
                task2?.addOnCompleteListener { task ->
                    dbFolderId = task.result?.id
                    if (isRestore) {
                        showMessage(activity.resources.getString(R.string.backup_missing))
                        isBusy = false
                        myEventListener?.run { onDownloaded(false) }
                    } else {
                        uploadFile()
                    }
                }
            }
        }
    }

    private fun getID() {
        //Get File
        val task = mDriveServiceHelper?.searchFile(dbMain.name)
        task?.addOnCompleteListener{
            dbFiles = if (it.result == null) listOf() else it.result!!
            if (isRestore && dbFiles.isEmpty()) {
                showMessage(activity.resources.getString(R.string.backup_missing))
                isBusy = false
                myEventListener?.run { onDownloaded(false) }
            } else {
                if (isRestore) downloadFile() else uploadFile()
            }
        }
    }

    private fun downloadFile() {
        //Download File
        val task = mDriveServiceHelper?.downloadFile(dbSync, dbFiles[0].id)
        task?.addOnCompleteListener {
            //showMessage(activity.resources.getString(R.string.download_sucess))
            isBusy = false
            myEventListener?.run { onDownloaded(true) }
        }
        task?.addOnFailureListener{
            showMessage(activity.resources.getString(R.string.download_failed))
            isBusy = false
            myEventListener?.run { onDownloaded(false) }
        }
    }

    private fun uploadFile() {
        if (dbFiles.isNotEmpty()) {
            //Delete File
            val task = mDriveServiceHelper?.deleteFileOrFolder(dbFiles)
            task?.addOnCompleteListener {
                dbFiles = listOf()
                uploadFile()
            }
        } else {
            //Upload File
            val task = mDriveServiceHelper?.uploadFile(dbMain)
            task?.addOnCompleteListener {
                //je potřeba vždy najít aktuální dbFileId = task.result?.id
                //showMessage(activity.resources.getString(R.string.backup_sucess)
                isBusy = false
                myEventListener?.run { onUploaded(true) }
            }
            task?.addOnFailureListener{
                showMessage(activity.resources.getString(R.string.backup_failed))
                isBusy = false
                myEventListener?.run { onUploaded(false) }
            }
        }
    }

    private fun internetConnection(): Boolean {
        val connectivityManager = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return true
            }
        }
        return false
    }
}