package com.ms8.irsmarthub.utils

import android.content.Context
import com.wajahatkarim3.easyvalidation.core.Validator
import com.wajahatkarim3.easyvalidation.core.rules.BaseRule
import com.wajahatkarim3.easyvalidation.core.view_ktx.validator

object MyValidators {
    fun PasswordValidator(str : String, context: Context? = null) = str.validator()
        .nonEmpty()
        .atleastOneLowerCase()
        .atleastOneNumber()
        .atleastOneUpperCase()
        .minLength(MIN_PASSWORD_LENGTH)
        .maxLength(MAX_PASSWORD_LEGNTH)

    fun EmailValidator(str: String) = str.validator()
        .validEmail()

    fun UsernameValidator(str: String) = str.validator()
        .nonEmpty()
        .noSpecialCharacters()
        .minLength(MIN_USERNAME_LENGTH)
        .maxLength(MAX_USERNAME_LENGTH)

    fun String.SignalNameValidator() = validator()
        .nonEmpty()
        .addRule(NoSpecialCharacterAllowSpaceAndUnderscoreRule())

    fun String.ButtonNameValidator() = validator()
        .nonEmpty()
        .addRule(NoSpecialCharacterAllowSpaceAndUnderscoreRule())

    fun String.RemoteNameValidator() = validator()
        .nonEmpty()
        .addRule(NoSpecialCharacterAllowSpaceAndUnderscoreRule())
        .minLength(1)
        .maxLength(MAX_REMOTE_NAME_LENGTH)


    const val MIN_REMOTE_NAME_LENGTH = 2
    const val MAX_REMOTE_NAME_LENGTH = 31
    const val MAX_USERNAME_LENGTH = 15
    const val MIN_USERNAME_LENGTH = 2
    const val MIN_PASSWORD_LENGTH = 5
    const val MAX_PASSWORD_LEGNTH = 50
}

/**
 * Returns true if text contain no special characters
 *
 * @author Wajahat Karim
 */
class NoSpecialCharacterAllowSpaceAndUnderscoreRule : BaseRule
{
    override fun validate(text: String): Boolean {
        if (text.isEmpty())
            return false

        return Validator(text).regex("[A-Za-z0-9/ /_]+").check()
    }

    override fun getErrorMessage(): String = "Should not contain any special characters"
}