package com.advmeds.advmeds_cardreader_lib.cardreader.acs.usb.decoder;

import android.util.Log;

import com.acs.smartcard.Reader;
import com.advmeds.advmeds_cardreader_lib.cardreader.acs.usb.AcsUsbDevice;
import com.advmeds.advmeds_cardreader_lib.cardreader.UserModel;

import java.text.SimpleDateFormat;
import java.util.Date;


public class AcsUsbThaiDecoder implements AcsUsbBaseDecoder {
    private final static byte[] SELECT_APDU_THAI = new byte[]{
            (byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00, (byte) 0x08,
            (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x54,
            (byte) 0x48, (byte) 0x00, (byte) 0x01};

    private static final byte[] THAI_PERSON_INFO = new byte[]{
            (byte) 0x80, (byte) 0xB0, (byte) 0x00, (byte) 0x11, (byte) 0x02,
            (byte) 0x00, (byte) 0xD1};

    private static final byte[] THAI_NATIONAL_ID = new byte[]{
            (byte) 0x80, (byte) 0xB0, (byte) 0x00, (byte) 0x04, (byte) 0x02,
            (byte) 0x00, (byte) 0x0D};

    private static final byte[] GET_RESPONSE_ID = new byte[]{
            (byte) 0x00, (byte) 0xC0, (byte) 0x00, (byte) 0x00, (byte) 0x0D};

    private static final byte[] GET_RESPONSE_INFO = new byte[]{
            (byte) 0x00, (byte) 0xC0, (byte) 0x00, (byte) 0x00, (byte) 0xD1};

    private static final byte[] THAI_ISSUE_EXPIRE = new byte[]{
            (byte) 0x80, (byte) 0xB0, (byte) 0x01, (byte) 0x67, (byte) 0x02,
            (byte) 0x00, (byte) 0x12};
    private static final byte[] GET_RESPONSE_DATE = new byte[]{
            (byte) 0x00, (byte) 0xC0, (byte) 0x00, (byte) 0x00, (byte) 0x12};

    @Override
    public String decode(Reader reader) {
        try {
            // Set Protocol
            int activeProtocol = reader.setProtocol(AcsUsbDevice.SMART_CARD_SLOT, Reader.PROTOCOL_TX);

            if (activeProtocol == Reader.PROTOCOL_T0) {
                Log.d("AcsUsbThaiDecoder","Set protocol success.");
            } else {
                Log.d("AcsUsbThaiDecoder", "Set protocol error.");

                return null;
            }

            String cardNumber = "";
            String cardName = "";
            String cardID = "";
            String cardBirth = "";
            String cardGender = "";
            String cardIssuedDate = "";

            byte[] response = new byte[300];

            reader.transmit(AcsUsbDevice.SMART_CARD_SLOT, SELECT_APDU_THAI, SELECT_APDU_THAI.length, response, response.length);

            String responseString = bytesToHex(response);

            if (responseString.startsWith("61 ")) {
                Log.d("AcsUsbThaiDecoder","Transmit select success.");
            } else {
                Log.d("AcsUsbThaiDecoder","Transmit select error.");

                return null;
            }

            response = new byte[300];

            reader.transmit(AcsUsbDevice.SMART_CARD_SLOT, THAI_NATIONAL_ID, THAI_NATIONAL_ID.length, response, response.length);

            responseString = bytesToHex(response);

            if (responseString.startsWith("61 0D")) {
                Log.d("AcsUsbThaiDecoder","Transmit read national id success.");

                response = new byte[15];

                reader.transmit(AcsUsbDevice.SMART_CARD_SLOT, GET_RESPONSE_ID, GET_RESPONSE_ID.length, response, response.length);

                responseString = bytesToHex(response);

                if (responseString.endsWith("90 00")) {
                    byte[] id = new byte[13];

                    System.arraycopy(response, 0, id, 0, 13);

                    cardNumber = new String(id);

                    cardID = new String(id);
                }
            } else {
                Log.d("AcsUsbThaiDecoder", "Transmit read national id error.");

                return null;
            }

            response = new byte[300];

            reader.transmit(AcsUsbDevice.SMART_CARD_SLOT, THAI_PERSON_INFO, THAI_PERSON_INFO.length, response, response.length);

            responseString = bytesToHex(response);

            if (responseString.startsWith("61 D1")) {
                Log.d("AcsUsbThaiDecoder", "Transmit personal info success.");


                response = new byte[211];

                reader.transmit(AcsUsbDevice.SMART_CARD_SLOT, GET_RESPONSE_INFO, GET_RESPONSE_INFO.length, response, response.length);

                responseString = bytesToHex(response);

                if (responseString.endsWith("90 00")) {
                    byte[] name = new byte[90];

                    System.arraycopy(response, 0, name, 0, 90);

                    String[] nameArray = new String(name, "TIS620").split("#");

                    cardName = nameArray[0];

                    if (nameArray.length > 1) {
                        for (int i = 1; i < nameArray.length; i++) {
                            if (!nameArray[i].isEmpty()) {
                                cardName += " " + nameArray[i];
                            }
                        }
                    }
                    cardName = cardName.trim();

                    byte[] birthYear = new byte[4];

                    System.arraycopy(response, 200, birthYear, 0, 4);

                    int year = Integer.parseInt(new String(birthYear)) - 2454; // From Thai Year to R.O.C. Year

                    byte[] birthDate = new byte[4];

                    System.arraycopy(response, 204, birthDate, 0, 4);

                    cardBirth = String.format("%03d", year) + new String(birthDate);

                    byte genderByte = response[208];

                    char genderChar = (char) genderByte;

                    if (genderChar == '1') {
                        cardGender = "M";
                    } else {
                        cardGender = "F";
                    }
                }
            } else {
                Log.d("AcsUsbThaiDecoder","Transmit personal info error.");

                return null;
            }

            response = new byte[300];

            reader.transmit(AcsUsbDevice.SMART_CARD_SLOT, THAI_ISSUE_EXPIRE, THAI_ISSUE_EXPIRE.length, response, response.length);

            responseString = bytesToHex(response);

            if (responseString.startsWith("61 12")) {
                Log.d("AcsUsbThaiDecoder", "Transmit issued / expired date success.");

                response = new byte[20];

                reader.transmit(AcsUsbDevice.SMART_CARD_SLOT, GET_RESPONSE_DATE, GET_RESPONSE_DATE.length, response, response.length);

                responseString = bytesToHex(response);

                if (responseString.endsWith("90 00")) {
                    // Issued date
                    byte[] yearArray = new byte[4];

                    System.arraycopy(response, 0, yearArray, 0, 4);

                    int year = Integer.parseInt(new String(yearArray)) - 543; // From Thai Year to A.D.

                    byte[] dateArray = new byte[4];

                    System.arraycopy(response, 4, dateArray, 0, 4);

                    cardIssuedDate = year + new String(dateArray);

                    // Expired date
                    System.arraycopy(response, 8, yearArray, 0, 4);

                    year = Integer.parseInt(new String(yearArray)) - 543; // From Thai Year to A.D.

                    System.arraycopy(response, 12, dateArray, 0, 4);

                    // [Issued]/[Expired], 20140512/20230324
                    cardIssuedDate += "/" + year + new String(dateArray);
                }
            } else {
                Log.d("AcsUsbThaiDecoder","Transmit issued / expired date error.");

                return null;
            }

//            Gson gson = new Gson();

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
        } catch (Exception e) {
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
