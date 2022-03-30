package com.okta.android.samples.authenticator.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.iterator
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputLayout
import com.okta.android.samples.authenticator.R
import com.okta.android.samples.authenticator.databinding.*
import com.okta.android.samples.authenticator.ui.inflateBinding
import com.okta.android.samples.authenticator.ui.loggedin.LoggedInUserActivity
import com.okta.android.samples.authenticator.ui.loggedin.LoggedInUserModel

/**
 * Present a login view and render dynamic views for enrolling authenticators and passing challenge
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val username = binding.username
        val password = binding.password
        val login = binding.login
        val loading = binding.loading

        loginViewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        loginViewModel.loginFormState.observe(this@LoginActivity, Observer {
            val loginState = it ?: return@Observer

            // disable login button unless both username / password is valid
            login.isEnabled = loginState.isDataValid

            if (loginState.usernameError != null) {
                username.error = getString(loginState.usernameError)
            }
            if (loginState.passwordError != null) {
                password.error = getString(loginState.passwordError)
            }
        })

        loginViewModel.loginResult.observe(this@LoginActivity, Observer {
            val loginResult = it ?: return@Observer

            loading.visibility = View.GONE
            if (loginResult.error != null) {
                showError(loginResult.error)
            }

            // if there are dynamic fields remove current view, iterate through fields and render them
            if (loginResult.dynamicFields.isNotEmpty()) {
                binding.dynamicContainer.removeAllViews()
                for (field in loginResult.dynamicFields) {
                    binding.dynamicContainer.addView(field.createView())
                }
            }
            // if login is success, update the LoggedInUserModel and switch to LoggedInUserActivity
            if (loginResult.success != null) {
                LoggedInUserModel.loggedInUserView = loginResult.success
                val intent = Intent(this, LoggedInUserActivity::class.java)
                startActivity(intent)
            }
            setResult(Activity.RESULT_OK)
        })

        username.afterTextChanged {
            loginViewModel.loginDataChanged(
                username.text.toString(),
                password.text.toString()
            )
        }

        password.apply {
            afterTextChanged {
                loginViewModel.loginDataChanged(
                    username.text.toString(),
                    password.text.toString()
                )
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        loginViewModel.login(
                            username.text.toString(),
                            password.text.toString()
                        )
                }
                false
            }

            login.setOnClickListener {
                loading.visibility = View.VISIBLE
                loginViewModel.login(username.text.toString(), password.text.toString())
            }
        }
    }

    /**
     * Extension method to render IdxDynamicFields dynamically on the given view
     */
    private fun IdxDynamicField.createView(): View {
        return when (this) {
            // render text fields
            is IdxDynamicField.Text -> {
                val textBinding = binding.dynamicContainer.inflateBinding(FormTextBinding::inflate)

                textBinding.textInputLayout.hint = label

                if (isSecure) {
                    // password or sensitive fields
                    textBinding.textInputLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
                    textBinding.editText.inputType = EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
                    textBinding.editText.transformationMethod = PasswordTransformationMethod.getInstance()
                }
                val valueField = ::value
                textBinding.editText.setText(valueField.get())
                textBinding.editText.doOnTextChanged { text, _, _, _ ->
                    valueField.set(text.toString())
                }

                errorsLiveData.observe(this@LoginActivity) { errorMessage ->
                    textBinding.textInputLayout.error = errorMessage
                }

                textBinding.root
            }
            // render actions as buttons
            is IdxDynamicField.Action -> {
                val actionBinding = binding.dynamicContainer.inflateBinding(FormActionPrimaryBinding::inflate)
                actionBinding.button.text = label
                // set the onclick function of the IDX field as listener
                actionBinding.button.setOnClickListener { onClick() }
                actionBinding.root
            }
            // render radio groups for authenticator selection
            is IdxDynamicField.Options -> {
                fun showSelectedContent(group: RadioGroup) {
                    for (view in group) {
                        val tagOption = view.getTag(R.id.option) as? IdxDynamicField.Options.Option?
                        if (tagOption != null) {
                            val nestedContentView = view.getTag(R.id.nested_content) as View
                            nestedContentView.visibility = if (tagOption == option) View.VISIBLE else View.GONE
                        }
                    }
                }

                val optionsBinding =
                    binding.dynamicContainer.inflateBinding(FormOptionsBinding::inflate)
                optionsBinding.labelTextView.text = label
                optionsBinding.labelTextView.visibility = if (label == null) View.GONE else View.VISIBLE
                for (option in options) {
                    val optionBinding = optionsBinding.radioGroup.inflateBinding(
                        FormOptionBinding::inflate,
                        attachToParent = true
                    )
                    optionBinding.radioButton.id = View.generateViewId()
                    optionBinding.radioButton.text = option.label
                    optionBinding.radioButton.setTag(R.id.option, option)
                    val nestedContentBinding = optionsBinding.radioGroup.inflateBinding(
                        FormOptionNestedBinding::inflate, attachToParent = true
                    )
                    optionBinding.radioButton.setTag(R.id.nested_content, nestedContentBinding.root)
                    for (field in option.fields) {
                        nestedContentBinding.nestedContent.addView(field.createView())
                    }
                }
                optionsBinding.radioGroup.setOnCheckedChangeListener { group, checkedId ->
                    val radioButton = group.findViewById<View>(checkedId)
                    option = radioButton.getTag(R.id.option) as IdxDynamicField.Options.Option?
                    showSelectedContent(group)
                }

                showSelectedContent(optionsBinding.radioGroup)
                optionsBinding.root
            }
            // render image for authenticator QR code
            is IdxDynamicField.Image -> {
                val imageBinding = binding.dynamicContainer.inflateBinding(FormImageBinding::inflate)
                imageBinding.labelTextView.text = label
                imageBinding.imageView.setImageBitmap(bitmap)
                if (sharedSecret != null) {
                    imageBinding.sharedSecretText.text = sharedSecret
                }
                imageBinding.root
            }
        }
    }

    private fun showError(error: Any) {
        val msg = when (error) {
            is String -> error
            is Int -> applicationContext.getString(error)
            else -> ""
        }
        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
    }
}


/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}