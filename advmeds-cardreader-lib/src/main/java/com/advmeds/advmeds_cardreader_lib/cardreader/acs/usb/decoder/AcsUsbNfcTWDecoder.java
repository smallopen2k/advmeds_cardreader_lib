package com.advmeds.advmeds_cardreader_lib.cardreader.acs.usb.decoder;

import android.util.Log;

import com.acs.smartcard.Reader;

import com.advmeds.advmeds_cardreader_lib.cardreader.acs.usb.AcsUsbDevice;
import com.advmeds.advmeds_cardreader_lib.cardreader.UserModel;


public class AcsUsbNfcTWDecoder implements AcsUsbBaseDecoder {
    private static final byte[] READ_NFC_CARD_NO = new byte[]{
            (byte) 0xFF, (byte) 0xCA, (byte) 0x00, (byte) 0x00, (byte) 0x00};

    @Override
    public String decode(Reader reader) {
        byte[] response;

        try {
            response = new byte[300];

            reader.control(AcsUsbDevice.NFC_CARD_SLOT, Reader.IOCTL_CCID_ESCAPE,
                    READ_NFC_CARD_NO, READ_NFC_CARD_NO.length,
                    response, response.length);

            String resultString = convertNfcBytesToHex(response);
            Log.d("AcsUsbThaiDecoder",resultString);

            UserModel userModel = new UserModel();

//            Gson gson = new Gson();

            if (resultString.contains("900000") && resultString.contains("414944")) {
                String cardNumber = resultString.split("900000")[0];

                userModel.setIcId(cardNumber);

                userModel.setName("Smart Card");

                userModel.setCardType(2);

                return userModel.toString();
            } else if (resultString.contains("900000")) {
                String cardNumber = resultString.split("900000")[0];

                if (cardNumber.length() >= 8) {
                    userModel.setIcId(cardNumber);

                    userModel.setName("Smart Card");

                    userModel.setCardType(2);

                    return userModel.toString();
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();

            return null;
        }

        return null;
    }

    private static String convertNfcBytesToHex(byte[] bytes) {
        char[] hexArray = "0123456789ABCDEF".toCharArray();

        char[] hexChars = new char[bytes.length * 2];

        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
