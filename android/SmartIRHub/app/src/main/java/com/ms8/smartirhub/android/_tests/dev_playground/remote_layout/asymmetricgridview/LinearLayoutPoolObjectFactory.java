package com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview;

import android.content.Context;
import android.widget.LinearLayout;

public class LinearLayoutPoolObjectFactory implements PoolObjectFactory<LinearLayout> {

  private final Context context;

  public LinearLayoutPoolObjectFactory(final Context context) {
    this.context = context;
  }

  @Override
  public LinearLayout createObject() {
    return new LinearLayout(context, null);
  }
}
