package nl.plaatsoft.warquest3;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.HashMap;

public class MainActivity extends Activity {
    private WebView webView;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        ((ImageView)findViewById(R.id.settings_button)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        webView = (WebView)findViewById(R.id.webview);
        webView.setBackgroundColor(android.R.color.transparent);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        if (preferences.getBoolean("zoom", true)) {
            webSettings.setBuiltInZoomControls(true);
            webSettings.setDisplayZoomControls(false);
        }
        TextView headerTitle = (TextView)findViewById(R.id.header_title);
        webView.setWebChromeClient(new WebChromeClient() {
            public void onReceivedTitle(WebView view, String title) {
                headerTitle.setText(title);
            }
        });
        HashMap<String, String> headers = new HashMap<String, String>();
        String username = preferences.getString("username", null);
        String password = preferences.getString("password", null);
        if (username == null && password == null) {
            long ms = System.currentTimeMillis();
            username = String.valueOf(ms);
            password = String.valueOf(ms);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("username", username);
            editor.putString("password", password);
            editor.apply();
            headers.put("eid", "2");
        } else {
            headers.put("eid", "1");
        }
        headers.put("username", username);
        headers.put("password", password);
        webView.loadUrl("https://www.warquest.nl/", headers);
    }
    protected void onPause() {
        super.onPause();
        webView.clearHistory();
    }
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}