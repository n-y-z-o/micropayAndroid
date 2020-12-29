package co.nyzo.verifier;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import co.nyzo.verifier.util.ByteUtil;
import co.nyzo.verifier.util.HashUtil;
import co.nyzo.verifier.util.KeyUtil;
import co.nyzo.verifier.util.SignatureUtil;

public class Transaction implements MessageObject {

    public static final long micronyzoMultiplierRatio = 1_000_000L;

    public static final byte typeCoinGeneration = 0;
    public static final byte typeSeed = 1;
    public static final byte typeStandard = 2;
    public static final byte typeCycle = 3;
    public static final byte typeCycleSignature = 4;

    public static final byte voteYes = (byte) 1;
    public static final byte voteNo = (byte) 0;

    public static final byte[] genesisBlockHash =
            ByteUtil.byteArrayFromHexString("bc4cca2a2a50a229-256ae3f5b2b5cd49-aa1df1e2d0192726-c4bb41cdcea15364",
                    FieldByteSize.hash);

    // Included in all transactions.
    private byte type;                   // 1 byte; types enumerated above
    private long timestamp;              // 8 bytes; 64-bit Unix timestamp of the transaction initiation, in milliseconds
    private long amount;                 // 8 bytes; 64-bit amount in micronyzos
    private byte[] receiverIdentifier;   // 32 bytes (256-bit public key of the recipient)

    // Only included in type-1, type-2, and type-3 transactions
    private long previousHashHeight;     // 8 bytes; 64-bit index of the block height of the previous-block hash
    private byte[] previousBlockHash;    // 32 bytes (SHA-256 of a recent block in the chain)
    private byte[] senderIdentifier;     // 32 bytes (256-bit public key of the sender)
    private byte[] senderData;           // up to 32 bytes

    // Included in all types except type-0 (coin generation)
    private byte[] signature;            // 64 bytes (512-bit signature)

    // Only included in type-3 transactions for the v1 blockchain.
    private Map<ByteBuffer, byte[]> cycleSignatures;

    // Only included in type-3 transactions for the balance list for the v2 blockchain.
    private Map<ByteBuffer, Transaction> cycleSignatureTransactions;

    // Only included in type-4 (cycle signature) transactions.
    private byte[] cycleTransactionSignature;
    private byte cycleTransactionVote = voteNo;


    private SignatureState signatureState = SignatureState.Undetermined;

    public static final Comparator<ByteBuffer> identifierComparator = new Comparator<ByteBuffer>() {
        @Override
        public int compare(ByteBuffer buffer1, ByteBuffer buffer2) {
            int result = 0;
            byte[] identifier1 = buffer1.array();
            byte[] identifier2 = buffer2.array();
            for (int i = 0; i < FieldByteSize.identifier && result == 0; i++) {
                int byte1 = identifier1[i] & 0xff;
                int byte2 = identifier2[i] & 0xff;
                if (byte1 < byte2) {
                    result = -1;
                } else if (byte2 < byte1) {
                    result = 1;
                }
            }

            return result;
        }
    };

    private Transaction() {
    }

    public byte getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getAmount() {
        return amount;
    }

    public long getAmountAfterFee() {
        return amount - getFee();
    }

    public byte[] getReceiverIdentifier() {
        return receiverIdentifier;
    }

    public long getPreviousHashHeight() {
        if (previousBlockHash == null) {
            assignPreviousBlockHash();
        }

        return previousHashHeight;
    }

    public byte[] getPreviousBlockHash() {
        if (previousBlockHash == null) {
            assignPreviousBlockHash();
        }

        return previousBlockHash;
    }

    public byte[] getSenderIdentifier() {
        return senderIdentifier;
    }

    public byte[] getSenderData() {
        return senderData;
    }

    public byte[] getSignature() {
        return signature;
    }

    private void assignPreviousBlockHash() {

        // This class is kept structurally similar to the Transaction class in the verifier codebase. However, the
        // Android app does not yet track the blockchain, so the Genesis block is always used as a reference.
        previousHashHeight = 0L;
        previousBlockHash = genesisBlockHash;
    }

    public static Transaction coinGenerationTransaction(long timestamp, long amount, byte[] receiverIdentifier) {

        Transaction transaction = new Transaction();
        transaction.type = typeCoinGeneration;
        transaction.timestamp = timestamp;
        transaction.amount = amount;
        transaction.receiverIdentifier = receiverIdentifier;

        return transaction;
    }

