package com.advmeds.advmeds_cardreader_lib.cardreader.acs.ble;


public interface AcsBaseCallBack {
    void onGattStatusChanged(int status);

    void onBtReaderStatusChanged(int status);

    void onGetCardData(String userModelStr);

    void onCardRemove();
}
