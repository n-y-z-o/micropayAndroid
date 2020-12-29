package co.nyzo.verifier.util;

import co.nyzo.verifier.Transaction;

public class PrintUtil {

    public static String printAmount(long micronyzos) {
        boolean amountIsNegative = micronyzos < 0;
        micronyzos = Math.abs(micronyzos);
        long whole = micronyzos / Transaction.micronyzoMultiplierRatio;
        long fraction = micronyzos % Transaction.micronyzoMultiplierRatio;
        return String.format(amountIsNegative ? "(∩%d.%06d)" : "∩%d.%06d", whole, fraction);
    }

    public static String compactPrintByteArray(byte[] array) {
        String result;
        if (array == null) {
            result = "(null)";
        } else if (array.length == 0) {
            result = "(empty)";
        } else if (array.length <= 4) {
            result = ByteUtil.arrayAsStringNoDashes(array);
        } else {
            result = String.format("%02x%02x...%02x%02x", array[0], array[1], array[array.length - 2],
                    array[array.length - 1]);
        }

        return result;
    }
}
