package com.ms8.smartirhub.android

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
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

}