    public static Transaction seedTransaction(long timestamp, long amount, byte[] receiverIdentifier,
                                              long previousHashHeight, byte[] previousBlockHash,
                                              byte[] senderIdentifier, byte[] senderData, byte[] signature) {

        Transaction transaction = new Transaction();
        transaction.type = typeSeed;
        transaction.timestamp = timestamp;
        transaction.amount = amount;
        transaction.receiverIdentifier = Arrays.copyOf(receiverIdentifier, FieldByteSize.identifier);
        transaction.previousHashHeight = previousHashHeight;
        transaction.previousBlockHash = Arrays.copyOf(previousBlockHash, FieldByteSize.hash);
        transaction.senderIdentifier = Arrays.copyOf(senderIdentifier, FieldByteSize.identifier);
        transaction.senderData = Arrays.copyOf(senderData, Math.min(senderData.length, 32));
        transaction.signature = signature;

        return transaction;
    }

    public static Transaction seedTransaction(long timestamp, long amount, byte[] receiverIdentifier,
                                              long previousHashHeight, byte[] previousBlockHash,
                                              byte[] senderData, byte[] signerSeed) {

        Transaction transaction = new Transaction();
        transaction.type = typeSeed;
        transaction.timestamp = timestamp;
        transaction.amount = amount;
        transaction.receiverIdentifier = receiverIdentifier;
        transaction.previousHashHeight = previousHashHeight;
        transaction.previousBlockHash = previousBlockHash;
        transaction.senderIdentifier = KeyUtil.identifierForSeed(signerSeed);
        transaction.senderData = senderData;
        transaction.signature = SignatureUtil.signBytes(transaction.getBytes(true), signerSeed);

        return transaction;
    }

    public static Transaction standardTransaction(long timestamp, long amount, byte[] receiverIdentifier,
                                                  long previousHashHeight, byte[] previousBlockHash,
                                                  byte[] senderIdentifier, byte[] senderData, byte[] signature) {

        Transaction transaction = new Transaction();
        transaction.type = typeStandard;
        transaction.timestamp = timestamp;
        transaction.amount = amount;
        transaction.receiverIdentifier = Arrays.copyOf(receiverIdentifier, FieldByteSize.identifier);
        transaction.previousHashHeight = previousHashHeight;
        transaction.previousBlockHash = Arrays.copyOf(previousBlockHash, FieldByteSize.hash);
        transaction.senderIdentifier = Arrays.copyOf(senderIdentifier, FieldByteSize.identifier);
        transaction.senderData = Arrays.copyOf(senderData, Math.min(senderData.length, 32));
        transaction.signature = signature;

        return transaction;
    }

    public static Transaction standardTransaction(long timestamp, long amount, byte[] receiverIdentifier,
                                                  long previousHashHeight, byte[] previousBlockHash,
                                                  byte[] senderData, byte[] signerSeed) {

        Transaction transaction = new Transaction();
        transaction.type = typeStandard;
        transaction.timestamp = timestamp;
        transaction.amount = amount;
        transaction.receiverIdentifier = receiverIdentifier;
        transaction.previousHashHeight = previousHashHeight;
        transaction.previousBlockHash = previousBlockHash;
        transaction.senderIdentifier = KeyUtil.identifierForSeed(signerSeed);
        transaction.senderData = senderData;
        transaction.signature = SignatureUtil.signBytes(transaction.getBytes(true), signerSeed);

        return transaction;
    }

    public static Transaction cycleTransaction(Transaction cycleTransaction) {

        Transaction transaction = new Transaction();
        transaction.type = typeCycle;
        transaction.timestamp = cycleTransaction.timestamp;
        transaction.amount = cycleTransaction.amount;
        transaction.receiverIdentifier = cycleTransaction.receiverIdentifier;
        transaction.previousHashHeight = cycleTransaction.previousHashHeight;
        transaction.previousBlockHash = cycleTransaction.previousBlockHash;
        transaction.senderIdentifier = cycleTransaction.senderIdentifier;
        transaction.senderData = cycleTransaction.senderData;
        transaction.signature = cycleTransaction.signature;
        transaction.cycleSignatures = new ConcurrentHashMap<>(cycleTransaction.cycleSignatures);
        transaction.cycleSignatureTransactions = new ConcurrentHashMap<>(cycleTransaction.cycleSignatureTransactions);

        return transaction;
    }

