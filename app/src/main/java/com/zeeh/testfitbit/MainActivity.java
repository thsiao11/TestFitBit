package com.zeeh.testfitbit;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private final String LOG_TAG = getClass().getSimpleName();
    private final int REQ_AUTH_CODE = 1;
    private final int REQ_AUTH_NEW_TOKEN = 2;

// Set the two variables below before running this test app
    final private String CLIENT_ID = "Put client_id here";
    final private String CLIENT_SECRET = "Put client secret here";

    final private String AUTHCODE ="code";
    final private String AUTHTOKEN = "token";
    final private String REFRESHTOKEN = "refresh_token";
    final private String ENDUSERID = "user_id";
    final private String TOKENTIME = "token_time";

    final private String AUTHCODE_URL = "https://www.fitbit.com/oauth2/authorize?";
    final private String TOKEN_URL = "https://api.fitbit.com/oauth2/token?";
    final private String USER_REQ_URL = "https://api.fitbit.com/1/user/";
    final private String REFRESH_TOKEN_URL = "https://api.fitbit.com/oauth2/token?";
    final private String REDIRECT_URI = "http%3A%2F%2Ftestcustomurischeme";

    final String GRANT_TYPE_PARAM = "grant_type";
    final String RESPONSE_TYPE_PARAM = "response_type";
    final String CLIENT_ID_PARAM = "client_id";
    final String REDIRECT_URI_PARAM = "redirect_uri";
    final String SCOPE_PARAM = "scope";

    //    variables used by getAuthInfo()
    private String authToken;
    private String refreshToken;
    private Integer authTokenTime;
    private String endUserId;

    private SharedPreferences prefs;

    WebView webView;

    private  String authCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        int testAuth = getAuthInfo();
        if (testAuth == 0) {
            webView = (WebView) findViewById(R.id.auth_webview);
            processCallBackURL();
        } else if (testAuth == 1) {
            new ReqRefreshTokenAsyncTask().execute();
        }
        // start next activity as needed
    }

    int getAuthInfo() {

        // Read the sharedPreference file for authToken, authDateTime, authRefreshToken
        // If sharedPreference file doesn't exist, then what?
        // If authDateTime < currentDateTime + 1 hr, then get new authToken and authRefreshToken

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        authToken = prefs.getString(AUTHTOKEN, null);
        if (authToken != null) {                //check for a valid auth token
            String authTokenTimeStr = prefs.getString(TOKENTIME, null);
            authTokenTime = Math.round(Integer.parseInt(authTokenTimeStr) / 1000);   //need to convert to datetime format
            long currentTime = Math.round(System.currentTimeMillis()/1000);
            if (currentTime < authTokenTime + 3600) {
                return 2;               //existing token is still valid, auth is completed.
            } else
                return 1;               // need to refresh token since current one has expired
        } else {
            return 0;  // Need to start the auth from scratch
        }
    }

    private void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    private String getAuthCode() {
        return this.authCode;
    }

    private void processCallBackURL() {

        Uri builtUri = Uri.parse(AUTHCODE_URL).buildUpon()
            .appendQueryParameter(RESPONSE_TYPE_PARAM, "code")
            .appendQueryParameter(CLIENT_ID_PARAM, CLIENT_ID)
//          .appendQueryParameter(REDIRECT_URI_PARAM, REDIRECT_URI)
            .appendQueryParameter(SCOPE_PARAM, "activity")
            .build();

        String urlStr = builtUri.toString();

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (url.substring(0, 26).compareTo("http://testcustomurischeme") == 0) {
                    setAuthCode(url.split("=")[1]);
                    new ReqTokenAsyncTask(getAuthCode()).execute();
                }
            }

            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.v(LOG_TAG, description + ", " + failingUrl);
                // For now, hide the error displayed in the webview for invalid callback url http://testcustomurischeme
                view.setVisibility(View.INVISIBLE);
            }
        });

        webView.loadUrl(urlStr);
    }

    // class request auth token
    class ReqTokenAsyncTask extends AsyncTask<Void, Void, Void> {

        private String authCode;

        private final String LOG_TAG = getClass().getSimpleName();

        // Declare Constructor
        public ReqTokenAsyncTask(String authCode) {
            this.authCode = authCode;
        }

        @Override
        protected Void doInBackground(Void... param) {
            getAuthToken();
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
//          Send user to new Activity
        }

        private void getAuthToken() {

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Generate a Base64 code
            final String concat = CLIENT_ID+":"+CLIENT_SECRET;
            final String base64AuthCode = Base64.encodeToString(concat.getBytes(), Base64.DEFAULT);

            try {
                Uri builtUri = Uri.parse(TOKEN_URL).buildUpon()
                        .appendQueryParameter(CLIENT_ID_PARAM, CLIENT_ID)
                        .appendQueryParameter(GRANT_TYPE_PARAM, "authorization_code")
//This line is not req'd           .appendQueryParameter(REDIRECT_URI_PARAM, REDIRECT_URI)
                        .appendQueryParameter(AUTHCODE, authCode)
                        .build();

                URL url = new URL(builtUri.toString());

                urlConnection = (HttpURLConnection) url.openConnection();

                // set https to POST
                urlConnection.setDoOutput(true);

                // set htttps two header params
                urlConnection.setRequestProperty("AUTHORIZATION", "Basic " + base64AuthCode);
                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String authLine = reader.readLine();
                Log.v(LOG_TAG, authLine);

// Test completed up to here.
                extractAuthInfo(authLine);

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();

                }
            }

        }

        private void extractAuthInfo(String authline) {

            // the test string below have escape character / inserted by Android studio for the double quote " inside it.
            String testStr = "{\"access_token\":\"eyJhbGciOiJIUzI1NiJ9.eyJleHAiOjE0NDQxNDI1MzUsInNjb3BlcyI6InJ3ZWkgcnBybyBybnV0IHJzbGUgcnNldCByc29jIHJhY3QiLCJzdWIiOiIzUjlSQksiLCJhdWQiOiIyMjlWU1EiLCJpc3MiOiJGaXRiaXQiLCJ0eXAiOiJhY2Nlc3NfdG9rZW4iLCJpYXQiOjE0NDQxMzg5MzV9.no3_di9eFa7nzxnVAP6tmXf-yEKx7uXRln7QGmP8oKw\",\"expires_in\":3600,\"refresh_token\":\"bf05301595c84101935c6616c2176b446c293d529fbdd82f3e77b0f0c11f399b\",\"scope\":\"profile settings activity weight sleep nutrition social\",\"token_type\":\"Bearer\",\"user_id\":\"3R7RBK\"}";
            Log.e("Test_String", testStr);

            // extract and save auth info in sharedPreference file
            try {
                JSONObject authStr = new JSONObject(authline);
                String authToken = authStr.getString("access_token");
                String refreshToken = authStr.getString(REFRESHTOKEN);
                String scope = authStr.getString("scope");
                String tokenType = authStr.getString("token_type");
                String userId = authStr.getString("user_id");

                // convert expireIn to actual time of expiration
                String expireIn = authStr.getString("expires_in");
                String expireTime = String.valueOf(Math.round(System.currentTimeMillis() / 1000) + Integer.parseInt(expireIn));

                // Now write auth info to sharedPreference file
                prefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
                SharedPreferences.Editor edit = prefs.edit();
                edit.putString("access_token", authToken);
                edit.putString(REFRESHTOKEN, refreshToken);
                edit.putString("scope", scope);
                edit.putString("token_type", tokenType);
                edit.putString("user_id", userId);
                edit.commit();

            } catch (JSONException e) {
                Log.v(LOG_TAG,"parse JSON string failed.");
            }
        }
        private void getUserInfo() {}

        private void getRefreshToken() {}
    }

    // class request refresh token
    class ReqRefreshTokenAsyncTask extends AsyncTask<Void, Void, Void> {

        private String authCode;

        private final String LOG_TAG = getClass().getSimpleName();

        @Override
        protected Void doInBackground(Void... param) {
            getAuthToken();
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
//          Send user to new Activity
        }

        private void getAuthToken() {

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            prefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
            authToken = prefs.getString(AUTHTOKEN, null);

            // Generate a Base64 code
            final String concat = CLIENT_ID+":"+CLIENT_SECRET;
            final String base64AuthCode = Base64.encodeToString(concat.getBytes(), Base64.DEFAULT);

            try {
                Uri builtUri = Uri.parse(TOKEN_URL).buildUpon()
                        .appendQueryParameter(CLIENT_ID_PARAM, CLIENT_ID)
                        .appendQueryParameter(GRANT_TYPE_PARAM, "authorization_code")
//                        .appendQueryParameter(REDIRECT_URI_PARAM, REDIRECT_URI)
                        .appendQueryParameter(AUTHCODE, authCode)
                        .build();

                URL url = new URL(builtUri.toString());
                Log.v(LOG_TAG,builtUri.toString());

                urlConnection = (HttpURLConnection) url.openConnection();

                // set https to POST
                urlConnection.setDoOutput(true);

                // set htttps two header params
                urlConnection.setRequestProperty("AUTHORIZATION", "Basic " + base64AuthCode);
                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                urlConnection.connect();

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

        }

        private void getUserInfo() {}

        private void getRefreshToken() {}
    }

    // class request auth code
//    class ReqAuthCodeAsyncTask extends AsyncTask<Void, Void, Void> {
//
//        private String authCode;
//
//        private final String LOG_TAG = getClass().getSimpleName();
//
//        @Override
//        protected Void doInBackground(Void... param) {
//            processAuthCode();
//
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(Void unused) {
//            super.onPostExecute(unused);
////            processCallBackURL();
////            Log.v(LOG_TAG,"Here is auth_code"+getAuthCode());
//        }
//
//        //get Authorization Code
//        private void processAuthCode() {
//
//            HttpURLConnection urlConnection = null;
//            BufferedReader reader = null;
//
//            try {
//                Uri builtUri = Uri.parse(AUTHCODE_URL).buildUpon()
//                        .appendQueryParameter(RESPONSE_TYPE_PARAM, "code")
//                        .appendQueryParameter(CLIENT_ID_PARAM, CLIENT_ID)
////                        .appendQueryParameter(REDIRECT_URI_PARAM, REDIRECT_URI)
//                        .appendQueryParameter(SCOPE_PARAM, "activity")
//                        .build();
//
//                URL url = new URL(builtUri.toString());
//                Log.v(LOG_TAG,builtUri.toString());
//
//                urlConnection = (HttpURLConnection) url.openConnection();
//                urlConnection.setRequestMethod("GET");
//                urlConnection.connect();
//            } catch (IOException e) {
//                Log.e(LOG_TAG, "Error ", e);
//            } finally {
//                if (urlConnection != null) {
//                    urlConnection.disconnect();
//                }
//            }
//            return;
//        }
//
//    }


}
