package com.okta.android.samples.authenticator.ui.login

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.okta.android.samples.authenticator.R
import com.okta.android.samples.authenticator.ui.loggedin.LoggedInUserView
import com.okta.idx.kotlin.client.IdxClient
import com.okta.idx.kotlin.client.IdxClientResult
import com.okta.idx.kotlin.dto.*
import com.okta.idx.kotlin.dto.IdxRemediation.Type.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginViewModel : ViewModel() {

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    @Volatile
    private var client: IdxClient? = null
    private var username: String? = null
    private var password: String? = null

    fun login(username: String, password: String) {
        // store the username password
        this.username = username
        this.password = password
        createClient()
    }

    private fun createClient() {
        _loginForm.value = LoginFormState(loading = true)
        viewModelScope.launch {
            // initiate the IDX client and start IDX flow
            when (val clientResult = IdxClient.start(IdxClientConfigurationProvider.get())) {
                is IdxClientResult.Error -> _loginResult.value = LoginResult(error = R.string.client_error_create)
                is IdxClientResult.Success -> {
                    client = clientResult.result
                    // calls the IDX API resume to receive the first IDX response.
                    when (val resumeResult = clientResult.result.resume()) {
                        is IdxClientResult.Error -> _loginResult.value = LoginResult(error = R.string.client_error_resume)
                        is IdxClientResult.Success -> handleResponse(resumeResult.result)
                    }
                }
            }
        }
    }

    private suspend fun handleResponse(response: IdxResponse) {
        // If a response is successful, immediately exchange it for a token and set it on LoggedInUserView
        if (response.isLoginSuccessful) {
            when (val exchangeCodesResult =
                client?.exchangeInteractionCodeForTokens(response.remediations[ISSUE]!!)) {
                is IdxClientResult.Error -> _loginResult.value = LoginResult(error = R.string.client_error_resume)
                is IdxClientResult.Success -> {
                    _loginResult.value = LoginResult(
                        success = LoggedInUserView(tokens = exchangeCodesResult.result)
                    )
                }
                else -> _loginResult.value = LoginResult(error = R.string.unknown_error)
            }
            return
        }

        // Check for messages, such as entering an incorrect code or auth error and abort if there is message.
        if (response.messages.isNotEmpty()) {
            _loginResult.value = LoginResult(error = response.messages.first().message)
            return
        }

        // If no remediations are present, abort the login process and show error
        if (response.remediations.isEmpty()) {
            _loginResult.value = LoginResult(error = R.string.client_error_remediation)
            return
        }

        // Handle the different sign-in steps (remediations) for a policy.
        val remediation = response.remediations.first()

        // check if there is a skip action in the remediations
        val skipRemediation = response.remediations[SKIP]

        when (remediation.type) {
            // Username and password, though password may be separate; see the next case.
            IDENTIFY -> handleIdentify(remediation)
            // Request a password or a passcode, such as one from an Authenticator.
            // Identity-first policies request the password separately.
            CHALLENGE_AUTHENTICATOR -> handleChallenge(remediation)
            // Display a list of authenticators to enroll
            SELECT_AUTHENTICATOR_ENROLL,
                // Select from an enrolled authenticator
            SELECT_AUTHENTICATOR_AUTHENTICATE,
                // Enroll in an Authenticator
            ENROLL_AUTHENTICATOR -> handleAuthenticatorEnrollOrChallenge(
                remediation,
                skipRemediation
            )
            else -> _loginResult.value = LoginResult(error = R.string.client_error_remediation)
        }
    }

    private fun handleIdentify(remediation: IdxRemediation) {
        // Update the values in the remediation object and proceed to the next step in IDX flow
        remediation["identifier"]?.value = username
        remediation["credentials.passcode"]?.value = password
        proceed(remediation)
    }

    private suspend fun handleChallenge(remediation: IdxRemediation) {
        // If no authenticators are found for challenge show error and abort
        if (remediation.authenticators.isEmpty()) {
            _loginResult.value = LoginResult(error = R.string.client_error_authenticator)
            return
        }
        val authenticator = remediation.authenticators.first()

        when (authenticator.type) {
            IdxAuthenticator.Kind.PASSWORD -> {
                // Update the value in the remediation object and proceed to the next step in IDX flow
                remediation["credentials.passcode"]?.value = password
                proceed(remediation)
            }
            IdxAuthenticator.Kind.APP, IdxAuthenticator.Kind.EMAIL -> {
                // get challenge form field from remediation
                handleAuthenticatorEnrollOrChallenge(remediation, null)
            }
            else -> {
                _loginResult.value = LoginResult(error = R.string.client_error_authenticator)
                return
            }
        }
    }

    /**
     * obtain fields, actions and images from remediation and collect as IdxDynamicFields
     */
    private suspend fun handleAuthenticatorEnrollOrChallenge(
        remediation: IdxRemediation,
        skipRemediation: IdxRemediation?
    ) {
        val fields = mutableListOf<IdxDynamicField>()
        fields += asTotpImageDynamicAuthField(remediation)
        for (visibleField in remediation.form.visibleFields) {
            fields += asIdxDynamicFields(visibleField)
        }
        fields += asDynamicAuthFieldActions(remediation)
        if (skipRemediation != null) {
            fields += asDynamicAuthFieldActions(skipRemediation)
        }
        _loginResult.value = LoginResult(dynamicFields = fields)
    }

    /**
     * Proceed to the next step in IDX flow using the current remediation
     */
    private fun proceed(remediation: IdxRemediation) {
        viewModelScope.launch {
            when (val resumeResult = client?.proceed(remediation)) {
                is IdxClientResult.Error -> _loginResult.value = LoginResult(error = R.string.client_error_proceed)
                is IdxClientResult.Success -> handleResponse(resumeResult.result)
                else -> _loginResult.value = LoginResult(error = R.string.unknown_error)
            }
        }
    }

    /**
     * Extract text fields and radio groups from IdxRemediation.form.visibleFields
     */
    private fun asIdxDynamicFields(field: IdxRemediation.Form.Field): List<IdxDynamicField> {
        return when (true) {
            // nested form inside a field
            field.form?.visibleFields?.isNullOrEmpty() == false -> {
                val result = mutableListOf<IdxDynamicField>()
                field.form?.visibleFields?.forEach {
                    result += asIdxDynamicFields(it)
                }
                result
            }
            // options represent multiple choice items like authenticators and can be nested
            field.options?.isNullOrEmpty() == false -> {
                field.options?.let { options ->
                    val transformed = options.map {
                        val fields = it.form?.visibleFields?.flatMap { field -> asIdxDynamicFields(field) } ?: emptyList()
                        IdxDynamicField.Options.Option(it, it.label, fields)
                    }
                    val displayMessages = field.messages.joinToString(separator = "\n") { it.message }
                    listOf(
                        IdxDynamicField.Options(
                            field.label,
                            transformed,
                            field.isRequired,
                            displayMessages
                        ) {
                            field.selectedOption = it
                        })
                } ?: emptyList()
            }
            // simple text field
            field.type == "string" -> {
                val displayMessages = field.messages.joinToString(separator = "\n") { it.message }
                val idxField = IdxDynamicField.Text(field.label ?: "", field.isRequired, field.isSecret, displayMessages) {
                    field.value = it
                }
                (field.value as? String?)?.let {
                    idxField.value = it
                }
                listOf(idxField)
            }
            else -> {
                emptyList()
            }
        }
    }

    /**
     * Extract a bitmap from IdxRemediation.authenticators
     */
    private suspend fun asTotpImageDynamicAuthField(remediation: IdxRemediation): List<IdxDynamicField> {
        val authenticator = remediation.authenticators.firstOrNull { it.capabilities.get<IdxTotpCapability>() != null }
            ?: return emptyList()
        val field = asTotpImageDynamicAuthField(authenticator) ?: return emptyList()
        return listOf(field)
    }

    /**
     * Extract a bitmap from IdxAuthenticator
     */
    private suspend fun asTotpImageDynamicAuthField(authenticator: IdxAuthenticator): IdxDynamicField? {
        val capability = authenticator.capabilities.get<IdxTotpCapability>() ?: return null
        val bitmap = withContext(Dispatchers.Default) {
            capability.asImage()
        } ?: return null
        val label = authenticator.displayName
            ?: "Launch Google Authenticator, tap the \"+\" icon, then select \"Scan a QR code\"."
        return IdxDynamicField.Image(label, bitmap, capability.sharedSecret)
    }

    /**
     * Create actions for IdxRemediations with visibleFields
     */
    private fun asDynamicAuthFieldActions(remediation: IdxRemediation): List<IdxDynamicField> {
        // Don't show action for actions that are pollable without visible fields.
        if (remediation.form.visibleFields.isEmpty() && remediation.capabilities.get<IdxPollRemediationCapability>() != null) {
            return emptyList()
        }

        val title = when (remediation.type) {
            SKIP -> "Skip"
            SELECT_AUTHENTICATOR_AUTHENTICATE, SELECT_AUTHENTICATOR_ENROLL -> "Choose Authenticator"
            else -> "Continue"
        }

        return listOf(IdxDynamicField.Action(title) { proceed(remediation) })
    }

    fun loginDataChanged(username: String, password: String) {
        if (!isUserNameValid(username)) {
            _loginForm.value = LoginFormState(usernameError = R.string.invalid_username)
        } else if (!isPasswordValid(password)) {
            _loginForm.value = LoginFormState(passwordError = R.string.invalid_password)
        } else {
            _loginForm.value = LoginFormState(isDataValid = true)
        }
    }

    // A placeholder username validation check
    private fun isUserNameValid(username: String): Boolean {
        return if (username.contains('@')) {
            Patterns.EMAIL_ADDRESS.matcher(username).matches()
        } else {
            username.isNotBlank()
        }
    }

    // A placeholder password validation check
    private fun isPasswordValid(password: String): Boolean {
        return password.length > 5
    }
}