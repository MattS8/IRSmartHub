package com.ms8.smartirhub.android.main_view.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.main_view.MainViewActivity
import com.ms8.smartirhub.android.main_view.MainViewActivity.Companion.LayoutState
import com.ms8.smartirhub.android.utils.extensions.getGenericComingSoonFlashbar

class MainViewFAB(context: Context, attrs: AttributeSet) : FloatingActionButton(context, attrs) {

/*
----------------------------------------------
    State Variables
----------------------------------------------
*/
    var layoutState : LayoutState? = null
    set(value) {
        field = value
        applyLayoutState()
    }

    var isListeningForSaveRemoteConfirmation : Boolean = false
    set(value) {
        field = value
        applyLayoutState()
    }


    fun applyLayoutState() {
        when (layoutState) {
            LayoutState.REMOTES_FAV ->
            {
                if (AppState.tempData.tempRemoteProfile.uid == "") {
                    // show 'create remote' icon
                    setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_new_remote_icon))
                    imageTintList = ContextCompat.getColorStateList(context, R.color.black)

                    // set background tint
                    backgroundTintList = ContextCompat.getColorStateList(context, R.color.white)

                    // set click listener to 'create remote'
                    setOnClickListener{(context as MainViewActivity).createRemote() }

                } else {
                    // show 'edit remote' icon
                    setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_mode_edit_black_24dp))
                    imageTintList = ContextCompat.getColorStateList(context, R.color.black)

                    // set background tint
                    backgroundTintList = ContextCompat.getColorStateList(context, R.color.colorFAB_Editing)

                    // set click listener to 'edit remote'
                    setOnClickListener { (context as MainViewActivity).editRemote() }
                }
            }
            LayoutState.REMOTES_FAV_EDITING ->
            {
                if (isListeningForSaveRemoteConfirmation) {
                    // show 'saving' icon
                    val animatedDrawable = AnimatedVectorDrawableCompat.create(context, R.drawable.edit_to_save)
                        ?.apply {
                            registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                                override fun onAnimationEnd(drawable: Drawable?) {
                                    val savingDrawable = AnimatedVectorDrawableCompat.create(context, R.drawable.remote_saving)
                                        ?.apply {
                                            registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                                                override fun onAnimationEnd(drawable: Drawable?) {
                                                    post { start() }
                                                }
                                            })
                                        }
                                    if (layoutState == LayoutState.REMOTES_FAV_EDITING && isListeningForSaveRemoteConfirmation) {
                                        setImageDrawable(savingDrawable)
                                        imageTintList = ContextCompat.getColorStateList(context, R.color.white)
                                        savingDrawable?.start()
                                    }
                                }
                            })
                        }
                    setImageDrawable(animatedDrawable)
                    animatedDrawable?.start()

                    // remove click listener
                    setOnClickListener {  }
                } else {
                    // show 'save edits' icon
                    setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_done_green_24dp))
                    imageTintList = ContextCompat.getColorStateList(context, R.color.white)

                    // set background tint
                    backgroundTintList = ContextCompat.getColorStateList(context, R.color.colorFAB_Done)

                    // set click listener to 'save remote'
                    setOnClickListener { (context as MainViewActivity).saveRemote() }
                }
            }
            LayoutState.REMOTES_ALL ->
            {
                // show 'create remote' icon
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_new_remote_icon))
                imageTintList = ContextCompat.getColorStateList(context, R.color.black)

                // set background tint
                backgroundTintList = ContextCompat.getColorStateList(context, R.color.white)

                // set click listener to 'create remote' AND page to VP position 0
                setOnClickListener {
                    (context as MainViewActivity).let {
                        it.switchInnerPage(0)
                        it.createRemote()
                    }
                }
            }
            LayoutState.DEVICES_HUBS ->
            {
                // show 'add hub' icon
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.nav_new_hub))
                imageTintList = ContextCompat.getColorStateList(context, R.color.black)

                // set background tint
                backgroundTintList = ContextCompat.getColorStateList(context, R.color.white)

                //todo set click listener to 'add hub'
                setOnClickListener {
                    (context as MainViewActivity).getGenericComingSoonFlashbar().build().show()
                }
            }
            LayoutState.DEVICES_ALL ->
            {
                // show 'add hub' icon
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.nav_new_device))
                imageTintList = ContextCompat.getColorStateList(context, R.color.black)

                // set background tint
                backgroundTintList = ContextCompat.getColorStateList(context, R.color.white)

                //todo set click listener to 'add hub'
                setOnClickListener {
                    (context as MainViewActivity).getGenericComingSoonFlashbar().build().show()
                }
            }
        }
    }
}