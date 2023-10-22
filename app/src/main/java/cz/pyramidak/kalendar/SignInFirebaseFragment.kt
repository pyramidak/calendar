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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import cz.pyramidak.kalendar.databinding.FragmentSigninFirebaseBinding
import cz.pyramidak.kalendar.firebase.MyFirebase

class SignInFirebaseFragment : Fragment(){

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth

    private var _binding: FragmentSigninFirebaseBinding? = null
    private val binding get() = _binding!!
    private val activityAuth: AuthActivity
        get() = (activity as AuthActivity)
    private val app: MyApplication
        get() = (activityAuth.application as MyApplication)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSigninFirebaseBinding.inflate(inflater, container, false)
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
            .requestEmail()
            .build()

        // Initialize Google Sign In
        googleSignInClient = GoogleSignIn.getClient(requireContext(), signInOptions)
        // Initialize Firebase Auth
        firebaseAuth = Firebase.auth
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
            firebaseAuthWithGoogle(account.idToken!!)
            updateUI()
        } catch (e: ApiException) {
            Toast.makeText(context,"Přihlášení se nezdařilo.",Toast.LENGTH_SHORT).show()
            updateUI()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(requireActivity()) { task ->
            if (task.isSuccessful) {
                updateUI()
            } else {

                Toast.makeText(context, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                updateUI()
            }
        }
    }

    private fun signOut() {
        firebaseAuth.signOut()
        updateUI()
    }

    private fun revokeAccess() {
        firebaseAuth.signOut()
        updateUI()
    }

    private fun updateUI() {
        val user = firebaseAuth.currentUser
        if (user != null) {
            binding.statusFirebase.text = getString(R.string.signed_in)
            binding.detailFirebase.text = getString(R.string.google_status_fmt, user.uid)
            if (app.version == 2) app.version = 3
        } else {
            binding.statusFirebase.text = getString(R.string.signed_out)
            binding.detailFirebase.text = null
            if (app.version == 3) app.version = 2
        }
        if (user != null) {
            binding.signInButton.visibility = View.GONE
            binding.signOutAndDisconnect.visibility = View.VISIBLE
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
