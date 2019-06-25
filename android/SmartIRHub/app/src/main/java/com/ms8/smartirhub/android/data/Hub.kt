package com.ms8.smartirhub.android.data

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
class Hub {
    var name = ""
    var owner = ""
    var ownerUsername = ""

    @Exclude
    var uid = ""

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