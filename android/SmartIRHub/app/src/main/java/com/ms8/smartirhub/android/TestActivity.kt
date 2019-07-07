package com.ms8.smartirhub.android

import android.Manifest
import android.content.pm.PackageManager
import androidx.databinding.DataBindingUtil
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import androidx.databinding.ObservableArrayMap
import androidx.databinding.ObservableMap
import com.google.firebase.auth.FirebaseAuth
import com.ms8.smartirhub.android.databinding.TestActivityBinding
import com.ms8.smartirhub.android.firebase.AuthActions
import com.ms8.smartirhub.android.firebase.FirestoreActions

class TestActivity : AppCompatActivity() {
    lateinit var binding: TestActivityBinding

    private val observableMap = ObservableArrayMap<String, Int>()
    private val callback = object : ObservableMap.OnMapChangedCallback<ObservableArrayMap<String, Int>, String, Int>() {
        override fun onMapChanged(sender: ObservableArrayMap<String, Int>?, key: String?) {
            if (sender != null) {
                var strKeys = "["
                sender.keys.forEach { k -> strKeys += "$k, " }
                strKeys += "]"
                var strValues = "["
                sender.values.forEach { k -> strValues += "$k, " }
                strValues += "]"
                Log.d("T#", "Sender has keys: $strKeys and values: $strValues (key was: $key)")
            } else {
                Log.d("T#", "Sender was null... (key was: $key)")
            }
        }
    }
    private var step = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.test__activity)

        if (FirebaseAuth.getInstance().currentUser == null)
            FirebaseAuth.getInstance().signInAnonymously()

        binding.btnTestCreateUser.setOnClickListener { testCreateUser() }
        binding.btnTestGetUser.setOnClickListener { testGetUser() }
        binding.btnTestCreateUserDB.setOnClickListener { testCreateUserDB() }
        binding.btnTestGetUserFromUID.setOnClickListener { testGetUserQuery() }
        binding.btnViewToken.setOnClickListener { printUserToken() }
        binding.btnConnectToIRHub.setOnClickListener { testConnectToIRHub() }
        binding.btnTestArrayMap.setOnClickListener { testObservableMapCallback() }

        observableMap.addOnMapChangedCallback(callback)
    }


    fun testObservableMapCallback() {
        when (step) {
            0 -> {
                Log.d("T#", "Adding 0..3 to map...")
                observableMap["Zero"] = 0
                observableMap["One"] = 1
                observableMap["Two"] = 2
                observableMap["Three"] = 3
                step++
            }
            1 -> {
                Log.d("T#", "removing 2 from map...")
                observableMap.remove("Two")
                step++
            }
            2 -> {
                Log.d("T#", " replacing 1 in map with 11...")
                observableMap["One"] = 11
                step++
            }
            else -> {
                Log.d("T#", "Clearing map...")
                observableMap.clear()
                step = 0
            }
        }
    }

    private fun connectToIRHub() {
//        val networkSSID = "SMART-IR-DEBUG_001"
//        val conf = WifiConfiguration().apply {
//            SSID = "\"" + networkSSID + "\""
//            allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
//        }
//
//        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
//        wifiManager.addNetwork(conf)
//
//
//        wifiManager.configuredNetworks.forEach { network ->
//            network?.SSID.let { SSID ->
//                if (SSID == ("\"" + networkSSID + "\"")) {
//                    wifiManager.disconnect()
//                    wifiManager.enableNetwork(network.networkId, true)
//                    wifiManager.reconnect()
//
//                    return@forEach
//                }
//            }
//        }

        // TODO something...
    }

    private fun testConnectToIRHub() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            REQ_WIFI_STATE)
    }

    private fun printUserToken() {
        FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.addOnCompleteListener { task ->
            when {
                task.isSuccessful -> {Log.d("TEST####", "User Token: ${task.result?.token}")}
                else -> {Log.d("TEST####", "Failed to get user token...")}
            }
        }
    }

    private fun testCreateUserDB() {
        Log.d("TEST####", "Creating user data with username: matts88")
        FirestoreActions.addUser("matts8")
            .addOnSuccessListener { Log.d("TEST####", "Successfully created new account data!") }
            .addOnFailureListener {e -> Log.d("TEST#####", "Failed to create account data... ${e::class.java}  (${e.message}) ($e)") }
    }

    private fun testCreateUser() {
        Log.d("TEST####", "Creating account with email: matthew.steinhardt@gmail.com password: F4d29095dc")
        AuthActions.createAccount("matthew.steinhardt@gmail.com", "F4d29095dc")
            .addOnSuccessListener {
                Log.d("TEST####", "Success!")
                testCreateUserDB()
            }
            .addOnFailureListener {e -> Log.d("TEST####", "Failure... $e)") }
    }

    private fun testGetUser() {
        Log.d("TEST####", "Fetching user data with username: matts88")
        FirestoreActions.getUser("matts8889")
            .addOnSuccessListener { Log.d("TEST####", "Got user data! ${it.data}") }
            .addOnFailureListener { e -> Log.d("TEST####", "Failed to get user data... $e") }
    }

    private fun testGetUserQuery() {
        Log.d("TEST####", "Testing getUser query on curent signed in user (${FirebaseAuth.getInstance().currentUser}")
        FirestoreActions.getUserFromUID()
            .addOnSuccessListener {snapshots ->
                when {
                    snapshots.isEmpty -> {Log.d("TEST####", "No user object found for ${FirebaseAuth.getInstance().currentUser}")}
                    else -> {
                        Log.d("TEST####", "Found the following users under ${FirebaseAuth.getInstance().currentUser}: \n\t")
                        snapshots.forEach {
                            Log.d("TEST####", "${it.data}\n\t")
                        }
                    }
                }
            }
            .addOnFailureListener {e -> Log.d("TEST####", "Query failed... $e")}
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQ_WIFI_STATE -> {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    Log.d("TEST####", "Got wifi permission")
                    connectToIRHub()
                } else {
                    Log.d("TEST####", "Wifi permission DENIED")
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    companion object {
        const val REQ_WIFI_STATE = 8
    }
}