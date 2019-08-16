package com.ms8.smartirhub.android.utils.extensions

import androidx.appcompat.app.AppCompatActivity
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.utils.MyValidators

object DynamicStrings {
    fun AppCompatActivity.getUsernameErrorString() : String {
        return "${getString(R.string.must_be_between)} ${MyValidators.MIN_USERNAME_LENGTH} - ${MyValidators.MAX_USERNAME_LENGTH} ${getString(
            R.string.and_no_characters)}"
    }

    fun AppCompatActivity.getPasswordErrorString() : String {
        return "${getString(R.string.pasasword_length_must_be)} ${MyValidators.MIN_PASSWORD_LENGTH} - ${MyValidators.MAX_PASSWORD_LEGNTH} ${getString(
            R.string.and_no_characters)}"
    }

}