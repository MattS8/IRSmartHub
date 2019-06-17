package com.ms8.smartirhub.android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableMap
import com.google.firebase.auth.FirebaseAuth
import com.mikepenz.materialdrawer.AccountHeaderBuilder
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import com.ms8.smartirhub.android.data.RemoteProfile
import com.ms8.smartirhub.android.database.LocalData
import com.ms8.smartirhub.android.databinding.ActivityMainViewBinding
import com.ms8.smartirhub.android.learn_signal.LSListenActivity
import com.ms8.smartirhub.android.learn_signal.LSWalkthroughActivity
import kotlinx.android.synthetic.main.activity_main_view.*

class MainViewActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainViewBinding
    private lateinit var drawer : Drawer

/* ----------------------------------------------- Database Listeners ----------------------------------------------- */
    private val remoteProfilesListener: ObservableMap.OnMapChangedCallback<out ObservableMap<String, RemoteProfile>, String, RemoteProfile>? = object :
        ObservableMap.OnMapChangedCallback<ObservableMap<String, RemoteProfile>, String, RemoteProfile>() {
        override fun onMapChanged(sender: ObservableMap<String, RemoteProfile>?, key: String?) {

        }
    }
/* ---------------------------------------------- Overridden Functions ---------------------------------------------- */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_view)
        binding.navView.setOnNavigationItemSelectedListener { i -> onNavItemSelected(i) }
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        /* --- Build drawer layout --- */
        val header = AccountHeaderBuilder()
            .withActivity(this)
            .withHeaderBackground(R.drawable.side_nav_bar)
            .addProfiles(
                ProfileDrawerItem()
                    .withName(LocalData.user?.username)
                    .withEmail(FirebaseAuth.getInstance().currentUser?.email)
            )
            .build()
        // Build base drawer
        drawer = DrawerBuilder()
            .withActivity(this)
            .withToolbar(binding.toolbar)
            .withAccountHeader(header)
            .addDrawerItems()
            .withOnDrawerItemClickListener(object : Drawer.OnDrawerItemClickListener {
                override fun onItemClick(view: View?, position: Int, drawerItem: IDrawerItem<*>): Boolean {
                    onDrawerItemClicked(view, position, drawerItem)
                    return false
                }
            })
            .build()



    }

    override fun onPause() {
        super.onPause()
        LocalData.remoteProfiles.removeOnMapChangedCallback(remoteProfilesListener)
    }

    override fun onResume() {
        super.onResume()
        LocalData.remoteProfiles.addOnMapChangedCallback(remoteProfilesListener)
    }

    /* ---------------------------------------------- Nav Drawer Functions ---------------------------------------------- */

    private fun onDrawerItemClicked(view: View?, position: Int, drawerItem: IDrawerItem<*>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /** Load proper fragment page based on item selected */
    private fun onNavItemSelected(item: MenuItem): Boolean {
        Log.d("####Test", "on ${item.itemId} selected")
        return when (item.itemId) {
            R.id.navigation_main_remote -> {
                //TODO Implement remotes page
                false
            }
            R.id.navigation_actions -> {
                Log.d("###Test###", "Starting test of Learning Signal Process")
                startActivityForResult(Intent(this, LSWalkthroughActivity::class.java), REQ_NEW_IR_SIG)
                false
            }
            else -> {
                false
            }
        }
    }

    companion object {
        const val REQ_NEW_IR_SIG = 2
    }
}
