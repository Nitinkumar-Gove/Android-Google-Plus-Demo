/*
 * Copyright 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ttl.googleplus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.plus.PlusShare;

/**
 * Display personalized greeting. This class contains boilerplate code to consume the token but
 * isn't integral to getting the tokens.
 */
public abstract class AbstractGetNameTask extends AsyncTask<Void, Void, Void>{
    private static final String TAG = "TokenInfoTask";
    private static final String NAME_KEY = "given_name";
    protected MainActivity mActivity;

    protected String mScope;
    protected String mEmail;

    AbstractGetNameTask(MainActivity activity, String email, String scope)
    {
        this.mActivity = activity;
        this.mScope = scope;
        this.mEmail = email;
    }

    @Override
    protected Void doInBackground(Void... params) {
      try {
        fetchNameFromProfileServer();
      } catch (IOException ex) {
        onError("Following Error occured, please try again. " + ex.getMessage(), ex);
      } catch (JSONException e) {
        onError("Bad response: " + e.getMessage(), e);
      }
      return null;
    }

    protected void onError(String msg, Exception e) {
        if (e != null) {
          Log.e(TAG, "Exception: ", e);
        }
        mActivity.show(msg);  // will be run in UI thread
    }

    /**
     * Get a authentication token if one is not available. If the error is not recoverable then
     * it displays the error message on parent activity.
     */
    protected abstract String fetchToken() throws IOException;

    /**
     * Contacts the user info server to get the profile of the user and extracts the first name
     * of the user from the profile. In order to authenticate with the user info server the method
     * first fetches an access token from Google Play services.
     * @throws IOException if communication with user info server failed.
     * @throws JSONException if the response from the server could not be parsed.
     */
    private void fetchNameFromProfileServer() throws IOException, JSONException 
    {
        String token = fetchToken();
        if (token == null) {
          // error has already been handled in fetchToken()
          return;
        }
        
        URL url = new URL("https://www.googleapis.com/oauth2/v1/userinfo?access_token=" + token);
      
        
        
     
     
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        int sc = con.getResponseCode();
        if (sc == 200)
        {
          InputStream is = con.getInputStream();
           
          String name = getFirstName(readResponse(is));
          Log.d("profile data",readResponse(is));
          
          mActivity.show("Hello " + name + "!");
          is.close();
          return;
        } 
        else if (sc == 401)
        {
            GoogleAuthUtil.invalidateToken(mActivity, token);
            onError("Server auth error, please try again.", null);
            Log.i(TAG, "Server auth error: " + readResponse(con.getErrorStream()));
            return;
        } 
        else
        {
          onError("Server returned the following error code: " + sc, null);
          return;
        }
    }

    /**
     * Reads the response from the input stream and returns it as a string.
     */
    private static String readResponse(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] data = new byte[2048];
        int len = 0;
        while ((len = is.read(data, 0, data.length)) >= 0) {
            bos.write(data, 0, len);
        }
        
        Log.d("User Profile Data",new String(bos.toByteArray(), "UTF-8"));
        
        return new String(bos.toByteArray(), "UTF-8");
    }

    
    /**
     * Parses the response and returns the first name of the user.
     * @throws JSONException if the response is not JSON or if first name does not exist in response
     */
    private String getFirstName(String jsonResponse) throws JSONException {
     
      JSONObject profile = new JSONObject(jsonResponse);
      
      Log.d("User Profile Information ***>>", 
    		  		  profile.getString("id") + "\n" +
    				  profile.getString("name") + "\n" +
    				  profile.getString("gender") + "\n" +
    				  profile.getString("given_name") + "\n" + 
    				  profile.getString("link") + "\n" +
    				  profile.getString("locale")+"\n"+
    				  profile.getString("family_name")+"\n"+
    				  profile.getString("picture") + "\n" +
    				  mEmail);
      
     
      new DownloadImageTask(MainActivity.imgProfilePic)
                  .execute(profile.getString("picture"));   // ### Fetch user image from profile
      
      
      Intent shareIntent = new PlusShare.Builder(MainActivity.m)
      .setType("text/plain")
      .setText("Checkout Tata Motors Commercial...")
      .setContentUrl(Uri.parse("https://www.youtube.com/watch?v=sD_ixDRrX3s"))
      .getIntent();

      MainActivity.m.startActivityForResult(shareIntent, 0);

      return profile.getString(NAME_KEY);
     
    }
    
    // ### Fetch Profile Image
    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }
    
}
