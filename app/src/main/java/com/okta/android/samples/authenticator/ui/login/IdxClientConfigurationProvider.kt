package com.okta.android.samples.authenticator.ui.login

import com.okta.android.samples.authenticator.BuildConfig
import com.okta.idx.kotlin.client.IdxClientConfiguration
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * Provides Okta org configurations for IDX client.
 */
internal object IdxClientConfigurationProvider {
    fun get(): IdxClientConfiguration {
        return IdxClientConfiguration(
            issuer = BuildConfig.ISSUER.toHttpUrl(),
            clientId = BuildConfig.CLIENT_ID,
            scopes = setOf("openid", "email", "profile", "offline_access"),
            redirectUri = BuildConfig.REDIRECT_URI,
        )
    }
}
