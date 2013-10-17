package com.tinylabproductions.u3d_gps_bridge;

import com.unity3d.player.UnityPlayer;

/**
 * Created by raneiromontalbo on 10/16/13.
 */
public class UnityConnectionCallbacks implements ConnectionCallbacks {
	private String unityGameObjectName;

	public UnityConnectionCallbacks(String gameObjectName) {
		unityGameObjectName = gameObjectName;
	}

	public void onConnected() {
		UnityPlayer.UnitySendMessage(unityGameObjectName, "onConnected", "");
	}

	public void onDisconnected() {
		UnityPlayer.UnitySendMessage(unityGameObjectName, "onDisconnected", "");
	}

	public void onSignIn() {
		UnityPlayer.UnitySendMessage(unityGameObjectName, "onSignIn", "");
	}

	public void onSignInFailed() {
		UnityPlayer.UnitySendMessage(unityGameObjectName, "onSignInFailed", "");
	}

	public void onConnectionFailed(int errorCode) {
		UnityPlayer.UnitySendMessage(
			unityGameObjectName,
			"onConnectionFailed",
			String.format("%d", errorCode)
		);
	}
}
