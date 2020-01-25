package com.ms8.irsmarthub.database.models

import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableField
import com.ms8.irsmarthub.remote_control.button.models.Button
import com.ms8.irsmarthub.remote_control.remote.models.Remote
import com.ms8.irsmarthub.utils.MyValidators.MAX_REMOTE_NAME_LENGTH
import com.ms8.irsmarthub.utils.NoSpecialCharacterAllowSpaceAndUnderscoreRule
import com.ms8.irsmarthub.utils.showInvalidRemoteNameFlashbar
import com.ms8.irsmarthub.utils.showRemoteNameEmptyFlashbar
import com.wajahatkarim3.easyvalidation.core.view_ktx.validator

class TempRemote(
    var uid: ObservableField<String> = ObservableField(""),
    var name: ObservableField<String> = ObservableField(""),
    var owner: ObservableField<String> = ObservableField(""),
    var ownerUsername: ObservableField<String> = ObservableField(""),
    val buttons: ObservableArrayList<Button> = ObservableArrayList(),
    var inEditMode: ObservableField<Boolean> = ObservableField(false)
) {

    fun copyFrom(remote: Remote?, startInEditMode: Boolean = false) {
        val copiedRemote = remote ?: Remote()
        copiedRemote.let {
            uid.set(it.uid)
            name.set(it.name)
            owner.set(it.owner)
            ownerUsername.set(it.ownerUsername)
            buttons.clear()
            buttons.addAll(it.buttons)
            inEditMode.set(startInEditMode)
        }
    }

    /**
     * Saves remote changes to firebase. If activity is not null, error messages will be
     * displayed via a Flashbar.
     */
    fun saveRemote(activity : AppCompatActivity? = null) : Boolean {
        return when {
            // show error if name is missing
            name.get()?.isEmpty() ?: true ->  {
                activity?.showRemoteNameEmptyFlashbar()
                false
            }

            // show error if name is invalid
            name.get()?.isValidRemoteName()?.not() ?: true -> {
                activity?.showInvalidRemoteNameFlashbar()
                false
            }

            uid.get()?.isEmpty() ?: true -> {
                //todo - create new remote
                //FirestoreActions.addRemote()
                true
            }
            else -> {
                //todo - update existing remote
                //FirestoreActions.updateRemote()
                true
            }
        }
    }

    private fun String?.isValidRemoteName() = this?.validator()
        ?.nonEmpty()
        ?.addRule(NoSpecialCharacterAllowSpaceAndUnderscoreRule())
        ?.minLength(1)
        ?.maxLength(MAX_REMOTE_NAME_LENGTH)
        ?.check() ?: false

/*
----------------------------------------------
    Companion Objects
----------------------------------------------
*/
    companion object {

    }
}
