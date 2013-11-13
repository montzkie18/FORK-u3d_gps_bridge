package com.tinylabproductions.u3d_gps_bridge;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

/**
 * Created by raneiromontalbo on 10/24/13.
 */
public class GameBoardActivity extends Activity {
	public static final int REQUEST_ACHIEVEMENTS = 20;
	public static final int REQUEST_LEADERBOARD = 21;

	private U3DGamesClient client;
	private int requestType;
	private String leaderboardId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		retrieveIntentData();
		startRequest();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		finish();
	}

	private void retrieveIntentData() {
		Intent intent = getIntent();
		long key = intent.getLongExtra(StaticData.KEY, 0);

		requestType = intent.getIntExtra(StaticData.TOKEN_TYPE, REQUEST_ACHIEVEMENTS);
		leaderboardId = intent.getStringExtra(StaticData.LEADERBOARD_ID);

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

	private void startRequest() {
		switch(requestType){
			case REQUEST_ACHIEVEMENTS:
				startActivityForResult(
						client.getAchievementsIntent(),
						REQUEST_ACHIEVEMENTS
				);
				break;
			case REQUEST_LEADERBOARD:
				startActivityForResult(
						client.getLeaderboardIntent(leaderboardId),
						REQUEST_LEADERBOARD
				);
				break;
		}
	}
}
