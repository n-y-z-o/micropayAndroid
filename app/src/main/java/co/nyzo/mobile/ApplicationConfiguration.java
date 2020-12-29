package co.nyzo.mobile;

import android.content.SharedPreferences;
import android.util.Log;

import co.nyzo.verifier.FieldByteSize;
import co.nyzo.verifier.Transaction;
import co.nyzo.verifier.nyzoString.NyzoString;
import co.nyzo.verifier.nyzoString.NyzoStringEncoder;
import co.nyzo.verifier.nyzoString.NyzoStringPrivateSeed;
import co.nyzo.verifier.util.ByteUtil;
import co.nyzo.verifier.util.PrintUtil;

public class ApplicationConfiguration {

    public static final String keyPrivateKey = "privateKey";
    public static final String keyBaseTip = "baseTip";
    public static final String keyMaximumMicropayAmount = "maximumMicropayAmount";

    public static final long maximumBaseTip = Transaction.micronyzoMultiplierRatio;  // 1 nyzo

    private String privateKeyString;
    private String baseTipString;
    private String maximumMicropayAmountString;

    public ApplicationConfiguration(String privateKeyString, String baseTipString, String maximumMicropayAmountString) {
        this.privateKeyString = privateKeyString;
        this.baseTipString = baseTipString;
        this.maximumMicropayAmountString = maximumMicropayAmountString;
    }

    public String getPrivateKeyString() {
        return privateKeyString;
    }

    public byte[] getPrivateKeyBytes() {
        NyzoString privateKey = NyzoStringEncoder.decode(privateKeyString);
        byte[] bytes = new byte[FieldByteSize.seed];
        if (privateKey instanceof NyzoStringPrivateSeed) {
            bytes = ((NyzoStringPrivateSeed) privateKey).getSeed();
        }

        return bytes;
    }

    public String getBaseTipString() {
        return baseTipString;
    }

    public long getBaseTipValue() {
        long value = -1L;
        try {
            value = (long) (Double.parseDouble(baseTipString) * Transaction.micronyzoMultiplierRatio);
        } catch (Exception ignored) { }

        return value;
    }

    public String getMaximumMicropayAmountString() {
        return maximumMicropayAmountString;
    }

    public long getMaximumMicropayAmountValue() {
        long value = -1L;
        try {
            value = (long) (Double.parseDouble(maximumMicropayAmountString) * Transaction.micronyzoMultiplierRatio);
        } catch (Exception ignored) { }

        return value;
    }

    public boolean isPrivateKeyValid() {
        byte[] privateKey = getPrivateKeyBytes();
        return !ByteUtil.isAllZeros(privateKey);
    }

    public boolean isBaseTipAmountValid() {
        long baseTipMicronyzos = getBaseTipValue();
        return baseTipMicronyzos > 1L && baseTipMicronyzos <= maximumBaseTip;
    }

    public boolean isMaximumMicropayAmountValid() {
        long maximumMicropayAmount = getMaximumMicropayAmountValue();
        return maximumMicropayAmount > 1L && maximumMicropayAmount <= MicropayConfiguration.maximumMicropayAmount;
    }

    public boolean isValid() {
        return isPrivateKeyValid() && isBaseTipAmountValid() && isMaximumMicropayAmountValid();
    }

    public static ApplicationConfiguration fromPreferences() {
        SharedPreferences preferences = MainActivity.getSharedPreferences();
        String privateKeyString = preferences.getString(keyPrivateKey, "");
        String baseTipString = preferences.getString(keyBaseTip, "");
        String maximumMicropayAmountString = preferences.getString(keyMaximumMicropayAmount, "");

        return new ApplicationConfiguration(privateKeyString, baseTipString, maximumMicropayAmountString);
    }

    public void saveToPreferences() {
        SharedPreferences preferences = MainActivity.getSharedPreferences();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(keyPrivateKey, privateKeyString);
        editor.putString(keyBaseTip, baseTipString);
        editor.putString(keyMaximumMicropayAmount, maximumMicropayAmountString);
        editor.apply();
    }
}
