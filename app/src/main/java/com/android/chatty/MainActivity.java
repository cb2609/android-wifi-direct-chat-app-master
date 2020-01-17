package com.android.chatty;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.chatty.InitThreads.ClientInit;
import com.android.chatty.InitThreads.ServerInit;
import com.android.chatty.Receivers.WifiDirectBroadcastReceiver;
import com.android.chatty.util.ActivityUtilities;

import java.util.ArrayList;

/*
 * This activity is the launcher activity. 
 * Once the connection established, the ChatActivity is launched.
 */
public class MainActivity extends Activity{
	public static final String TAG = "MainActivity";	
	public static final String DEFAULT_CHAT_NAME = "";
	private WifiP2pManager mManager;
	private Channel mChannel;
	private static final int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 1001;
	private WifiDirectBroadcastReceiver mReceiver;
	private IntentFilter mIntentFilter;
	private Button goToChat;
	private ImageView goToSettings;
	private TextView goToSettingsText;
	private TextView setChatNameLabel;
	private EditText setChatName;
	private ImageView disconnect;
	public static String chatName;
	public static ServerInit server;
	WifiManager wifiManager;

	//Getters and Setters
    public WifiP2pManager getmManager() { return mManager; }
	public Channel getmChannel() { return mChannel; }
	public WifiDirectBroadcastReceiver getmReceiver() { return mReceiver; }
	public IntentFilter getmIntentFilter() { return mIntentFilter; }
	public Button getGoToChat(){ return goToChat; }
	public TextView getSetChatNameLabel() { return setChatNameLabel; }
	public ImageView getGoToSettings() { return goToSettings; }
	public EditText getSetChatName() { return setChatName; }
	public TextView getGoToSettingsText() { return goToSettingsText; }
	public ImageView getDisconnect() { return disconnect; }
	
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
			requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);
			//After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method

		}
        //Init the Channel, Intent filter and Broadcast receiver
        init();

        //Button Go to Settings
        goToSettings = findViewById(R.id.goToSettings);
        goToSettings();
        
        //Go to Settings text
        goToSettingsText = findViewById(R.id.textGoToSettings);
        
        //Button Go to Chat
        goToChat = findViewById(R.id.goToChat);
        goToChat();
        
        //Set the chat name
        setChatName = findViewById(R.id.setChatName);
        setChatNameLabel = findViewById(R.id.setChatNameLabel);
        setChatName.setText(loadChatName(this));
        
        //Button Disconnect
        disconnect = findViewById(R.id.disconnect);
        disconnect();
    }
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions,
										   int[] grantResults) {
		if (requestCode == PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION
				&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			// Do something with granted permission
			init();
			exqListener();
		}
	}

    @Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		
		ActivityUtilities.customiseActionBar(this);
	}
    
	@Override
    public void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
        
		mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
					
			@Override
			public void onSuccess() {
				Log.v(TAG, "Discovery process succeeded");
			}
			
			@Override
			public void onFailure(int reason) {
				Log.v(TAG, "Discovery process failed");
			}
		});
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        int idItem = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

	private void exqListener() {

		wifiManager.setWifiEnabled(true);
	}

    public void init(){
    	mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = WifiDirectBroadcastReceiver.createInstance();
        mReceiver.setmManager(mManager);
        mReceiver.setmChannel(mChannel);
        mReceiver.setmActivity(this);
        
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }
    
    public void goToChat(){
    	goToChat.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				if(!setChatName.getText().toString().equals("")){
					//Set the chat name
					saveChatName(MainActivity.this, setChatName.getText().toString());
					chatName = loadChatName(MainActivity.this);
					
					//Start the init process
					if(mReceiver.isGroupeOwner() ==  WifiDirectBroadcastReceiver.IS_OWNER){
						Toast.makeText(MainActivity.this, "I'm the group owner  " + mReceiver.getOwnerAddr().getHostAddress(), Toast.LENGTH_SHORT).show();
						server = new ServerInit();
						server.start();
					}
					else if(mReceiver.isGroupeOwner() ==  WifiDirectBroadcastReceiver.IS_CLIENT){
						Toast.makeText(MainActivity.this, "I'm the client", Toast.LENGTH_SHORT).show();
						ClientInit client = new ClientInit(mReceiver.getOwnerAddr());
						client.start();
					}
					
					//Open the ChatActivity
					Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
					startActivity(intent);
				}
				else{
					Toast.makeText(MainActivity.this, "Please enter a chat name", Toast.LENGTH_SHORT).show();
				}					
			}
		});    	
    }
    
    public void disconnect(){
    	disconnect.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mManager.removeGroup(mChannel, null);
		    	finish();
			}
		});    	
    }
    
    public void goToSettings(){    	
    	goToSettings.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {

				//Open Wifi settings
		        startActivityForResult(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS), 0);
			}
		});    	
    }
    
    //Save the chat name to SharedPreferences
  	public void saveChatName(Context context, String chatName) {
  		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
  		Editor edit = prefs.edit();
  		edit.putString("chatName", chatName);
  		edit.commit();
  	}

  	//Retrieve the chat name from SharedPreferences
  	public static String loadChatName(Context context) {
  		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
  		return prefs.getString("chatName", DEFAULT_CHAT_NAME);
  	}
}