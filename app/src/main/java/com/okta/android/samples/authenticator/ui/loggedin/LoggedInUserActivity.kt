package com.okta.android.samples.authenticator.ui.loggedin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.okta.android.samples.authenticator.R
import com.okta.android.samples.authenticator.databinding.ActivityLoggedInUserBinding
import com.okta.android.samples.authenticator.databinding.RowDashboardClaimBinding
import com.okta.android.samples.authenticator.ui.inflateBinding
import com.okta.android.samples.authenticator.ui.login.LoginActivity

/**
 * Show claims for a logged in User.
 */
class LoggedInUserActivity : AppCompatActivity() {
    private lateinit var viewModel: LoggedInUserViewModel
    private lateinit var binding: ActivityLoggedInUserBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoggedInUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[LoggedInUserViewModel::class.java]

        binding.signOutButton.setOnClickListener {
            viewModel.logout()
        }

        // Render claims dynamically.
        viewModel.userInfoLiveData.observe(this@LoggedInUserActivity) { userInfo ->
            binding.claimsTitle.visibility = if (userInfo.isEmpty()) View.GONE else View.VISIBLE
            for (entry in userInfo) {
                val nestedBinding =
                    binding.claimsParent.inflateBinding(RowDashboardClaimBinding::inflate)
                nestedBinding.textViewKey.text = entry.key
                nestedBinding.textViewValue.text = entry.value
                nestedBinding.textViewValue.setTag(R.id.claim, entry.key)
                binding.claimsLinearLayout.addView(nestedBinding.root)
            }
        }

        viewModel.logoutStateLiveData.observe(this@LoggedInUserActivity) { state ->
            when (state) {
                LoggedInUserViewModel.LogoutState.Failed -> {
                    binding.signOutButton.isEnabled = true
                    Toast.makeText(applicationContext, "Logout failed.", Toast.LENGTH_LONG).show()
                }
                LoggedInUserViewModel.LogoutState.Idle -> {
                    binding.signOutButton.isEnabled = true
                }
                LoggedInUserViewModel.LogoutState.Loading -> {
                    binding.signOutButton.isEnabled = false
                }
                LoggedInUserViewModel.LogoutState.Success -> {
                    viewModel.acknowledgeLogoutSuccess()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                }
            }
        }
    }
}