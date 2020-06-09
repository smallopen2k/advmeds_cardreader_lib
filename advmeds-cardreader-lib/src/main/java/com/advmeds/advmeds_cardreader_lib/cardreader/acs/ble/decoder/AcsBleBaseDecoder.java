package com.advmeds.advmeds_cardreader_lib.cardreader.acs.ble.decoder;

import com.acs.bluetooth.BluetoothReader;

public interface AcsBleBaseDecoder {
    /**
     * @param reader
     * @param apdu
     *      response from card reader
     * @return
     *      please return null if has something error
     *      return "" if has next decode step
     *      return String that convert from object by gson and confirm that start with "{"
     */
    String decode(BluetoothReader reader, byte[] apdu);

    boolean onAtrAvailable(BluetoothReader reader);
}
