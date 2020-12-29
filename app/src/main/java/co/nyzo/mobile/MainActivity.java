package co.nyzo.mobile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import android.util.Log;
import android.view.inputmethod.InputMethodManager;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.nyzo.verifier.Transaction;
import co.nyzo.verifier.json.Json;
import co.nyzo.verifier.json.JsonObject;
import co.nyzo.verifier.nyzoString.NyzoString;
import co.nyzo.verifier.nyzoString.NyzoStringEncoder;
import co.nyzo.verifier.nyzoString.NyzoStringTransaction;
import co.nyzo.verifier.util.ByteUtil;
import co.nyzo.verifier.util.NetworkUtil;
import co.nyzo.verifier.util.PrintUtil;
import co.nyzo.verifier.web.WebUtil;

public class MainActivity extends Activity {

    // This is stored for convenience for hiding the keyboard.
    private static MainActivity newestInstance = null;

    private MainView mainView;
    private InterfaceState interfaceState;

    // These are the results of sending the transaction.
    private List<String> messages;
    private List<String> warnings;
    private List<String> errors;
    private boolean success;

    // These are the two variables that we want to exist beyond the activity.
    private static MicropayConfiguration micropayConfiguration = null;
    private static boolean settingsViewVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Store a static reference to this for convenience for hiding the keyboard and accessing shared preferences.
        newestInstance = this;

        mainView = new MainView(this);
        mainView.setCancelButtonOnClickListener(view -> finish());
        mainView.setConfirmButtonOnClickListener(view -> sendTransaction());
        mainView.setClearButtonOnClickListener(view -> clearTransaction());
        mainView.setRetryButtonOnClickListener(view -> retry());
        setContentView(mainView);

        // Initialize the transaction-response values.
        messages = Collections.emptyList();
        warnings = Collections.emptyList();
        errors = Collections.emptyList();
        success = false;

        // Set the initial interface state.
        interfaceState = InterfaceState.WaitingForInput;

