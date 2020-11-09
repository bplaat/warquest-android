package nl.plaatsoft.warquest3;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.net.URLEncoder;
import org.json.JSONArray;
import org.json.JSONObject;

// The settings activity
public class SettingsActivity extends BaseActivity implements FetchDataTask.OnLoadListener {
    public static final int LANGUAGE_DEFAULT = 2;
    public static final int THEME_DEFAULT = 1;
    private static final int LOGIN_ACTIVITY_REQUEST_CODE = 1;

    private AccountsAdapter accountsAdapter;

    // Create activity
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Settings back button
        ((ImageView)findViewById(R.id.settings_back_button)).setOnClickListener((View view) -> {
            finish();
        });

        // Accounts list view create header and footer views and adapter
        ListView accountsList = (ListView)findViewById(R.id.settings_accounts_list);

        accountsList.addHeaderView(getLayoutInflater().inflate(R.layout.item_settings_header, accountsList, false), null, false);

        LinearLayout footerView = (LinearLayout)getLayoutInflater().inflate(R.layout.item_settings_footer, accountsList, false);
        accountsList.addFooterView(footerView);

        accountsAdapter = new AccountsAdapter(this, settings.getLong("selected_account_id", 0));
        accountsList.setAdapter(accountsAdapter);

        // Accounts item click event
        accountsList.setOnItemClickListener((AdapterView<?> adapterView, View view, int position, long id) -> {
            position -= 1;

            // Normal account button
            if (position < accountsAdapter.getCount() - 2) {
                // Get account
                Account account = accountsAdapter.getItem(position);

                // Save new selected account id
                SharedPreferences.Editor settingsEditor = settings.edit();
                settingsEditor.putLong("selected_account_id", account.getId());
                settingsEditor.apply();

                // Close activity
                finish();
            }

            // Add existing account button
            if (position == accountsAdapter.getCount() - 2) {
                // Open login activity
                startActivityForResult(new Intent(this, LoginActivity.class), SettingsActivity.LOGIN_ACTIVITY_REQUEST_CODE);
            }

            // Create new account button
            if (position == accountsAdapter.getCount() - 1) {
                // Send register request
                new FetchDataTask(this, Config.WARQUEST_URL + "/api/auth/register?key=" + Config.WARQUEST_API_KEY, false, false, (String response) -> {
                    try {
                        // Parse response
                        JSONObject jsonResponse = new JSONObject(response);

                        // When successfull add and open account
                        if (jsonResponse.getBoolean("success")) {
                            Account account = Account.fromJsonApiResponse(jsonResponse);
                            addAccount(account);
                            openAccount(account);
                            return;
                        }
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }

                    // When an error occurt or success is false show an error message
                    Toast.makeText(this, getResources().getString(R.string.register_error_message), Toast.LENGTH_SHORT).show();
                });
            }
        });

        // Accounts long press event
        accountsList.setOnItemLongClickListener((AdapterView<?> adapterView, View view, int position, long id) -> {
            position -= 1;

            // Normal account button when long pressed some extra account information
            if (position < accountsAdapter.getCount() - 2) {
                // Get account
                Account account = accountsAdapter.getItem(position);

                // Show toast with extra information
                Toast.makeText(
                    this,
                    "Username: " + account.getUsername() + "\n" +
                    "Email: " + (account.getEmail().equals("") ? "?" : account.getEmail()) + "\n" +
                    "Password: " + account.getPassword(),
                    Toast.LENGTH_LONG
                ).show();

                return true;
            }

            return false;
        });

