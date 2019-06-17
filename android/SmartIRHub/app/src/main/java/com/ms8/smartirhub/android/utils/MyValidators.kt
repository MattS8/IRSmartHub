package com.ms8.smartirhub.android.utils

import com.wajahatkarim3.easyvalidation.core.view_ktx.validator

object MyValidators {
    fun PasswordValidator(str : String) = str.validator()
        .nonEmpty()
        .atleastOneLowerCase()
        .atleastOneNumber()
        .atleastOneUpperCase()
        .minLength(5)

    fun EmailValidator(str: String) = str.validator()
        .validEmail()

    fun UsernameValidator(str: String) = str.validator()
        .nonEmpty()
        .noSpecialCharacters()
        .minLength(5)
        .maxLength(15)

    fun SignalNameValidator(str: String) = str.validator()
        .nonEmpty()
        .noSpecialCharacters()
}