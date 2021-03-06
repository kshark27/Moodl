package com.herbron.moodl.Activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.applandeo.FilePicker;
import com.applandeo.listeners.OnSelectFileListener;
import com.herbron.moodl.BuildConfig;
import com.herbron.moodl.DataManagers.DataCrypter;
import com.herbron.moodl.DataManagers.DatabaseManager;
import com.herbron.moodl.DataManagers.PreferencesManager;
import com.herbron.moodl.FingerprintToolkit.FingerprintDialogFragment;
import com.herbron.moodl.FingerprintToolkit.FingerprintHandler;
import com.herbron.moodl.MoodlBox;
import com.herbron.moodl.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Drawable gradientDrawable = getResources().getDrawable(R.drawable.gradient_background);

        actionBar.setBackgroundDrawable(gradientDrawable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        //loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || ExchangePreferenceFragment.class.getName().equals(fragmentName)
                || MainPreferenceFragment.class.getName().equals(fragmentName);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class ExchangePreferenceFragment extends PreferenceFragment {

        private static final String KEY_NAME = "NAUKEY";
        private Cipher cipher;
        private KeyStore keyStore;
        private KeyGenerator keyGenerator;
        private FingerprintManager.CryptoObject cryptoObject;
        private FingerprintManager fingerprintManager;
        private KeyguardManager keyguardManager;

        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_exchange);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference("hitbtc_publickey"));
            bindPreferenceSummaryToValue(findPreference("binance_publickey"));

            bindPreferenceSummaryToValue(findPreference("hitbtc_privatekey"));
            bindPreferenceSummaryToValue(findPreference("binance_privatekey"));

            findPreference("enable_fingerprint").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {

                    Log.d("moodl", "> " + o + " " + preference);

                    return (boolean) o;
                }
            });

            findPreference("enable_hitbtc").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    boolean isChecked = ((SwitchPreference) findPreference("enable_hitbtc")).isChecked();

                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());

                    SharedPreferences.Editor editor = preferences.edit();

                    editor.putBoolean("mustUpdateSummary", true);
                    editor.apply();

                    return isChecked;
                }
            });

            findPreference("enable_binance").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    boolean isChecked = ((SwitchPreference) findPreference("enable_binance")).isChecked();

                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());

                    SharedPreferences.Editor editor = preferences.edit();

                    editor.putBoolean("mustUpdateSummary", true);
                    editor.apply();

                    return isChecked;
                }
            });

            startFingerprintProtocol();
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                //startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }


        private void startFingerprintProtocol()
        {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.getActivity().getBaseContext());
            FingerprintDialogFragment newFragment = FingerprintDialogFragment.newInstance();
            SwitchPreference touchdIdSwitch = (SwitchPreference) findPreference("enable_fingerprint");

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                keyguardManager = (KeyguardManager) this.getActivity().getSystemService(KEYGUARD_SERVICE);
                fingerprintManager = (FingerprintManager) this.getActivity().getSystemService(FINGERPRINT_SERVICE);

                try {
                    if(!fingerprintManager.isHardwareDetected())
                    {
                        touchdIdSwitch.setEnabled(false);
                    }

                    if(ActivityCompat.checkSelfPermission(this.getActivity().getBaseContext(), Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED)
                    {
                        touchdIdSwitch.setEnabled(false);
                    }

                    if(!fingerprintManager.hasEnrolledFingerprints())
                    {
                        touchdIdSwitch.setEnabled(false);
                    }

                    if(!keyguardManager.isKeyguardSecure())
                    {
                        touchdIdSwitch.setEnabled(false);
                    }
                    else
                    {
                        try {
                            generateKey();
                        } catch (FingerprintException e) {
                            e.printStackTrace();
                        }

                        if(initCipher())
                        {
                            cryptoObject = new FingerprintManager.CryptoObject(cipher);

                            FingerprintHandler helper = new FingerprintHandler(this.getActivity().getBaseContext(), newFragment);
                            helper.startAuth(fingerprintManager, cryptoObject);
                        }
                    }
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }

            if(preferences.getBoolean("enable_fingerprint", false))
            {
                newFragment.setCancelable(false);
                newFragment.show(getFragmentManager(), "dialog");
            }
        }

        @TargetApi(23)
        private void generateKey() throws FingerprintException
        {
            try {
                keyStore = KeyStore.getInstance("AndroidKeyStore");
                keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
                keyStore.load(null);
                keyGenerator.init(new KeyGenParameterSpec.Builder(KEY_NAME, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                        .setUserAuthenticationRequired(true)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                        .build());

                keyGenerator.generateKey();

            } catch (KeyStoreException
                    | NoSuchAlgorithmException
                    | NoSuchProviderException
                    | InvalidAlgorithmParameterException
                    | CertificateException
                    | IOException e) {
                e.printStackTrace();
                throw new FingerprintException(e);
            }
        }

        @TargetApi(23)
        public boolean initCipher()
        {
            try {
                cipher = Cipher.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES + "/"
                                + KeyProperties.BLOCK_MODE_CBC + "/"
                                + KeyProperties.ENCRYPTION_PADDING_PKCS7);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
                throw new RuntimeException("Failed to get Cipher", e);
            }

            try {
                keyStore.load(null);
                SecretKey key = (SecretKey) keyStore.getKey(KEY_NAME, null);
                cipher.init(Cipher.ENCRYPT_MODE, key);
                return true;
            } catch (KeyPermanentlyInvalidatedException e) {
                return false;
            } catch (KeyStoreException | CertificateException
                    | UnrecoverableKeyException | IOException
                    | NoSuchAlgorithmException | InvalidKeyException e) {
                throw new RuntimeException("Failed to init Cipher", e);
            }
        }

        private class FingerprintException extends Exception {
            public FingerprintException(Exception e)
            {
                super(e);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class MainPreferenceFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_main);

            findPreference("version").setSummary(BuildConfig.VERSION_NAME);

            bindPreferenceSummaryToValue(findPreference("default_currency"));
            bindPreferenceSummaryToValue(findPreference("minimum_value_displayed"));

            PreferenceCategory developperCategory = (PreferenceCategory) findPreference("developper_category");

            if(!BuildConfig.DEBUG)
            {
                getPreferenceScreen().removePreference(developperCategory);
            }
            else
            {
                developperCategory.getPreference(0).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        File cacheDir = getActivity().getBaseContext().getCacheDir();
                        File[] cachedFiles = cacheDir.listFiles();

                        for(int i = 0; i < cachedFiles.length; i++)
                        {
                            if(cachedFiles[i].isFile())
                            {
                                cachedFiles[i].delete();
                            }
                        }

                        return false;
                    }
                });
            }

            findPreference("export").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    Context context = getActivity().getBaseContext();
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainPreferenceFragment.this.getActivity());
                    View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_export_data, null, true);
                    dialogBuilder.setView(dialogView);

                    final CheckBox backupManualEntriesCheckbox = dialogView.findViewById(R.id.checkboxBackupManualEntries);
                    final CheckBox backupWatchlistCheckbox = dialogView.findViewById(R.id.checkboxBackupWatchlist);
                    final CheckBox backupKeysCheckbox = dialogView.findViewById(R.id.checkboxBackupKeys);
                    final CheckBox enterPasswordCheckbox = dialogView.findViewById(R.id.checkboxEnterPassword);
                    final TextInputLayout textInputLayoutPassword = dialogView.findViewById(R.id.textInputLayoutPassword);
                    final TextView textViewFilePath = dialogView.findViewById(R.id.textViewFilePath);

                    File backupDirectory = new File(Environment.getExternalStorageDirectory(), getString(R.string.app_name));

                    createDefaultBackupDirectory(backupDirectory);

                    textViewFilePath.setText(backupDirectory.getAbsolutePath());
                    textViewFilePath.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            new FilePicker.Builder(getActivity(), new OnSelectFileListener() {
                                @Override
                                public void onSelect(File file) {
                                    textViewFilePath.setText(file.getAbsolutePath());
                                }
                            }).fileType(".moodl")
                                    .hideFiles(true)
                                    .directory(backupDirectory.getAbsolutePath())
                                    .mainDirectory(Environment.getExternalStorageDirectory().getAbsolutePath())
                                    .show();
                        }
                    });

                    enterPasswordCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                            if(b && textInputLayoutPassword.getVisibility() == View.GONE)
                            {
                                MoodlBox.expandH(textInputLayoutPassword);
                            }

                            if(!b && textInputLayoutPassword.getVisibility() == View.VISIBLE)
                            {
                                MoodlBox.collapseH(textInputLayoutPassword);
                            }
                        }
                    });

                    dialogBuilder.setTitle(getString(R.string.create_backup));
                    dialogBuilder.setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            checkPermissions();
                            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
                            Date currentDate = new Date();
                            String fileName = "Bakup_" + formatter.format(currentDate) + ".moodl";
                            DatabaseManager databaseManager = new DatabaseManager(getActivity().getBaseContext());

                            if(enterPasswordCheckbox.isChecked())
                            {
                                if(textInputLayoutPassword.getEditText().getText().equals(""))
                                {
                                    textInputLayoutPassword.setError(getString(R.string.must_be_filled));
                                }
                                else
                                {
                                    DataCrypter.updateKey(textInputLayoutPassword.getEditText().getText().toString());
                                }
                            }

                            File backupFile = new File(textViewFilePath.getText() + "/" + fileName);

                            try (PrintWriter printWriter = new PrintWriter(new FileWriter(backupFile, true))) {

                                try {
                                    JSONObject backupJson = new JSONObject();

                                    initiateJsonBackup(backupJson, enterPasswordCheckbox.isChecked());

                                    if(backupManualEntriesCheckbox.isChecked())
                                    {
                                        backupJson.put("transactions",
                                                databaseManager.getDatabaseBackup(getActivity().getBaseContext(),
                                                        DatabaseManager.TABLE_MANUAL_TRANSACTIONS,
                                                        enterPasswordCheckbox.isChecked()));
                                    }

                                    if(backupWatchlistCheckbox.isChecked())
                                    {
                                        backupJson.put("watchlist",
                                                databaseManager.getDatabaseBackup(getActivity().getBaseContext(),
                                                        DatabaseManager.TABLE_WATCHLIST,
                                                        enterPasswordCheckbox.isChecked()));
                                    }

                                    if(backupKeysCheckbox.isChecked())
                                    {
                                        backupJson.put("apiKeys",
                                                databaseManager.getDatabaseBackup(getActivity().getBaseContext(),
                                                        DatabaseManager.TABLE_EXCHANGE_KEYS,
                                                        enterPasswordCheckbox.isChecked()));
                                    }

                                    printWriter.write(backupJson.toString());
                                } catch (JSONException e) {
                                    Log.d("moodl", "Error while creating backup json " + e.getMessage());
                                }

                                printWriter.close();
                            } catch (IOException e) {
                                Log.d("moodl", "Error > " + e);
                            }

                            dialog.dismiss();
                        }
                    });

                    dialogBuilder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.dismiss();
                        }
                    });

                    AlertDialog alertDialog = dialogBuilder.create();
                    alertDialog.show();

                    return false;
                }
            });

            findPreference("import").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    Context context = getActivity().getBaseContext();
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainPreferenceFragment.this.getActivity());
                    View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_import_data, null, true);
                    dialogBuilder.setView(dialogView);

                    final TextView textViewFilePath = dialogView.findViewById(R.id.textViewFilePath);
                    final TextInputLayout textInputLayoutPassword = dialogView.findViewById(R.id.textInputLayoutPassword);
                    final CheckBox enterPasswordCheckbox = dialogView.findViewById(R.id.checkboxEnterPassword);
                    final CheckBox restoreManualEntriesCheckbox = dialogView.findViewById(R.id.checkboxRestoreEntries);
                    final CheckBox restoreWatchlistCheckbox = dialogView.findViewById(R.id.checkboxRestoreWatchlist);
                    final CheckBox restoreApiKeysCheckbox = dialogView.findViewById(R.id.checkboxRestoreKeys);

                    final CheckBox wipeManualEntriesCheckbox = dialogView.findViewById(R.id.checkboxWipeManualEntries);
                    final CheckBox wipeWatchlistCheckbox = dialogView.findViewById(R.id.checkboxWipeWatchlist);
                    final CheckBox wipeApiKeyxCheckbox = dialogView.findViewById(R.id.checkboxWipeAPIKeys);

                    restoreManualEntriesCheckbox.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(restoreManualEntriesCheckbox.isChecked())
                            {
                                MoodlBox.expandH(wipeManualEntriesCheckbox);
                            }
                            else
                            {
                                MoodlBox.collapseH(wipeManualEntriesCheckbox);
                            }
                        }
                    });

                    restoreWatchlistCheckbox.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(restoreWatchlistCheckbox.isChecked())
                            {
                                MoodlBox.expandH(wipeWatchlistCheckbox);
                            }
                            else
                            {
                                MoodlBox.collapseH(wipeWatchlistCheckbox);
                            }
                        }
                    });

                    restoreApiKeysCheckbox.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(restoreApiKeysCheckbox.isChecked())
                            {
                                MoodlBox.expandH(wipeApiKeyxCheckbox);
                            }
                            else
                            {
                                MoodlBox.collapseH(wipeApiKeyxCheckbox);
                            }
                        }
                    });

                    enterPasswordCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                            if(b && textInputLayoutPassword.getVisibility() == View.GONE)
                            {
                                MoodlBox.expandH(textInputLayoutPassword);
                            }

                            if(!b && textInputLayoutPassword.getVisibility() == View.VISIBLE)
                            {
                                MoodlBox.collapseH(textInputLayoutPassword);
                            }
                        }
                    });

                    File backupDirectory = new File(Environment.getExternalStorageDirectory(), getString(R.string.app_name));

                    createDefaultBackupDirectory(backupDirectory);

                    textViewFilePath.setText(backupDirectory.getAbsolutePath());
                    textViewFilePath.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            new FilePicker.Builder(getActivity(), new OnSelectFileListener() {
                                @Override
                                public void onSelect(File file) {
                                    textViewFilePath.setText(file.getAbsolutePath());
                                }
                            }).hideFiles(false)
                                    .directory(backupDirectory.getAbsolutePath())
                                    .mainDirectory(Environment.getExternalStorageDirectory().getAbsolutePath())
                                    .show();
                        }
                    });

                    dialogBuilder.setTitle(getString(R.string.restoreBackup));
                    dialogBuilder.setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int whichButton) {

                            checkPermissions();

                            DatabaseManager databaseManager = new DatabaseManager(context);

                            if(enterPasswordCheckbox.isChecked())
                            {
                                DataCrypter.updateKey(textInputLayoutPassword.getEditText().getText().toString());
                            }

                            File backupFile = new File(textViewFilePath.getText().toString());

                            try {
                                FileReader fileReader = new FileReader(backupFile);
                                BufferedReader bufferedReader = new BufferedReader(fileReader);

                                String str;
                                String completeFile = "";

                                while ((str = bufferedReader.readLine()) != null) {
                                    completeFile += str;
                                }

                                try {
                                    JSONObject backupJson = new JSONObject(completeFile);
                                    String checker;

                                    if(enterPasswordCheckbox.isChecked())
                                    {
                                        checker = DataCrypter.decrypt(getActivity().getBaseContext(), backupJson.getString("encodeChecker"));
                                    }
                                    else
                                    {
                                        checker = backupJson.getString("encodeChecker");
                                    }

                                    if(checker.equals("NaukVerification"))
                                    {
                                        if(restoreManualEntriesCheckbox.isChecked())
                                        {
                                            if(wipeManualEntriesCheckbox.isChecked())
                                            {
                                                databaseManager.wipeData(DatabaseManager.TABLE_MANUAL_TRANSACTIONS);
                                            }

                                            if(backupJson.has("transactions"))
                                            {
                                                JSONArray transactionsArray = backupJson.getJSONArray("transactions");

                                                for(int i = 0; i < transactionsArray.length(); i++)
                                                {
                                                    JSONObject transactionObject = transactionsArray.getJSONObject(i);

                                                    databaseManager.addRowTransaction(transactionObject, getActivity().getBaseContext(), enterPasswordCheckbox.isChecked());
                                                }
                                            }
                                        }

                                        if(restoreWatchlistCheckbox.isChecked())
                                        {
                                            if(wipeWatchlistCheckbox.isChecked())
                                            {
                                                databaseManager.wipeData(DatabaseManager.TABLE_WATCHLIST);
                                            }

                                            if(backupJson.has("watchlist"))
                                            {
                                                JSONArray watchlistArray = backupJson.getJSONArray("watchlist");

                                                for(int i = 0; i < watchlistArray.length(); i++)
                                                {
                                                    JSONObject watchlistObject = watchlistArray.getJSONObject(i);

                                                    databaseManager.addRowWatchlist(watchlistObject, getActivity().getBaseContext(), enterPasswordCheckbox.isChecked());
                                                }
                                            }
                                        }

                                        if(restoreApiKeysCheckbox.isChecked())
                                        {
                                            if(wipeApiKeyxCheckbox.isChecked())
                                            {
                                                databaseManager.wipeData(DatabaseManager.TABLE_EXCHANGE_KEYS);
                                            }

                                            if(backupJson.has("apiKeys"))
                                            {
                                                JSONArray apiArray = backupJson.getJSONArray("apiKeys");

                                                for(int i = 0; i < apiArray.length(); i++)
                                                {
                                                    JSONObject apiKeysObject = apiArray.getJSONObject(i);

                                                    databaseManager.addRowApiKeys(apiKeysObject, getActivity().getBaseContext(), enterPasswordCheckbox.isChecked());
                                                }
                                            }
                                        }

                                        PreferencesManager preferencesManager = new PreferencesManager(getContext());
                                        preferencesManager.setMustUpdateSummary(true);
                                    }
                                    else
                                    {
                                        textInputLayoutPassword.setError(getString(R.string.wrong_password));
                                    }

                                } catch (JSONException e) {
                                    Log.d("moodl", "Error while creating backup json " + e);
                                }

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    dialogBuilder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });

                    AlertDialog alertDialog = dialogBuilder.create();
                    alertDialog.show();

                    return false;
                }
            });

            DatabaseManager databaseManager = new DatabaseManager(getActivity().getBaseContext());
            int disabledAcount = databaseManager.getDisabledExchangeAccountsNumber();
            PreferenceScreen preferenceScreen = (PreferenceScreen) findPreference("exchange");

            if(disabledAcount > 0)
            {
                preferenceScreen.setWidgetLayoutResource(R.layout.alert_layout);
            }

            preferenceScreen.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    Intent exchangeListIntent = new Intent(getActivity().getBaseContext(), ExchangeListActivity.class);
                    startActivity(exchangeListIntent);

                    return false;
                }
            });

            EditTextPreference editTextPreference = (EditTextPreference) findPreference("minimum_value_displayed");
            editTextPreference.setPositiveButtonText(getString(R.string.save));
            editTextPreference.setNegativeButtonText(getString(R.string.cancel));
        }

        private void initiateJsonBackup(JSONObject backupJson, boolean mustEncrypt) throws JSONException
        {
            if(mustEncrypt)
            {
                backupJson.put("encodeChecker", DataCrypter.encrypt(getActivity().getBaseContext(), "NaukVerification"));
            }
            else
            {
                backupJson.put("encodeChecker", "NaukVerification");
            }
        }

        private void createDefaultBackupDirectory(File backupDirectory)
        {
            if (!backupDirectory.exists()) {
                if (!backupDirectory.mkdirs()) {
                    Log.d("moodl", "Error while creating directory");
                }
            }
        }

        private boolean checkPermissions() {

            String[] permissions = new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
            };

            int result;
            List<String> listPermissionsNeeded = new ArrayList<>();
            for (String p : permissions) {
                result = ContextCompat.checkSelfPermission(getActivity().getBaseContext(), p);
                if (result != PackageManager.PERMISSION_GRANTED) {
                    listPermissionsNeeded.add(p);
                }
            }
            if (!listPermissionsNeeded.isEmpty()) {
                ActivityCompat.requestPermissions(getActivity(), listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 100);
                return false;
            }
            return true;
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                //startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }

            return super.onOptionsItemSelected(item);
        }
    }


}
