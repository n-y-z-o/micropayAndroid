package co.nyzo.mobile;

import java.util.Map;

import co.nyzo.verifier.Transaction;
import co.nyzo.verifier.nyzoString.NyzoString;
import co.nyzo.verifier.nyzoString.NyzoStringEncoder;
import co.nyzo.verifier.nyzoString.NyzoStringPublicIdentifier;
import co.nyzo.verifier.util.ByteUtil;

public class MicropayConfiguration {

    public static final long maximumMicropayAmount = 5L * Transaction.micronyzoMultiplierRatio;  // 5 nyzos

    private String clientUrl;
    private byte[] receiverId;
    private String displayName;
    private long amountMicronyzos;
    private String tag;
    private String callbackUrl;

    private MicropayConfiguration(String clientUrl, byte[] receiverId, String displayName,
                                  long amountMicronyzos, String tag, String callbackUrl) {
        this.clientUrl = clientUrl;
        this.receiverId = receiverId;
        this.displayName = displayName;
        this.amountMicronyzos = amountMicronyzos;
        this.tag = tag;
        this.callbackUrl = callbackUrl;
    }

    public String getClientUrl() {
        return clientUrl;
    }

    public byte[] getReceiverId() {
        return receiverId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getAmountMicronyzos() {
        return amountMicronyzos;
    }

    public String getTag() {
        return tag;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public boolean isValid() {
        return amountMicronyzos > 1 && amountMicronyzos < maximumMicropayAmount && !ByteUtil.isAllZeros(receiverId);
    }

    public String uniqueReferenceKey() {
        // The receiver ID and tag uniquely identify a Micropay resource. The amount can change over time, and a new
        // amount should not automatically invalidate a previous transaction, especially if the new amount is less than
        // the previous amount.
        NyzoStringPublicIdentifier receiverIdNyzoString = new NyzoStringPublicIdentifier(receiverId);
        return NyzoStringEncoder.encode(receiverIdNyzoString) + ":" + tag;
    }

    public static MicropayConfiguration fromMap(Map<String, String> map) {
        
        String clientUrl = MapUtil.getOrDefault(map, "clientUrl", "").trim();
        String receiverIdString = MapUtil.getOrDefault(map, "receiverId", "").trim();
        NyzoString receiverIdNyzoString = NyzoStringEncoder.decode(receiverIdString);
        String displayName = MapUtil.getOrDefault(map, "displayName", "").trim();
        long amountMicronyzos = parseAmount(MapUtil.getOrDefault(map, "amount", "").trim());
        String tag = MapUtil.getOrDefault(map, "tag", "").trim();
        String callbackUrl = MapUtil.getOrDefault(map, "callbackUrl", "").trim();

        MicropayConfiguration result = null;
        if (!clientUrl.isEmpty() && (receiverIdNyzoString instanceof NyzoStringPublicIdentifier) &&
                !displayName.isEmpty() && !tag.isEmpty() && !callbackUrl.isEmpty()) {
            byte[] receiverId = ((NyzoStringPublicIdentifier) receiverIdNyzoString).getIdentifier();
            result = new MicropayConfiguration(clientUrl, receiverId, displayName, amountMicronyzos, tag, callbackUrl);
        }

        return result;
    }

    private static long parseAmount(String amountString) {
        long amount = 0L;
        try {
            amountString = amountString.replace("∩", "");
            amount = (long) (Double.parseDouble(amountString) *
                    Transaction.micronyzoMultiplierRatio);
        } catch (Exception ignored) { }

        return amount;
    }
}
