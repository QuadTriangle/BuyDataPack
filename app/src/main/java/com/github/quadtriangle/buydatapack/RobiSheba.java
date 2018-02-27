package com.github.quadtriangle.buydatapack;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.content.Context.MODE_PRIVATE;


public class RobiSheba {
    // Robi Sheba API
    private String BASE;
    private String AUTO_LOGIN_INFO;
    private final String LOGIN = "/accounts/login";
    private final String LOGOUT = "/accounts/logout";
    private final String AUTO_LOGIN = "/auto-login/check";
    private final String BALANCE = "/dashboard/get-airtel-details";
    private final String USAGE_HISTORY = "/bill/get-usage-history";
    private final String REWARDS = "/offer/get-available-campaigns";
    private final String PAYMENT_HISTORY = "/bill/get-payment-history";
    private final String PACKAGES = "/data-packages/get-data-packages";
    private final String MY_PACKAGE = "/data-packages/my-data-packages";
    private final String BUY_PACKAGE = "/data-packages/activate-data-package";
    private final String VOICE_PACK = "/voice-packages/get-current-voice-package";
    private final String AIRTEL_BASE = "https://ecare-app.robi.com.bd/airtel_sc/index.php?r=";
    private final String ROBI_BASE = "https://ecare-app.robi.com.bd/robi_sc_2_4/index.php?r=";
    private final String AIRTEL_AUTO_LOGIN_INFO = "http://appsuite.robi.com.bd/airtel_sc/getMsisdn.php";
    private final String ROBI_AUTO_LOGIN_INFO = "http://appsuite.robi.com.bd/robi_sc_2_4/getMsisdn.php";

    private Context ctx;
    private RequestBody formBody;
    private SharedPreferences loginPrefs;
    private final PersistentCookieJar cookieJar;
    private SharedPreferences.Editor loginPrefsEd;

    public String dataPlan;

    public RobiSheba(Context context, String operator) {
        ctx = context;
        loginPrefs = ctx.getSharedPreferences("loginPrefs", MODE_PRIVATE);
        loginPrefsEd = loginPrefs.edit();
        cookieJar =  new PersistentCookieJar(
                new SetCookieCache(), new SharedPrefsCookiePersistor(ctx));
        if (operator != null) setupOperator(operator);
    }

    public void setupOperator(String operator) {
        BASE = operator.equals("Airtel") ? AIRTEL_BASE : ROBI_BASE;
        AUTO_LOGIN_INFO = operator.equals("Airtel") ? AIRTEL_AUTO_LOGIN_INFO : ROBI_AUTO_LOGIN_INFO;
    }

    public String login(String number, String password) throws JSONException, IOException {
        if (number == null)
            return autoLogin();
        loginPrefsEd.putString("device_imsi", md5(number).substring(0, 16)).commit();
        formBuilder("login", number, password, null, null);
        String body = getRespBody(BASE + LOGIN, 15);
        JSONObject loginRespJson = new JSONObject(body);
        String status = loginStatus(body, loginRespJson);
        saveLoginInfo(status, loginRespJson);
        return status;
    }

    private String autoLogin() throws JSONException, IOException {
        formBuilder("getLoginInfo", null, null, null, null);
        String body = getRespBody(AUTO_LOGIN_INFO, 15);
        JSONObject respJson = new JSONObject(body);
        if (!respJson.getBoolean("success")) {
            return ctx.getString(R.string.auto_login_failed_msg);
        }
        String id = respJson.getString("id");
        String number = respJson.getString("msisdn");
        loginPrefsEd.putString("device_imsi", md5(number).substring(0, 16)).commit();
        formBuilder("autoLogin", number, null, id, null);
        body = getRespBody(BASE + AUTO_LOGIN, 15);
        JSONObject loginRespJson = new JSONObject(body);
        String status = loginStatus(body, loginRespJson);
        saveLoginInfo(status, loginRespJson);
        return status;
    }

    public JSONObject getPackages() throws JSONException, IOException {
        formBuilder("getPack", null, null, null, null);
        String body = getRespBody(BASE + PACKAGES, 15);
        return new JSONObject(body);
    }

    public String buyPack(String secret) throws JSONException, IOException {
        String formType = secret != null ? "buyReqSecret" : "buyReq";
        formBuilder(formType, null, null, null, secret);
        String body = getRespBody(BASE + BUY_PACKAGE, 15);
        return buyStatus(body);
    }

    public JSONObject getAccountBalance() throws JSONException, IOException {
        formBuilder("balance", null, null, null, null);
        String body = getRespBody(BASE + BALANCE, 15);
        return new JSONObject(body);
    }

    public JSONObject getDataBalance() throws JSONException, IOException {
        formBuilder("myPacks", null, null, null, null);
        String body = getRespBody(BASE + MY_PACKAGE, 15);
        return new JSONObject(body);
    }

//    public JSONObject getVoicePack() throws JSONException, IOException {
//        formBuilder("voicePack", null, null, null, null);
//        String body = getRespBody(BASE + VOICE_PACK);
//        return new JSONObject(body);
//    }

