package com.advmeds.advmeds_cardreader_lib.cardreader.acs.usb;

public interface AcsUsbCallBack {
    void onGetCardData(String userModelStr);

    void onCardRemove();

    void onConnectSucceess();

    void onConnectFail();
}