    public static Transaction cycleTransaction(long timestamp, long amount, byte[] receiverIdentifier,
                                               long previousHashHeight, byte[] previousBlockHash,
                                               byte[] senderIdentifier, byte[] senderData, byte[] signature,
                                               Map<ByteBuffer, byte[]> cycleSignatures,
                                               Map<ByteBuffer, Transaction> cycleSignatureTransactions) {

        Transaction transaction = new Transaction();
        transaction.type = typeCycle;
        transaction.timestamp = timestamp;
        transaction.amount = amount;
        transaction.receiverIdentifier = receiverIdentifier;
        transaction.previousHashHeight = previousHashHeight;
        transaction.previousBlockHash = previousBlockHash;
        transaction.senderIdentifier = senderIdentifier;
        transaction.senderData = senderData;
        transaction.signature = signature;
        transaction.cycleSignatures = cycleSignatures;
        transaction.cycleSignatureTransactions = cycleSignatureTransactions;

        return transaction;
    }

    public static Transaction cycleTransaction(long timestamp, long amount, byte[] receiverIdentifier,
                                               byte[] senderData, byte[] signerSeed) {

        Transaction transaction = new Transaction();
        transaction.type = typeCycle;
        transaction.timestamp = timestamp;
        transaction.amount = amount;
        transaction.receiverIdentifier = receiverIdentifier;
        transaction.previousHashHeight = 0L;
        transaction.previousBlockHash = genesisBlockHash;
        transaction.senderIdentifier = KeyUtil.identifierForSeed(signerSeed);  // initiator identifier, in this case
        transaction.senderData = senderData;
        transaction.signature = SignatureUtil.signBytes(transaction.getBytes(true), signerSeed);
        transaction.cycleSignatures = new ConcurrentHashMap<>();
        transaction.cycleSignatureTransactions = new ConcurrentHashMap<>();

        return transaction;
    }

    public static Transaction cycleSignatureTransaction(long timestamp, byte[] senderIdentifier,
                                                        byte cycleTransactionVote, byte[] cycleTransactionSignature,
                                                        byte[] signature) {

        Transaction transaction = new Transaction();
        transaction.type = typeCycleSignature;
        transaction.timestamp = timestamp;
        transaction.senderIdentifier = senderIdentifier;
        transaction.cycleTransactionVote = cycleTransactionVote;
        transaction.cycleTransactionSignature = cycleTransactionSignature;
        transaction.signature = signature;

        return transaction;
    }

    public static Transaction cycleSignatureTransaction(long timestamp, byte cycleTransactionVote,
                                                        byte[] cycleTransactionSignature, byte[] signerSeed) {

        Transaction transaction = new Transaction();
        transaction.type = typeCycleSignature;
        transaction.timestamp = timestamp;
        transaction.senderIdentifier = KeyUtil.identifierForSeed(signerSeed);
        transaction.cycleTransactionVote = cycleTransactionVote;
        transaction.cycleTransactionSignature = cycleTransactionSignature;
        transaction.signature = SignatureUtil.signBytes(transaction.getBytes(true), signerSeed);

        return transaction;
    }


    public long getFee() {
        return type == typeCycle || type == typeCycleSignature ? 0 : (getAmount() + 399L) / 400L;
    }

    @Override
    public int getByteSize() {
        return getByteSize(false);
    }

