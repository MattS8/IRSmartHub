package com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview;

import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;

interface AGVBaseAdapter<T extends RecyclerView.ViewHolder> {
  int getActualItemCount();
  AsymmetricItem getItem(int position);
  void notifyDataSetChanged();
  int getItemViewType(int actualIndex);
  AsymmetricViewHolder<T> onCreateAsymmetricViewHolder(int position, ViewGroup parent, int viewType);
  void onBindAsymmetricViewHolder(AsymmetricViewHolder<T> holder, ViewGroup parent, int position);
}
