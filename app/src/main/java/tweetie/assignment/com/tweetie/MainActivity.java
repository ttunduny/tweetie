package tweetie.assignment.com.tweetie;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public class MainActivity extends ActionBarActivity {
    SharedPreferences pref;
    private static String CONSUMER_KEY = "AUBuOJj1XRsNk2pH93bKmkRrH";
    private static String CONSUMER_SECRET = "4qIQWtheOsWAxMm8k4Mhxk9emN6y2WhrUwZa4JZAGriSvKR9kn";
    private static String TWITTER_CALLBACK_URL = "http://multimediaclass.com";
    private static String TWITTER_OAUTH_VERIFIER = "oauth_verifier";
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pref = getPreferences(0);
        SharedPreferences.Editor edit = pref.edit();
        edit.putString("CONSUMER_KEY", CONSUMER_KEY);
        edit.putString("CONSUMER_SECRET", CONSUMER_SECRET);
        edit.putString("TWITTER_CALLBACK_URL",TWITTER_CALLBACK_URL);
        edit.commit();

        Fragment login = new LoginFragment();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.content_frame, login);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.addToBackStack(null);
        ft.commit();
    }
}