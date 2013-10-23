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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by raneiromontalbo on 10/17/13.
 */
public class AuthorizationActivity extends Activity {

	public static final int REQUEST_AUTH_CODE = 10;
	public static final int REQUEST_ID_TOKEN = 11;
	private U3DGamesClient client;

	private int requestType;
	private String authCode;
	private String idToken;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		retrieveIntentData();
		getTokenInAsyncTask(requestType);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		retrieveIntentData();

		if(resultCode != Activity.RESULT_OK) {
			failRequest();
			return;
		}

		// after permissions have been given by the user,
		// try getting the same token again
		getTokenInAsyncTask(requestCode);
	}

	private void retrieveIntentData() {
		Intent intent = getIntent();
		long key = intent.getLongExtra(StaticData.KEY, 0);
		requestType = intent.getIntExtra(StaticData.TOKEN_TYPE, REQUEST_AUTH_CODE);

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

	private void receiveToken(String token, int requestCode) {
		client.connectionCallbacks.onAuthorizationSuccess(token);
		finish();
	}

	private void failRequest() {
		client.connectionCallbacks.onAuthorizationFailed();
		finish();
	}

	private void getAuthCode() {
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
		String scope = String.format(
				"oauth2:server:client_id:%s:api_scope:%s",
				clientId,
				TextUtils.join(" ", scopes)
		);
		getTokenBlocking(scope, appActivities, REQUEST_AUTH_CODE);
	}

	private void getIdToken() {
		Context context = getApplicationContext();
		String appId = getResourceString("app_id");
		String clientId = String.format("%s.apps.googleusercontent.com", appId);
		String scope = String.format("audience:server:client_id:%s",clientId);
		getTokenBlocking(scope, null, REQUEST_ID_TOKEN);
	}

	private void getTokenBlocking(String scope, Bundle appActivities, int requestCode) {
		Context context = getApplicationContext();
		String accountName = client.getAccountName();

		try {
			// Retrieve a token for the given account and scope. It will always return either
			// a non-empty String or throw an exception.
			if(appActivities == null) {
				final String token = GoogleAuthUtil.getToken(context, accountName, scope);
				receiveToken(token, requestCode);
			}else{
				final String code = GoogleAuthUtil.getToken(context, accountName, scope, appActivities);
				receiveToken(code, requestCode);
			}
		} catch (GooglePlayServicesAvailabilityException playEx) {
			Dialog alert = GooglePlayServicesUtil.getErrorDialog(
					playEx.getConnectionStatusCode(),
					this,
					requestCode);
			alert.show();
			failRequest();
		} catch (UserRecoverableAuthException userAuthEx) {
			// Start the user recoverable action using the intent returned by
			// getIntent()
			startActivityForResult(userAuthEx.getIntent(), requestCode);
		} catch (IOException transientEx) {
			// network or server error, the call is expected to succeed if you try again later.
			// Don't attempt to call again immediately - the request is likely to
			// fail, you'll hit quotas or back-off.
			failRequest();
		} catch(GoogleAuthException googleAuthEx) {
			failRequest();
		}
	}

	private void getTokenInAsyncTask(final int requestCode) {
		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				switch (requestCode) {
					case REQUEST_AUTH_CODE:
						getAuthCode(); break;
					case REQUEST_ID_TOKEN:
						getIdToken(); break;
				}
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
