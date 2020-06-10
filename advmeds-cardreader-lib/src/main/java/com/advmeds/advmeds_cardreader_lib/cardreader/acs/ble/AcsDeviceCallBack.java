package com.advmeds.advmeds_cardreader_lib.cardreader.acs.ble;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.advmeds.advmeds_cardreader_lib.cardreader.UserModel;

public interface AcsDeviceCallBack {
    void onGetUsbDevicePermission(UsbManager usbManager, UsbDevice usbDevice);

    void onNotFoundUsbCr();

    void onUsbCrConnectSuccess();

    void onUsbCrConnectFail();

    void onConnectSucceed();

    void onGetCardData(UserModel userModel);

    void onCardRemove();
}
