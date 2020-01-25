package com.ms8.irsmarthub.main_menu.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ms8.irsmarthub.main_menu.MainMenuAdapter.Companion.LayoutState
import com.ms8.irsmarthub.R
import com.ms8.irsmarthub.database.AppState.tempData
import com.ms8.irsmarthub.main_menu.MainActivity
import com.ms8.irsmarthub.utils.getGenericComingSoonFlashbar


class MainFAB(context: Context, attrs: AttributeSet) : FloatingActionButton(context, attrs) {

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
                if (tempData.tempRemote.uid.get() ?: "" == "") {
                    // show 'create remote' icon
                    setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_new_remote))
                    imageTintList = ContextCompat.getColorStateList(context, android.R.color.black)

                    // set background tint
                    backgroundTintList = ContextCompat.getColorStateList(context, R.color.colorBgFAB)

                    // set click listener to 'create remote'
                    setOnClickListener{ tempData.tempRemote.copyFrom(null, true) }

                } else {
                    // show 'edit remote' icon
                    setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_mode_edit_white_24dp))
                    imageTintList = ContextCompat.getColorStateList(context, android.R.color.black)

                    // set background tint
                    backgroundTintList = ContextCompat.getColorStateList(context, R.color.colorBgFABEditMode)

                    // set click listener to 'edit remote'
                    setOnClickListener { tempData.tempRemote.inEditMode.set(true) }
                }
            }
            LayoutState.REMOTES_FAV_EDITING ->
            {
                if (isListeningForSaveRemoteConfirmation) {
                    // show 'saving' icon
                    val animatedDrawable = AnimatedVectorDrawableCompat.create(context, R.drawable.av_edit_to_save)
                        ?.apply {
                            registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                                override fun onAnimationEnd(drawable: Drawable?) {
                                    val savingDrawable = AnimatedVectorDrawableCompat.create(context, R.drawable.av_remote_saving)
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
                    setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_done_white_24dp))
                    imageTintList = ContextCompat.getColorStateList(context, R.color.colorFABIconEditDone)

                    // set background tint
                    backgroundTintList = ContextCompat.getColorStateList(context, R.color.colorBgFABDoneEditing)

                    // set click listener to 'save remote'
                    setOnClickListener { (context as MainActivity).saveRemote() }
                }
            }
            LayoutState.REMOTES_ALL ->
            {
                // show 'create remote' icon
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_new_remote))
                imageTintList = ContextCompat.getColorStateList(context, android.R.color.black)

                // set background tint
                backgroundTintList = ContextCompat.getColorStateList(context, R.color.white)

                // set click listener to 'create remote' AND page to VP position 0
                setOnClickListener {
                    (context as MainActivity).let {
                        it.switchInnerPage(0)
                        it.createRemote()
                    }
                }
            }
            LayoutState.DEVICES_HUBS ->
            {
                // show 'add hub' icon
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_new_hub_black_24dp))
                imageTintList = ContextCompat.getColorStateList(context, android.R.color.black)

                // set background tint
                backgroundTintList = ContextCompat.getColorStateList(context, R.color.white)

                //todo set click listener to 'add hub'
                setOnClickListener {
                    (context as MainActivity).getGenericComingSoonFlashbar().build().show()
                }
            }
            LayoutState.DEVICES_ALL ->
            {
                // show 'add hub' icon
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_new_device_black_24dp))
                imageTintList = ContextCompat.getColorStateList(context, android.R.color.black)

                // set background tint
                backgroundTintList = ContextCompat.getColorStateList(context, R.color.white)

                //todo set click listener to 'add hub'
                setOnClickListener {
                    (context as MainActivity).getGenericComingSoonFlashbar().build().show()
                }
            }
        }
    }
}