package com.ms8.smartirhub.android.utils

import com.wajahatkarim3.easyvalidation.core.Validator
import com.wajahatkarim3.easyvalidation.core.rules.BaseRule
import com.wajahatkarim3.easyvalidation.core.view_ktx.validator

@Suppress("FunctionName")
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

    fun String.SignalNameValidator() = validator()
        .nonEmpty()
        .addRule(NoSpecialCharacterAllowSpaceAndUnderscoreRule())

    fun String.ButtonNameValidator() = validator()
        .nonEmpty()
        .addRule(NoSpecialCharacterAllowSpaceAndUnderscoreRule())
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