package tweetie.assignment.com.tweetie;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import tweetie.assignment.com.tweetie.connection.Connection_Detector;
import tweetie.assignment.com.tweetie.utils.AlertDialogManager;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;


public class MainActivity extends ActionBarActivity implements OnClickListener {

    // Constants
    /**
     * Register your here app https://dev.twitter.com/apps/new and get your
     * consumer key and secret
     */
    static String TWITTER_CONSUMER_KEY = "FFfN4wXpbNb60HIlq7EBreRc0";
    static String TWITTER_CONSUMER_SECRET = "9putnkeNGOP28ECYAHHuHcaYhuoLI7O91tKP8YTqktgbRPX0HY";

    // Preference Constants
    static String PREFERENCE_NAME = "twitter_oauth";
    static final String PREF_KEY_OAUTH_TOKEN = "oauth_token";
    static final String PREF_KEY_OAUTH_SECRET = "oauth_token_secret";
    static final String PREF_KEY_TWITTER_LOGIN = "isTwitterLogedIn";

    static final String TWITTER_CALLBACK_URL = "http://tweetie.com";

    // Twitter oauth urls
    static final String URL_TWITTER_AUTH = "auth_url";
    static final String URL_TWITTER_OAUTH_VERIFIER = "oauth_verifier";
    static final String URL_TWITTER_OAUTH_TOKEN = "oauth_token";

    // Login button
    Button btnLoginTwitter;
    // Update status button
    Button btnUpdateStatus;
    // Logout button
    Button btnLogoutTwitter;
    // EditText for update
    EditText txtUpdate;
    // lbl update
    TextView lblUpdate;
    TextView lblUserName;

    // Progress dialog
    ProgressDialog pDialog;

    // Twitter
    private static Twitter twitter;
    private static RequestToken requestToken;

    // Shared Preferences
    private static SharedPreferences mSharedPreferences;

    // Internet Connection detector
    private Connection_Detector cd;

    // Alert Dialog Manager
    AlertDialogManager alert = new AlertDialogManager();

    String TAG = MainActivity.class.getSimpleName();


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        cd = new Connection_Detector(getApplicationContext());

        // Check if Internet present
        if (!cd.isConnectingToInternet())
        {
            // Internet Connection is not present
            alert.showAlertDialog(MainActivity.this, "Internet Connection Error", "Please connect to working Internet connection", false);
            // stop executing code by return
            return;
        }

        // Check if twitter keys are set
        if (TWITTER_CONSUMER_KEY.trim().length() == 0 || TWITTER_CONSUMER_SECRET.trim().length() == 0) {
            // Internet Connection is not present
            alert.showAlertDialog(MainActivity.this, "Twitter oAuth tokens", "Please set your twitter oauth tokens first!", false);
            // stop executing code by return
            return;
        }

        getViewByIDs();

