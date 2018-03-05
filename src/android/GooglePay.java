package com.cordova.plugin;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.CardRequirements;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.stripe.android.model.Token;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

public class GooglePay extends CordovaPlugin {
    private final int LOAD_PAYMENT_DATA_REQUEST_CODE = 666;
    private PaymentsClient mPaymentsClient;

    public static final List<Integer> SUPPORTED_METHODS = Arrays.asList(
        WalletConstants.PAYMENT_METHOD_CARD,
        WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD
    );

    public static final List<Integer> SUPPORTED_NETWORKS = Arrays.asList(
        WalletConstants.CARD_NETWORK_AMEX,
        WalletConstants.CARD_NETWORK_DISCOVER,
        WalletConstants.CARD_NETWORK_VISA,
        WalletConstants.CARD_NETWORK_MASTERCARD
    );

    private CallbackContext paymentCallbackContext;

    @Override
    protected void pluginInitialize() {
        mPaymentsClient =
            Wallet.getPaymentsClient(
                cordova.getActivity(),
                new Wallet.WalletOptions.Builder()
                    .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                    .build()
            );
    }

    @Override
    public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                if ("isReadyToPay".equals(action)) {
                    isReadyToPay(callbackContext);
                } else if ("makePayment".equals(action)) {
                    makePayment(args, callbackContext);
                }
            }
        });
        return true;
    }

    private void isReadyToPay(final CallbackContext callbackContext) {
        IsReadyToPayRequest request =
            IsReadyToPayRequest.newBuilder()
                .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_CARD)
                .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD)
                .build();
        Task<Boolean> task = mPaymentsClient.isReadyToPay(request);
        task.addOnCompleteListener(
            new OnCompleteListener<Boolean>() {
                public void onComplete(Task<Boolean> task) {
                    try {
                        boolean result = task.getResult(ApiException.class);
                        Log.i("GooglePay", "Is ready to pay: " + result);
                        callbackContext.success();
                    } catch (ApiException exception) {
                        callbackContext.error(exception.getMessage());
                    }
                }
            }
        );
    }

    private PaymentDataRequest createPaymentDataRequest(JSONObject data) {
        PaymentDataRequest.Builder request = PaymentDataRequest.newBuilder()
            .setTransactionInfo(TransactionInfo.newBuilder()
                .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                .setTotalPrice(data.optString("price"))
                .setCurrencyCode(data.optString("currency"))
                .build()
            )
            .addAllowedPaymentMethods(SUPPORTED_METHODS)
            .setCardRequirements(CardRequirements.newBuilder()
                .addAllowedCardNetworks(SUPPORTED_NETWORKS)
                .setAllowPrepaidCards(true)
                .build()
            );

        PaymentMethodTokenizationParameters params = PaymentMethodTokenizationParameters.newBuilder()
            .setPaymentMethodTokenizationType(
                WalletConstants.PAYMENT_METHOD_TOKENIZATION_TYPE_PAYMENT_GATEWAY
            )
            .addParameter("gateway", "stripe")
            .addParameter("stripe:publishableKey", data.optString("StripePublishableKey"))
            .addParameter("stripe:version", "5.1.0")
            .build();

        request.setPaymentMethodTokenizationParameters(params);
        return request.build();
    }

    private void makePayment(JSONArray data, CallbackContext callbackContext) {
        PaymentDataRequest request = createPaymentDataRequest(data.optJSONObject(0));

        if (request != null) {
            Log.i("GooglePay", "PaymentRequest: 'request' is not null!");
            AutoResolveHelper.resolveTask(
                mPaymentsClient.loadPaymentData(request),
                cordova.getActivity(),
                LOAD_PAYMENT_DATA_REQUEST_CODE
            );
        }
        paymentCallbackContext = callbackContext;
        cordova.setActivityResultCallback(this);
    }

    @Override
    public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
        paymentCallbackContext = callbackContext;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case LOAD_PAYMENT_DATA_REQUEST_CODE:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        PaymentData paymentData = PaymentData.getFromIntent(data);
                        String rawToken = paymentData.getPaymentMethodToken().getToken();
                        final Token stripeToken = Token.fromString(rawToken);
                        paymentCallbackContext.success(stripeToken.getId());
                        Log.i("GooglePay", "Payment OK");
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i("GooglePay", "Payment Canceled");
                        break;
                    case AutoResolveHelper.RESULT_ERROR:
                        Log.i("GooglePay", "Payment Error");
                        Status status = AutoResolveHelper.getStatusFromIntent(data);
                        break;
                    default:
                        // Do nothing.
                }
                break;
            default:
                // Do nothing.
        }
    }
}
