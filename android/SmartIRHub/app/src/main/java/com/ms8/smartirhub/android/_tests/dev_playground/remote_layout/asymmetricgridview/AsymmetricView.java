package com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview;

import android.view.View;

interface AsymmetricView {
  boolean isDebugging();
  int getNumColumns();
  boolean isAllowReordering();
  void fireOnItemClick(int index, View v);
  boolean fireOnItemLongClick(int index, View v);
  int getColumnWidth();
  int getDividerHeight();
  int getRequestedHorizontalSpacing();
}
