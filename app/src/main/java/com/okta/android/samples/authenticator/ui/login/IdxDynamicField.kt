package com.okta.android.samples.authenticator.ui.login

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.okta.idx.kotlin.dto.IdxRemediation

/**
 * Data model classes representing `IdxRemediation` as a View Model.
 * Fields normally sent in remediations are: Text fields, password fields, QR Code images, checkbox, options, labels and action buttons.
 */
sealed class IdxDynamicField {
    /**
     * `DynamicAuthField.Text` are displayed as a `TextInputLayout`, and represents a `IdxRemediation.Form.Field.type` `string` fields.
     * Supports inline validation.
     */
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

    /**
     * `DynamicAuthField.Options` is displayed as a `RadioButton`, and represents a `IdxRemediation.Form.Field` with `options` fields.
     * The class can hold nested options and can be used to render radio button groups, usually used for authenticator selection and so on.
     * Selection state is maintained on the `IdxRemediation.Form.Field` instance.
     */
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

    /**
     * `DynamicAuthField.Action` is displayed as a `Button`, and typically represents a call submitting an `IdxRemediation` to `IdxClient.proceed`.
     */
    data class Action(
        val label: String,
        val onClick: () -> Unit
    ) : IdxDynamicField()

    /**
     * `DynamicAuthField.Image` is displayed as an `ImageView`, and represents an `IdxRemediation.authenticators` capability of `IdxTotpCapability`.
     * Typically used for bitmap QR code data sent for an authenticator enrollment in remediation
     */
    data class Image(
        val label: String,
        val bitmap: Bitmap,
        val sharedSecret: String?,
    ) : IdxDynamicField()

    open fun validate(): Boolean {
        return true
    }
}
