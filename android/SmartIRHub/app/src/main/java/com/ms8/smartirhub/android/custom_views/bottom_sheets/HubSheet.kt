package com.ms8.smartirhub.android.custom_views.bottom_sheets

import android.content.Context
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.learn_signal.HubCardListAdapter

class HubSheet(context: Context) : SimpleListDescSheet(context) {

    fun onPause() {
        binding?.sheetList?.adapter?.let {
            (it as HubCardListAdapter).listen(false)
        }
    }

    fun onResume() {
        binding?.btnSaveCommand?.text = context.getString(R.string.select)
        binding?.sheetList?.adapter?.let {
            (it as HubCardListAdapter).listen(true)
        }

    }

}