package com.github.quadtriangle.buydatapack;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.customtabs.CustomTabsIntent;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.stfalcon.smsverifycatcher.SmsVerifyCatcher;

import org.json.JSONException;

import java.io.IOException;


public class LoginActivity extends AppCompatActivity {

    private EditText mNumberView;
    private EditText mPasswordView;
    private MaterialDialog dialog;
    private View mainLayout;
    private View loginLayout;

    private Context context;
    private LoginTask mAuthTask = null;
    private RobiSheba robiSheba;
    private CustomTabsIntent customTabsIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        robiSheba = new RobiSheba(this);
        Common.setAppTheme(this);
        setContentView(R.layout.activity_login);
        Common.setupToolbar(this, true);
        setupView();
        SmsVerifyCatcher.isStoragePermissionGranted(this, null);
        setupCustomTabs();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Common.setAppLocale(base));
    }

    private void setupCustomTabs() {
        customTabsIntent = new CustomTabsIntent.Builder()
                .addDefaultShareMenuItem()
                .setToolbarColor(this.getResources()
                        .getColor(R.color.colorPrimary))
                .setShowTitle(true)
                .build();
    }

    private void setupView() {
        mainLayout = findViewById(R.id.main_layout);
        loginLayout = findViewById(R.id.login_layout);
        mNumberView = findViewById(R.id.number);
        mPasswordView = findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener((textView, id, event) -> {
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin();
                return true;
            }
            return false;
        });
        Button mSignInButton = findViewById(R.id.login_btn);
        mSignInButton.setOnClickListener(view -> attemptLogin());
    }

    public void onBackBtn(View view) {
        loginLayout.setVisibility(View.GONE);
        mainLayout.setVisibility(View.VISIBLE);
    }

    public void onLoginWithPasswordBtn(View view) {
        mainLayout.setVisibility(View.GONE);
        loginLayout.setVisibility(View.VISIBLE);
    }

    public void onAutoLoginBtn(View view) {
        dialog = Common.showIndeterminateProgressDialog(context, R.string.login,
                R.string.trying_auto_login);
        mAuthTask = new LoginTask(null, null);
        mAuthTask.execute((Void) null);
    }

    public void onForgotPassBtn(View view) {
        String url = "https://ecare.robi.com.bd/myairtel/faces/pages/forgetUnPass/forgetPassContainer.jspx";
        customTabsIntent.launchUrl(this, Uri.parse(url));
    }

    public void onRegistrationBtn(View view) {
        String url = "https://ecare.robi.com.bd/selfcare/faces/register1";
        customTabsIntent.launchUrl(this, Uri.parse(url));
    }

    private boolean isNumberValid(String number) {
        return number.startsWith("016") && number.length() == 11;
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 4;
    }


    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mNumberView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String number = mNumberView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid numbers.
        if (TextUtils.isEmpty(number)) {
            mNumberView.setError(getString(R.string.error_field_required));
            focusView = mNumberView;
            cancel = true;
        } else if (!isNumberValid(number)) {
            mNumberView.setError(getString(R.string.error_invalid_number));
            focusView = mNumberView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            InputMethodManager inputManager = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            try {
                assert inputManager != null;
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
            } catch (NullPointerException e) {
                e.printStackTrace();

            }
            dialog = Common.showIndeterminateProgressDialog(context, R.string.login, R.string.trying_login);
            mAuthTask = new LoginTask(number, password);
            mAuthTask.execute((Void) null);
        }
    }

    public class LoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mNumber;
        private final String mPassword;
        private String status;

        LoginTask(String number, String password) {
            mNumber = number;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                status = robiSheba.login(mNumber, mPassword);
            } catch (JSONException e) {
                e.printStackTrace();
                status = e.toString();
            } catch (IOException e) {
                e.printStackTrace();
                status = getString(R.string.connect_problem_msg);
            }
            return status.equals("success");
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            dialog.dismiss();
            if (success) {
                Toast.makeText(context, R.string.login_success, Toast.LENGTH_LONG).show();
                finish();
            } else if (status.equals("invalid")) {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            } else {
                showStatus(status);
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            dialog.dismiss();
        }

        private void showStatus(String status) {
            final String dialogTitle = getString(R.string.login_failed);
            String message;
            switch (status) {
                case "maintenance":
                    message = getString(R.string.maintenance_msg);
                    break;
                case "down":
                    message = getString(R.string.down_msg);
                    break;
                default:
                    message = status;
                    break;
            }

            final String dialogMessage = message;
            new MaterialDialog.Builder(context)
                    .title(dialogTitle)
                    .content(dialogMessage)
                    .negativeText(R.string.ok)
                    .show();
        }
    }
}

