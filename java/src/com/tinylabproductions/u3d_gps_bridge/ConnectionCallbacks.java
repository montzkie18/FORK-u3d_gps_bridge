package com.tinylabproductions.u3d_gps_bridge;

import android.*;
import android.R;

public interface ConnectionCallbacks {
  public void onConnected();

  public void onDisconnected();

  public void onSignIn();

  public void onSignInFailed();

  public void onConnectionFailed(int errorCode);

	public void onAuthorizationSuccess(String authorizationCode);

	public void onAuthorizationFailed();
}
