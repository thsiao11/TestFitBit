package com.zeeh.testfitbit;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private final String LOG_TAG = getClass().getSimpleName();
    private final int REQ_AUTH_CODE = 1;
    private final int REQ_AUTH_TOKEN = 2;
    private final int REQ_USER_INFO = 3;
    private final int REQ_AUTH_NEW_TOKEN = 4;

    final private String CLIENT_ID = "229VSQ";
    final private String CLIENT_SECRET = "3892f75c76c6a1578bbcf2489a49c50b";

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

    private String authCode;
    private String authToken;
    private String refreshToken;
    private String endUserId;
    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = (WebView) findViewById(R.id.auth_webview);

        new ReqAsyncTask(REQ_AUTH_CODE).execute();

//        new ReqAsyncTask(REQ_AUTH_TOKEN).execute();

//        new ReqAsyncTask(REQ_USER_INFO).execute();

//        new ReqAsyncTask(REQ_AUTH_NEW_TOKEN).execute();

    }

    class ReqAsyncTask extends AsyncTask<Void, Void, Void> {

        private int requestCode;

        private final String LOG_TAG = getClass().getSimpleName();

        // Declare Constructor
        public ReqAsyncTask(int reqCode) {
            this.requestCode = reqCode;
        }

        @Override
        protected Void doInBackground(Void... param) {
            switch(requestCode) {
                case REQ_AUTH_CODE:
                    getAuthCode();
                    break;
                case REQ_AUTH_TOKEN:
                    getAuthToken();
                    break;
                case REQ_USER_INFO:
                    getUserInfo();
                    break;
                case REQ_AUTH_NEW_TOKEN:
                    getRefreshToken();
                    break;
                default: break;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            processCallBackURL();
        }

        //get Authorization Code
        private void getAuthCode() {

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            try {
                Uri builtUri = Uri.parse(AUTHCODE_URL).buildUpon()
                        .appendQueryParameter(RESPONSE_TYPE_PARAM, "code")
                        .appendQueryParameter(CLIENT_ID_PARAM, CLIENT_ID)
//                        .appendQueryParameter(REDIRECT_URI_PARAM, REDIRECT_URI)
                        .appendQueryParameter(SCOPE_PARAM, "activity")
                        .build();

                URL url = new URL(builtUri.toString());
                Log.v(LOG_TAG,builtUri.toString());

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
            return;
        }

        private void processCallBackURL() {

            webView.setWebViewClient(new WebViewClient() {

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    if (url.substring(0,26).compareTo("http://testcustomurischeme") == 0) {
                        authCode = url.split("=")[1];
                    }
                    Log.v(LOG_TAG, "Here's the auth_code "+authCode);
                }
// Not needed to trap the auth_code
//                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
//                    Log.v(LOG_TAG,description+", "+failingUrl);
//                }
            });

            webView.loadUrl("https://www.fitbit.com/oauth2/authorize?response_type=code&client_id=229VWX&scope=activity");
        }

        private void getAuthToken() {

        }

        private void getUserInfo() {}

        private void getRefreshToken() {}
    }
}

