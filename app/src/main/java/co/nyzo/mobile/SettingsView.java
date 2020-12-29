package co.nyzo.mobile;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import co.nyzo.mobile.util.LayoutUtil;
import co.nyzo.verifier.util.PrintUtil;

public class SettingsView extends ViewGroup implements TextWatcher {

    private TextView titleTextView;

    private TextView privateKeyTextView;
    private TextView baseTipAmountTextView;
    private TextView maximumMicropayAmountTextView;

    private EditText privateKeyEditText;
    private EditText baseTipAmountEditText;
    private EditText maximumMicropayAmountEditText;

    private TextView[] noticeTextViews;
    private static final String[] notices = {
            "Tip functionality is not yet implemented.",
            "The tip amount must be at least\n∩0.000002 and no more than " +
                    PrintUtil.printAmount(ApplicationConfiguration.maximumBaseTip) + ".",
            "The maximum Micropay amount must\nbe at least ∩0.000002 and no more than\n" +
                    PrintUtil.printAmount(MicropayConfiguration.maximumMicropayAmount) + "."

    };

    private RoundRectButton closeButton;
    private OnClickListener closeButtonOnClickListener;

    private TextPaint privateKeyEditTextPaint;

    public SettingsView(Context context) {
        super(context);
        setBackgroundColor(Color.WHITE);

        // Get the configuration from preferences.
        ApplicationConfiguration configuration = ApplicationConfiguration.fromPreferences();

        titleTextView = new TextView(context);
        titleTextView.setTextColor(Color.BLACK);
        titleTextView.setText("Settings");
        titleTextView.setTypeface(titleTextView.getTypeface(), Typeface.BOLD);
        addView(titleTextView);

        privateKeyTextView = new TextView(context);
        privateKeyTextView.setTextColor(Color.BLACK);
        privateKeyTextView.setText("Nyzo string private key");
        addView(privateKeyTextView);

        privateKeyEditText = new EditText(context);
        privateKeyEditText.setText(configuration.getPrivateKeyString());
        privateKeyEditText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        privateKeyEditText.addTextChangedListener(this);
        addView(privateKeyEditText);

        privateKeyEditTextPaint = new TextPaint(privateKeyEditText.getPaint());

        baseTipAmountTextView = new TextView(context);
        baseTipAmountTextView.setTextColor(Color.BLACK);
        baseTipAmountTextView.setText("base tip amount, in Nyzos");
        addView(baseTipAmountTextView);

        baseTipAmountEditText = new EditText(context);
        baseTipAmountEditText.setText(configuration.getBaseTipString());
        baseTipAmountEditText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        baseTipAmountEditText.addTextChangedListener(this);
        addView(baseTipAmountEditText);

        maximumMicropayAmountTextView = new TextView(context);
        maximumMicropayAmountTextView.setTextColor(Color.BLACK);
        maximumMicropayAmountTextView.setText("maximum Micropay amount, in Nyzos");
        addView(maximumMicropayAmountTextView);

        maximumMicropayAmountEditText = new EditText(context);
        maximumMicropayAmountEditText.setText(configuration.getMaximumMicropayAmountString());
        maximumMicropayAmountEditText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        maximumMicropayAmountEditText.addTextChangedListener(this);
        addView(maximumMicropayAmountEditText);

        int numberOfNotices = notices.length;
        noticeTextViews = new TextView[numberOfNotices];
        for (int i = 0; i < numberOfNotices; i++) {
            TextView textView = new TextView(context);
            textView.setTextColor(Color.BLACK);
            textView.setBackgroundColor(0xfff8f7f6);
            textView.setTextAlignment(TEXT_ALIGNMENT_CENTER);
            textView.setText(notices[i]);
            addView(textView);
            noticeTextViews[i] = textView;
        }

        closeButton = new RoundRectButton(context);
        closeButton.setOnClickListener(v -> closeButtonClicked());
        closeButton.setIcon(Icon.makeCloseIcon(context));
        closeButton.setLabel("Done");
        addView(closeButton);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);

        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();

        float editTextFontSize = LayoutUtil.size(16.0f, metrics);
        int editTextPadding = (int) (editTextFontSize * 0.15f);

        float textViewSize = editTextFontSize * 0.8f;
        int textViewPadding = (int) (textViewSize * 0.15f);