        // Load accounts from settings
        JSONArray jsonAccounts;
        try {
            jsonAccounts = new JSONArray(settings.getString("accounts", "[]"));
            for (int i = 0; i < jsonAccounts.length(); i++) {
                accountsAdapter.add(Account.fromJson(jsonAccounts.getJSONObject(i)));
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        // Add two nulls for action buttons
        accountsAdapter.add(null);
        accountsAdapter.add(null);

        // Settings language selector button
        String[] languages = getResources().getStringArray(R.array.languages);
        int language = settings.getInt("language", SettingsActivity.LANGUAGE_DEFAULT);
        ((TextView)findViewById(R.id.settings_language_label)).setText(languages[language]);

        ((LinearLayout)findViewById(R.id.settings_language_button)).setOnClickListener((View view) -> {
            new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.settings_language_label))
                .setSingleChoiceItems(languages, language, (DialogInterface dialog, int which) -> {
                    SharedPreferences.Editor settingsEditor = settings.edit();
                    settingsEditor.putInt("language", which);
                    settingsEditor.apply();

                    dialog.dismiss();
                    recreate();
                })
                .setNegativeButton(getResources().getString(R.string.settings_cancel), null)
                .show();
        });

        // Settings theme selector button
        String[] themes = getResources().getStringArray(R.array.themes);
        int theme = settings.getInt("theme", SettingsActivity.THEME_DEFAULT);
        ((TextView)findViewById(R.id.settings_theme_label)).setText(themes[theme]);

