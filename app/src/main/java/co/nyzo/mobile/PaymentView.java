package co.nyzo.mobile;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import co.nyzo.mobile.util.LayoutUtil;
import co.nyzo.verifier.Transaction;
import co.nyzo.verifier.nyzoString.NyzoString;
import co.nyzo.verifier.util.PrintUtil;

public class PaymentView extends ViewGroup {

    private TextView textView;

    private RoundRectButton settingsButton;
    private RoundRectButton cancelButton;
    private RoundRectButton confirmButton;
    private RoundRectButton clearButton;
    private RoundRectButton retryButton;

    private OnClickListener settingsButtonOnClickListener;
    private OnClickListener cancelButtonOnClickListener;
    private OnClickListener confirmButtonOnClickListener;
    private OnClickListener clearButtonOnClickListener;
    private OnClickListener retryButtonOnClickListener;

    private MicropayConfiguration micropayConfiguration;
    private Transaction existingTransaction;
    private InterfaceState interfaceState;

    private List<String> messages;
    private List<String> warnings;
    private List<String> errors;
    private boolean success;

    public PaymentView(Context context) {
        super(context);

        settingsButton = new RoundRectButton(context);
        settingsButton.setIcon(Icon.makeSettingsIcon(context));
        settingsButton.setLabel("Settings");
        settingsButton.setOnClickListener(v -> settingsButtonClicked());
        addView(settingsButton);

        textView = new TextView(context);
        textView.setTextColor(Color.BLACK);
        textView.setTextAlignment(TEXT_ALIGNMENT_CENTER);
        textView.setText("This is the primary text view on the payment view.");
        addView(textView);

        cancelButton = new RoundRectButton(context);
        cancelButton.setIcon(Icon.makeCancelIcon(context));
        cancelButton.setLabel("Cancel");
        cancelButton.setOnClickListener(v -> cancelButtonClicked());
        addView(cancelButton);

        confirmButton = new RoundRectButton(context);
        confirmButton.setIcon(Icon.makeConfirmIcon(context));
        confirmButton.setLabel("Yes");
        confirmButton.setOnClickListener(v -> confirmButtonClicked());
        addView(confirmButton);

        clearButton = new RoundRectButton(context);
        clearButton.setLabel("Clear tx");
        clearButton.setOnClickListener(v -> clearButtonClicked());
        addView(clearButton);

        retryButton = new RoundRectButton(context);
        retryButton.setLabel("Retry");
        retryButton.setOnClickListener(v -> retryButtonClicked());
        addView(retryButton);

        interfaceState = InterfaceState.WaitingForInput;

        configure();
    }

    public void configure(MicropayConfiguration micropayConfiguration, Transaction existingTransaction,
                          InterfaceState interfaceState, List<String> messages, List<String> warnings,
                          List<String> errors, boolean success) {
        this.micropayConfiguration = micropayConfiguration;
        this.existingTransaction = existingTransaction;
        this.interfaceState = interfaceState;
        this.messages = messages;
        this.warnings = warnings;
        this.errors = errors;
        this.success = success;

        configure();
    }

