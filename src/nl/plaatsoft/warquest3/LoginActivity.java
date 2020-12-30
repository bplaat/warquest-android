package nl.plaatsoft.warquest3;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import java.net.URLEncoder;
import org.json.JSONObject;

public class LoginActivity extends BaseActivity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ((ImageButton)findViewById(R.id.login_back_button)).setOnClickListener((View view) -> {
            finish();
        });

        EditText usernameInput = (EditText)findViewById(R.id.login_username_input);
        EditText passwordInput = (EditText)findViewById(R.id.login_password_input);

        ((Button)findViewById(R.id.login_login_button)).setOnClickListener((View view) -> {
            // Do an API login request
            String url = null;
            try {
                url = Config.APP_WARQUEST_URL + "/api/auth/login?key=" + Config.APP_WARQUEST_API_KEY +
                    "&username=" + URLEncoder.encode(usernameInput.getText().toString(), "UTF-8") +
                    "&password=" + URLEncoder.encode(passwordInput.getText().toString(), "UTF-8");
            } catch (Exception exception) {
                exception.printStackTrace();
            }

            FetchDataTask.with(this).load(url).then((String response) -> {
                try {
                    // Parse json response
                    JSONObject jsonResponse = new JSONObject(response);

                    // When success parse account and return to parent activity
                    if (jsonResponse.getBoolean("success")) {
                        Intent intent = new Intent();
                        Bundle bundle = new Bundle();
                        bundle.putSerializable("account", Account.fromJsonApiResponse(jsonResponse));
                        intent.putExtras(bundle);
                        setResult(Activity.RESULT_OK, intent);
                        finish();
                    }

                    // Show message when banned
                    else if (jsonResponse.getBoolean("banned")) {
                        Toast.makeText(this, getResources().getString(R.string.login_banned_message), Toast.LENGTH_SHORT).show();
                    }

                    // Show message when username and / or password is wrong
                    else {
                        Toast.makeText(this, getResources().getString(R.string.login_failed_message), Toast.LENGTH_SHORT).show();
                    }
                }

                // Show toast when request failed
                catch (Exception exception) {
                    exception.printStackTrace();

                    Toast.makeText(this, getResources().getString(R.string.login_error_message), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
