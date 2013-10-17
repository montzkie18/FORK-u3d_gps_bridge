package com.tinylabproductions.u3d_gps_bridge;

import android.*;
import android.R;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.games.GamesClient;
import com.unity3d.player.UnityPlayer;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class U3DGamesClient {
  public static final String TAG = "U3DGamesClient";
  public static final String GPGS = "Google Play Game Services";

  // An arbitrary integer that you define as the request code.
  private static final int REQUEST_LEADERBOARD = 0;
  private static final int REQUEST_ACHIEVEMENTS = 1;
	private static final int REQUEST_AUTHORIZATION = 10;

  private final int playServicesSupported;
  private final GamesClient client;
  private final Activity activity;
  public final ConnectionCallbacks connectionCallbacks;
  private final long id;

  private final GooglePlayServicesClient.ConnectionCallbacks
    gpscCallbacks = new GooglePlayServicesClient.ConnectionCallbacks() {
    @Override
    public void onConnected(Bundle bundle) {
      Log.d(TAG, "Connected to " + GPGS + ".");
      connectionCallbacks.onConnected();
    }

    @Override
    public void onDisconnected() {
      Log.d(TAG, "Disconnected from " + GPGS + ".");
      connectionCallbacks.onDisconnected();
    }
  };

  private final GooglePlayServicesClient.OnConnectionFailedListener
    connectionFailedListener =
    new GooglePlayServicesClient.OnConnectionFailedListener() {
      @Override
      public void onConnectionFailed(final ConnectionResult connectionResult) {
        switch (connectionResult.getErrorCode()) {
          case ConnectionResult.NETWORK_ERROR:
            Log.i(TAG, "Network error to " + GPGS + ". Reconnecting.");
            client.reconnect();
            break;
          case ConnectionResult.SIGN_IN_REQUIRED:
            Log.d(TAG, "Not signed in to " + GPGS + ". Signing in.");

            StaticData.results.put(id, connectionResult);

            Intent intent = new Intent(activity, CallbackActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            intent.putExtra(StaticData.KEY, id);

            Log.d(TAG, "Invoking Intent: " + intent);
            activity.startActivity(intent);
            break;
          default:
            Log.w(
              TAG, "Connection to Google Play Game Services failed. Reason: " +
              connectionResult
            );
            connectionCallbacks.
              onConnectionFailed(connectionResult.getErrorCode());
        }
      }
    };

  public U3DGamesClient(String gameObjectName) {
    activity = UnityPlayer.currentActivity;
    this.connectionCallbacks = new UnityConnectionCallbacks(gameObjectName);

    playServicesSupported =
      GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);

    if (playServicesSupported == ConnectionResult.SUCCESS) {
      GamesClient.Builder builder = new GamesClient.Builder(
        activity, gpscCallbacks, connectionFailedListener
      );
      client = builder.create();
    } else {
      client = null;
    }

    id = System.currentTimeMillis();
    StaticData.clients.put(id, this);
  }

  public void connect() {
    Log.d(TAG, "connect()");
    client.connect();
  }

  public void reconnect() {
    Log.d(TAG, "reconnect()");
    client.reconnect();
  }

  public void disconnect() {
    Log.d(TAG, "disconnect()");
    client.disconnect();
  }

  public String getAccountId() {
    return client.getCurrentPlayerId();
  }

  public String getAccountName() {
    return client.getCurrentAccountName();
  }

  public String getAuthorizationCode() {
    Context context = activity.getApplicationContext();
    Bundle appActivities = new Bundle();
    appActivities.putString(GoogleAuthUtil.KEY_REQUEST_VISIBLE_ACTIVITIES, "");
    List<String> scopes = Arrays.asList(new String[]{
      "https://www.googleapis.com/auth/games",
      "https://www.googleapis.com/auth/userinfo.profile",
      "https://www.googleapis.com/auth/userinfo.email"
    });
    String appId = getResourceString("app_id");
    String clientId = String.format("%s.apps.googleusercontent.com", appId);
    String scope = String.format("oauth2:server:client_id:%s:api_scope:%s", clientId, TextUtils.join(" ", scopes));
    String code = "";
    try {
      code = GoogleAuthUtil.getToken(
	            context,                          // Context context
              client.getCurrentAccountName(),   // String accountName
              scope,                            // String scope
              appActivities                     // Bundle bundle
      );
    } catch (IOException transientEx) {
      // network or server error, the call is expected to succeed if you try again later.
      // Don't attempt to call again immediately - the request is likely to
      // fail, you'll hit quotas or back-off.
      Log.d(TAG, transientEx.getMessage());
    } catch (UserRecoverableAuthException e) {
      // Recover
      Log.d(TAG, e.getMessage());
	    activity.startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
    } catch (Exception e) {
      // fail silently
      Log.d(TAG, e.getMessage());
    }
    return code;
  }

  public boolean isSupported() {
    return playServicesSupported == ConnectionResult.SUCCESS;
  }

  public boolean isServiceMissing() {
    return playServicesSupported == ConnectionResult.SERVICE_MISSING;
  }

  public boolean isServiceVersionUpdateRequired() {
    return playServicesSupported ==
      ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED;
  }

  public boolean isServiceDisabled() {
    return playServicesSupported == ConnectionResult.SERVICE_DISABLED;
  }

  public boolean isServiceInvalid() {
    return playServicesSupported == ConnectionResult.SERVICE_INVALID;
  }

  public boolean isConnected() {
    return client != null && client.isConnected();
  }

  public void submitScore(String leaderboardId, long score) {
    Log.d(TAG, String.format(
      "Submitting score %d to leaderboard %s", score, leaderboardId
    ));
    assertConnectivity();
    client.submitScore(leaderboardId, score);
  }

  public void unlockAchievement(String achievementId) {
    Log.d(TAG, String.format("Unlocking achievement %s.", achievementId));
    assertConnectivity();
    client.unlockAchievement(achievementId);
  }

  public boolean showAchievements() {
    if (! tryConnectivity()) {
      Log.i(TAG, String.format(
        "Cannot show achievements because %s is not connected.",
        GPGS
      ));
      return false;
    }

    Log.d(TAG, "Showing achievements.");
    activity.startActivityForResult(
      client.getAchievementsIntent(),
      REQUEST_ACHIEVEMENTS
    );
    return true;
  }

  public boolean showLeaderboard(String leaderboardId) {
    if (! tryConnectivity()) {
      Log.i(TAG, String.format(
        "Cannot show leaderboard %s, because %s is not connected.",
        leaderboardId, GPGS
      ));
      return false;
    }

    Log.d(TAG, "Starting activity to show leaderboard " + leaderboardId);
    activity.startActivityForResult(
      client.getLeaderboardIntent(leaderboardId),
      REQUEST_LEADERBOARD
    );
    return true;
  }

  private boolean tryConnectivity() {
    if (! isConnected()) {
      if (! client.isConnecting()) connect();
      return false;
    }

    return true;
  }

  private void assertConnectivity() {
    if (!isConnected())
      throw new IllegalStateException(
        "You need to be connected to perform this operation!"
      );
  }

    @SuppressLint("NewApi")
  private String getResourceString(String name) {
    Context context = activity.getApplicationContext();
    int nameResourceID = 0;
    if(context != null) {
      ApplicationInfo appInfo = context.getApplicationInfo();
      if(appInfo != null) {
        nameResourceID = context.getResources().getIdentifier(name, "string", appInfo.packageName);
      }
    }
    if (nameResourceID == 0) {
      throw new IllegalArgumentException("No resource string found with name " + name);
    }
    return context.getString(nameResourceID);
  }
}
