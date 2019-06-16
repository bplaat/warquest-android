package nl.plaatsoft.warquest3;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;

public class SettingsActivity extends Activity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ((ImageView)findViewById(R.id.back_button)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        EditText usernameInput = (EditText)findViewById(R.id.username_input);
        usernameInput.setText(preferences.getString("username", null));
        usernameInput.setSelection(usernameInput.getText().length());

        EditText passwordInput = (EditText)findViewById(R.id.password_input);
        passwordInput.setText(preferences.getString("password", null));

        Switch zoomSwitch = (Switch)findViewById(R.id.zoom_switch);
        zoomSwitch.setChecked(preferences.getBoolean("zoom", true));

        ((Button)findViewById(R.id.save_button)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("username", usernameInput.getText().toString());
                editor.putString("password", passwordInput.getText().toString());
                editor.putBoolean("zoom", zoomSwitch.isChecked());
                editor.apply();

                Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
    }
}
