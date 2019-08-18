package ru.orangesoftware.financisto.barcode;

public interface QRCodeListener {
    void onQRCodeChanged(String qrcode, long amount);
    void onElectronicReceiptChanged(String data);
}
