package com.advmeds.advmeds_cardreader_lib.cardreader.acs.usb.decoder;

import android.util.Log;

import com.acs.smartcard.Reader;
import com.advmeds.advmeds_cardreader_lib.cardreader.acs.usb.AcsUsbDevice;
import com.advmeds.advmeds_cardreader_lib.cardreader.UserModel;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;


public class AcsUsbTWDecoder implements AcsUsbBaseDecoder {
    //暫時不接NFC
//    private final static byte[] NFC_CARD_NO_APDU = new byte[]{
//            (byte) 0xFF, (byte) 0xCA, (byte) 0x00, (byte) 0x00, (byte) 0x00};
//
//    private static byte[] NFC_READ_BLOCK_APDU = new byte[]{
//            (byte) 0xFF, (byte) 0xB0, (byte) 0x00, (byte) 0x00, (byte) 0x10};

    private final static byte[] SELECT_APDU = new byte[]{
            (byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00, (byte) 0x10,
            (byte) 0xD1, (byte) 0x58, (byte) 0x00, (byte) 0x00, (byte) 0x01,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x11,
            (byte) 0x00};

    private final static byte[] READ_PROFILE_APDU = new byte[]{
            (byte) 0x00, (byte) 0xca, (byte) 0x11, (byte) 0x00, (byte) 0x02,
            (byte) 0x00, (byte) 0x00};


    @Override
    public String decode(Reader reader) {
        byte[] response = new byte[300];

        try {
            // Set Protocol: T=1
            int activeProtocol = reader.setProtocol(AcsUsbDevice.SMART_CARD_SLOT, Reader.PROTOCOL_T1);

            if (activeProtocol == Reader.PROTOCOL_T1) {
                Log.d("AcsUsbTWDecoder","Set protocol success.");
            } else {
                Log.d("AcsUsbTWDecoder", "Set protocol error.");

                return null;
            }

            reader.transmit(AcsUsbDevice.SMART_CARD_SLOT, SELECT_APDU, SELECT_APDU.length, response, response.length);

            String responseString = bytesToHex(response);

            if (responseString.startsWith("90 00")) {
                Log.d("AcsUsbTWDecoder","Transmit select success.");
            } else {
                Log.d("AcsUsbTWDecoder","Transmit select error.");

                return null;
            }

            // Transmit: Read profile APDU
            response = new byte[300];

            reader.transmit(AcsUsbDevice.SMART_CARD_SLOT, READ_PROFILE_APDU, READ_PROFILE_APDU.length, response, response.length);

            if(bytesToHex(response).startsWith("90 00")) {
                Log.d("AcsUsbTWDecoder","Transmit read profile fail 1.");

                return null;
            }

            String[] responseTmp = bytesToHex(response).split("90 00");

            if (responseTmp.length > 1) {
                Log.d("AcsUsbTWDecoder","Transmit read profile success.");

                String cardNumber = new String(Arrays.copyOfRange(response, 0, 12));
                String cardName = new String(Arrays.copyOfRange(response, 12, 32), "Big5").trim();
                String cardID = new String(Arrays.copyOfRange(response, 32, 42));
                String cardBirth = new String(Arrays.copyOfRange(response, 42, 49));
                String cardGender = new String(Arrays.copyOfRange(response, 49, 50));
                String cardIssuedDate = new String(Arrays.copyOfRange(response, 50, 57));

//                String cardData = "卡號:" + cardNumber + "\n" + "姓名:" + cardName + "\n" +
//                        "身分證號碼:" + cardID + "\n" + "出生日期:" + cardBirth + "\n" +
//                        "性別:" + cardGender + "\n" + "發卡日期:" + cardIssuedDate;
//                Log.d("AcsUsbTWDecoder","Name = " + cardData);

//                Gson gson = new Gson();

                UserModel userModel = new UserModel();

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");

                int west = 1911 + Integer.valueOf(cardBirth.substring(0, 3));

                Date date = sdf.parse(west + "/" + cardBirth.substring(3, 5) + "/" + cardBirth.substring(5, 7));

                userModel.setIcId(cardID);
                userModel.setBirthday(new java.sql.Date(date.getTime()));
                userModel.setName(cardName);
                userModel.setGender(cardGender);
                userModel.setCardType(1);

                return userModel.toString();
            } else if (responseTmp.length == 1) {
                Log.d("AcsUsbTWDecoder","Transmit read profile fail 2.");

                return null;
            } else {
                Log.d("AcsUsbTWDecoder","Transmit read profile fail 3.");

                return null;
            }
        }
        catch(Exception e){
            e.printStackTrace();

            return null;
        }
    }

    private String bytesToHex(byte[] bytes) {
        String hexString = "";

        for (int i = 0; i < bytes.length; i++) {
            hexString += " " + String.format("%02X", bytes[i]);
        }
        return hexString.trim();
    }
}
