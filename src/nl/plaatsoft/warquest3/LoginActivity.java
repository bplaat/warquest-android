package nl.plaatsoft.warquest3;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import java.net.URLEncoder;
import org.json.JSONObject;

// Login activity
public class LoginActivity extends BaseActivity implements FetchDataTask.OnLoadListener {
    // Create activity
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Login back button handler
        ((ImageView)findViewById(R.id.login_back_button)).setOnClickListener((View view) -> {
            finish();
        });

        // Get input fields
        EditText usernameInput = (EditText)findViewById(R.id.login_username_input);
        usernameInput.requestFocus();
        EditText passwordInput = (EditText)findViewById(R.id.login_password_input);

        // Login button handler
        ((Button)findViewById(R.id.login_login_button)).setOnClickListener((View view) -> {
            // Create request url for login request
            String url = null;
            try {
                url = Config.WARQUEST_URL + "/api/auth/login?key=" + Config.WARQUEST_API_KEY +
                    "&username=" + URLEncoder.encode(usernameInput.getText().toString(), "UTF-8") +
                    "&password=" + URLEncoder.encode(passwordInput.getText().toString(), "UTF-8");
            } catch (Exception exception) {
                exception.printStackTrace();
            }

            // Do request
            new FetchDataTask(this, url, false, false, this);
        });
    }

    // Login request onload handler
    public void onLoad(String response) {
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
    }
}
