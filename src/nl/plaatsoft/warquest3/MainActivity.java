package nl.plaatsoft.warquest3;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.net.URLEncoder;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;

// The main (webview) activity
public class MainActivity extends Activity implements FetchDataTask.OnLoadListener {
    private static final int OPEN_SETTINGS_ACTIVITY = 1;

    private SharedPreferences settings;
    private Account activeAccount;
    private WebView webview;

    // Supress warnings to get deprecated old settings
    @SuppressWarnings("deprecation")
    public SharedPreferences getOldSettings() {
        return android.preference.PreferenceManager.getDefaultSharedPreferences(this);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Load settings
        settings = getSharedPreferences("settings", Context.MODE_PRIVATE);

        // Main settings button handler
        ((ImageView)findViewById(R.id.main_settings_button)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                startActivityForResult(new Intent(MainActivity.this, SettingsActivity.class), OPEN_SETTINGS_ACTIVITY);
            }
        });

        // Init webview
        webview = (WebView)findViewById(R.id.main_webview);
        webview.setBackgroundColor(Color.TRANSPARENT);

        WebSettings webSettings = webview.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(settings.getBoolean("zoom", true));
        webSettings.setDisplayZoomControls(false);

        // Init disconnected page
        LinearLayout disconnectedPage = (LinearLayout)findViewById(R.id.main_disconnected_page);

        // Disconnected page refresh button
        ((Button)findViewById(R.id.main_disconnected_refresh_button)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // Refresh the webview
                loadWebview(Config.WARQUEST_URL + "/");
            }
        });

        // Webview update title handler
        TextView headerTitle = (TextView)findViewById(R.id.main_header_title);
        webview.setWebChromeClient(new WebChromeClient() {
            public void onReceivedTitle(WebView view, String title) {
                // Filter weird Android bug out
                if (title.equals(Config.WARQUEST_URL)) {
                    return;
                }

                // Check if the disconnected page must bee soon
                if (title.equals("about:blank")) {
                    headerTitle.setText("WarQuest");
                    webview.clearHistory();
                    webview.setVisibility(View.GONE);
                    disconnectedPage.setVisibility(View.VISIBLE);
                }

                // Else just load normal and set title
                else {
                    headerTitle.setText(title);

                    // Check if the webview is hidden
                    if (disconnectedPage.getVisibility() == View.VISIBLE) {
                        webview.clearHistory();
                        webview.setVisibility(View.VISIBLE);
                        disconnectedPage.setVisibility(View.GONE);
                    }
                }
            }
        });

        // Set webview client for error (no internet handling)
        webview.setWebViewClient(new WebViewClient() {
            // When an error occcurt no internet show disconnected page
            @SuppressWarnings("deprecation")
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                webview.stopLoading();
                webview.loadUrl("about:blank");
            }

            // Send new version of the API to old deprecated onReceivedError function
            public void onReceivedError(WebView view, WebResourceRequest webResourceRequest, WebResourceError webResourceError) {
                if (webResourceRequest.isForMainFrame()) {
                    onReceivedError(
                        view,
                        webResourceError.getErrorCode(),
                        webResourceError.getDescription().toString(),
                        webResourceRequest.getUrl().toString()
                    );
                }
            }
        });

        // Convert old settings to new format
        SharedPreferences oldSettings = getOldSettings();
        if (oldSettings.contains("username")) {
            // Create request url for login request
            String url = null;
            try {
                url = Config.WARQUEST_URL + "/api/auth/login?key=" + Config.WARQUEST_API_KEY +
                    "&username=" + URLEncoder.encode(oldSettings.getString("username", null), "UTF-8") +
                    "&password=" + URLEncoder.encode(oldSettings.getString("password", null), "UTF-8");
            } catch (Exception exception) {
                exception.printStackTrace();
            }

            // Do login request and save and open
            FetchDataTask.fetchData(this, url, false, false, this);

            // Set and save old zoom when disabled
            boolean oldZoom = oldSettings.getBoolean("zoom", true);
            if (oldZoom == false) {
                webSettings.setBuiltInZoomControls(false);

                SharedPreferences.Editor settingsEditor = settings.edit();
                settingsEditor.putBoolean("zoom", false);
                settingsEditor.apply();
            }
        }

        // When no old account convert
        else {
            // Load account
            try {
                JSONArray jsonAccounts = new JSONArray(settings.getString("accounts", "[]"));

                // When there are no accounts create new one
                if (jsonAccounts.length() == 0) {
                    // Do register request and save and open
                    FetchDataTask.fetchData(this, Config.WARQUEST_URL + "/api/auth/register?key=" + Config.WARQUEST_API_KEY, false, false, this);
                }

                // Else get selected account and load webview
                else {
                    // Get selected account id
                    long selectedAccountId = settings.getLong("selected_account_id", 0);
                    if (selectedAccountId == 0) {
                        selectedAccountId = Account.fromJson(jsonAccounts.getJSONObject(0)).getId();
                    }

                    // Find selected account
                    for (int i = 0; i < jsonAccounts.length(); i++) {
                        Account account =Account.fromJson(jsonAccounts.getJSONObject(i));
                        if (account.getId() == selectedAccountId) {
                            activeAccount = account;
                            break;
                        }
                    }

                    // Load the webview
                    loadWebview(Config.WARQUEST_URL + "/");
                }
            }

            // When there is an error reading the accounts json create a new account
            catch (Exception exception) {
                exception.printStackTrace();

                // Do register request and save and open
                FetchDataTask.fetchData(this, Config.WARQUEST_URL + "/api/auth/register?key=" + Config.WARQUEST_API_KEY, false, false, this);
            }
        }

        // Check rating alert
        RatingAlert.check(MainActivity.this);
    }

    // When a warquest link is opend open the page
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getAction() == Intent.ACTION_VIEW) {
            loadWebview(intent.getDataString());
        }
    }

    // When the settings activity is closed update stuff
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OPEN_SETTINGS_ACTIVITY) {
            // Set zoom value
            boolean zoom = settings.getBoolean("zoom", true);
            WebSettings webSettings = webview.getSettings();
            webSettings.setBuiltInZoomControls(zoom);

            // Reset zoom when zoom changed or active account changed
            long selectedAccountId = settings.getLong("selected_account_id", 0);
            if (!zoom || activeAccount.getId() != selectedAccountId) {
                webSettings.setLoadWithOverviewMode(false);
                webSettings.setLoadWithOverviewMode(true);
            }

            // When active account has changed
            if (activeAccount.getId() != selectedAccountId) {
                // Try to find new active account
                try {
                    JSONArray jsonAccounts = new JSONArray(settings.getString("accounts", "[]"));
                    for (int i = 0; i < jsonAccounts.length(); i++) {
                        Account account =Account.fromJson(jsonAccounts.getJSONObject(i));
                        if (account.getId() == selectedAccountId) {
                            activeAccount = account;
                            break;
                        }
                    }

                    // Reload webview
                    loadWebview(Config.WARQUEST_URL + "/");
                }

                // When there is an error reading the accounts json create a new account
                catch (Exception exception) {
                    exception.printStackTrace();

                    // Do register request and save and open
                    FetchDataTask.fetchData(this, Config.WARQUEST_URL + "/api/auth/register?key=" + Config.WARQUEST_API_KEY, false, false, this);
                }
            }
        }
    }

    // Clear webview history on pause
    public void onPause() {
        super.onPause();
        webview.clearHistory();
    }

    // Do webview back when back button is pressed
    public void onBackPressed() {
        if (webview.canGoBack()) {
            webview.goBack();
        } else {
            super.onBackPressed();
        }
    }

    // Create a new account and open the webview onload
    public void onLoad(String response) {
        try {
            JSONObject jsonResponse = new JSONObject(response);
            if (jsonResponse.getBoolean("success")) {
                // Set the active account
                activeAccount = Account.fromJsonApiResponse(jsonResponse);

                // Add account to json accounts
                JSONArray jsonAccounts = new JSONArray();
                jsonAccounts.put(activeAccount.toJson());

                // Save data
                SharedPreferences.Editor settingsEditor = settings.edit();
                settingsEditor.putString("accounts", jsonAccounts.toString());
                settingsEditor.putLong("selected_account_id", activeAccount.getId());
                settingsEditor.apply();

                // Reload the webview
                loadWebview(Config.WARQUEST_URL + "/");

                // Remove old settings when we where converting
                SharedPreferences oldSettings = getOldSettings();
                if (oldSettings.contains("username")) {
                    SharedPreferences.Editor oldSettingsEditor = oldSettings.edit();
                    oldSettingsEditor.clear();
                    oldSettingsEditor.apply();
                }

                return;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        // When an error occurt or success is false show an error message
        Toast.makeText(MainActivity.this, getResources().getString(R.string.register_error_message), Toast.LENGTH_SHORT).show();
    }

    // Load url in webview
    private void loadWebview(String url) {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("eid", "1");
        headers.put("username", activeAccount.getUsername());
        headers.put("password", activeAccount.getPassword());
        webview.loadUrl(url, headers);
    }
}
