package ru.orangesoftware.financisto.barcode;

public interface QRCodeListener {
    void onQRCodeChanged(String qrcode, long amount, long date);
    void onElectronicReceiptChanged(String data);
}
