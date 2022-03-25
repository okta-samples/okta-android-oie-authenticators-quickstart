package com.okta.android.samples.authenticator.ui.login

import com.okta.android.samples.authenticator.ui.loggedin.LoggedInUserView
import com.okta.idx.kotlin.dto.TokenResponse

/**
 * Authentication result : success (user details) or error message.
 */
data class LoginResult(
    val success: LoggedInUserView? = null,
    val error: Any? = null,
    val dynamicFields: List<IdxDynamicField> = emptyList(),
)