        // Shared Preferences
        mSharedPreferences = getApplicationContext().getSharedPreferences("MyPref", 0);
        getOAuthVerifier();
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnLoginTwitter)
        {
            loginToTwitter();
        }

        if(v.getId() == R.id.btnUpdateStatus)
        {
            String status = txtUpdate.getText().toString();

            // Check for blank text
            if (status.trim().length() > 0) {
                // update status
                new updateTwitterStatus().execute(status);
            } else
            {
                // EditText is empty
                Toast.makeText(getApplicationContext(),"Please enter status message", Toast.LENGTH_SHORT).show();
            }

        }
        if(v.getId() == R.id.btnLogoutTwitter)
        {
            logoutFromTwitter();
        }

}

 public void getOAuthVerifier()
 {
     /** This if conditions is tested once is
      * redirected from twitter page. Parse the uri to get oAuth
      * Verifier
      * */
     if (!isTwitterLoggedInAlready()) {
         Uri uri = getIntent().getData();
         if (uri != null && uri.toString().startsWith(TWITTER_CALLBACK_URL)) {
             // oAuth verifier
             String verifier = uri.getQueryParameter(URL_TWITTER_OAUTH_VERIFIER);

             try {
                 // Get the access token
                 AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, verifier);

                 // Shared Preferences
                 SharedPreferences.Editor e = mSharedPreferences.edit();

                 // After getting access token, access token secret
                 // store them in application preferences
                 e.putString(PREF_KEY_OAUTH_TOKEN, accessToken.getToken());
                 e.putString(PREF_KEY_OAUTH_SECRET, accessToken.getTokenSecret());
                 // Store login status - true
                 e.putBoolean(PREF_KEY_TWITTER_LOGIN, true);
                 e.commit(); // save changes

                 Log.e(TAG,"Twitter OAuth Token" + "> " + accessToken.getToken());

                 // Hide login button
                 btnLoginTwitter.setVisibility(View.GONE);

                 // Show Update Twitter
                 lblUpdate.setVisibility(View.VISIBLE);
                 txtUpdate.setVisibility(View.VISIBLE);
                 btnUpdateStatus.setVisibility(View.VISIBLE);
                 btnLogoutTwitter.setVisibility(View.VISIBLE);

                 // Getting user details from twitter
                 // For now i am getting his name only
                 long userID = accessToken.getUserId();
                 User user = twitter.showUser(userID);
                 String username = user.getName();

                 // Displaying in xml ui
                 lblUserName.setText(Html.fromHtml("<b>Welcome " + username + "</b>"));
             } catch (Exception e) {
                 // Check log for login errors
                 Log.d(TAG,"Twitter Login Error" + "> " + e.getMessage());
             }
         }
     }
 }


 private void loginToTwitter()
 {

        // Check if already logged in
    if (!isTwitterLoggedInAlready()) {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
        builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);

        Configuration configuration = builder.build();

        TwitterFactory factory = new TwitterFactory(configuration);
        twitter = factory.getInstance();

        try
        {
            requestToken = twitter.getOAuthRequestToken(TWITTER_CALLBACK_URL);
            this.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(requestToken.getAuthenticationURL())));
        } catch (TwitterException e)
        {
            Log.d(TAG,"The exception Sheldon is "+ e.getErrorMessage());
            Log.d(TAG,"The exception Sheldon is "+ e.getExceptionCode());
            Log.d(TAG,"The exception Sheldon is "+ e.getMessage());

            e.printStackTrace();
        }
    } else {
        // user already logged into twitter
        Toast.makeText(getApplicationContext(),"Already Logged into twitter", Toast.LENGTH_LONG).show();
            }
 }

    //Checking if we had already saved details of user
    private boolean isTwitterLoggedInAlready()
    {
        // return twitter login status from Shared Preferences
        return mSharedPreferences.getBoolean(PREF_KEY_TWITTER_LOGIN, false);
    }

    //this class enables us to retrieve the IDs of the edittexts and labels of fields
    public void getViewByIDs()
    {
        // All UI elements
        btnLoginTwitter = (Button) findViewById(R.id.btnLoginTwitter);
        btnUpdateStatus = (Button) findViewById(R.id.btnUpdateStatus);
        btnLogoutTwitter = (Button) findViewById(R.id.btnLogoutTwitter);
        txtUpdate = (EditText) findViewById(R.id.txtUpdateStatus);
        lblUpdate = (TextView) findViewById(R.id.lblUpdate);
        lblUserName = (TextView) findViewById(R.id.lblUserName);
        btnLoginTwitter.setOnClickListener(this);

    }
    /**
     * Function to update status
     * */
    class updateTwitterStatus extends AsyncTask<String, String, String> {

        /**
         * Before starting background thread Show Progress Dialog
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Updating to twitter...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        /**
         * getting Places JSON
         * */
        protected String doInBackground(String... args) {
            Log.d(TAG,"Tweet Text" + "> " + args[0]);
            String status = args[0];
            try {
                ConfigurationBuilder builder = new ConfigurationBuilder();
                builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
                builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);

                // Access Token
                String access_token = mSharedPreferences.getString(PREF_KEY_OAUTH_TOKEN, "");
                // Access Token Secret
                String access_token_secret = mSharedPreferences.getString(PREF_KEY_OAUTH_SECRET, "");

                AccessToken accessToken = new AccessToken(access_token, access_token_secret);
                Twitter twitter = new TwitterFactory(builder.build()).getInstance(accessToken);

                // Update status
                twitter4j.Status response = twitter.updateStatus(status);

                Log.d(TAG,"Sheldon Status" + "> " + response.getText());
            } catch (TwitterException e) {
                // Error in updating status
                Log.d(TAG,"Sheldon Twitter Update Error" + e.getMessage());
            }
            return null;
        }

        /**
         * After completing background task Dismiss the progress dialog and show
         * the data in UI Always use runOnUiThread(new Runnable()) to update UI
         * from background thread, otherwise you will get error
         * **/
        protected void onPostExecute(String file_url) {
            // dismiss the dialog after getting all products
            pDialog.dismiss();
            // updating UI from Background Thread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),
                            "Status tweeted successfully", Toast.LENGTH_SHORT)
                            .show();
                    // Clearing EditText field
                    txtUpdate.setText("");
                }
            });
        }

    }

    /**
     * Function to logout from twitter
     * It will just clear the application shared preferences
     * */
    private void logoutFromTwitter()
    {
        // Clear the shared preferences
        SharedPreferences.Editor e = mSharedPreferences.edit();
        e.remove(PREF_KEY_OAUTH_TOKEN);
        e.remove(PREF_KEY_OAUTH_SECRET);
        e.remove(PREF_KEY_TWITTER_LOGIN);
        e.commit();

        // After this take the appropriate action
        // I am showing the hiding/showing buttons again
        // You might not needed this code
        btnLogoutTwitter.setVisibility(View.GONE);
        btnUpdateStatus.setVisibility(View.GONE);
        txtUpdate.setVisibility(View.GONE);
        lblUpdate.setVisibility(View.GONE);
        lblUserName.setText("");
        lblUserName.setVisibility(View.GONE);

        btnLoginTwitter.setVisibility(View.VISIBLE);
    }



    protected void onResume() {
        super.onResume();
    }





}

