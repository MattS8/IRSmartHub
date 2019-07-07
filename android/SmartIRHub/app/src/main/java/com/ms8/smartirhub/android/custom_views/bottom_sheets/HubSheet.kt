package com.ms8.smartirhub.android.custom_views.bottom_sheets

import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.learn_signal.HubCardListAdapter

class HubSheet : SimpleListDescSheet() {

    override fun onPause() {
        binding?.sheetList?.adapter?.let {
            (it as HubCardListAdapter).listen(false)
        }
        super.onPause()
    }

    override fun onResume() {
        binding?.btnSaveCommand?.text = getString(R.string.select)
        binding?.sheetList?.adapter?.let {
            (it as HubCardListAdapter).listen(true)
        }
        super.onResume()
    }

}