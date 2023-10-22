package cz.pyramidak.kalendar

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import cz.pyramidak.kalendar.databinding.FragmentSigninDriveBinding

class SignInDriveFragment : Fragment(){

    private lateinit var googleSignInClient: GoogleSignInClient

    private var _binding: FragmentSigninDriveBinding? = null
    private val binding get() = _binding!!
    private val activityAuth: AuthActivity
        get() = (activity as AuthActivity)
    private val app: MyApplication
        get() = (activityAuth.application as MyApplication)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSigninDriveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Button listeners
        binding.signInButton.setOnClickListener { signIn() }
        binding.signOutButton.setOnClickListener { signOut() }
        binding.disconnectButton.setOnClickListener { revokeAccess() }
        binding.continueButton.setOnClickListener { activity?.finish() }

        // Configure Google Sign In
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) //Firebase
            .requestScopes( Scope(DriveScopes.DRIVE_FILE)) //Drive Scope(Scopes.PROFILE),
            .requestEmail()
            .build()

        // Initialize Google Sign In
        googleSignInClient = GoogleSignIn.getClient(requireContext(), signInOptions)
    }

    override fun onStart() {
        super.onStart()
        updateUI()
    }

    private fun signIn() {
        launcherSignIn.launch(googleSignInClient.signInIntent)
    }

    private var launcherSignIn= registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) { resultSignIn(result.data) }
    }

    private fun resultSignIn(data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            updateUI()
        } catch (e: ApiException) {
            Toast.makeText(context,"Přihlášení se nezdařilo.",Toast.LENGTH_SHORT).show()
            updateUI()
        }
    }

    private fun signOut() {
        googleSignInClient.signOut().addOnCompleteListener(requireActivity()) {
            updateUI()
        }
    }

    private fun revokeAccess() {
        googleSignInClient.revokeAccess().addOnCompleteListener(requireActivity()) {
            updateUI()
        }
    }

    private fun updateUI() {
        val account = GoogleSignIn.getLastSignedInAccount(app)
        val scopeDrive: Boolean = account?.grantedScopes?.any { it.scopeUri.contains("auth/drive.file") } ?:false

        if (account != null) {
            binding.status.text = getString(R.string.signed_in)
            binding.detail.text = getString(R.string.google_status_fmt, account.email)
        } else {
            binding.status.text = getString(R.string.signed_out)
            binding.detail.text = null
        }
        if (account != null && scopeDrive) {
            binding.signInButton.visibility = View.GONE
            binding.signOutAndDisconnect.visibility = View.VISIBLE
            binding.disconnectButton.visibility = View.VISIBLE
            binding.continueButton.visibility = View.VISIBLE
        } else {
            binding.signInButton.visibility = View.VISIBLE
            binding.signOutAndDisconnect.visibility = View.GONE
            binding.continueButton.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
