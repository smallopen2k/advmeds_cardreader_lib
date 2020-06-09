package com.advmeds.advmeds_cardreader_lib.cardreader.acs.usb;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.acs.smartcard.Reader;
import com.advmeds.mphr_health_go.cardreader.acs.usb.decoder.AcsUsbBaseDecoder;

import io.reactivex.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

public class AcsUsbDevice {
    private AcsUsbCallBack callBack;

    private Reader reader;

    private UsbManager usbManager;

    public static final int SMART_CARD_SLOT = 0;

    public static final int NFC_CARD_SLOT = 1;

    private AcsUsbBaseDecoder[] usbDecoder;

    private AcsUsbBaseDecoder nfcDecoder;

    private Context context;

    public void init(UsbManager _usbManager, AcsUsbCallBack _callBack, Context context) {
        usbManager = _usbManager;

        callBack = _callBack;

        this.context = context;

        reader = new Reader(usbManager);
    }

    public void setUsbDecoder(AcsUsbBaseDecoder... _decoder) {
        usbDecoder = _decoder;
    }

    public void setNfcDecoder(AcsUsbBaseDecoder _decoder) {
        nfcDecoder = _decoder;
    }

    public UsbDevice getSupportDevice() {
        for (UsbDevice device : usbManager.getDeviceList().values()) {
            if (reader != null && reader.isSupported(device)) {
                return device;
            }
        }

        return null;
    }

    public boolean checkConnectStatus() {
        boolean readerStatus = reader != null && reader.isOpened();

        UsbDevice usbDevice = getSupportDevice();

        boolean deviceStatus = usbDevice != null && usbManager.hasPermission(usbDevice);

        return readerStatus && deviceStatus;
    }

    public void connectDevice(UsbDevice device) {
        reader.setOnStateChangeListener(new Reader.OnStateChangeListener() {
            @Override
            public void onStateChange(int cardType, int prevState, int cardAction) {
                if (cardAction < Reader.CARD_UNKNOWN || cardAction > Reader.CARD_SPECIFIC) {
                    cardAction = Reader.CARD_UNKNOWN;
                }

                if (cardAction == Reader.CARD_PRESENT ) { //卡片插入
                    Timber.d("Card present , slotNum = " + cardType);
                    switch (cardType) {
                        case SMART_CARD_SLOT:
                            if(usbDecoder != null && powerOnSmartCard()) {
                                String response = null;

                                for (AcsUsbBaseDecoder acsUsbBaseDecoder : usbDecoder) {
                                    response = acsUsbBaseDecoder.decode(reader);

                                    if (response != null) {
                                        break;
                                    }
                                }

                                if(response != null) {
                                    if (response.startsWith("{")) {
                                        Timber.d("startsWith { ");

                                        Timber.d(response);

                                        String responseBuf = response;

                                        AndroidSchedulers.mainThread().scheduleDirect(
                                                new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        callBack.onGetCardData(responseBuf);
                                                    }
                                                }
                                        );
                                    } else {
                                        Timber.d(response);
                                    }
                                }
                            }

                            break;
                        case NFC_CARD_SLOT:
                            String response = nfcDecoder.decode(reader);

                            if(response != null) {

                                if (response.startsWith("{")) {
                                    Timber.d("startsWith { ");

                                    AndroidSchedulers.mainThread().scheduleDirect(
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    callBack.onGetCardData(response);
                                                }
                                            }
                                    );
                                } else {
                                    Timber.d(response);
                                }
                            }

                            break;
                    }
                } else if (cardAction == Reader.CARD_ABSENT) { //卡片抽離
                    Timber.d("Card absent : " + cardType);

                    if (cardType == SMART_CARD_SLOT) {
                        callBack.onCardRemove();
                    }
                    else if(cardType == NFC_CARD_SLOT) {
                        callBack.onCardRemove();
                    }
                }
            }
        });
        try {
            reader.open(device);

            if(reader.isOpened()) {
                Timber.d("Connect Success");

                callBack.onConnectSucceess();
            }
            else {
                Timber.d("Connect Fail");

                callBack.onConnectFail();
            }
        }
        catch (Exception ex) {
            Timber.d("Connect Fail");

            callBack.onConnectFail();
        }
    }

    private boolean powerOnSmartCard() {
        try {
            byte[] atr = reader.power(SMART_CARD_SLOT, Reader.CARD_WARM_RESET);

            if (atr == null) {
                Timber.d("Power error.");

                return false;
            } else {
                Timber.d("Power success.");

                return true;
            }
        }
        catch (Exception e) {
            e.printStackTrace();

            return false;
        }
    }
//
//    private boolean readSmartCard() {
//        try {
//            // Power: warm reset
//            byte[] atr = reader.power(SMART_CARD_SLOT, Reader.CARD_WARM_RESET);
//
//            if (atr == null) {
//                Log.i(TAG, "Power error.");
//                return false;
//            } else {
//                Log.i(TAG, "Power success.");
//            }
//
//            // Set Protocol: T=1
//            int activeProtocol = reader.setProtocol(SMART_CARD_SLOT, Reader.PROTOCOL_T1);
//            if (activeProtocol == Reader.PROTOCOL_T1) {
//                Log.i(TAG, "Set protocol success.");
//            } else {
//                Log.i(TAG, "Set protocol error.");
//                return false;
//            }
//
//            // Transmit: Select APDU
//            byte[] response = new byte[300];
//            reader.transmit(SMART_CARD_SLOT, SELECT_APDU, SELECT_APDU.length, response, response.length);
//
//            String responseString = bytesToHex(response);
//            if (responseString.startsWith("90 00")) {
//                Log.i(TAG, "Transmit select success.");
//            } else {
//                Log.i(TAG, "Transmit select error.");
//                return false;
//            }
//
//            // Transmit: Read profile APDU
//            response = new byte[300];
//            reader.transmit(SMART_CARD_SLOT, READ_PROFILE_APDU, READ_PROFILE_APDU.length, response, response.length);
//
//            String[] responseTmp = bytesToHex(response).split("90 00");
//            if (responseTmp.length > 1) {
//                Log.i(TAG, "Transmit read profile success.");
//
//                String cardNumber = new String(Arrays.copyOfRange(response, 0, 12));
//                String cardName = new String(Arrays.copyOfRange(response, 12, 32), "Big5").trim();
//                String cardID = new String(Arrays.copyOfRange(response, 32, 42));
//                String cardBirth = new String(Arrays.copyOfRange(response, 42, 49));
//                String cardGender = new String(Arrays.copyOfRange(response, 49, 50));
//                String cardIssuedDate = new String(Arrays.copyOfRange(response, 50, 56));
//                String cardData = "卡號:" + cardNumber + "\n" + "姓名:" + cardName + "\n" +
//                        "身分證號碼:" + cardID + "\n" + "出生日期:" + cardBirth + "\n" +
//                        "性別:" + cardGender + "\n" + "發卡日期:" + cardIssuedDate;
//                Log.i(TAG, "Name = " + cardData);
//                Patient patient = new Patient(cardName,cardID,cardNumber,cardGender,cardBirth,cardIssuedDate);
//                cardReaderCallback.newPatient(patient);
//
//                return true;
//            } else {
//                Log.i(TAG, "Transmit read profile error.");
//
//                return false;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//
//            return false;
//        }
//    }

    public void checkACSDevice() {

    }
}
