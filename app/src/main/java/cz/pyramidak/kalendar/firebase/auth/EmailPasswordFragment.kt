package cz.pyramidak.kalendar.firebase.auth

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import cz.pyramidak.kalendar.R
import cz.pyramidak.kalendar.databinding.FragmentEmailpasswordBinding

class EmailPasswordFragment : BaseFragment() {

    private lateinit var auth: FirebaseAuth

    private var _binding: FragmentEmailpasswordBinding? = null
    private val binding: FragmentEmailpasswordBinding
        get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEmailpasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setProgressBar(binding.progressBar)

        // Buttons
        with (binding) {
            emailSignInButton.setOnClickListener {
                val email = binding.fieldEmail.text.toString()
                val password = binding.fieldPassword.text.toString()
                signIn(email, password)
            }
            emailCreateAccountButton.setOnClickListener {
                val email = binding.fieldEmail.text.toString()
                val password = binding.fieldPassword.text.toString()
                createAccount(email, password)
            }
            signOutButton.setOnClickListener { signOut() }
            verifyEmailButton.setOnClickListener { sendEmailVerification() }
            reloadButton.setOnClickListener { activity?.finish() }
        }

        // Initialize Firebase Auth
        auth = Firebase.auth
    }

    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if(currentUser != null){
            reload()
        }
    }

    val mainHandler: Handler = Handler(Looper.getMainLooper())
    private val timerUpdate = object : Runnable {
        override fun run() {
            updateUser()
            mainHandler.postDelayed(this, 3000)
        }
    }
    private fun updateUser() {
        auth.currentUser?.let { user ->
            user.reload().addOnCompleteListener {
                if (user.isEmailVerified) {
                    mainHandler.removeCallbacks(timerUpdate)
                    updateUI(user)
                }
            }
        }
    }

    private fun createAccount(email: String, password: String) {
        Log.d(TAG, "createAccount:$email")
        if (!validateForm()) {
            return
        }
        hideKeyboard(requireView())

        showProgressBar()

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "createUserWithEmail:success")
                        val user = auth.currentUser
                        updateUI(user)
                        user?.let {
                            if (!it.isEmailVerified) {
                                sendEmailVerification()
                                val builder = AlertDialog.Builder(requireActivity())
                                builder.setTitle("Ověření emailu")
                                builder.setMessage("Otevřete si prosím právě obdržený email a klikněte na link v něm pro ověření emailu.")
                                builder.setPositiveButton("OK"){_, _ ->
                                    if (!it.isEmailVerified) mainHandler.post(timerUpdate)
                                }
                                builder.show()
                            }
                        }
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "createUserWithEmail:failure", task.exception)
                        Toast.makeText(context, "Neúspěšné vytroření účtu.", Toast.LENGTH_SHORT).show()
                        updateUI(null)
                    }

                    hideProgressBar()
                }
    }

    private fun signIn(email: String, password: String) {
        Log.d(TAG, "signIn:$email")
        if (!validateForm()) {
            return
        }
        hideKeyboard(requireView())

        showProgressBar()

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithEmail:success")
                        val user = auth.currentUser
                        updateUI(user)
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithEmail:failure", task.exception)
                        Toast.makeText(context, "Přihlášení se nezdařilo.",
                                Toast.LENGTH_SHORT).show()
                        updateUI(null)
                    }

                    if (!task.isSuccessful) {
                        binding.status.setText(R.string.auth_failed)
                    }
                    hideProgressBar()
                }
    }

    private fun signOut() {
        auth.signOut()
        updateUI(null)
    }

    private fun sendEmailVerification() {
        // Send verification email
        val user = auth.currentUser!!
        user.sendEmailVerification()
            .addOnCompleteListener(requireActivity()) { task ->
                // Re-enable button
                binding.verifyEmailButton.isEnabled = true

                if (task.isSuccessful) {
                    Toast.makeText(context,
                            "Ověřovací email poslán na ${user.email} ",
                            Toast.LENGTH_SHORT).show()
                } else {
                    Log.e(TAG, "sendEmailVerification", task.exception)
                    Toast.makeText(context, "Nepodařilo se odeslat ověřovací email.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun reload() {
        auth.currentUser!!.reload().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                updateUI(auth.currentUser)
                Toast.makeText(context, "Aktualizováno.", Toast.LENGTH_SHORT).show()
            } else {
                Log.e(TAG, "reload", task.exception)
                Toast.makeText(context, "Připojení se nezdařilo.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateForm(): Boolean {
        var valid = true

        val email = binding.fieldEmail.text.toString()
        if (TextUtils.isEmpty(email)) {
            binding.fieldEmail.error = "Email je povinný."
            valid = false
        } else {
            binding.fieldEmail.error = null
        }

        val password = binding.fieldPassword.text.toString()
        if (TextUtils.isEmpty(password)) {
            binding.fieldPassword.error = "Heslo je povinné."
            valid = false
        } else {
            if (password.length < 5) {
                binding.fieldPassword.error = "Krátké heslo. Minimum je 6 znaků."
                valid = false
            } else {
                binding.fieldPassword.error = null
            }
        }

        return valid
    }

    private fun updateUI(user: FirebaseUser?) {
        hideProgressBar()
        if (user != null) {
            binding.status.text = getString(R.string.emailpassword_status_fmt,
                    user.email, if (user.isEmailVerified) "ANO" else "NE" )
            binding.detail.text = getString(R.string.firebase_status_fmt, user.uid)

            binding.emailPasswordButtons.visibility = View.GONE
            binding.emailPasswordFields.visibility = View.GONE
            binding.signedInButtons.visibility = View.VISIBLE

            if (user.isEmailVerified) {
                binding.verifyEmailButton.visibility = View.GONE
            } else {
                binding.verifyEmailButton.visibility = View.VISIBLE
            }
        } else {
            binding.status.setText(R.string.signed_out)
            binding.detail.text = null

            binding.emailPasswordButtons.visibility = View.VISIBLE
            binding.emailPasswordFields.visibility = View.VISIBLE
            binding.signedInButtons.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (mainHandler.hasCallbacks(timerUpdate)) mainHandler.removeCallbacks(timerUpdate)
        _binding = null
    }

    companion object {
        private const val TAG = "EmailPassword"
    }
}
