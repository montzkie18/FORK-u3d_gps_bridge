package com.tinylabproductions.u3d_gps_bridge;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Window;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.unity3d.player.UnityPlayerActivity;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by raneiromontalbo on 10/17/13.
 */
public class AuthorizationActivity extends Activity {

	private static final int REQUEST_AUTHORIZATION = 10;
	private U3DGamesClient client;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		retrieveIntentData();
		getAndUseAuthTokenInAsyncTask();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		retrieveIntentData();
		if(requestCode == REQUEST_AUTHORIZATION) {
			if(resultCode == Activity.RESULT_OK) {
				getAndUseAuthTokenInAsyncTask();
			}else{
				client.connectionCallbacks.onAuthorizationFailed();
				finish();
			}
		}
	}

	private void retrieveIntentData() {
		Intent intent = getIntent();
		long key = intent.getLongExtra(StaticData.KEY, 0);
		if (key == 0)
			throw new IllegalStateException("Cannot get key from intent " + intent);

		client = StaticData.clients.get(key);

		Log.d(U3DGamesClient.TAG, String.format(
				"Retrieved data from intent[key: %d, client: %s]",
				key,
				client
		));
		Log.d(U3DGamesClient.TAG, StaticData.asString());
	}

	private void getAndUseAuthTokenBlocking() {
		Context context = getApplicationContext();
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

		try {
			// Retrieve a token for the given account and scope. It will always return either
			// a non-empty String or throw an exception.
			final String token = GoogleAuthUtil.getToken(
					context,                          // Context context
					client.getAccountName(),          // String accountName
					scope,                            // String scope
					appActivities                     // Bundle bundle
			);
			// Do work with token.
			client.connectionCallbacks.onAuthorizationSuccess(token);
			finish();
		} catch (GooglePlayServicesAvailabilityException playEx) {
			Dialog alert = GooglePlayServicesUtil.getErrorDialog(
					playEx.getConnectionStatusCode(),
					this,
					REQUEST_AUTHORIZATION);
			alert.show();
			client.connectionCallbacks.onAuthorizationFailed();
			finish();
		} catch (UserRecoverableAuthException userAuthEx) {
			// Start the user recoverable action using the intent returned by
			// getIntent()
			startActivityForResult(
					userAuthEx.getIntent(),
					REQUEST_AUTHORIZATION);
		} catch (IOException transientEx) {
			// network or server error, the call is expected to succeed if you try again later.
			// Don't attempt to call again immediately - the request is likely to
			// fail, you'll hit quotas or back-off.
			client.connectionCallbacks.onAuthorizationFailed();
			finish();
		} catch (GoogleAuthException authEx) {
				// Failure. The call is not expected to ever succeed so it should not be
				// retried.
			client.connectionCallbacks.onAuthorizationFailed();
			finish();
		}
	}

	private void getAndUseAuthTokenInAsyncTask() {
		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				getAndUseAuthTokenBlocking();
				return (Void)null;
			}
		};
		task.execute((Void)null);
	}

	@SuppressLint("NewApi")
	private String getResourceString(String name) {
		Context context = getApplicationContext();
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
