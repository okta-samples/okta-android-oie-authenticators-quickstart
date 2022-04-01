/*
 * Copyright 2021-Present Okta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.okta.android.samples.authenticator.ui.loggedin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.okta.android.samples.authenticator.BuildConfig
import com.okta.android.samples.authenticator.ui.login.IdxClientConfigurationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.Request
import java.io.IOException

internal class LoggedInUserViewModel : ViewModel() {
    private val _logoutStateLiveData = MutableLiveData<LogoutState>(LogoutState.Idle)
    val logoutStateLiveData: LiveData<LogoutState> = _logoutStateLiveData

    private val _userInfoLiveData = MutableLiveData<Map<String, String>>(emptyMap())
    val userInfoLiveData: LiveData<Map<String, String>> = _userInfoLiveData

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getClaims()?.let { _userInfoLiveData.postValue(it) }
            } catch (e: IOException) {
                error("User info request failed. $e")
            }
        }
    }

    fun logout() {
        _logoutStateLiveData.value = LogoutState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val refreshToken = LoggedInUserModel.tokens.refreshToken
                if (refreshToken != null) {
                    // Revoking the refresh token revokes both!
                    revokeToken("refresh_token", refreshToken)
                } else {
                    revokeToken("access_token", LoggedInUserModel.tokens.accessToken)
                }

                _logoutStateLiveData.postValue(LogoutState.Success)
            } catch (e: Exception) {
                _logoutStateLiveData.postValue(LogoutState.Failed)
            }
        }
    }

    // Get claims using the logged in users accessToken.
    private fun getClaims(): Map<String, String>? {
        val accessToken = LoggedInUserModel.tokens.accessToken
        val request = Request.Builder()
            .addHeader("authorization", "Bearer $accessToken")
            .url("${BuildConfig.ISSUER}/v1/userinfo")
            .build()
        val response =
            IdxClientConfigurationProvider.get().okHttpCallFactory.newCall(request).execute()
        if (response.isSuccessful) {
            val parser = ObjectMapper().createParser(response.body?.byteStream())
            val json = parser.readValueAsTree<JsonNode>()
            val map = mutableMapOf<String, String>()
            for (entry in json.fields()) {
                map[entry.key] = entry.value.asText()
            }
            return map
        }

        return null
    }

    private fun revokeToken(tokenType: String, token: String) {
        val formBody = FormBody.Builder()
            .add("client_id", BuildConfig.CLIENT_ID)
            .add("token_type_hint", tokenType)
            .add("token", token)
            .build()

        val request = Request.Builder()
            .url("${BuildConfig.ISSUER}/v1/revoke")
            .post(formBody)
            .build()

        val response =
            IdxClientConfigurationProvider.get().okHttpCallFactory.newCall(request).execute()
        println("Revoke Token Response: $response")
    }

    fun acknowledgeLogoutSuccess() {
        _logoutStateLiveData.value = LogoutState.Idle
    }

    sealed class LogoutState {
        object Idle : LogoutState()
        object Loading : LogoutState()
        object Success : LogoutState()
        object Failed : LogoutState()
    }
}
