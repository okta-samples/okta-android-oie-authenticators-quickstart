package com.okta.android.samples.authenticator.ui.login

/**
 * Data validation state of the login form.
 */
data class LoginFormState(
    val usernameError: Int? = null,
    val passwordError: Int? = null,
    val loading: Boolean? = false,
    val isDataValid: Boolean = false,
)