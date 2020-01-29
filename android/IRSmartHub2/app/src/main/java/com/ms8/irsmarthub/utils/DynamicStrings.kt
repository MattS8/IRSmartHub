package com.ms8.irsmarthub.utils

import android.content.Context
import com.ms8.irsmarthub.R

object DynamicStrings {
    fun Context.getUsernameErrorString() : String {
        return "${getString(R.string.err_username_p1)} ${MyValidators.MIN_USERNAME_LENGTH} - ${MyValidators.MAX_USERNAME_LENGTH} ${getString(
            R.string.err_username_p2)}"
    }

    fun Context.getPasswordErrorString() : String {
        return "${getString(R.string.err_password_p1)} ${MyValidators.MIN_PASSWORD_LENGTH} - ${MyValidators.MAX_PASSWORD_LEGNTH} ${getString(
            R.string.err_username_p2)}"
    }

}