package ru.orangesoftware.financisto.barcode;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import ru.orangesoftware.financisto.utils.MyPreferences;

import static ru.orangesoftware.financisto.barcode.BarcodeHelper.splitQuery;

public class RequestReceiptTask extends AsyncTask<String, String, String> {

    private volatile boolean isRunning = true;

    private final Context context;
    private final String qrcode;

    private QRCodeListener listener = null;
    public void setListener(QRCodeListener listener) {
        this.listener = listener;
    }

    public RequestReceiptTask(Context context, String qrcode) {
        this.context = context;
        this.qrcode = qrcode;
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            Log.i("RequestReceiptTask", "doInBackground");
            String result = null;

            Map<String, String> mQRCode = splitQuery(qrcode);

            String url_1 = "https://proverkacheka.nalog.ru:9999/v1/ofds/*/inns/*/fss/" + mQRCode.get("fn") + "/operations/" + mQRCode.get("n") + "/tickets/" + mQRCode.get("i") + "?fiscalSign=" + mQRCode.get("fp") + "&date=" + mQRCode.get("t") + "&sum=" + mQRCode.get("s").replaceAll("\\.", "");
            String url_2 = "https://proverkacheka.nalog.ru:9999/v1/inns/*/kkts/*/fss/" + mQRCode.get("fn") + "/tickets/" + mQRCode.get("i") + "?fiscalSign=" + mQRCode.get("fp") + "&sendToEmail=no";

            String login = MyPreferences.getNalogLogin(context);
            String pass = MyPreferences.getNalogPassword(context);

            OkHttpClient client = new OkHttpClient();
            Request.Builder builder = new Request.Builder()
                    .header("Authorization", "Basic " + Base64.encodeToString((login+":"+pass).getBytes(), Base64.NO_WRAP))
                    .header("Device-Id", "")
                    .header("Device-OS", "");

            Request request = builder.url(url_1).build();
            Response response_check = client.newCall(request).execute();
            result = Integer.toString(response_check.code());

            if (response_check.code() == 204) {
                request = builder.url(url_2).build();
                Response response_get = client.newCall(request).execute();
                result = Integer.toString(response_get.code());

                if (response_get.code() == 202) {
                    response_get.close();
                    Thread.sleep(500);
                    response_get = client.newCall(request).execute();
                }

                if (response_get.code() == 200) {
                    JSONObject jsonObject;
                    try {
                        jsonObject = new JSONObject(response_get.body().string());
                        jsonObject = jsonObject.getJSONObject("document").getJSONObject("receipt");
                        jsonObject.remove("rawData");
                        result = jsonObject.toString();
                    } catch (Exception ex) {
                        Log.e("CheckRequest", "Unexpected error", ex);
                        result = response_get.body().string();
                    }
                }

                response_get.close();
            }
            response_check.close();
            return result;
        } catch (Exception ex) {
            Log.e("CheckRequest", "Unexpected error", ex);
            return "Error";
        }
    }

    @Override
    protected void onPostExecute(String result) {
        if (isRunning) {
            Log.i("RequestReceiptTask", "onPostExecute");
            if (listener != null)
                listener.onElectronicReceiptChanged(result);
        }
    }

    public void stop() {
        isRunning = false;
    }
}
