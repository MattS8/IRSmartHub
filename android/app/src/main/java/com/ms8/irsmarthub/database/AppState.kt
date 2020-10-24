package com.ms8.irsmarthub.database

import androidx.databinding.ObservableField
import com.ms8.irsmarthub.firebase.HubException
import com.ms8.irsmarthub.models.IrSignal

object AppState {
    var hubUID = "11360452"

    val errorData = ErrorData()

    val pairedSignal : ObservableField<IrSignal?> = ObservableField()

    data class ErrorData (
        var pairSignalError : ObservableField<HubException?> = ObservableField(),
        var saveSignalError : ObservableField<Exception?> = ObservableField()
    )
}