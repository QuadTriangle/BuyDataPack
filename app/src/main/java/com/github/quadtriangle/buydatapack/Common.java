package com.github.quadtriangle.buydatapack;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.Locale;

public class Common {
    public static void setAppTheme(Context ctx) {
        ctx.setTheme(getAppTheme(ctx));
    }

    public static int getAppTheme(Context ctx) {
        String selectedTheme = PreferenceManager.getDefaultSharedPreferences(ctx).getString("theme", "Light");
        switch (selectedTheme) {
            case "Light":
                return R.style.LightTheme;
            case "Dark":
                return R.style.DarkTheme;
            default:
                return R.style.LightTheme;
        }
    }

    public static Context setAppLocale(Context baseContext) {
        String lang = PreferenceManager.getDefaultSharedPreferences(baseContext).getString("language", "en");
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        Resources res = baseContext.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        if (Build.VERSION.SDK_INT >= 17) {
            config.setLocale(locale);
            baseContext = baseContext.createConfigurationContext(config);
        } else {
            config.locale = locale;
            res.updateConfiguration(config, res.getDisplayMetrics());
        }

        return baseContext;
    }

    public static OnSharedPreferenceChangeListener registerPrefsChangeListener(AppCompatActivity activity) {
        OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> {
            if (key.equals("theme") || key.equals("language")) {
                activity.recreate();
            }
        };
        PreferenceManager.getDefaultSharedPreferences(activity)
                .registerOnSharedPreferenceChangeListener(listener);
        return listener;
    }

    public static void setupToolbar(AppCompatActivity activity, boolean showUpBtn) {
        Toolbar toolbar = activity.findViewById(R.id.toolbar);
        activity.setSupportActionBar(toolbar);
        if (showUpBtn) {
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    public static MaterialDialog showIndeterminateProgressDialog(Context ctx,
                                                                 int title, int content) {
        return new MaterialDialog.Builder(ctx)
                .title(title)
                .content(content)
                .cancelable(false)
                .progress(true, 0)
                .progressIndeterminateStyle(true)
                .show();
    }

    public static String getOperator(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("operator", "Airtel");
    }
}
