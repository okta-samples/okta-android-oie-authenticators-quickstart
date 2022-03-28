package com.okta.android.samples.authenticator.ui.login

import com.okta.android.samples.authenticator.ui.loggedin.LoggedInUserView

/**
 * Authentication result : success (user details) or error message or dynamic fields for next step
 */
data class LoginResult(
    val success: LoggedInUserView? = null,
    val error: Any? = null,
    val dynamicFields: List<IdxDynamicField> = emptyList(),
)