    public JSONObject getRewards() throws JSONException, IOException {
        formBuilder("rewards", null, null, null, null);
        String body = getRespBody(BASE + REWARDS, 15);
        return new JSONObject(body);
    }

//    public void logout() throws JSONException, IOException {
//        formBuilder("logout", null, null, null, null);
//        String body = getRespBody(BASE + LOGOUT);
//    }

    public JSONObject getUsageHistory() throws JSONException, IOException {
        formBuilder("usageInfo", null, null, null, null);
        String body = getRespBody(BASE + USAGE_HISTORY, 60);
        return new JSONObject(body);
    }
//
//    public JSONObject getRechargeHistory() throws JSONException, IOException {
//        formBuilder("payInfo", null, null, null, null);
//        String body = getRespBody(BASE + PAYMENT_HISTORY);
//        return new JSONObject(body);
//    }

    private String getRespBody(String URL, int readTimeOut) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(readTimeOut, TimeUnit.SECONDS)
                .build();
        Request request = new Request.Builder()
                .url(URL)
                .post(formBody)
                .build();
        Response response = client.newCall(request).execute();
        String body = response.body().string();
        Log.d("buydatapack", body);
        return body;
    }

    private void formBuilder(String formType, String number, String password, String id,
                             String secret) throws JSONException {
        FormBody.Builder builder = new FormBody.Builder()
                .add("app_type", "mobile_app")
                .add("network_type", "mobile")
                .add("language", "en")
                .add("device_imsi", loginPrefs.getString("device_imsi", ""));
        // (-_-)
        switch (formType) {
            case "login":
                builder.add("conn", number)
                        .add("password", password);
                break;
            case "autoLogin":
                builder.add("conn", number)
                        .add("id", id);
                break;
            case "getLoginInfo":
                break;
            case "buyReqSecret":
                builder.add("secret_code", secret);
            case "buyReq":
                builder.add("plan_id", dataPlan)
                        .add("name", dataPlan);
            case "usageInfo":
            case "payInfo":
                switch (formType) {
                    case "usageInfo":
                    case "payInfo":
                        builder.add("startDate", String.format(Locale.ENGLISH, "%1$td-%1$tb-%1$tY", getDateDaysAgo(10)))
                                .add("endDate", String.format(Locale.ENGLISH, "%1$td-%1$tb-%1$tY", new Date()));
                }
            case "myPacks":
            case "rewards":
            case "voicePack":
            case "balance":
            case "logout":
            case "getPack":
                builder.add("session_key", loginPrefs.getString("session_key", ""))
                        .add("ref_number", loginPrefs.getString("ref_number", ""))
                        .add("conn_type", loginPrefs.getString("conn_type", ""))
                        .add("operator", loginPrefs.getString("operator", ""))
                        .add("conn", loginPrefs.getString("conn", ""));
                break;
            default:
                break;
        }
        formBody = builder.build();
    }

    private String loginStatus(String body, JSONObject respJson) {
        if (respJson.has("sessionKey")) {
            return "success";
        } else if (body.contains("invalid_user_credentials")) {
            return "invalid";
        } else if (body.contains("err_unknown")) {
            return "down";
        } else if (body.contains("maintenance")) {
            return "maintenance";
        } else {
            return body;
        }
    }

    private String buyStatus(String body) {
        if (body.contains("pin_sent")) {
            return ctx.getString(R.string.secret_sent);
        } else if (body.contains("Invalid Secret")) {
            return ctx.getString(R.string.wrong_secret);
        } else if (body.contains("\"success\":true")) {
            return ctx.getString(R.string.status_success);
        } else {
            return body;
        }
    }

    // https://stackoverflow.com/a/39420545
    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            BigInteger md5Data = new BigInteger(1, md.digest(input.getBytes()));
            return String.format("%032x", md5Data);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "0123456789abcdef";
        }
    }

    // https://stackoverflow.com/a/20073222
    public static Date getDateDaysAgo(int numOfDaysAgo) {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DAY_OF_YEAR, -1 * numOfDaysAgo);
        return c.getTime();
    }

    private void saveLoginInfo(String status, JSONObject loginRespJson) throws JSONException {
        if (status.equals("success")) {
            loginPrefsEd.putBoolean("isLoggedIn", true);
            loginPrefsEd.putString("session_key", loginRespJson.getString("sessionKey"));
            loginPrefsEd.putString("ref_number", loginRespJson.getJSONObject("user").getString("mobile"));
            loginPrefsEd.putString("conn_type", loginRespJson.getJSONArray("connections")
                    .getJSONObject(0).getString("conn_type"));
            loginPrefsEd.putString("operator", loginRespJson.getJSONArray("connections")
                    .getJSONObject(0).getString("operator"));
            loginPrefsEd.putString("conn", loginRespJson.getJSONArray("connections")
                    .getJSONObject(0).getString("conn"));
        }
        loginPrefsEd.apply();
    }
}
