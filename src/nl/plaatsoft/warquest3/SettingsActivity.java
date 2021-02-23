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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.net.URLEncoder;
import org.json.JSONArray;
import org.json.JSONObject;

public class SettingsActivity extends BaseActivity {
    private static final int LOGIN_ACTIVITY_REQUEST_CODE = 1;

    private AccountsAdapter accountsAdapter;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ((ImageButton)findViewById(R.id.settings_back_button)).setOnClickListener((View view) -> {
            finish();
        });

        // Accounts list
        ListView accountsList = (ListView)findViewById(R.id.settings_accounts_list);
        accountsList.addHeaderView((LinearLayout)getLayoutInflater().inflate(R.layout.view_settings_header, accountsList, false));
        accountsList.addFooterView((LinearLayout)getLayoutInflater().inflate(R.layout.view_settings_footer, accountsList, false));

        accountsAdapter = new AccountsAdapter(this);
        accountsAdapter.setSelectedAccountId(settings.getLong("selected_account_id", -1));
        accountsList.setAdapter(accountsAdapter);

        // Accounts list item click event
        accountsList.setOnItemClickListener((AdapterView<?> adapterView, View view, int position, long id) -> {
            openAccount(accountsAdapter.getItem(position - 1));
        });

        // Accounts list item long press event
        accountsList.setOnItemLongClickListener((AdapterView<?> adapterView, View view, int position, long id) -> {
            Account account = accountsAdapter.getItem(position - 1);
            Toast.makeText(
                this,
                "Username: " + account.getUsername() + "\n" +
                "Email: " + (account.getEmail().equals("") ? "?" : account.getEmail()) + "\n" +
                "Password: " + account.getPassword(),
                Toast.LENGTH_LONG
            ).show();
            return true;
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

        // Login button
        ((LinearLayout)findViewById(R.id.settings_login_account_button)).setOnClickListener((View view) -> {
            startActivityForResult(new Intent(this, LoginActivity.class), SettingsActivity.LOGIN_ACTIVITY_REQUEST_CODE);
        });

        // Create button
        ((LinearLayout)findViewById(R.id.settings_register_account_button)).setOnClickListener((View view) -> {
            new AlertDialog.Builder(this)
                .setTitle(R.string.settings_create_alert_title_label)
                .setMessage(R.string.settings_create_alert_message_label)
                .setPositiveButton(R.string.settings_create_alert_create_button, (DialogInterface dialog, int whichButton) -> {
                    // Send register request
                    FetchDataTask.with(this).load(Config.APP_WARQUEST_URL + "/api/auth/register?key=" + Config.APP_WARQUEST_API_KEY).then((String response) -> {
                        try {
                            // Parse response
                            JSONObject jsonResponse = new JSONObject(response);

                            // When successfull add and open account
                            if (jsonResponse.getBoolean("success")) {
                                Account account = Account.fromJsonApiResponse(jsonResponse);
                                saveAccount(account);
                                openAccount(account);
                                return;
                            }
                        }
                        catch (Exception exception) {
                            // Show message when response json parse failed
                            Toast.makeText(this, getResources().getString(R.string.settings_response_error_message), Toast.LENGTH_SHORT).show();
                        }
                    }, (Exception exception) -> {
                        // Show message when connection error occurt
                        Toast.makeText(this, getResources().getString(R.string.settings_connection_error_message), Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton(R.string.settings_create_alert_cancel_button, null)
                .show();
        });

        // Init language switcher button
        String[] languages = getResources().getStringArray(R.array.settings_languages);
        int language = settings.getInt("language", Config.SETTINGS_LANGUAGE_DEFAULT);
        ((TextView)findViewById(R.id.settings_language_label)).setText(languages[language]);

        ((LinearLayout)findViewById(R.id.settings_language_button)).setOnClickListener((View view) -> {
            new AlertDialog.Builder(this)
                .setTitle(R.string.settings_language_alert_title_label)
                .setSingleChoiceItems(languages, language, (DialogInterface dialog, int which) -> {
                    dialog.dismiss();
                    if (language != which) {
                        SharedPreferences.Editor settingsEditor = settings.edit();
                        settingsEditor.putInt("language", which);
                        settingsEditor.apply();
                        recreate();
                    }
                })
                .setNegativeButton(R.string.settings_language_alert_cancel_button, null)
                .show();
        });

        // Init themes switcher button
        String[] themes = getResources().getStringArray(R.array.settings_themes);
        int theme = settings.getInt("theme", Config.SETTINGS_THEME_DEFAULT);
        ((TextView)findViewById(R.id.settings_theme_label)).setText(themes[theme]);

        ((LinearLayout)findViewById(R.id.settings_theme_button)).setOnClickListener((View view) -> {
            new AlertDialog.Builder(this)
                .setTitle(R.string.settings_theme_alert_title_label)
                .setSingleChoiceItems(themes, theme, (DialogInterface dialog, int which) ->  {
                    dialog.dismiss();
                    if (theme != which) {
                        SharedPreferences.Editor settingsEditor = settings.edit();
                        settingsEditor.putInt("theme", which);
                        settingsEditor.apply();
                        recreate();
                    }
                })
                .setNegativeButton(R.string.settings_theme_alert_cancel_button, null)
                .show();
        });

        // Init zoom button
        Switch zoomSwitch = (Switch)findViewById(R.id.settings_zoom_switch);
        zoomSwitch.setChecked(settings.getBoolean("zoom", Config.SETTINGS_ZOOM_DEFAULT));
        zoomSwitch.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            SharedPreferences.Editor settingsEditor = settings.edit();
            settingsEditor.putBoolean("zoom", isChecked);
            settingsEditor.apply();
        });

        ((LinearLayout)findViewById(R.id.settings_zoom_button)).setOnClickListener((View view) -> {
            zoomSwitch.toggle();
        });

        // Init version button easter egg
        try {
            ((TextView)findViewById(R.id.settings_version_label)).setText("v" + getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        int versionButtonClickCounterHolder[] = { 0 };
        ((LinearLayout)findViewById(R.id.settings_version_button)).setOnClickListener((View view) -> {
            versionButtonClickCounterHolder[0]++;
            if (versionButtonClickCounterHolder[0] == 8) {
                versionButtonClickCounterHolder[0] = 0;
                Toast.makeText(this, R.string.settings_version_message, Toast.LENGTH_SHORT).show();
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://youtu.be/dQw4w9WgXcQ?t=43")));
            }
        });

        // Init rate button
        ((LinearLayout)findViewById(R.id.settings_rate_button)).setOnClickListener((View view) -> {
            Utils.openStorePage(this);
        });

        // Init share button
        ((LinearLayout)findViewById(R.id.settings_share_button)).setOnClickListener((View view) -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.settings_share_message) + " " + Utils.getStorePageUrl(this));
            startActivity(Intent.createChooser(intent, null));
        });

        // Init about button
        ((LinearLayout)findViewById(R.id.settings_about_button)).setOnClickListener((View view) -> {
            new AlertDialog.Builder(this)
                .setTitle(R.string.settings_about_alert_title_label)
                .setMessage(R.string.settings_about_alert_message_label)
                .setNegativeButton(R.string.settings_about_alert_website_button, (DialogInterface dialog, int which) ->  {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Config.SETTINGS_ABOUT_WEBSITE_URL)));
                })
                .setPositiveButton(R.string.settings_about_alert_ok_button, null)
                .show();
        });

        // Init footer button
        ((TextView)findViewById(R.id.settings_footer_button)).setOnClickListener((View view) -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Config.SETTINGS_ABOUT_WEBSITE_URL)));
        });

        // Check if accounts update timeout is ellapsed
        long accountsUpdateTime = settings.getLong("accounts_update_time", 0);
        if (System.currentTimeMillis() - accountsUpdateTime > Config.SETTINGS_ACCOUNTS_UPDATE_TIMEOUT) {
            // Save new time
            SharedPreferences.Editor settingsEditor = settings.edit();
            settingsEditor.putLong("accounts_update_time", System.currentTimeMillis());
            settingsEditor.apply();

            // Update accounts
            updateAccounts();
        }
    }

    // When the login page is succesfull add and open account
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SettingsActivity.LOGIN_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // When the login activity returns an account add and open it
            Account account = (Account)data.getExtras().getSerializable("account");
            saveAccount(account);
            openAccount(account);
        }
    }

    // Update accounts info
    private void updateAccounts() {
        // Load accounts json
        JSONArray jsonAccounts;
        try {
            jsonAccounts = new JSONArray(settings.getString("accounts", "[]"));
        } catch (Exception exception) {
            exception.printStackTrace();
            jsonAccounts = new JSONArray();
        }

        // Create request url
        String url = Config.APP_WARQUEST_URL + "/api/players?key=" + Config.APP_WARQUEST_API_KEY;
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
        FetchDataTask.with(this).load(url).then((String response) -> {
            try {
                // Parse response
                JSONObject jsonResponse = new JSONObject(response);

                // When successfull
                if (jsonResponse.getBoolean("success")) {
                    JSONArray jsonPlayers = jsonResponse.getJSONArray("players");
                    JSONArray newJsonAccounts = new JSONArray();

                    // When successfull clear list
                    accountsAdapter.clear();

                    // Add newly fetched accounts to the list and to array
                    for (int i = 0; i < jsonPlayers.length(); i++) {
                        Account account = Account.fromJsonApiResponse(jsonPlayers.getJSONObject(i));
                        accountsAdapter.add(account);
                        newJsonAccounts.put(account.toJson());
                    }

                    // And save new accounts data
                    SharedPreferences.Editor settingsEditor = settings.edit();
                    settingsEditor.putString("accounts", newJsonAccounts.toString());
                    settingsEditor.apply();
                    return;
                }
            }
            catch (Exception exception) {
                // Show message when response json parse failed
                Toast.makeText(this, getResources().getString(R.string.settings_response_error_message), Toast.LENGTH_SHORT).show();
            }
        }, (Exception exception) -> {
            // Show message when connection error occurt
            Toast.makeText(this, getResources().getString(R.string.settings_connection_error_message), Toast.LENGTH_SHORT).show();
        });
    }

    // Save an new account
    private void saveAccount(Account account) {
        try  {
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
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    // Remove account from the accounts list
    public void removeAccount(Account account) {
        android.util.Log.e("wq", "Remove " + account.getNickname());

        try {
            JSONArray jsonAccounts = new JSONArray(settings.getString("accounts", "[]"));

            // Remove account from json accounts
            int position = 0;
            for (int i = 0; i < jsonAccounts.length(); i++) {
                Account otherAccount = Account.fromJson(jsonAccounts.getJSONObject(i));
                if (otherAccount.getId() == account.getId()) {
                    position = i;
                    jsonAccounts.remove(i);
                }
            }

            // Remove account from accounts list
            accountsAdapter.clear();
            for (int i = 0; i < jsonAccounts.length(); i++) {
                accountsAdapter.add(Account.fromJson(jsonAccounts.getJSONObject(i)));
            }

            // Save the new json accounts list
            SharedPreferences.Editor settingsEditor = settings.edit();
            settingsEditor.putString("accounts", jsonAccounts.toString());
            settingsEditor.apply();

            // Do some extra checks when the account was the selected account
            long selectedAccountId = settings.getLong("selected_account_id", -1);
            if (selectedAccountId == account.getId()) {
                // When no accounts are left create new own
                if (jsonAccounts.length() == 0) {
                    // Send register request
                    FetchDataTask.with(this).load(Config.APP_WARQUEST_URL + "/api/auth/register?key=" + Config.APP_WARQUEST_API_KEY).then((String response) -> {
                        try {
                            // Parse response
                            JSONObject jsonResponse = new JSONObject(response);

                            // When successfull add and update accounts list
                            if (jsonResponse.getBoolean("success")) {
                                // Parse account
                                Account otherAccount = Account.fromJsonApiResponse(jsonResponse);

                                // Save it
                                saveAccount(otherAccount);

                                // Add to the accounts list
                                accountsAdapter.add(otherAccount);

                                // Select it
                                selectAccount(otherAccount);
                                return;
                            }
                        }
                        catch (Exception exception) {
                            // Show message when response json parse failed
                            Toast.makeText(this, getResources().getString(R.string.settings_response_error_message), Toast.LENGTH_SHORT).show();
                        }
                    }, (Exception exception) -> {
                        // Show message when connection error occurt
                        Toast.makeText(this, getResources().getString(R.string.settings_connection_error_message), Toast.LENGTH_SHORT).show();
                    });
                }

                // When more accounts exists select the next one
                else {
                    selectAccount(Account.fromJson(jsonAccounts.getJSONObject(position == 0 ? 0 : position - 1)));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    // Select and account in the accounts list
    private void selectAccount(Account account) {
        accountsAdapter.setSelectedAccountId(account.getId());

        SharedPreferences.Editor settingsEditor = settings.edit();
        settingsEditor.putLong("selected_account_id", account.getId());
        settingsEditor.apply();
    }

    // Open an account and close activity
    private void openAccount(Account account) {
        SharedPreferences.Editor settingsEditor = settings.edit();
        settingsEditor.putLong("selected_account_id", account.getId());
        settingsEditor.apply();

        finish();
    }
}
