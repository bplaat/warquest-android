package nl.plaatsoft.warquest3;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.os.Handler;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextSwitcher;
import android.widget.Toast;
import java.net.URLEncoder;
import java.util.Map;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends BaseActivity {
    public static final int SETTINGS_ACTIVITY_REQUEST_CODE = 1;

    private Handler handler;

    private LinearLayout webviewPage;
    private WebView webview;
    private String webviewTitle;

    private LinearLayout disconnectedPage;

    private Account activeAccount;
    private FetchDataTask.OnLoadListener saveAndOpenFirstAccount;
    private FetchDataTask.OnErrorListener connectionError;
    private int oldLanguage = -1;
    private int oldTheme = -1;
    private boolean isRatingAlertUpdated = false;

    @SuppressWarnings("deprecation")
    public SharedPreferences getOldSettings() {
        return android.preference.PreferenceManager.getDefaultSharedPreferences(this);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler(Looper.getMainLooper());

        // Webview page
        webviewPage = (LinearLayout)findViewById(R.id.main_webview_page);

        webview = (WebView)findViewById(R.id.main_webview_webview);
        webview.setBackgroundColor(0);

        WebSettings webSettings = webview.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(settings.getBoolean("zoom", Config.SETTINGS_ZOOM_DEFAULT));
        webSettings.setDisplayZoomControls(false);

        View.OnClickListener settingsOnClick = (View view) -> {
            oldLanguage = settings.getInt("language", Config.SETTINGS_LANGUAGE_DEFAULT);
            oldTheme = settings.getInt("theme", Config.SETTINGS_THEME_DEFAULT);
            startActivityForResult(new Intent(this, SettingsActivity.class), MainActivity.SETTINGS_ACTIVITY_REQUEST_CODE);
        };
        ((ImageButton)findViewById(R.id.main_webview_settings_button)).setOnClickListener(settingsOnClick);

        // Disconnected page
        disconnectedPage = (LinearLayout)findViewById(R.id.main_disconnected_page);

        View.OnClickListener refreshOnClick = (View view) -> {
            webviewLoadBaseUrl();
        };
        ((ImageButton)findViewById(R.id.main_disconnected_refresh_button)).setOnClickListener(refreshOnClick);
        ((Button)findViewById(R.id.main_disconnected_hero_button)).setOnClickListener(refreshOnClick);

        ((ImageButton)findViewById(R.id.main_disconnected_settings_button)).setOnClickListener(settingsOnClick);

        // Webview handlers
        TextSwitcher webviewTitleLabel = (TextSwitcher)findViewById(R.id.main_webview_title_label);
        webviewTitleLabel.setCurrentText(getResources().getString(R.string.app_name));
        webview.setWebChromeClient(new WebChromeClient() {
            public void onReceivedTitle(WebView view, String title) {
                webviewTitle = title;
                webviewTitleLabel.setText(title);
            }
        });

        Uri baseUri = Uri.parse(Config.APP_WARQUEST_URL);
        webview.setWebViewClient(new WebViewClient() {
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return shouldOverrideUrlLoading(view, Uri.parse(url));
            }

            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return shouldOverrideUrlLoading(view, request.getUrl());
            }

            private boolean shouldOverrideUrlLoading(WebView view, Uri uri) {
                if (uri.getScheme().equals(baseUri.getScheme()) && uri.getHost().equals(baseUri.getHost())) {
                    return false;
                } else {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                        return true;
                    } catch (Exception exception) {
                        exception.printStackTrace();
                        return false;
                    }
                }
            }

            @SuppressWarnings("deprecation")
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                onReceivedError(view);
            }

            public void onReceivedError(WebView view, WebResourceRequest webResourceRequest, WebResourceError webResourceError) {
                if (webResourceRequest.isForMainFrame()) {
                    onReceivedError(view);
                }
            }

            private void onReceivedError(WebView view) {
                view.stopLoading();

                if (disconnectedPage.getVisibility() == View.GONE) {
                    webviewPage.setVisibility(View.GONE);
                    disconnectedPage.setVisibility(View.VISIBLE);
                }
            }

            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (disconnectedPage.getVisibility() == View.VISIBLE) {
                    disconnectedPage.setVisibility(View.GONE);
                    webviewPage.setVisibility(View.VISIBLE);
                }
            }
        });

        // Save a new account and open it in the webview
        saveAndOpenFirstAccount = (String response) -> {
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

                    // Remove old settings when we where converting
                    SharedPreferences oldSettings = getOldSettings();
                    if (oldSettings.contains("username")) {
                        SharedPreferences.Editor oldSettingsEditor = oldSettings.edit();
                        oldSettingsEditor.clear();
                        oldSettingsEditor.apply();
                    }

                    // Reload the webview
                    webviewLoadBaseUrl();
                    return;
                }
            }
            catch (Exception exception) {
                // Show message when response json parse failed
                Toast.makeText(this, getResources().getString(R.string.main_response_error_message), Toast.LENGTH_SHORT).show();
            }
        };

        connectionError = (Exception exception) -> {
            // Show message when connection error occurt
            Toast.makeText(this, getResources().getString(R.string.main_connection_error_message), Toast.LENGTH_SHORT).show();
        };

        // Convert old settings to new format
        SharedPreferences oldSettings = getOldSettings();
        if (oldSettings.contains("username")) {
            // Set and save old zoom when disabled
            boolean oldZoom = oldSettings.getBoolean("zoom", Config.SETTINGS_ZOOM_DEFAULT);
            webSettings.setBuiltInZoomControls(oldZoom);

            SharedPreferences.Editor settingsEditor = settings.edit();
            settingsEditor.putBoolean("zoom", false);
            settingsEditor.apply();

            // Create request url for login request
            String url = null;
            try {
                url = Config.APP_WARQUEST_URL + "/api/auth/login?key=" + Config.APP_WARQUEST_API_KEY +
                    "&username=" + URLEncoder.encode(oldSettings.getString("username", null), "UTF-8") +
                    "&password=" + URLEncoder.encode(oldSettings.getString("password", null), "UTF-8");
            } catch (Exception exception) {
                exception.printStackTrace();
            }

            // Do login request and save and open
            FetchDataTask.with(this).load(url).then(saveAndOpenFirstAccount, connectionError);
        }

        // When no old account convert
        else {
            // Load account
            try {
                JSONArray jsonAccounts = new JSONArray(settings.getString("accounts", "[]"));

                // When there are no accounts create new one
                if (jsonAccounts.length() == 0) {
                    // Do register request and save and open
                    FetchDataTask.with(this).load(Config.APP_WARQUEST_URL + "/api/auth/register?key=" + Config.APP_WARQUEST_API_KEY).then(saveAndOpenFirstAccount, connectionError);
                }

                // Else get selected account and load webview
                else {
                    // Get selected account id
                    long selectedAccountId = settings.getLong("selected_account_id", -1);
                    if (selectedAccountId == -1) {
                        selectedAccountId = Account.fromJson(jsonAccounts.getJSONObject(0)).getId();
                    }

                    // Find selected account
                    for (int i = 0; i < jsonAccounts.length(); i++) {
                        Account account = Account.fromJson(jsonAccounts.getJSONObject(i));
                        if (account.getId() == selectedAccountId) {
                            activeAccount = account;
                            break;
                        }
                    }

                    // Load the webview
                    webviewLoadBaseUrl();
                }
            }

            // When there is an error reading the accounts json create a new account
            catch (Exception exception) {
                exception.printStackTrace();

                // Do register request and save and open
                FetchDataTask.with(this).load(Config.APP_WARQUEST_URL + "/api/auth/register?key=" + Config.APP_WARQUEST_API_KEY).then(saveAndOpenFirstAccount, connectionError);
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MainActivity.SETTINGS_ACTIVITY_REQUEST_CODE) {
            // Set zoom value
            boolean zoom = settings.getBoolean("zoom", Config.SETTINGS_ZOOM_DEFAULT);
            WebSettings webSettings = webview.getSettings();
            webSettings.setBuiltInZoomControls(zoom);

            // Reset zoom when zoom changed or active account changed
            long selectedAccountId = settings.getLong("selected_account_id", -1);
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
                        Account account = Account.fromJson(jsonAccounts.getJSONObject(i));
                        if (account.getId() == selectedAccountId) {
                            activeAccount = account;
                            break;
                        }
                    }

                    // Reload webview
                    webviewLoadBaseUrl();
                }

                // When there is an error reading the accounts json create a new account
                catch (Exception exception) {
                    exception.printStackTrace();

                    // Do register request and save and open
                    FetchDataTask.with(this).load(Config.APP_WARQUEST_URL + "/api/auth/register?key=" + Config.APP_WARQUEST_API_KEY).then(saveAndOpenFirstAccount, connectionError);
                }
            }

            // Recreate when language or theme change
            if (oldLanguage != -1 && oldTheme != -1) {
                if (
                    oldLanguage != settings.getInt("language", Config.SETTINGS_LANGUAGE_DEFAULT) ||
                    oldTheme != settings.getInt("theme", Config.SETTINGS_THEME_DEFAULT)
                ) {
                    handler.post(() -> {
                        recreate();
                    });
                }
            }
        }
    }

    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getAction() == Intent.ACTION_VIEW) {
            webviewLoadUrl(intent.getDataString());
        }
    }

    public void onBackPressed() {
        if (disconnectedPage.getVisibility() == View.VISIBLE) {
            super.onBackPressed();
        }

        if (
            webviewTitle.startsWith("WarQuest - Overview") ||
            webviewTitle.startsWith("WarQuest - Overzicht") ||
            webviewTitle.startsWith("WarQuest - Übersicht") ||
            webviewTitle.startsWith("WarQuest - Riepilogo") ||
            webviewTitle.startsWith("WarQuest - privire generala") ||
            webviewTitle.startsWith("WarQuest - Resumen")
        ) {
            super.onBackPressed();
        }

        if (webview.canGoBack()) {
            webview.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private void webviewLoadBaseUrl() {
        Intent intent = getIntent();
        if (intent.getAction() == Intent.ACTION_VIEW) {
            webviewLoadUrl(intent.getDataString());
        } else {
            webviewLoadUrl(Config.APP_WARQUEST_URL + "/");
        }

        if (!isRatingAlertUpdated) {
            isRatingAlertUpdated = true;
            RatingAlert.updateAndShow(this);
        }
    }

    private void webviewLoadUrl(String url) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("eid", "1");
        headers.put("username", activeAccount.getUsername());
        headers.put("password", activeAccount.getPassword());
        webview.loadUrl(url, headers);
    }
}
