package com.advmeds.advmeds_cardreader_lib.cardreader.acs.ble.decoder;

import com.acs.bluetooth.BluetoothReader;
import com.advmeds.mphr_health_go.room.model.UserModel;
import com.google.gson.Gson;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import timber.log.Timber;

/**
 * Don't Use!!!!
 * This Decoder not correctly work.
 */
public class AcsBleThaiDecoder implements AcsBleBaseDecoder {
    private byte[] apduCommand1 = new byte[]{
            (byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00, (byte) 0x08,
            (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x54,
            (byte) 0x48, (byte) 0x00, (byte) 0x01};

    private byte[] apduCommand2 = new byte[]{
            (byte) 0x80, (byte) 0xB0, (byte) 0x00, (byte) 0x04, (byte) 0x02,
            (byte) 0x00, (byte) 0x0D};

    private byte[] apduCommand3 = new byte[]{
            (byte) 0x00, (byte) 0xC0, (byte) 0x00, (byte) 0x00, (byte) 0x0D};

    private byte[] apduCommand4 = new byte[]{
            (byte) 0x80, (byte) 0xB0, (byte) 0x00, (byte) 0x11, (byte) 0x02,
            (byte) 0x00, (byte) 0xD1};

    private byte[] apduCommand5 = new byte[]{
            (byte) 0x00, (byte) 0xC0, (byte) 0x00, (byte) 0x00, (byte) 0xD1};

    private byte[] apduCommand6 = new byte[]{
            (byte) 0x80, (byte) 0xB0, (byte) 0x01, (byte) 0x67, (byte) 0x02,
            (byte) 0x00, (byte) 0x12};

    private byte[] apduCommand7 = new byte[]{
            (byte) 0x00, (byte) 0xC0, (byte) 0x00, (byte) 0x00, (byte) 0x12};

    private byte[] commandPointer = null;

    String cardNumber = "";
    String cardName = "";
    String cardID = "";
    String cardBirth = "";
    String cardGender = "";
    String cardIssuedDate = "";

    private void init() {
        cardNumber = "";
        cardName = "";
        cardID = "";
        cardBirth = "";
        cardGender = "";
        cardIssuedDate = "";

        commandPointer = null;
    }

    private String combineCardData() {
        try {
            Gson gson = new Gson();

            UserModel userModel = new UserModel();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");

            int west = 1911 + Integer.valueOf(cardBirth.substring(0, 3));

            Date date = null;
            date = sdf.parse(west + "/" + cardBirth.substring(3, 5) + "/" + cardBirth.substring(5, 7));

            userModel.setIcId(cardID);
            userModel.setBirthday(new java.sql.Date(date.getTime()));
            userModel.setName(cardName);
            userModel.setGender(cardGender);
            userModel.setCardType(1);

            return gson.toJson(userModel);
        }
        catch (ParseException e) {
            e.printStackTrace();

            return null;
        }
    }

    private boolean sendCommand(BluetoothReader reader, byte[] command) {
        if(reader == null) {
            Timber.d("CardReader not ready");

            init();

            return false;
        }

        /* Transmit APDU command. */
        if (!reader.transmitApdu(apduCommand1)) {
            Timber.d("CardReader not ready");

            init();

            return false;
        }

        commandPointer = command;

        return true;
    }

    @Override
    public String decode(BluetoothReader reader, byte[] apdu) {
        try {
            String response = bytesToHex(apdu);

            if (commandPointer == apduCommand1) {
                if (response.startsWith("61 ")) {
                    Timber.d("Transmit apdu success.");

                    if(!sendCommand(reader, apduCommand2)) return null;
                } else {
                    init();

                    return null;
                }
            } else if (commandPointer == apduCommand2) {
                if (response.startsWith("61 0D")) {
                    Timber.d("Transmit read national id success.");

                    if(!sendCommand(reader, apduCommand3)) return null;
                } else {
                    init();

                    return null;
                }
            } else if (commandPointer == apduCommand3) {
                if (response.endsWith("90 00")) {
                    Timber.d("Transmit read response id success.");

                    byte[] id = new byte[13];

                    System.arraycopy(apdu, 0, id, 0, 13);

                    cardNumber = new String(id);

                    cardID = new String(id);

                    if(!sendCommand(reader, apduCommand4)) return null;
                } else {
                    init();

                    return null;
                }
            } else if (commandPointer == apduCommand4) {
                if (response.startsWith("61 D1")) {
                    Timber.d("Transmit read personal info success.");

                    if(!sendCommand(reader, apduCommand5)) return null;
                } else {
                    init();

                    return null;
                }
            } else if (commandPointer == apduCommand5) {
                if (response.endsWith("90 00")) {
                    Timber.d("Transmit read response info success.");

                    byte[] name = new byte[100];

                    System.arraycopy(apdu, 100, name, 0, 100);

                    String[] nameArray = new String(name).split("#");

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

                    System.arraycopy(apdu, 200, birthYear, 0, 4);

                    int year = Integer.parseInt(new String(birthYear)) - 2454; // From Thai Year to R.O.C. Year

                    byte[] birthDate = new byte[4];

                    System.arraycopy(apdu, 204, birthDate, 0, 4);

                    cardBirth = String.format("%03d", year) + new String(birthDate);

                    byte genderByte = apdu[208];

                    char genderChar = (char) genderByte;

                    if (genderChar == '1') {
                        cardGender = "M";
                    } else {
                        cardGender = "F";
                    }

                    if(!sendCommand(reader, apduCommand6)) return null;
                } else {
                    init();

                    return null;
                }
            } else if (commandPointer == apduCommand6) {
                if (response.startsWith("61 12")) {
                    Timber.d("Transmit issued / expired date success.");

                    if(!sendCommand(reader, apduCommand7)) return null;
                } else {
                    init();

                    return null;
                }
            } else if (commandPointer == apduCommand7) {
                if (response.endsWith("90 00")) {
                    Timber.d("Transmit read response date success.");

                    byte[] yearArray = new byte[4];

                    System.arraycopy(apdu, 0, yearArray, 0, 4);

                    int year = Integer.parseInt(new String(yearArray)) - 543; // From Thai Year to A.D.

                    byte[] dateArray = new byte[4];

                    System.arraycopy(apdu, 4, dateArray, 0, 4);

                    cardIssuedDate = year + new String(dateArray);

                    // Expired date
                    System.arraycopy(apdu, 8, yearArray, 0, 4);

                    year = Integer.parseInt(new String(yearArray)) - 543; // From Thai Year to A.D.

                    System.arraycopy(apdu, 12, dateArray, 0, 4);

                    // [Issued]/[Expired], 20140512/20230324
                    cardIssuedDate += "/" + year + new String(dateArray);

                    return combineCardData();
                } else {
                    init();

                    return null;
                }
            } else {
                Timber.d("CommandPointer Not Catched ");

                init();

                return null;
            }

            return "";
        } catch (Exception e) {
            e.printStackTrace();

            init();

            return null;
        }
    }

    @Override
    public boolean onAtrAvailable(BluetoothReader reader) {
        init();

        return sendCommand(reader, apduCommand1);
    }

    private String bytesToHex(byte[] bytes) {
        String hexString = "";

        for (int i = 0; i < bytes.length; i++) {
            hexString += " " + String.format("%02X", bytes[i]);
        }

        return hexString.trim();
    }
}
