package com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview;


import androidx.recyclerview.widget.RecyclerView;

public abstract class AGVRecyclerViewAdapter<VH extends RecyclerView.ViewHolder>
    extends RecyclerView.Adapter<VH> {
  public abstract AsymmetricItem getItem(int position);
}
