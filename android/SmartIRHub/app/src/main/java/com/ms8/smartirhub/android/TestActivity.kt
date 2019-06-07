package com.ms8.smartirhub.android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.databinding.DataBindingUtil
import android.net.ConnectivityManager
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ms8.smartirhub.android.data.User
import com.ms8.smartirhub.android.databinding.TestActivityBinding
import com.ms8.smartirhub.android.firebase.FirebaseAuthActions
import com.ms8.smartirhub.android.firebase.FirestoreActions

class TestActivity : AppCompatActivity() {
    lateinit var binding: TestActivityBinding


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
        binding.btnTestMakeUser.setOnClickListener { testMakeUser() }
    }

    private fun testMakeUser() {
        val user = User(FirebaseAuth.getInstance().currentUser!!.uid)
        FirebaseFirestore.getInstance().collection("users").document("testUser008").set(user)
            .addOnCompleteListener {
                Log.d("T#", "done adding new user... success? ${it.isSuccessful}")
            }
    }

    private fun connectToIRHub() {
        val networkSSID = "SMART-IR-DEBUG_001"
        val conf = WifiConfiguration().apply {
            SSID = "\"" + networkSSID + "\""
            allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
        }

        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiManager.addNetwork(conf)


        wifiManager.configuredNetworks.forEach { network ->
            network?.SSID.let { SSID ->
                if (SSID == ("\"" + networkSSID + "\"")) {
                    wifiManager.disconnect()
                    wifiManager.enableNetwork(network.networkId, true)
                    wifiManager.reconnect()

                    return@forEach
                }
            }
        }

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
        FirestoreActions.createNewUser("matts8")
            .addOnSuccessListener { Log.d("TEST####", "Successfully created new account data!") }
            .addOnFailureListener {e -> Log.d("TEST#####", "Failed to create account data... ${e::class.java}  (${e.message}) ($e)") }
    }

    private fun testCreateUser() {
        Log.d("TEST####", "Creating account with email: matthew.steinhardt@gmail.com password: F4d29095dc")
        FirebaseAuthActions.createAccount("matthew.steinhardt@gmail.com", "F4d29095dc")
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