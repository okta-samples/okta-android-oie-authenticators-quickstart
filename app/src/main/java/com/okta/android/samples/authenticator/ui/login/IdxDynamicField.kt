package com.okta.android.samples.authenticator.ui.login

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.okta.idx.kotlin.dto.IdxRemediation

sealed class IdxDynamicField {
    data class Text(
        val label: String,
        val isRequired: Boolean,
        val isSecure: Boolean,
        val errorMessage: String?,
        private val valueUpdater: (String) -> Unit
    ) : IdxDynamicField() {
        private val _errorsLiveData = MutableLiveData(errorMessage ?: "")
        val errorsLiveData: LiveData<String> = _errorsLiveData

        var value: String = ""
            set(value) {
                field = value
                valueUpdater(value)
            }

        override fun validate(): Boolean {
            return if (isRequired) {
                if (value.isNotEmpty()) {
                    _errorsLiveData.value = ""
                    true
                } else {
                    _errorsLiveData.value = "Field is required."
                    false
                }
            } else {
                true
            }
        }
    }

    data class Options(
        val label: String?,
        val options: List<Option>,
        val isRequired: Boolean,
        val errorMessage: String?,
        private val valueUpdater: (IdxRemediation.Form.Field?) -> Unit,
    ) : IdxDynamicField() {
        data class Option(
            private val field: IdxRemediation.Form.Field,
            val label: String?,
            val fields: List<IdxDynamicField>,
        ) {
            fun update(valueUpdater: (IdxRemediation.Form.Field?) -> Unit) {
                valueUpdater(field)
            }
        }

        var option: Option? = null
            set(value) {
                field = value
                value?.update(valueUpdater) ?: valueUpdater(null)
            }
    }

    data class Action(
        val label: String,
        val onClick: (context: Context) -> Unit
    ) : IdxDynamicField()

    data class Image(
        val label: String,
        val bitmap: Bitmap,
        val sharedSecret: String?,
    ) : IdxDynamicField()

    open fun validate(): Boolean {
        return true
    }
}
