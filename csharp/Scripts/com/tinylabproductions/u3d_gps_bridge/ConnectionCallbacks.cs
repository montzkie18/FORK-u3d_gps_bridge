﻿#if UNITY_ANDROID
using System;
using UnityEngine;

namespace com.tinylabproductions.u3d_gps_bridge {
	/**
	 * Connection callbacks class.
	 * 
	 * When Google Play Game Services events occur, these delegates are called
	 * 
	 * Be warned, that they might happen on any thread, so if you want to do 
	 * something with Unity (for example access PlayerPrefs), you need to do that on
	 * a main thread.
	 * 
	 * In practice you should use a tool like Loom (http://unitygems.com/threads/)
	 * like this:
	 * 
	 *    client.callbacks.OnConnected += onConnection;
	 * 
	 *    protected void onConnection() {
	 *      Debug.Log("Connected to highscores service.");
	 *      Loom.QueueOnMainThread(() => {
	 *        PlayerPrefs.SetInt("foo", 10);
	 *      });
	 *    }
	 *    
	 * Be sure to initialize Loom in one of your scripts, in Awake() or Start() methods:
	 * 
	 *    Debug.Log(Loom.Current); // Initialize Loom
	 **/
	public class ConnectionCallbacks : MonoBehaviour {
		//    public ConnectionCallbacks() :
		//    base("com.tinylabproductions.u3d_gps_bridge.ConnectionCallbacks")
		//    {}

		public static readonly string PARENT_GAMEOBJECT_NAME = "GooglePlayGameServices";

		private static ConnectionCallbacks instance;
		public static ConnectionCallbacks GetInstance() {
			if(instance == null) {
				instance = GameObject.FindObjectOfType(typeof(ConnectionCallbacks)) as ConnectionCallbacks;
				if(instance == null) {
					GameObject go = new GameObject(PARENT_GAMEOBJECT_NAME);
					instance = go.AddComponent<ConnectionCallbacks>();
					DontDestroyOnLoad(go);
				}
			}
			return instance;
		}

		// Called when client connects to google play services.
		public Action OnConnected = delegate {};

		// Called when client disconnects from google play services.
		public Action OnDisconnected = delegate {};

		/**
	     * Called when user signs in to google play services (after connection).
	     * 
	     * Usually you want to connect again after sign-in:
	     * 
	     *   client.callbacks.OnSignIn += client.connect;
	     **/
		public Action OnSignIn = delegate {};

		// Called when user sign in to google play services fails (after connection).
		public Action OnSignInFailed = delegate {};

		// Called when connection to google play services fails.
		// Params: errorCode - possible values at http://developer.android.com/reference/com/google/android/gms/common/ConnectionResult.html
		public Action<int> OnConnectionFailed = delegate {};

		/** Java interface methods **/

		void onConnected(string response) {
			OnConnected.Invoke();
			Debug.Log("[Google Play Game Services] onConnected " + response);
		}

		void onDisconnected(string response) {
			OnDisconnected.Invoke();
			Debug.Log("[Google Play Game Services] onDisconnected " + response);
		}

		void onSignIn(string response) {
			OnSignIn.Invoke();
			Debug.Log("[Google Play Game Services] onSignIn " + response);
		}

		void onSignInFailed(string response) {
			OnSignInFailed.Invoke();
			Debug.Log("[Google Play Game Services] onSignInFailed " + response);
		}

		void onConnectionFailed(string errorCode) {
			OnConnectionFailed.Invoke(Convert.ToInt32(errorCode));
			Debug.Log("[Google Play Game Services] onConnectionFailed " + errorCode);
		}
	}
}
#endif
