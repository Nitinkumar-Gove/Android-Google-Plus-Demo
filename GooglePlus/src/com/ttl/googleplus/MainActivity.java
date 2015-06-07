package com.ttl.googleplus;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.plus.PlusShare;

public class MainActivity extends Activity 
{

	private static final String TAG = "PlayHelloActivity";
     private static final String SCOPE = "oauth2:https://www.googleapis.com/auth/userinfo.profile";
 
    public static final String EXTRA_ACCOUNTNAME = "extra_accountname";

    public static MainActivity m;
    
    private TextView mOut;
    private String mEmail;

    static final int REQUEST_CODE_PICK_ACCOUNT = 1000;
    static final int REQUEST_CODE_RECOVER_FROM_AUTH_ERROR = 1001;
    static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR = 1002;

    public static ImageView imgProfilePic;
    
    public static String TYPE_KEY = "type_key";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		
		
		mOut=(TextView)findViewById(R.id.mAuthError);
		
		m=MainActivity.this;
		
		Button b=(Button)findViewById(R.id.sign_in_button);
		b.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				// ### initiate login
				pickUserAccount();
			}
		});
		
		Button b2=(Button)findViewById(R.id.share_button);
		b2.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				// Launch the Google+ share dialog with attribution to your app.
			      Intent shareIntent = new PlusShare.Builder(MainActivity.this)
			          .setType("text/plain")
			          .setText("Checkout Tata Motors Commercial...")
			          .setContentUrl(Uri.parse("https://www.youtube.com/watch?v=sD_ixDRrX3s"))
			          .getIntent();

			      startActivityForResult(shareIntent, 0);

			}
		});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	// ### over riding base methods

	/**
     * This method is a hook for background threads and async tasks that need to provide the
     * user a response UI when an exception occurs.
     */
    public void handleException(final Exception e) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (e instanceof GooglePlayServicesAvailabilityException) {
                    // The Google Play services APK is old, disabled, or not present.
                    // Show a dialog created by Google Play services that allows
                    // the user to update the APK
                    int statusCode = ((GooglePlayServicesAvailabilityException)e)
                            .getConnectionStatusCode();
                    Dialog dialog = GooglePlayServicesUtil.getErrorDialog(statusCode,
                    		MainActivity.this,
                            REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                    dialog.show();
                } else if (e instanceof UserRecoverableAuthException) {
                    // Unable to authenticate, such as when the user has not yet granted
                    // the app access to the account, but the user can fix this.
                    // Forward the user to an activity in Google Play services.
                    Intent intent = ((UserRecoverableAuthException)e).getIntent();
                    startActivityForResult(intent,
                            REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                }
            }
        });
    }
    
    /** Starts an activity in Google Play Services so the user can pick an account */
    private void pickUserAccount() 
    {
        String[] accountTypes = new String[]{"com.google"};
        Intent intent = AccountPicker.newChooseAccountIntent(null, null,
                accountTypes, false, null, null, null, null);  // ### alwaysPromptForAccount  - false
        startActivityForResult(intent, REQUEST_CODE_PICK_ACCOUNT);
    }
    
    // ### handle account selection
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_ACCOUNT) 
        {
            if (resultCode == RESULT_OK)
            {
                mEmail = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                getUsername();
                Toast.makeText(getApplicationContext(), mEmail + " selected ", Toast.LENGTH_SHORT).show();
            } 
            else if (resultCode == RESULT_CANCELED) 
            {
                Toast.makeText(this, "You must pick an account", Toast.LENGTH_SHORT).show();
            }
        } 
        else if ((requestCode == REQUEST_CODE_RECOVER_FROM_AUTH_ERROR ||
                requestCode == REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR)
                && resultCode == RESULT_OK) 
        {
            handleAuthorizeResult(resultCode, data);
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    
    /** Attempt to get the user name. If the email address isn't known yet,
     * then call pickUserAccount() method so the user can pick an account.
     */
    private void getUsername() {
        if (mEmail == null) {
            pickUserAccount();
        } else {
            if (isDeviceOnline()) 
            {
                getTask(MainActivity.this, mEmail, SCOPE).execute();
            } 
            else
            {
                Toast.makeText(this, "No network connection available", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /** Checks whether the device currently has a network connection */
    private boolean isDeviceOnline()
    {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) 
        {
            return true;
        }
        return false;
    }
    
    private void handleAuthorizeResult(int resultCode, Intent data) {
        if (data == null) 
        {
            show("Unknown error, click the button again");
            return;
        }
        if (resultCode == RESULT_OK)
        {
            Log.i(TAG, "Retrying");
            getTask(this, mEmail, SCOPE).execute();
            //Toast.makeText(getApplicationContext(), "re trying...",Toast.LENGTH_SHORT).show();
            return;
        }
        if (resultCode == RESULT_CANCELED) 
        {
            show("User rejected authorization.");
            return;
        }
        show("Unknown error, click the button again");
    }

    /**
     * This method is a hook for background threads and async tasks that need to update the UI.
     * It does this by launching a runnable under the UI thread.
     */
    public void show(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
            	Toast.makeText(getApplicationContext(), message,Toast.LENGTH_SHORT).show();
            	mOut.setText(message);
            }
        });
    }

    /**
     * Note: This approach is for demo purposes only. Clients would normally not get tokens in the
     * background from a Foreground activity.
     */
    private AbstractGetNameTask getTask(
            MainActivity activity, String email, String scope) {
       
                return new GetNameInForeground(activity, email, scope);
           
        }
   
}