        // Configure. This should be the last statement in this method.
        configureWithIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        configureWithIntent(intent);
    }

    private void configureWithIntent(Intent intent) {

        // If a micropay configuration was provided, parse it and store in the static field.
        Uri data = intent.getData();
        if (data != null) {
            micropayConfiguration = parseMicropayConfiguration(data.toString());
        }
        configureMainView();
    }

    private void configureMainView() {
        mainView.configure(micropayConfiguration, existingTransaction(), interfaceState, messages, warnings, errors,
                success);
    }

    private void sendTransaction() {
        new Thread(this::sendTransactionThread).start();
    }

    private void sendTransactionThread() {
        // Try to get the transaction from preferences. If unavailable, create a new transaction.
        if (existingTransaction() == null) {
            interfaceState = InterfaceState.SendingTransaction;
            runOnUiThread(this::configureMainView);
            createTransactionAndSendToClient();
        }

        // If the transaction was successfully sent, it will now be stored. If the application configuration is valid
        // and the transaction is present, redirect to the specified redirect URL.
        ApplicationConfiguration applicationConfiguration = ApplicationConfiguration.fromPreferences();
        Transaction transaction = existingTransaction();
        if (applicationConfiguration.isValid() && transaction != null) {
            // Make the supplemental transaction.
            byte[] signerSeed = applicationConfiguration.getPrivateKeyBytes();
            Transaction supplementalTransaction = createSupplementalTransaction(transaction, signerSeed);
            String supplementalTransactionString =
                    NyzoStringEncoder.encode(new NyzoStringTransaction(supplementalTransaction));

            // Redirect to the page.
            String transactionString = NyzoStringEncoder.encode(new NyzoStringTransaction(transaction));
            String redirectUrl = micropayConfiguration.getCallbackUrl() + "?transaction=" + transactionString +
                    "&supplementalTransaction=" + supplementalTransactionString;
            Uri uri = Uri.parse(redirectUrl);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            if (intent.resolveActivity(newestInstance.getPackageManager()) != null) {
                newestInstance.startActivity(intent);
            }
        }
    }

    private void clearTransaction() {
        SharedPreferences preferences = getSharedPreferences();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(micropayConfiguration.uniqueReferenceKey(), null);
        editor.apply();

        configureMainView();
    }

    private void retry() {
        interfaceState = InterfaceState.WaitingForInput;
        configureMainView();
    }

    private void createTransactionAndSendToClient() {

        boolean success = false;
        List<String> messages = null;
        List<String> warnings = null;
        List<String> errors = null;

        ApplicationConfiguration applicationConfiguration = ApplicationConfiguration.fromPreferences();
        if (applicationConfiguration.isValid() && micropayConfiguration != null && micropayConfiguration.isValid() &&
                micropayConfiguration.getAmountMicronyzos() <=
                        applicationConfiguration.getMaximumMicropayAmountValue()) {
            // Create the transaction.
            long timestamp = System.currentTimeMillis() + 10000L;
            long amount = micropayConfiguration.getAmountMicronyzos();
            byte[] receiverIdentifier = micropayConfiguration.getReceiverId();
            long previousHashHeight = 0L;
            byte[] previousBlockHash = Transaction.genesisBlockHash;
            byte[] senderData = micropayConfiguration.getTag().getBytes(StandardCharsets.UTF_8);
            byte[] signerSeed = applicationConfiguration.getPrivateKeyBytes();
            Transaction transaction = Transaction.standardTransaction(timestamp, amount, receiverIdentifier,
                    previousHashHeight, previousBlockHash, senderData, signerSeed);
            String transactionString = NyzoStringEncoder.encode(new NyzoStringTransaction(transaction));

            // Send the transaction to the client.
            String clientFullUrl = micropayConfiguration.getClientUrl() + "?transaction=" + transactionString;
            Object response = Json.parse(NetworkUtil.stringForUrl(clientFullUrl, 1500));

            // Process the client response.
            if (response == null) {
                errors = Collections.singletonList("The response from the server was not valid.");
            } else if (response instanceof JsonObject) {
                // Store the warnings and errors.
                JsonObject responseObject = (JsonObject) response;
                errors = responseObject.getStringList("errors");
                warnings = responseObject.getStringList("notices");

                // If the transaction was forwarded, indicate success.
                JsonObject result = Json.traverseGetObject(responseObject, "result", 0);
                if (result != null) {
                    if (result.getBoolean("forwarded", false) && result.getLong("blockHeight", -1L) > 0L) {
                        success = true;
                        messages = Collections.singletonList("The transaction was forwarded to the cycle for " +
                                "incorporation into block " + result.getLong("blockHeight", -1L) + ".");
                    }
                }
            }

            // If the client response indicates success, store the transaction.
            if (success) {
                SharedPreferences preferences = getSharedPreferences();
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(micropayConfiguration.uniqueReferenceKey(), transactionString);
                editor.apply();
            }
        }

        // Ensure some feedback is provided.
        if (messages == null && warnings == null && errors == null) {
            errors = Collections.singletonList("The transaction failed to send.");
        }

        if (messages == null) {
            messages = Collections.emptyList();
        }

        if (warnings == null) {
            warnings = Collections.emptyList();
        }

        if (errors == null) {
            errors = Collections.emptyList();
        }

        // Store the results in the class variables.
        this.messages = messages;
        this.warnings = warnings;
        this.errors = errors;
        this.success = success;
        interfaceState = InterfaceState.SentTransaction;

        // Update the main view.
        runOnUiThread(this::configureMainView);
    }

    private static Transaction existingTransaction() {
        // If a transaction is available, it is stored with the unique reference key of the Micropay configuration.
        Transaction transaction = null;
        if (micropayConfiguration != null) {
            String transactionString = getSharedPreferences().getString(micropayConfiguration.uniqueReferenceKey(), "");
            NyzoString transactionNyzoString = NyzoStringEncoder.decode(transactionString);
            if (transactionNyzoString instanceof NyzoStringTransaction) {
                transaction = ((NyzoStringTransaction) transactionNyzoString).getTransaction();
            }
        }

        return transaction;
    }

    private static MicropayConfiguration parseMicropayConfiguration(String data) {
        MicropayConfiguration configuration = null;
        if (data.toLowerCase().startsWith("nyzo://micropay?")) {
            Map<String, String> queryParameters = mapForString(data.substring(16));
            configuration = MicropayConfiguration.fromMap(queryParameters);
        }

        return configuration;
    }

    private static Map<String, String> mapForString(String string) {

        // Note that this *does not* work properly for an ampersand (&) is encoded in a parameter. To do this properly,
        // the raw query must be used, and the individual parameters must be decoded.
        Map<String, String> map = new HashMap<>();
        if (string != null) {
            String[] querySplit = string.split("&");
            for (int i = 0; i < querySplit.length; i++) {
                String[] keyValue = querySplit[i].split("=");
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim();
                    String value = WebUtil.removePercentEncoding(keyValue[1].trim());
                    if (!key.isEmpty() && !value.isEmpty()) {
                        map.put(key, value);
                    }
                }
            }
        }

        return map;
    }

    public static boolean isSettingsViewVisible() {
        return settingsViewVisible;
    }

    public static void setSettingsViewVisible(boolean settingsViewVisible) {
        MainActivity.settingsViewVisible = settingsViewVisible;
    }

    public static void hideKeyboard() {
        if (newestInstance != null) {
            InputMethodManager manager = (InputMethodManager) newestInstance
                    .getSystemService(Activity.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(newestInstance.mainView.getWindowToken(), 0);
        }
    }

    public static SharedPreferences getSharedPreferences() {
        SharedPreferences preferences = null;
        if (newestInstance != null) {
            preferences = newestInstance.getPreferences(Context.MODE_PRIVATE);
        }

        return preferences;
    }

    private static Transaction createSupplementalTransaction(Transaction transaction, byte[] signerSeed) {
        // Create and sign the transaction with a current timestamp, an amount of 0, and all other fields the same as
        // the reference transaction.
        long timestamp = System.currentTimeMillis();
        long amount = 0L;
        byte[] receiverIdentifier = transaction.getReceiverIdentifier();
        long previousHashHeight = 0L;
        byte[] previousBlockHash = Transaction.genesisBlockHash;
        byte[] senderData = transaction.getSenderData();
        return Transaction.standardTransaction(timestamp, amount, receiverIdentifier, previousHashHeight,
                previousBlockHash, senderData, signerSeed);
    }
}
