package com.okta.android.samples.authenticator.ui

import android.view.LayoutInflater
import android.view.ViewGroup

/**
 * Utility to Inflate the given binding.
 */
fun <B> ViewGroup.inflateBinding(
    bindingFactory: (
        inflater: LayoutInflater,
        container: ViewGroup?,
        attachToParent: Boolean
    ) -> B,
    attachToParent: Boolean = false
): B {
    val inflater = LayoutInflater.from(context)
    return bindingFactory(inflater, this, attachToParent)
}