    public void configure() {

        boolean cancelAndConfirmButtonsActive = false;
        if (interfaceState == InterfaceState.WaitingForInput) {
            ApplicationConfiguration applicationConfiguration = ApplicationConfiguration.fromPreferences();
            if (applicationConfiguration.isValid()) {
                if (micropayConfiguration != null && micropayConfiguration.isValid()) {
                    SpannableStringBuilder builder = new SpannableStringBuilder();
                    if (existingTransaction == null && micropayConfiguration.getAmountMicronyzos() <=
                            applicationConfiguration.getMaximumMicropayAmountValue()) {
                        builder.append("Do you want to pay ");
                        builder.append(span(PrintUtil.printAmount(micropayConfiguration.getAmountMicronyzos()),
                                Typeface.BOLD_ITALIC));
                        builder.append(" for ");
                        builder.append(span(cleanDisplayName(micropayConfiguration.getDisplayName()), Typeface.ITALIC));
                        builder.append("?");
                        cancelAndConfirmButtonsActive = true;
                    } else if (existingTransaction == null) {
                        builder.append("The requested amount, ");
                        builder.append(span(PrintUtil.printAmount(micropayConfiguration.getAmountMicronyzos()),
                                Typeface.BOLD_ITALIC));
                        builder.append(", is greater than your specified Micropay maximum, ");
                        builder.append(span(PrintUtil.printAmount(
                                applicationConfiguration.getMaximumMicropayAmountValue()), Typeface.BOLD_ITALIC));
                        builder.append(".");
                    } else {
                        builder.append("Do you want to resend your transaction for ");
                        builder.append(span(cleanDisplayName(micropayConfiguration.getDisplayName()), Typeface.ITALIC));
                        builder.append("?");
                        cancelAndConfirmButtonsActive = true;
                    }

                    textView.setText(builder);
                } else if (micropayConfiguration != null) {
                    textView.setText("The configuration provided by the web page is not valid.");
                } else {
                    textView.setText("You're ready to use Micropay!");
                }
            } else {
                SpannableStringBuilder builder = new SpannableStringBuilder();
                builder.append("Please tap the ");
                builder.append(span("Settings", Typeface.BOLD_ITALIC));
                builder.append(" button and provide a valid configuration.");

                textView.setText(builder);
            }
        } else if (interfaceState == InterfaceState.SendingTransaction) {
            textView.setText("Sending...");
        } else if (interfaceState == InterfaceState.SentTransaction) {
            SpannableStringBuilder builder = new SpannableStringBuilder();
            if (success) {
                builder.append(span("Success!", Typeface.BOLD_ITALIC));
                if (messages.size() > 0) {
                    builder.append("\n\n").append(messages.get(0));
                }
                if (warnings.size() > 0) {
                    builder.append("\n\n").append(warnings.get(0));
                }
            } else {
                builder.append(span("Oops!", Typeface.BOLD_ITALIC));
                if (errors.size() > 0) {
                    builder.append("\n\n").append(errors.get(0));
                }
            }
            textView.setText(builder);
        }

        // Set the visibility of the cancel and confirm buttons.
        if (cancelAndConfirmButtonsActive) {
            cancelButton.setVisibility(VISIBLE);
            confirmButton.setVisibility(VISIBLE);
        } else {
            cancelButton.setVisibility(INVISIBLE);
            confirmButton.setVisibility(INVISIBLE);
        }

        // Set the visibility of the clear button and retry button.
        clearButton.setVisibility(cancelAndConfirmButtonsActive && existingTransaction != null ? VISIBLE : INVISIBLE);
        retryButton.setVisibility(interfaceState == InterfaceState.SentTransaction && !success ? VISIBLE : INVISIBLE);

        post(() -> {
            invalidate();
            updateLayout();
        });
    }

    private static SpannableString span(String value, int typeface) {
        SpannableString string = new SpannableString(value);
        string.setSpan(new StyleSpan(typeface), 0, value.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        return string;
    }

    private static String cleanDisplayName(String displayName) {
        // This method removes characters from the display name that would cause it to display oddly or trick the user
        // into paying a different amount than they think they are paying. It removes the Nyzo sign (intersection
        // symbol) and the period, and it limits the length to no more than 20 characters. Users are also protected by
        // the maximum Micropay amount, specified in the settings.
        displayName = displayName.replace("∩", "");
        displayName = displayName.replace(".", "");
        if (displayName.length() > 20) {
            displayName = displayName.substring(0, 20) + "...";
        }

        return displayName;
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);

        // Only update the text sizes when the screen size changes.
        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
        float textSize = LayoutUtil.size(24.0f, metrics);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        updateLayout();
    }

