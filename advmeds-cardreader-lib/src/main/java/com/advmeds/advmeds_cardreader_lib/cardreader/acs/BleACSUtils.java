/*
 * Copyright (C) 2014 Advanced Card Systems Ltd. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of Advanced
 * Card Systems Ltd. ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with the terms
 * of the license agreement you entered into with ACS.
 */

package com.advmeds.advmeds_cardreader_lib.cardreader.acs;

import android.content.Context;
import android.text.Editable;
import android.util.Log;
import android.widget.EditText;

import com.advmeds.advmeds_cardreader_lib.cardreader.UserModel;


import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;


/**
 * The
 * <code>CustomContextWrapper<code> class contains static methods which operate on arrays and
 * string.
 *
 * @author Gary Wong
 * @version 1.0, 4 Jun 2014
 */
public class BleACSUtils {

    /**
     * Creates a hexadecimal <code>String</code> representation of the
     * <code>byte[]</code> passed. Each element is converted to a
     * <code>String</code> via the {@link Integer#toHexString(int)} and
     * separated by <code>" "</code>. If the groupArray is <code>null</code>, then
     * <code>""<code> is returned.
     *
     * @param array the <code>byte</code> groupArray to convert.
     * @return the <code>String</code> representation of <code>groupArray</code> in
     * hexadecimal.
     */
    public static String toHexString(byte[] array) {

        String bufferString = "";

        if (array != null) {
            for (int i = 0; i < array.length; i++) {
                String hexChar = Integer.toHexString(array[i] & 0xFF);
                if (hexChar.length() == 1) {
                    hexChar = "0" + hexChar;
                }
                bufferString += hexChar.toUpperCase(Locale.US) + " ";
            }
        }
        return bufferString;
    }

    private static boolean isHexNumber(byte value) {
        if (!(value >= '0' && value <= '9') && !(value >= 'A' && value <= 'F')
                && !(value >= 'a' && value <= 'f')) {
            return false;
        }
        return true;
    }

    /**
     * Checks a hexadecimal <code>String</code> that is contained hexadecimal
     * value or not.
     *
     * @param string the string to check.
     * @return <code>true</code> the <code>string</code> contains Hex number
     * only, <code>false</code> otherwise.
     * @throws NullPointerException if <code>string == null</code>.
     */
    public static boolean isHexNumber(String string) {
        if (string == null)
            throw new NullPointerException("string was null");

        boolean flag = true;

        for (int i = 0; i < string.length(); i++) {
            char cc = string.charAt(i);
            if (!isHexNumber((byte) cc)) {
                flag = false;
                break;
            }
        }
        return flag;
    }

    private static byte uniteBytes(byte src0, byte src1) {
        byte _b0 = Byte.decode("0x" + new String(new byte[]{src0}))
                .byteValue();
        _b0 = (byte) (_b0 << 4);
        byte _b1 = Byte.decode("0x" + new String(new byte[]{src1}))
                .byteValue();
        byte ret = (byte) (_b0 ^ _b1);
        return ret;
    }

    /**
     * Creates a <code>byte[]</code> representation of the hexadecimal
     * <code>String</code> passed.
     *
     * @param string the hexadecimal string to be converted.
     * @return the <code>groupArray</code> representation of <code>String</code>.
     * @throws IllegalArgumentException if <code>string</code> length is not in even number.
     * @throws NullPointerException     if <code>string == null</code>.
     * @throws NumberFormatException    if <code>string</code> cannot be parsed as a byte value.
     */
    public static byte[] hexString2Bytes(String string) {
        if (string == null)
            throw new NullPointerException("string was null");

        int len = string.length();

        if (len == 0)
            return new byte[0];
        if (len % 2 == 1)
            throw new IllegalArgumentException(
                    "string length should be an even number");

        byte[] ret = new byte[len / 2];
        byte[] tmp = string.getBytes();

        for (int i = 0; i < len; i += 2) {
            if (!isHexNumber(tmp[i]) || !isHexNumber(tmp[i + 1])) {
                throw new NumberFormatException(
                        "string contained invalid value");
            }
            ret[i / 2] = uniteBytes(tmp[i], tmp[i + 1]);
        }
        return ret;
    }

