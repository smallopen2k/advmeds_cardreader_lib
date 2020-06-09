package com.advmeds.advmeds_cardreader_lib.cardreader.acs.ble.decoder;

import com.acs.bluetooth.BluetoothReader;
import com.advmeds.mphr_health_go.cardreader.acs.BleACSUtils;
import com.advmeds.mphr_health_go.room.model.UserModel;
import com.google.gson.Gson;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import timber.log.Timber;

public class AcsBleTWDecoder implements AcsBleBaseDecoder {
    private byte[] apduCommand1 = new byte[]{(byte) 0x00,
            (byte) 0xA4, (byte) 0x04, (byte) 0x00, (byte) 0x10,
            (byte) 0xD1, (byte) 0x58, (byte) 0x00, (byte) 0x00,
            (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x11, (byte) 0x00};

    private byte[] apduCommand2 = new byte[]{(byte) 0x00, (byte) 0xca, (byte) 0x11,
            (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x00};

    @Override
    public String decode(BluetoothReader reader, byte[] apdu) {
        String response = BleACSUtils.toHexString(apdu);

        if(response.startsWith("90 00")) {
            if(!reader.transmitApdu(apduCommand2)) {
                Timber.d("CardReader not ready");

                return null;
            }
            else {
                return "";
            }
        }
        else if(response.contains("90 00") && response.split("90 00").length > 1){

            Timber.d("Transmit read profile success.");
            try {
                String cardNumber = new String(Arrays.copyOfRange(apdu, 0, 12));
                String cardName = new String(Arrays.copyOfRange(apdu, 12, 32), "Big5").trim();
                String cardID = new String(Arrays.copyOfRange(apdu, 32, 42));
                String cardBirth = new String(Arrays.copyOfRange(apdu, 42, 49));
                String cardGender = new String(Arrays.copyOfRange(apdu, 49, 50));
                String cardIssuedDate = new String(Arrays.copyOfRange(apdu, 50, 57));
                String cardData = "卡號:" + cardNumber + "\n" + "姓名:" + cardName + "\n" +
                        "身分證號碼:" + cardID + "\n" + "出生日期:" + cardBirth + "\n" +
                        "性別:" + cardGender + "\n" + "發卡日期:" + cardIssuedDate;
                Timber.d("Name = " + cardData);
                Gson gson = new Gson();

                UserModel userModel = new UserModel();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
                int west = 1911 + Integer.valueOf(cardBirth.substring(0, 3));
                Date date =
                        null;
                try {
                    date = sdf.parse(west + "/" + cardBirth.substring(3, 5) + "/" + cardBirth.substring(5, 7));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                userModel.setIcId(cardID);
                userModel.setBirthday(new java.sql.Date(date.getTime()));
                userModel.setName(cardName);
                userModel.setGender(cardGender);
                userModel.setCardType(1);
                Timber.d("userModel = " + userModel.getGender());

                return gson.toJson(userModel);
            } catch (Exception e) {
                e.printStackTrace();

                return null;
            }

        }
        else {
            return null;
        }
    }

    @Override
    public boolean onAtrAvailable(BluetoothReader reader) {
        if(reader == null) {
            Timber.d("CardReader not ready");

            return false;
        }

        /* Transmit APDU command. */
        if (!reader.transmitApdu(apduCommand1)) {
            Timber.d("CardReader not ready");

            return false;
        }

        return true;
    }
}