    private void updateLayout() {

        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
        int width = getWidth();
        int height = getHeight();

        // Place the Settings button in the top-right corner.
        int buttonHeight = (int) LayoutUtil.size(60.0f, metrics);
        int margin = (int) LayoutUtil.size(LayoutUtil.recommendedMarginSize, metrics);
        settingsButton.layout(width - buttonHeight - margin, margin, width - margin, buttonHeight + margin);

        // Center the TextView and the buttons in the remaining space.
        int availableHeight = height - buttonHeight - margin * 2;
        int yc = buttonHeight + margin + (int) (availableHeight * 0.45);
        int textViewWidth = Math.min(width - margin * 2, buttonHeight * 10);
        textView.measure(MeasureSpec.AT_MOST | textViewWidth, 0);
        textViewWidth = textView.getMeasuredWidth();
        int textViewHeight = textView.getMeasuredHeight();
        int clearButtonHeight = (int) (buttonHeight * 0.7);
        int combinedHeight = textViewHeight +                                             // text view
                (cancelButton.getVisibility() == VISIBLE ? margin + buttonHeight : 0) +   // cancel and confirm buttons
                (clearButton.getVisibility() == VISIBLE ? margin + clearButtonHeight : 0) +  // clear button
                (retryButton.getVisibility() == VISIBLE ? margin + clearButtonHeight : 0);
        int textViewTop = yc - combinedHeight / 2;
        textView.layout((width - textViewWidth) / 2, textViewTop, (width + textViewWidth) / 2,
                textViewTop + textViewHeight);

        int buttonTop = textViewTop + textViewHeight + margin;
        int buttonWidth = Math.min((int) (buttonHeight * 2.5), (width - margin * 3) / 2);
        if (cancelButton.getVisibility() == VISIBLE) {
            int buttonBottom = buttonTop + buttonHeight;
            cancelButton.layout((width - margin) / 2 - buttonWidth, buttonTop, (width - margin) / 2,
                    buttonBottom);
            confirmButton.layout((width + margin) / 2, buttonTop, (width + margin) / 2 + buttonWidth,
                    buttonBottom);
            buttonTop += buttonHeight + margin;
        }

        if (clearButton.getVisibility() == VISIBLE) {
            clearButton.layout((width - buttonWidth) / 2, buttonTop, (width + buttonWidth) / 2, buttonTop +
                    clearButtonHeight);
            buttonTop += clearButtonHeight + margin;
        }

        if (retryButton.getVisibility() == VISIBLE) {
            retryButton.layout((width - buttonWidth) / 2, buttonTop, (width + buttonWidth) / 2, buttonTop +
                    clearButtonHeight);
        }
    }

    public void setSettingsButtonOnClickListener(OnClickListener listener) {
        settingsButtonOnClickListener = listener;
    }

    public void setCancelButtonOnClickListener(OnClickListener listener) {
        cancelButtonOnClickListener = listener;
    }

    public void setConfirmButtonOnClickListener(OnClickListener listener) {
        confirmButtonOnClickListener = listener;
    }

    public void setClearButtonOnClickListener(OnClickListener listener) {
        clearButtonOnClickListener = listener;
    }

    public void setRetryButtonOnClickListener(OnClickListener retryButtonOnClickListener) {
        this.retryButtonOnClickListener = retryButtonOnClickListener;
    }

    private void settingsButtonClicked() {
        if (settingsButtonOnClickListener != null) {
            settingsButtonOnClickListener.onClick(this);
        }
    }

    private void cancelButtonClicked() {
        if (cancelButtonOnClickListener != null) {
            cancelButtonOnClickListener.onClick(this);
        }
    }

    private void confirmButtonClicked() {
        if (confirmButtonOnClickListener != null) {
            confirmButtonOnClickListener.onClick(this);
        }
    }

    private void clearButtonClicked() {
        if (clearButtonOnClickListener != null) {
            clearButtonOnClickListener.onClick(this);
        }
    }

    private void retryButtonClicked() {
        if (retryButtonOnClickListener != null) {
            retryButtonOnClickListener.onClick(this);
        }
    }
}
