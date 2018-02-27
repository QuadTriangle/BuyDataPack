package com.github.quadtriangle.buydatapack;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.customtabs.CustomTabsIntent;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.json.JSONException;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLHandshakeException;


public class LoginActivity extends AppCompatActivity {

    private EditText mNumberView;
    private EditText mPasswordView;
    private MaterialDialog dialog;
    private MaterialDialog selectOperator;
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
        robiSheba = new RobiSheba(this, Common.getOperator(this));
        Common.setAppTheme(this);
        setContentView(R.layout.activity_login);
        Common.setupToolbar(this, true);
        setupView();
        setupCustomTabs();
        showChangeLocaleOnFirstStart();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Common.setAppLocale(base));
    }

    private void showChangeLocaleOnFirstStart() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefsEdit = prefs.edit();
        if (prefs.getBoolean("isFirstStart", true)) {
            new MaterialDialog.Builder(context)
                    .title("Language")
                    .content(R.string.change_lang_msg)
                    .cancelable(false)
                    .positiveText("English")
                    .negativeText("বাংলা")
                    .onAny((dialog, which) -> {
                        if (which.equals(DialogAction.POSITIVE)) {
                            prefsEdit.putString("language", "en");
                        } else {
                            prefsEdit.putString("language", "bn");
                        }
                        prefsEdit.putBoolean("isFirstStart", false);
                        prefsEdit.apply();
                        // recreate();
                    })
                    .show();
        }
    }

    private void autoLoginSelectOperator() {
        String[] items = {"Airtel", "Robi"};
        selectOperator = new MaterialDialog.Builder(context)
                .title(R.string.select_operator)
                .items(items)
                .cancelable(false)
                .alwaysCallSingleChoiceCallback()
                .itemsCallbackSingleChoice(0, (dialog, view, which, text) -> true)
                .positiveText(R.string.ok)
                .onPositive((dialog, which) -> {
                    int index = selectOperator.getSelectedIndex();
                    robiSheba.setupOperator(items[index]);
                    setOperator(items[index]);
                    this.dialog = Common.showIndeterminateProgressDialog(context, R.string.login,
                            R.string.trying_auto_login);
                    mAuthTask = new LoginTask(null, null);
                    mAuthTask.execute((Void) null);
                })
                .show();
    }

    private void setOperator(String operator) {
        SharedPreferences.Editor prefsEdit = PreferenceManager.
                getDefaultSharedPreferences(context).edit();
        prefsEdit.putString("operator", operator);
        prefsEdit.apply();
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
        autoLoginSelectOperator();
    }

    public void onForgotPassBtn(View view) {
        new MaterialDialog.Builder(context)
                .content(R.string.select_operator)
                .positiveText("Airtel")
                .negativeText("Robi")
                .onAny((dialog, which) -> {
                    String url;
                    if (which.equals(DialogAction.POSITIVE)) {
                        url = "https://ecare.robi.com.bd/myairtel/faces/pages/forgetUnPass/forgetPassContainer.jspx";
                    } else {
                        url = "https://ecare.robi.com.bd/selfcare/faces/forgetUnamePw";
                    }
                    customTabsIntent.launchUrl(this, Uri.parse(url));
                })
                .show();
    }

    public void onRegistrationBtn(View view) {
        String url = "https://ecare.robi.com.bd/selfcare/faces/register1";
        customTabsIntent.launchUrl(this, Uri.parse(url));
    }

    private boolean isNumberValid(String number) {
        return (number.startsWith("016") || number.startsWith("018")) && number.length() == 11;
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
            String operator = number.startsWith("016") ? "Airtel" : "Robi";
            setOperator(operator);
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
            } catch (UnknownHostException | SocketException e) {
                e.printStackTrace();
                status = getString(R.string.connect_problem_msg);
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
                status = getString(R.string.server_overloaded_msg);
            } catch (SSLHandshakeException e) {
                e.printStackTrace();
                status = getString(R.string.connection_failed);
            } catch (JSONException | IOException e) {
                e.printStackTrace();
                status = e.toString();
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
            MaterialDialog.Builder builder = new MaterialDialog.Builder(context)
                    .title(dialogTitle)
                    .content(message)
                    .cancelable(false)
                    .negativeText(R.string.ok);
            if (status.equals(getString(R.string.auto_login_failed_msg))) {
                builder.title(R.string.auto_login_failed)
                        .onNegative((dialog, which) -> onLoginWithPasswordBtn(null));
            }
            builder.show();
        }
    }
}

