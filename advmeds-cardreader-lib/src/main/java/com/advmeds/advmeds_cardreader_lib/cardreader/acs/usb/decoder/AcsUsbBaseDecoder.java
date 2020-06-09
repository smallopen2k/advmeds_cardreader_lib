package com.advmeds.advmeds_cardreader_lib.cardreader.acs.usb.decoder;

import com.acs.smartcard.Reader;

public interface AcsUsbBaseDecoder {
    String decode(Reader reader);
}
