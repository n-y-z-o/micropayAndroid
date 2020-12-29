package co.nyzo.mobile;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import co.nyzo.verifier.Transaction;

public class MainView extends ViewGroup {

    private PaymentView paymentView;
    private SettingsView settingsView;

    private OnClickListener cancelButtonOnClickListener;
    private OnClickListener confirmButtonOnClickListener;
    private OnClickListener clearButtonOnClickListener;
    private OnClickListener retryButtonOnClickListener;

    public MainView(Context context) {
        super(context);

        paymentView = new PaymentView(context);
        paymentView.setSettingsButtonOnClickListener(view -> showSettingsView());
        paymentView.setCancelButtonOnClickListener(view -> {
            if (cancelButtonOnClickListener != null) {
                cancelButtonOnClickListener.onClick(view);
            }
        });
        paymentView.setConfirmButtonOnClickListener(view -> {
            if (confirmButtonOnClickListener != null) {
                confirmButtonOnClickListener.onClick(view);
            }
        });
        paymentView.setClearButtonOnClickListener(view -> {
            if (clearButtonOnClickListener != null) {
                clearButtonOnClickListener.onClick(view);
            }
        });
        paymentView.setRetryButtonOnClickListener(view -> {
            if (retryButtonOnClickListener != null) {
                retryButtonOnClickListener.onClick(view);
            }
        });
        addView(paymentView);

        settingsView = new SettingsView(context);
        settingsView.setCloseButtonOnClickListener(view -> hideSettingsView());
        addView(settingsView);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;

        paymentView.layout(0, 0, width, height);
        settingsView.layout(0, 0, width, height);
        settingsView.setTranslationY(MainActivity.isSettingsViewVisible() ? 0.0f : height);
    }

    public void setCancelButtonOnClickListener(OnClickListener listener) {
        this.cancelButtonOnClickListener = listener;
    }

    public void setConfirmButtonOnClickListener(OnClickListener listener) {
        this.confirmButtonOnClickListener = listener;
    }

    public void setClearButtonOnClickListener(OnClickListener listener) {
        this.clearButtonOnClickListener = listener;
    }

    public void setRetryButtonOnClickListener(OnClickListener listener) {
        this.retryButtonOnClickListener = listener;
    }

    private void showSettingsView() {
        MainActivity.setSettingsViewVisible(true);
        settingsView.animate().translationY(0.0f);
    }

    private void hideSettingsView() {
        MainActivity.setSettingsViewVisible(false);
        settingsView.animate().translationY(getHeight()).withEndAction(() -> {
            // Placing reconfiguration in the end action can eliminate visual oddities (jumps of location) in the
            // animation, and it's actually kind of cool for the user to see the settings applied after the settings
            // view is hidden.
            paymentView.configure();
        });
    }

    public void configure(MicropayConfiguration micropayConfiguration, Transaction existingTransaction,
                          InterfaceState interfaceState, List<String> messages, List<String> warnings,
                          List<String> errors, boolean success) {
        paymentView.configure(micropayConfiguration, existingTransaction, interfaceState, messages, warnings, errors,
                success);
    }
}