    public int getByteSize(boolean forSigning) {

        // All transactions begin with a type and timestamp.
        int size = FieldByteSize.transactionType +      // type
                FieldByteSize.timestamp;                // timestamp

        if (type == typeCycleSignature) {
            size += FieldByteSize.identifier +          // verifier (signer) identifier
                    FieldByteSize.booleanField +        // yes/no
                    FieldByteSize.signature;            // cycle transaction signature
            if (!forSigning) {
                size += FieldByteSize.signature;        // signature
            }
        } else {
            size += FieldByteSize.transactionAmount +   // amount
                    FieldByteSize.identifier;           // receiver identifier
        }

        if (type == typeSeed || type == typeStandard || type == typeCycle) {

            if (forSigning) {
                size += FieldByteSize.hash;           // previous-block hash for signing
            } else {
                size += FieldByteSize.blockHeight;    // previous-hash height for storage and transmission
            }
            size += FieldByteSize.identifier;         // sender identifier

            if (forSigning) {
                size += FieldByteSize.hash;           // sender data hash for signing
            } else {
                size += 1 + senderData.length +       // length specifier + sender data
                        FieldByteSize.signature;      // transaction signature

                if (type == typeCycle) {
                    // These are stored differently in the v1 and v2 blockchains. The cycleSignatures field is used for
                    // the v1 blockchain, and the cycleSignatureTransactions field is used for the v2 blockchain.
                    if (cycleSignatures != null && !cycleSignatures.isEmpty()) {
                        // The v1 blockchain stores identifier and signature for each.
                        size += FieldByteSize.unnamedInteger + cycleSignatures.size() * (FieldByteSize.identifier +
                                FieldByteSize.signature);
                    } else {
                        // The v2 blockchain stores timestamp, identifier, vote, and signature for each.
                        size += FieldByteSize.unnamedInteger + cycleSignatureTransactions.size() *
                                (FieldByteSize.timestamp + FieldByteSize.identifier + FieldByteSize.booleanField +
                                        FieldByteSize.signature);

                    }
                }
            }
        }

        return size;
    }

    @Override
    public byte[] getBytes() {

        return getBytes(false);
    }

    public byte[] getBytes(boolean forSigning) {

        byte[] array = new byte[getByteSize(forSigning)];

        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.put(type);
        buffer.putLong(timestamp);

        if (type == typeCoinGeneration || type == typeSeed || type == typeStandard || type == typeCycle) {
            buffer.putLong(amount);
            buffer.put(receiverIdentifier);
        } else if (type == typeCycleSignature) {
            buffer.put(senderIdentifier);
            buffer.put(cycleTransactionVote);
            buffer.put(cycleTransactionSignature);
            if (!forSigning) {
                buffer.put(signature);
            }
        }

        if (type == typeSeed || type == typeStandard || type == typeCycle) {

            if (forSigning) {
                buffer.put(getPreviousBlockHash());      // may be null initially and need to be determined
            } else {
                buffer.putLong(getPreviousHashHeight()); // may be unspecified initially and need to be determined
            }
            buffer.put(senderIdentifier);

            // For serializing, we use the raw sender data with a length specifier. For signing, we use the double-
            // SHA-256 of the user data. This will allow us to remove inappropriate or illegal metadata from the
            // blockchain at a later date by replacing it with its double-SHA-256 without compromising the signature
            // integrity.
            if (forSigning) {
                buffer.put(HashUtil.doubleSHA256(senderData));
            } else {
                buffer.put((byte) senderData.length);
                buffer.put(senderData);
            }

            if (!forSigning) {
                buffer.put(signature);

                // For cycle transactions, order the signatures by verifier identifier. In the v1 blockchain, the
                // cycleSignatures field is used. In the v2 blockchain, the cycleSignatureTransactions field is used.
                if (type == typeCycle) {
                    if (cycleSignatures != null && !cycleSignatures.isEmpty()) {
                        List<ByteBuffer> signatureIdentifiers = new ArrayList<>(cycleSignatures.keySet());
                        Collections.sort(signatureIdentifiers, identifierComparator);

                        buffer.putInt(cycleSignatures.size());
                        for (ByteBuffer identifier : signatureIdentifiers) {
                            buffer.put(identifier.array());
                            buffer.put(cycleSignatures.get(identifier));
                        }
                    } else {
                        List<ByteBuffer> signatureIdentifiers = new ArrayList<>(cycleSignatureTransactions.keySet());
                        Collections.sort(signatureIdentifiers, identifierComparator);

                        buffer.putInt(cycleSignatureTransactions.size());
                        for (ByteBuffer identifier : signatureIdentifiers) {
                            Transaction signatureTransaction = cycleSignatureTransactions.get(identifier);
                            buffer.putLong(signatureTransaction.timestamp);
                            buffer.put(signatureTransaction.senderIdentifier);
                            buffer.put(signatureTransaction.cycleTransactionVote);
                            buffer.put(signatureTransaction.signature);
                        }
                    }
                }
            }
        }

        return array;
    }

    public static Transaction fromByteBuffer(ByteBuffer buffer) {

        return fromByteBuffer(buffer, 0, new byte[FieldByteSize.hash], false);
    }

