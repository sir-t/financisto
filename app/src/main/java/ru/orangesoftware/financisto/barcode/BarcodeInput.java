package ru.orangesoftware.financisto.barcode;

import android.Manifest;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.ViewById;

import java.util.Map;

import ru.orangesoftware.financisto.R;

import static ru.orangesoftware.financisto.activity.RequestPermission.isRequestingPermission;
import static ru.orangesoftware.financisto.barcode.BarcodeHelper.splitQuery;

@EFragment(R.layout.barcode_input)
public class BarcodeInput extends DialogFragment {

    @ViewById(R.id.etDateTime)
    protected EditText etDateTime;

    @ViewById(R.id.etTypeOperation)
    protected EditText etTypeOperation;

    @ViewById(R.id.etSum)
    protected EditText etSum;

    @ViewById(R.id.etFN)
    protected EditText etFN;

    @ViewById(R.id.etFD)
    protected EditText etFD;

    @ViewById(R.id.etFPD)
    protected EditText etFPD;

    @SystemService
    protected Vibrator vibrator;

    @FragmentArg
    protected String qrcode;
    private long amount = 0;

    private static final int BARCODE_READER_REQUEST_CODE = 1;

    private QRCodeListener listener;
    public void setListener(QRCodeListener listener) {
        this.listener = listener;
    }

    @AfterInject
    public void init() {

    }

    @AfterViews
    public void initUi() {
        int bgColorResource = R.color.mdtp_date_picker_view_animator_dark_theme;
        int bgColor = ContextCompat.getColor(getActivity(), bgColorResource);
        getView().setBackgroundColor(bgColor);
        setQRCode(qrcode);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Click(R.id.bScan)
    public void onScan() {
        if (isRequestingPermission(getActivity(), Manifest.permission.CAMERA)) {
            Toast.makeText(getActivity(), R.string.permission_camera_rationale, Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(getActivity(), BarcodeCaptureActivity.class);
        startActivityForResult(intent, BARCODE_READER_REQUEST_CODE);
    }

    @Click(R.id.bOK)
    public void onOk() {
        if (!genQRCode()) {
            Toast.makeText(getActivity(), "genQRCode is false", Toast.LENGTH_LONG).show();
            return;
        }
        if (qrcode != null)
            listener.onQRCodeChanged(qrcode, amount);
        else
            Toast.makeText(getActivity(), "QRCode not new or null", Toast.LENGTH_LONG).show();
        dismiss();
    }

    @Click(R.id.bCancel)
    public void onCancel() {
        dismiss();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BARCODE_READER_REQUEST_CODE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    Point[] p = barcode.cornerPoints;
                    setQRCode(barcode.displayValue);
                    Log.i("Financisto", "BARCODE " + barcode.displayValue);
                } else {
                    Log.e("Financisto", "BARCODE No Result Found");
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private boolean checkQRCode(Map<String, String> mQRCode) {
        if (mQRCode.get("t").length() == 0) {
            Toast.makeText(getActivity(), "Error \'t\'", Toast.LENGTH_LONG).show();
            return false;
        }
        if (mQRCode.get("s").length() == 0) {
            Toast.makeText(getActivity(), "Error \'s\'", Toast.LENGTH_LONG).show();
            return false;
        }
        if (mQRCode.get("n").length() != 1) {
            Toast.makeText(getActivity(), "Error \'n\' length", Toast.LENGTH_LONG).show();
            return false;
        }
        if (mQRCode.get("fn").length() != 16 || mQRCode.get("i").length() > 10 || mQRCode.get("fp").length() > 10) {
            Toast.makeText(getActivity(), "Error with \'fn\' or \'i\' or \'fp\'", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private boolean genQRCode() {
        String qrcode = "";
        qrcode += "t=" + etDateTime.getText().toString()
                + "&s=" + etSum.getText().toString()
                + "&fn=" + etFN.getText().toString().replaceAll(" ", "")
                + "&i=" + etFD.getText().toString().replaceAll(" ", "")
                + "&fp=" + etFPD.getText().toString().replaceAll(" ", "")
                + "&n=" + etTypeOperation.getText().toString();

        try {
            Log.i("Financisto", "genQRCode " + qrcode);
            Map<String, String> mQRCode = splitQuery(qrcode);
            if (checkQRCode(mQRCode)) {
                Log.i("Financisto", "genQRCode checked true -> this.qrcode = " + this.qrcode);
                long amount = (Integer.parseInt(mQRCode.get("n")) == 1 ? -1 : 1) * Long.parseLong(mQRCode.get("s").replaceAll("\\.", ""));
                Log.i("Financisto", "genQRCode amount = " + amount);

                this.qrcode = qrcode;
                this.amount = amount;

                return true;
            }
        } catch (Exception ex) {
            Log.e("Financisto", "Unknown error", ex);
        }
        return false;
    }

    private void setQRCode(String qrcode) {
        if (qrcode == null || qrcode.isEmpty())
            return;

        try {
            long amount = 0;
            String n = "";

            Map<String, String> mQRCode = splitQuery(qrcode);
            for (String key : mQRCode.keySet()) {
                String val = mQRCode.get(key);
                switch (key) {
                    case "t":
                        etDateTime.setText(val);
                        break;
                    case "s":
                        etSum.setText(val);
                        amount = Long.parseLong(val.replaceAll("\\.", ""));
                        break;
                    case "fn":
                        etFN.setText(val);
                        break;
                    case "fp":
                        etFPD.setText(val);
                        break;
                    case "i":
                        etFD.setText(val);
                        break;
                    case "n":
                        n = val;
                        etTypeOperation.setText(val);
                        break;
                }
            }
            if (checkQRCode(mQRCode)) {
                if (n.charAt(0) == '1')
                    amount *= -1;
            }
            this.qrcode = qrcode;
            this.amount = amount;
        } catch (Exception ex) {
            Log.e("Financisto", "Unknown error", ex);
        }
    }

}
