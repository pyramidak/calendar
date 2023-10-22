package cz.pyramidak.kalendar

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController

class AuthActivity : AppCompatActivity() {

    private val app: MyApplication
        get() = (application as MyApplication)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        app.finishedAuthActivity = false
        when (intent.extras!!.getString("FRAGMENT")) {
            "drive" -> findNavController(R.id.nav_host_fragment).setGraph(R.navigation.nav_drive)
            "firebase" -> findNavController(R.id.nav_host_fragment).setGraph(R.navigation.nav_firebase)
        }

        if (app.version == 2) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.trial_title))
            builder.setMessage(getString(R.string.trial_text) + "\n\n" + getString(R.string.trial_ask))
            builder.setPositiveButton(getString(R.string.ok)){_, _ -> }
            builder.show()
        }
    }

    override fun onStop() {
        super.onStop()
        app.finishedAuthActivity = true
    }
}