package com.ms8.smartirhub.android

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.view.MenuItem
import android.support.design.widget.NavigationView
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import com.google.firebase.auth.FirebaseAuth
import com.ms8.smartirhub.android.databinding.ActivityMainBinding
import com.ms8.smartirhub.android.firebase.FirebaseAuthActions

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    lateinit var binding : ActivityMainBinding


    val navItemSelectedListener = NavigationView.OnNavigationItemSelectedListener {item ->
        when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) = super.onCreate(savedInstanceState).also {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setSupportActionBar(binding.toolbar)

        // Run FirebaseAuth initialization procedure
        FirebaseAuthActions.init(this)

        // Setup ActionBar
        val toggle = ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close)
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Bind Sliding Nav Bar
        binding.userActionsView.setNavigationItemSelectedListener(this)

        // Bind Bottom Nav Bar
        binding.bottomNav.setOnNavigationItemSelectedListener {item ->
            when (item.itemId) {
                binding.bottomNav.selectedItemId -> {}
                 else -> {
                     // TODO load fragment
                 }
            }
            return@setOnNavigationItemSelectedListener true
        }

    }

    override fun onBackPressed() {
        when (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            true -> binding.drawerLayout.closeDrawer(GravityCompat.START)
            false -> super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }


    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                // Handle the camera action
            }
            R.id.nav_gallery -> {

            }
            R.id.nav_slideshow -> {

            }
            R.id.nav_tools -> {

            }
            R.id.nav_share -> {

            }
            R.id.nav_send -> {

            }
            else -> Log.e(TAG, "Unknown item selected (${item.itemId}")
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    companion object {
        const val TAG = "MainActivity"
    }
}
