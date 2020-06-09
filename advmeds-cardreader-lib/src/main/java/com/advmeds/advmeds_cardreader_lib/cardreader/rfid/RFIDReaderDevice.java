package com.advmeds.advmeds_cardreader_lib.cardreader.rfid;

import android.hardware.usb.UsbDevice;

public enum RFIDReaderDevice {
    SCANNER01("USB Virtual PS2 Port", 256, 1204),
    SENSOR01("USB Reader", 53, 65535),
    SENSOR02("SYC ID&IC USB Reader", 53, 65535);

    private String productName;

    private int productId;

    private int vendorId;

    RFIDReaderDevice(String _productName, int _productId, int _vendorId) {
        productName = _productName;

        productId = _productId;

        vendorId = _vendorId;
    }

    public static boolean checkHasRFIDReader(UsbDevice searchDevice) {
        for(RFIDReaderDevice device : RFIDReaderDevice.values()) {
            if(device.productId ==searchDevice.getProductId() && device.vendorId ==searchDevice.getVendorId()) {
                return true;
            }
        }

        return false;
    }
}
