package com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview;

import android.os.Parcelable;

public interface AsymmetricItem extends Parcelable {
  int getColumnSpan();
  int getRowSpan();
}