    /**
     * Creates a <code>byte[]</code> representation of the hexadecimal
     * <code>String</code> in the EditText control.
     *
     * @param editText the EditText control which contains hexadecimal string to be
     *                 converted.
     * @return the <code>groupArray</code> representation of <code>String</code> in
     * the EditText control. <code>null</code> if the string format is
     * not correct.
     */
    public static byte[] getEditTextinHexBytes(EditText editText) {
        Editable edit = editText.getText();

        if (edit == null) {
            return null;
        }

        String rawdata = edit.toString();

        if (rawdata == null || rawdata.isEmpty()) {
            return null;
        }

        String command = rawdata.replace(" ", "").replace("\n", "");

        if (command.isEmpty() || command.length() % 2 != 0
                || isHexNumber(command) == false) {
            return null;
        }

        return hexString2Bytes(command);
    }

    public static byte[] getStringinHexBytes(String rawdata) {

        if (rawdata == null || rawdata.isEmpty()) {
            return null;
        }

        String command = rawdata.replace(" ", "").replace("\n", "");

        if (command.isEmpty() || command.length() % 2 != 0
                || isHexNumber(command) == false) {
            return null;
        }

        return hexString2Bytes(command);
    }

    /**
     * ACS讀卡機_插卡
     * @param response
     * @return
     */
    public static String getTranslationHealthCardInfo(byte[] response) {
        try {
            if (toHexString(response).startsWith("90 00")) {
                Log.d("BleACSUtils ","Transmit select success.");
                return "第一階段讀取成功";
            }

            String[] responseTmp = toHexString(response).split("90 00");
            if (responseTmp.length > 1) {
                Log.d("BleACSUtils ","Transmit read profile success.");

                String cardNumber = new String(Arrays.copyOfRange(response, 0, 12));
                String cardName = new String(Arrays.copyOfRange(response, 12, 32), "Big5").trim();
                String cardID = new String(Arrays.copyOfRange(response, 32, 42));
                String cardBirth = new String(Arrays.copyOfRange(response, 42, 49));
                String cardGender = new String(Arrays.copyOfRange(response, 49, 50));
                String cardIssuedDate = new String(Arrays.copyOfRange(response, 50, 57));
                String cardData = "卡號:" + cardNumber + "\n" + "姓名:" + cardName + "\n" +
                        "身分證號碼:" + cardID + "\n" + "出生日期:" + cardBirth + "\n" +
                        "性別:" + cardGender + "\n" + "發卡日期:" + cardIssuedDate;
                Log.d("BleACSUtils ","Card info = " + cardData);


                UserModel userModel = new UserModel();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
                int west = 1911 + Integer.valueOf(cardBirth.substring(0, 3));
                Date date =
                        sdf.parse(west + "/" + cardBirth.substring(3, 5) + "/" + cardBirth.substring(5, 7));
                userModel.setIcId(cardID);
                userModel.setBirthday(new java.sql.Date(date.getTime()));
                userModel.setName(cardName);
                userModel.setGender(cardGender);
                userModel.setCardType(1);

                return userModel.toString();
//                Patient patient = new Patient(cardName, cardID, cardNumber, cardGender, cardBirth
//                        , cardIssuedDate);
//                cardReaderCallback.newPatient(patient);
            } else if (responseTmp.length == 1) {
                return "第一階段讀取成功";
            } else {
                Log.d("BleACSUtils ","Transmit read profile error.");
                return "Transmit read profile error.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Crash :" + e;
        }
    }

    /**
     * ACS讀卡機_感應讀卡
     * @param response
     * @return
     */
    public static String getTranslationNfcCardInfo(byte[] response, Context context) {
        String resultString = convertNfcBytesToHex(response);
        Log.d("BleACSUtils ",resultString);

        UserModel userModel = new UserModel();


        if(resultString.contains("900000") && resultString.contains("414944")) {
            String cardNumber = resultString.split("900000")[0];

            userModel.setIcId(cardNumber);

            userModel.setName("Smart Card");

            userModel.setCardType(2);

            return userModel.toString();
        }
        else if(resultString.contains("900000")) {
            String cardNumber = resultString.split("900000")[0];

            if(cardNumber.length() >= 8) {
                userModel.setIcId(cardNumber);

                userModel.setName("Smart Card");

                userModel.setCardType(2);

                return userModel.toString();
            }
        }

        return "解析失敗";
    }

    public static String convertNfcBytesToHex(byte[] bytes) {
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
