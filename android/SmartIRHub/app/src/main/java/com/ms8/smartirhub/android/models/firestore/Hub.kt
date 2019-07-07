package com.ms8.smartirhub.android.models.firestore

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
class Hub {
    var name            : String = ""
    var owner           : String = ""
    var ownerUsername   : String = ""

    @Exclude
    var uid             : String = ""


    override fun equals(other: Any?): Boolean {
        return other != null && other is Hub && uid == other.uid
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + owner.hashCode()
        result = 31 * result + uid.hashCode()
        return result
    }
}