        float titleFontSize = editTextFontSize * 1.2f;
        int titlePadding = (int) (titleFontSize * 0.15f);
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, titleFontSize);
        titleTextView.setPadding(titlePadding, titlePadding, titlePadding, titlePadding);

        TextView[] textViews = { privateKeyTextView, baseTipAmountTextView, maximumMicropayAmountTextView };
        EditText[] editTexts = { privateKeyEditText, baseTipAmountEditText, maximumMicropayAmountEditText };
        for (int i = 0; i < textViews.length; i++) {
            TextView textView = textViews[i];
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textViewSize);
            textView.setPadding(textViewPadding, textViewPadding, textViewPadding, textViewPadding);

            EditText editText = editTexts[i];
            editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, editTextFontSize);
            editText.setPadding(editTextPadding, editTextPadding, editTextPadding, editTextPadding);
        }

        for (int i = 0; i < noticeTextViews.length; i++) {
            TextView textView = noticeTextViews[i];
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textViewSize);
            textView.setPadding(textViewPadding, textViewPadding, textViewPadding, textViewPadding);
        }

        // Perform an initial update of the text field properties.
        updateTextFieldProperties();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
        int width = getWidth();

        // Center the title at the top.
        int buttonSize = (int) LayoutUtil.size(60.0f, metrics);
        int titleMargin = (int) LayoutUtil.size (LayoutUtil.recommendedMarginSize, metrics);
        int titleYc = titleMargin + buttonSize / 2;
        titleTextView.measure(0, 0);
        int titleTextViewWidth = titleTextView.getMeasuredWidth();
        int titleTextViewHeight = titleTextView.getMeasuredHeight();
        titleTextView.layout(width / 2 - titleTextViewWidth / 2, titleYc - titleTextViewHeight / 2, width / 2 +
                titleTextViewWidth / 2, titleYc + titleTextViewHeight / 2);

        // Place the close button in the top-right corner.
        closeButton.layout(width - buttonSize - titleMargin, titleMargin, width - titleMargin, buttonSize +
                titleMargin);

        // Measure and place the text views.
        int textViewMargin = (int) LayoutUtil.size(LayoutUtil.recommendedMarginSize, metrics);

        TextView[] textViews = { privateKeyTextView, baseTipAmountTextView, maximumMicropayAmountTextView };
        EditText[] editTexts = { privateKeyEditText, baseTipAmountEditText, maximumMicropayAmountEditText };
        int currentTop = buttonSize + textViewMargin;
        int verticalSpace = 0;
        for (int i = 0; i < 3; i++) {
            TextView textView = textViews[i];
            textView.measure(0, 0);
            int textViewHeight = textView.getMeasuredHeight();
            textView.layout(textViewMargin, currentTop, width - textViewMargin, currentTop + textViewHeight);

            currentTop += textViewHeight;

            EditText editText = editTexts[i];
            editText.measure(0, 0);
            int editTextHeight = editText.getMeasuredHeight();
            editText.layout(textViewMargin, currentTop, width - textViewMargin, currentTop + editTextHeight);

            if (verticalSpace == 0) {
                verticalSpace = editTextHeight / 5;
            }
            currentTop += editTextHeight + verticalSpace;
        }

        // Position the notices.
        currentTop += verticalSpace;
        for (int i = 0; i < noticeTextViews.length; i++) {
            TextView textView = noticeTextViews[i];
            textView.measure(0, 0);
            int textViewWidth = textView.getMeasuredWidth();
            int textViewHeight = textView.getMeasuredHeight();
            textView.layout((width - textViewWidth) / 2, currentTop, (width + textViewWidth) / 2,
                    currentTop + textViewHeight);

            currentTop += textViewHeight + verticalSpace;
        }

        updateTextFieldProperties();
    }

    public void setCloseButtonOnClickListener(OnClickListener listener) {
        closeButtonOnClickListener = listener;
    }

    private void closeButtonClicked() {
        if (closeButtonOnClickListener != null) {
            // Remove focus from all EditTexts and hide the keyboard.
            privateKeyEditText.clearFocus();
            baseTipAmountEditText.clearFocus();
            maximumMicropayAmountEditText.clearFocus();
            MainActivity.hideKeyboard();

            closeButtonOnClickListener.onClick(this);
        }
    }

    public void beforeTextChanged(CharSequence string, int start, int count, int after) {
    }

    public void onTextChanged(CharSequence string, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable editable) {
        // Get the values from the fields.
        String privateKeyString = privateKeyEditText.getText().toString();
        String baseTipString = baseTipAmountEditText.getText().toString();
        String maximumMicropayAmountString = maximumMicropayAmountEditText.getText().toString();

        // Save the values to preferences.
        ApplicationConfiguration configuration = new ApplicationConfiguration(privateKeyString, baseTipString,
                maximumMicropayAmountString);
        configuration.saveToPreferences();

        // Update the text field colors and request a redraw.
        updateTextFieldProperties();
    }

    private void updateTextFieldProperties() {
        int colorValid = AppearanceUtil.colorValid;
        int colorInvalid = AppearanceUtil.colorInvalid;

        ApplicationConfiguration configuration = ApplicationConfiguration.fromPreferences();
        privateKeyEditText.setBackgroundColor(configuration.isPrivateKeyValid() ? colorValid : colorInvalid);
        baseTipAmountEditText.setBackgroundColor(configuration.isBaseTipAmountValid() ? colorValid : colorInvalid);
        maximumMicropayAmountEditText.setBackgroundColor(configuration.isMaximumMicropayAmountValid() ? colorValid :
                colorInvalid);

        privateKeyEditTextPaint.setTextSize(privateKeyEditText.getTextSize());
        float textWidth = privateKeyEditTextPaint.measureText(privateKeyEditText.getText() + "");
        float availableWidth = privateKeyEditText.getWidth() - privateKeyEditText.getPaddingLeft() -
                privateKeyEditText.getPaddingRight();
        if (availableWidth > 0 && (textWidth > privateKeyEditText.getWidth() ||
                        (privateKeyEditText.getTextScaleX() < 1.0f))) {
            // The text scale is not applied continuously on the devices tested. It seems to jump in increments of
            // roughly 0.05, though not aligned with multiples of 0.05. To avoid field overflow, 0.05 is subtracted from
            // the calculated ratio.
            float ratio = Math.min(1.0f, Math.max(0.5f, availableWidth / textWidth)) - 0.05f;
            privateKeyEditText.setTextScaleX(ratio);
        }
    }
}