    public static Transaction fromByteBuffer(ByteBuffer buffer, long transactionHeight, byte[] previousHashInChain,
                                             boolean balanceListCycleTransaction) {

        // All transactions start with type and timestamp.
        byte type = buffer.get();
        long timestamp = buffer.getLong();

        // Build the transaction object, getting the appropriate fields for each type.
        Transaction transaction = null;
        if (type == typeCoinGeneration) {
            long amount = buffer.getLong();
            byte[] receiverIdentifier = Message.getByteArray(buffer, FieldByteSize.identifier);
            transaction = coinGenerationTransaction(timestamp, amount, receiverIdentifier);
        } else if (type == typeSeed || type == typeStandard || type == typeCycle) {
            long amount = buffer.getLong();
            byte[] receiverIdentifier = Message.getByteArray(buffer, FieldByteSize.identifier);
            long previousHashHeight = buffer.getLong();

            // The app does not currently track the blockchain, so it only supports transactions referenced to the
            // Genesis block.
            byte[] previousBlockHash = previousHashHeight == 0 ? genesisBlockHash : new byte[FieldByteSize.hash];
            byte[] senderIdentifier = Message.getByteArray(buffer, FieldByteSize.identifier);

            int senderDataLength = Math.min(buffer.get(), 32);
            byte[] senderData = Message.getByteArray(buffer, senderDataLength);

            byte[] signature = Message.getByteArray(buffer, FieldByteSize.signature);
            if (type == typeSeed) {
                transaction = seedTransaction(timestamp, amount, receiverIdentifier, previousHashHeight,
                        previousBlockHash, senderIdentifier, senderData, signature);
            } else if (type == typeStandard) {
                transaction = standardTransaction(timestamp, amount, receiverIdentifier, previousHashHeight,
                        previousBlockHash, senderIdentifier, senderData, signature);
            } else {  // type == typeCycle

                Map<ByteBuffer, byte[]> cycleSignatures = new HashMap<>();
                Map<ByteBuffer, Transaction> cycleSignatureTransactions = new HashMap<>();
                int numberOfCycleSignatures = buffer.getInt();

                if (!balanceListCycleTransaction) {
                    // If not explicitly marked as a balance list cycle transaction, read the signatures as simple
                    // identifier/signature pairs.
                    for (int i = 0; i < numberOfCycleSignatures; i++) {
                        ByteBuffer identifier = ByteBuffer.wrap(Message.getByteArray(buffer, FieldByteSize.identifier));
                        byte[] cycleSignature = Message.getByteArray(buffer, FieldByteSize.signature);
                        if (!ByteUtil.arraysAreEqual(identifier.array(), senderIdentifier)) {
                            cycleSignatures.put(identifier, cycleSignature);
                        }
                    }
                } else {
                    // When the explicitly marked as a balance list cycle transaction, read the additional fields for
                    // cycle transaction signatures.
                    for (int i = 0; i < numberOfCycleSignatures; i++) {
                        long childTimestamp = buffer.getLong();
                        byte[] childSenderIdentifier = Message.getByteArray(buffer, FieldByteSize.identifier);
                        byte childCycleTransactionVote = buffer.get() == 1 ? voteYes : voteNo;
                        byte[] childSignature = Message.getByteArray(buffer, FieldByteSize.signature);
                        cycleSignatureTransactions.put(ByteBuffer.wrap(childSenderIdentifier),
                                cycleSignatureTransaction(childTimestamp, childSenderIdentifier,
                                        childCycleTransactionVote, signature, childSignature));
                    }
                }
                transaction = cycleTransaction(timestamp, amount, receiverIdentifier, previousHashHeight,
                        previousBlockHash, senderIdentifier, senderData, signature, cycleSignatures,
                        cycleSignatureTransactions);
            }
        } else if (type == typeCycleSignature) {
            byte[] senderIdentifier = Message.getByteArray(buffer, FieldByteSize.identifier);
            byte cycleTransactionVote = buffer.get() == 1 ? voteYes : voteNo;
            byte[] cycleTransactionSignature = Message.getByteArray(buffer, FieldByteSize.signature);
            byte[] signature = Message.getByteArray(buffer, FieldByteSize.signature);
            transaction = cycleSignatureTransaction(timestamp, senderIdentifier, cycleTransactionVote,
                    cycleTransactionSignature, signature);
        } else {
            System.err.println("Unknown type: " + type);
        }

        return transaction;
    }
}
