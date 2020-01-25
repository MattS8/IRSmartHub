package com.ms8.irsmarthub.main_menu.fragments

class MyRemotesFragment: MainFragment() {


    override fun newInstance(): MainFragment { return MyRemotesFragment() }

    companion object {
        const val recyclerViewTag = "AllRemotesRV"
    }
}