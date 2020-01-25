package com.ms8.irsmarthub.user.models

import android.util.Log
import com.google.firebase.auth.FirebaseAuth

data class User(
    var defaultHub: String = "",
    var favRemote: String = "",
    var uid: String = "",
    var groups: ArrayList<String> = ArrayList(),
    var username: String = "",
    var hubs: ArrayList<String> = ArrayList(),
    var irSignals: ArrayList<String> = ArrayList(),
    var remotes: ArrayList<String> = ArrayList()
) {
    override fun toString(): String {
        return "USER: uid = ${uid}, defaultHub = $defaultHub, groups (size) = ${groups.size}, username = ${username}, " +
                "hubs (size) = ${hubs.size}, irSignals (size) = ${irSignals.size}, remotes (size) = ${remotes.size}"
    }

    fun toFirebaseObject() : Map<String, Any?> {
        return HashMap<String, Any?>()
            .apply {
                put("uid", uid)
                put("favRemote", favRemote)
                put("defaultHub", defaultHub)
                put("hubs", ArrayList<String>()
                    .apply {
                        addAll(hubs)
                    })
                put("irSignals", ArrayList<String>()
                    .apply {
                        addAll(irSignals)
                    })
                put("remotes", ArrayList<String>()
                    .apply {
                        addAll(remotes)
                    })
            }
    }

    companion object {
        const val TAG = "User"

        fun fromFirebaseObject(userMap: Map<String, Any?>, username: String): User {
            val newUser = User()

            newUser.username = username
            newUser.uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            if (userMap.containsKey("defaultHub"))
                newUser.defaultHub = userMap["defaultHub"] as String
            if (userMap.containsKey("favRemote"))
                newUser.favRemote = userMap["favRemote"] as String
            if (userMap.containsKey("hubs"))
                try {
                    newUser.hubs.clear()
                    @Suppress("UNCHECKED_CAST")
                    newUser.hubs.addAll(userMap["hubs"] as Collection<String>)
                } catch (e : Exception) { Log.e(TAG, "$e") }
            if (userMap.containsKey("irSignals"))
                try {
                    newUser.irSignals.clear()
                    @Suppress("UNCHECKED_CAST")
                    newUser.irSignals.addAll(userMap["irSignals"] as Collection<String>)
                } catch (e : Exception) { Log.e(TAG, "$e") }
            if (userMap.containsKey("remotes"))
                try {
                    newUser.remotes.clear()
                    @Suppress("UNCHECKED_CAST")
                    newUser.remotes.addAll(userMap["remotes"] as Collection<String>)
                } catch (e : Exception) { Log.e(TAG, "$e") }

            return newUser
        }
    }
}