        ((LinearLayout)findViewById(R.id.settings_theme_button)).setOnClickListener((View view) -> {
            new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.settings_theme_label))
                .setSingleChoiceItems(themes, theme, (DialogInterface dialog, int which) ->  {
                    SharedPreferences.Editor settingsEditor = settings.edit();
                    settingsEditor.putInt("theme", which);
                    settingsEditor.apply();

                    dialog.dismiss();
                    recreate();
                })
                .setNegativeButton(getResources().getString(R.string.settings_cancel), null)
                .show();
        });

        // Zoom switch save code
        Switch zoomSwitch = (Switch)footerView.findViewById(R.id.settings_zoom_switch);
        zoomSwitch.setChecked(settings.getBoolean("zoom", true));
        zoomSwitch.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            // Save data when changed
            SharedPreferences.Editor settingsEditor = settings.edit();
            settingsEditor.putBoolean("zoom", isChecked);
            settingsEditor.apply();
        });

        // Settings about button
        ((TextView)findViewById(R.id.settings_about_button)).setOnClickListener((View view) -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://bastiaan.ml/")));
        });

        // Check to update accounts data
        updateAccountsData();
    }

    // When the login page is succesfull add and open account
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SettingsActivity.LOGIN_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // When the login activity returns an account add and open it
            Account account = (Account)data.getExtras().getSerializable("account");
            addAccount(account);
            openAccount(account);
        }
    }

    // Request to remove account
    public void removeAccount(int position) {
        try {
            JSONArray jsonAccounts = new JSONArray(settings.getString("accounts", "[]"));

            Account account = Account.fromJson(jsonAccounts.getJSONObject(position));

            jsonAccounts.remove(position);

            // Remove item from accounts list
            accountsAdapter.remove(accountsAdapter.getItem(position));
            accountsAdapter.notifyDataSetChanged();

            SharedPreferences.Editor settingsEditor = settings.edit();
            settingsEditor.putString("accounts", jsonAccounts.toString());

            // Do some extra checks when the account was the selected account
            long selectedAccountId = settings.getLong("selected_account_id", 0);
            if (selectedAccountId == account.getId()) {
                // When no accounts are left create new own
                if (jsonAccounts.length() == 0) {
                    // Send register request
                    new FetchDataTask(this, Config.WARQUEST_URL + "/api/auth/register?key=" + Config.WARQUEST_API_KEY, false, false, (String response) -> {
                        try {
                            // Parse response
                            JSONObject jsonResponse = new JSONObject(response);

                            // When successfull add and update accounts list
                            if (jsonResponse.getBoolean("success")) {
                                // Parse account
                                Account otherAccount = Account.fromJsonApiResponse(jsonResponse);

                                // Save it
                                addAccount(otherAccount);

                                // Add to the accounts list
                                accountsAdapter.clear();
                                accountsAdapter.add(otherAccount);
                                accountsAdapter.add(null);
                                accountsAdapter.add(null);
                                accountsAdapter.setSelectedAccountId(otherAccount.getId());

                                // Save new selected account id
                                SharedPreferences.Editor otherSettingsEditor = settings.edit();
                                otherSettingsEditor.putLong("selected_account_id", otherAccount.getId());
                                otherSettingsEditor.apply();
                                return;
                            }
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }

                        // When an error occurt or success is false show an error message
                        Toast.makeText(this, getResources().getString(R.string.register_error_message), Toast.LENGTH_SHORT).show();
                    });
                }

                // When more accounts exists select the next one
                else {
                    Account nextAccount = Account.fromJson(jsonAccounts.getJSONObject(position == 0 ? 0 : position - 1));
                    settingsEditor.putLong("selected_account_id", nextAccount.getId());
                    accountsAdapter.setSelectedAccountId(nextAccount.getId());
                }
            }
            settingsEditor.apply();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    // Add account
    private void addAccount(Account account) {
        // Load accounts json
        JSONArray jsonAccounts;
        try {
            jsonAccounts = new JSONArray(settings.getString("accounts", "[]"));
        } catch (Exception exception) {
            exception.printStackTrace();
            jsonAccounts = new JSONArray();
        }

        // Add account to json
        jsonAccounts.put(account.toJson());

        // Save the accounts
        SharedPreferences.Editor settingsEditor = settings.edit();
        settingsEditor.putString("accounts", jsonAccounts.toString());
        settingsEditor.apply();
    }

    // Open account
    private void openAccount(Account account) {
        // Set the selected account to the account id
        SharedPreferences.Editor settingsEditor = settings.edit();
        settingsEditor.putLong("selected_account_id", account.getId());
        settingsEditor.apply();

        // Close activity
        finish();
    }

    // Update accounts data
    private void updateAccountsData() {
        // Check if accounts update timeout is ellapsed
        long accountsUpdateTime = settings.getLong("accounts_update_time", 0);
        if (System.currentTimeMillis() - accountsUpdateTime > Config.ACCOUNTS_UPDATE_TIMEOUT) {
            // Save new time
            SharedPreferences.Editor settingsEditor = settings.edit();
            settingsEditor.putLong("accounts_update_time", System.currentTimeMillis());
            settingsEditor.apply();

            // Load accounts json
            JSONArray jsonAccounts;
            try {
                jsonAccounts = new JSONArray(settings.getString("accounts", "[]"));
            } catch (Exception exception) {
                exception.printStackTrace();
                jsonAccounts = new JSONArray();
            }

            // Create request url
            String url = Config.WARQUEST_URL + "/api/players?key=" + Config.WARQUEST_API_KEY;
            try {
                for (int i = 0; i < jsonAccounts.length(); i++) {
                    Account account = Account.fromJson(jsonAccounts.getJSONObject(i));
                    url += "&usernames[]=" + URLEncoder.encode(account.getUsername(), "UTF-8") +
                        "&passwords[]=" + URLEncoder.encode(account.getPassword(), "UTF-8");
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }

            // Do request
            new FetchDataTask(this, url, false, false, this);
        }
    }

    // Update onload listener
    public void onLoad(String response) {
        try {
            // Parse response
            JSONObject jsonResponse = new JSONObject(response);

            // When successfull
            if (jsonResponse.getBoolean("success")) {
                JSONArray jsonPlayers = jsonResponse.getJSONArray("players");
                JSONArray jsonAccounts = new JSONArray();

                // When successfull clear list
                accountsAdapter.clear();

                // Add newly fetched accounts to the list and to array
                for (int i = 0; i < jsonPlayers.length(); i++) {
                    Account account = Account.fromJsonApiResponse(jsonPlayers.getJSONObject(i));
                    accountsAdapter.add(account);
                    jsonAccounts.put(account.toJson());
                }

                // Add two nulls for action buttons and notify change
                accountsAdapter.add(null);
                accountsAdapter.add(null);
                accountsAdapter.notifyDataSetChanged();

                // And save new accounts data
                SharedPreferences.Editor settingsEditor = settings.edit();
                settingsEditor.putString("accounts", jsonAccounts.toString());
                settingsEditor.apply();
                return;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        // When an load error occurt or success is false show update error toast
        Toast.makeText(this, getResources().getString(R.string.settings_update_error_message), Toast.LENGTH_SHORT).show();
    }
}
