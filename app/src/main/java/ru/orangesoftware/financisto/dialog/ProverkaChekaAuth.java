package ru.orangesoftware.financisto.dialog;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.annotations.ViewById;
import org.json.JSONObject;

import java.util.Base64;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.utils.MyPreferences;

import static ru.orangesoftware.financisto.barcode.BarcodeHelper.splitQuery;

@EFragment(R.layout.proverka_cheka_auth)
public class ProverkaChekaAuth extends DialogFragment {

    @ViewById(R.id.etEmail)
    protected EditText etEmail;

    @ViewById(R.id.etName)
    protected EditText etName;

    @ViewById(R.id.etPhone)
    protected EditText etPhone;

    @ViewById(R.id.etPassword)
    protected EditText etPassword;

    @FragmentArg
    protected int type;

    @AfterViews
    protected void initUi() {
        int bgColorResource = R.color.mdtp_date_picker_view_animator_dark_theme;
        int bgColor = ContextCompat.getColor(getActivity(), bgColorResource);
        getView().setBackgroundColor(bgColor);

        switch (type) {
            case 1: // auth
                etPhone.setText(MyPreferences.getNalogLogin(getActivity()));
                etPassword.setText(MyPreferences.getNalogPassword(getActivity()));
                etEmail.getRootView().findViewById(R.id.pEmail).setVisibility(View.GONE);
                etName.getRootView().findViewById(R.id.pName).setVisibility(View.GONE);
                break;
            case 2: // sign up
                etPassword.getRootView().findViewById(R.id.pPassword).setVisibility(View.GONE);
                break;
            case 3: // recovery pass
                etPhone.setText(MyPreferences.getNalogLogin(getActivity()));
                etEmail.getRootView().findViewById(R.id.pEmail).setVisibility(View.GONE);
                etName.getRootView().findViewById(R.id.pName).setVisibility(View.GONE);
                etPassword.getRootView().findViewById(R.id.pPassword).setVisibility(View.GONE);
                break;
            default:
                Toast.makeText(getActivity(), "Unknown type of dialog", Toast.LENGTH_SHORT).show();
                dismiss();
                break;
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Click(R.id.bOK)
    public void onOk() {
        switch (type) {
            case 1:
                new NalogLoginTask(result -> {
                    switch (result.code) {
                        case 200:
                            MyPreferences.storeNalogAuth(getActivity(), etPhone.getText().toString(), etPassword.getText().toString());
                            Toast.makeText(getActivity(), R.string.sign_in_success, Toast.LENGTH_LONG).show();
                            dismiss();
                            break;
                        case 403:
                            Toast.makeText(getActivity(), R.string.user_not_found, Toast.LENGTH_LONG).show();
                            break;
                        default:
                            Toast.makeText(getActivity(), "Error " + result.code + "\n" + result.body, Toast.LENGTH_LONG).show();
                            break;
                    }
                }).execute(etPhone.getText().toString(), etPassword.getText().toString());
                break;
            case 2:
                new NalogPostTask(result -> {
                    switch (result.code) {
                        case 204:
                            MyPreferences.storeNalogAuth(getActivity(), etPhone.getText().toString(), null);
                            Toast.makeText(getActivity(), R.string.sign_up_success, Toast.LENGTH_LONG).show();
                            dismiss();
                            break;
                        case 400:
                            Toast.makeText(getActivity(), R.string.incorrect_email_format, Toast.LENGTH_LONG).show();
                            break;
                        case 409:
                            Toast.makeText(getActivity(), R.string.user_exists, Toast.LENGTH_LONG).show();
                            break;
                        case 500:
                            Toast.makeText(getActivity(), R.string.incorrect_phone, Toast.LENGTH_LONG).show();
                            break;
                        default:
                            Toast.makeText(getActivity(), "Error " + result.code + "\n" + result.body, Toast.LENGTH_LONG).show();
                            break;
                    }
                }).execute("signup", "{\"email\":\"" + etEmail.getText().toString() + "\",\"name\":\"" + etName.getText().toString() + "\",\"phone\":\"" + etPhone.getText().toString() + "\"}");
                break;
            case 3:
                new NalogPostTask(result -> {
                    switch (result.code) {
                        case 204:
                            MyPreferences.removeNalogPassword(getActivity());
                            Toast.makeText(getActivity(), R.string.recovery_pass_success, Toast.LENGTH_LONG).show();
                            dismiss();
                            break;
                        case 404:
                            Toast.makeText(getActivity(), R.string.user_not_found, Toast.LENGTH_LONG).show();
                            break;
                        default:
                            Toast.makeText(getActivity(), "Error " + result.code + "\n" + result.body, Toast.LENGTH_LONG).show();
                            break;
                    }
                }).execute("restore", "{\"phone\":\"" + etPhone.getText().toString() + "\"}");
                break;
        }
    }

    @Click(R.id.bCancel)
    public void onCancel() {
        dismiss();
    }

    private class NalogAuthData {
        public int code;
        public String body;
    }
    private interface NalogAuthListener {
        void callback(NalogAuthData result);
    }

    private class NalogPostTask extends AsyncTask<String, String, NalogAuthData> {

        private final NalogAuthListener listener;

        private NalogPostTask(@NonNull NalogAuthListener listener) {
            this.listener = listener;
        }

        @Override
        protected NalogAuthData doInBackground(String... params) {
            if (params.length < 2 || !(params[0].equals("signup") || params[0].equals("restore")))
                return null;

            NalogAuthData data = new NalogAuthData();
            try {
                OkHttpClient client = new OkHttpClient();
                RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), params[1]); // "{\"email\":\"" + params[0] + "\",\"name\":\"" + params[1] + "\",\"phone\":\"" + params[2] + "\"}"
                Request request = new Request.Builder().url("https://proverkacheka.nalog.ru:9999/v1/mobile/users/" + params[0]).post(body).build(); // "https://proverkacheka.nalog.ru:9999/v1/mobile/users/signup"
                Response response = client.newCall(request).execute();

                data.code = response.code();
                data.body = response.body().string();

                response.close();
            } catch (Exception ex) {
                data.code = 1;
            }
            return data;
        }

        @Override
        protected void onPostExecute(NalogAuthData result) {
            listener.callback(result);
        }
    }

    private class NalogLoginTask extends AsyncTask<String, String, NalogAuthData> {

        private final NalogAuthListener listener;

        private NalogLoginTask(@NonNull NalogAuthListener listener) {
            this.listener = listener;
        }

        @Override
        protected NalogAuthData doInBackground(String... params) {
            if (params.length < 2)
                return null;

            NalogAuthData data = new NalogAuthData();
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder()
                            .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((params[0] + ":" + params[1]).getBytes()))
                            .header("Device-Id", "")
                            .header("Device-OS", "")
                            .url("https://proverkacheka.nalog.ru:9999/v1/mobile/users/login").build();
                    Response response = client.newCall(request).execute();

                    data.code = response.code();
                    data.body = response.body().string();

                    response.close();
                }
            } catch (Exception ex) {
                data.code = 1;
            }
            return data;
        }

        @Override
        protected void onPostExecute(NalogAuthData result) {
            listener.callback(result);
        }
    }

}
