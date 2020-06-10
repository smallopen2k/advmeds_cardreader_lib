package com.advmeds.advmeds_cardreader_lib.cardreader.acs.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import com.acs.bluetooth.Acr1255uj1Reader;
import com.acs.bluetooth.Acr3901us1Reader;
import com.acs.bluetooth.BluetoothReader;
import com.acs.bluetooth.BluetoothReaderGattCallback;
import com.acs.bluetooth.BluetoothReaderManager;
import com.advmeds.advmeds_cardreader_lib.cardreader.acs.BleACSUtils;
import com.advmeds.advmeds_cardreader_lib.cardreader.acs.ble.decoder.AcsBleBaseDecoder;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;


public class AcsBaseDevice {
    private AcsBaseCallBack callBack;

    private AcsBleBaseDecoder[] bleDecoder;

    private int nowDecoderIndex = 0;

    //以下為ACS藍芽讀卡機連線相關
    /* Detected reader. */
    private BluetoothReader mBluetoothReader;

    /* ACS Bluetooth reader library. */
    private BluetoothReaderManager mBluetoothReaderManager;

    private BluetoothReaderGattCallback mGattCallback;

    private int mConnectState = BluetoothReader.STATE_DISCONNECTED;

    /* Bluetooth GATT client. */
    private BluetoothGatt mBluetoothGatt;

    public void setBleDecoder(AcsBleBaseDecoder... _decoder) {
        bleDecoder = _decoder;
    }

    public void init(AcsBaseCallBack _callBack) {
        callBack = _callBack;
        /* Initialize BluetoothReaderGattCallback. */
        mGattCallback = new BluetoothReaderGattCallback();

        /* Register BluetoothReaderGattCallback's listeners */
        mGattCallback
                .setOnConnectionStateChangeListener(new BluetoothReaderGattCallback.OnConnectionStateChangeListener() {

                    @Override
                    public void onConnectionStateChange(
                            final BluetoothGatt gatt, final int state,
                            final int newState) {
                        Log.d("AcsBaseDevice ", "GATT VIEW_NOW_STATUS : " + state);

                        Log.d("AcsBaseDevice ", "GATT NEW VIEW_NOW_STATUS : " + newState);

                        if (newState == BluetoothProfile.STATE_CONNECTED) {

                            if (mBluetoothReaderManager != null) {
                                mBluetoothReaderManager.detectReader(
                                        gatt, mGattCallback);
                            }
                            Log.d("AcsBaseDevice ", "GATT STATE_CONNECTED");
                        } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                            Log.d("AcsBaseDevice ", "STATE_CONNECTING");
                        } else {
                            Log.d("AcsBaseDevice ", "STATE_DISCONNECTED OR ELSE");

                            mBluetoothReader = null;

                            if (mBluetoothGatt != null) {
                                mBluetoothGatt.close();
                                mBluetoothGatt = null;
                            }

                            callBack.onBtReaderStatusChanged(newState);
                        }
                    }
                });

        /* Initialize mBluetoothReaderManager. */
        mBluetoothReaderManager = new BluetoothReaderManager();

        /* Register BluetoothReaderManager's listeners */
        mBluetoothReaderManager
                .setOnReaderDetectionListener(new BluetoothReaderManager.OnReaderDetectionListener() {

                    @Override
                    public void onReaderDetection(BluetoothReader reader) {
                        if (reader instanceof Acr3901us1Reader) {
                            /* The connected reader is ACR3901U-S1 reader. */
                            Log.e("AcsBaseDevice ", "On Acr3901us1Reader Detected.");
                        } else if (reader instanceof Acr1255uj1Reader) {
                            /* The connected reader is ACR1255U-J1 reader. */
                            Log.e("AcsBaseDevice ", "On Acr1255uj1Reader Detected.");
                        } else {
                            disconnectReader();

                            Log.d("AcsBaseDevice ", "斷線 STATE_DISCONNECTED");

                            return;
                        }

                        mBluetoothReader = reader;

                        setListener(mBluetoothReader);

                        activateReader(mBluetoothReader);
                    }
                });
    }

    /*
     * Create a GATT connection with the reader. And detect the connected reader
     * once service list is available.
     */
    public void connectReader(Context context, String mDeviceAddress) {
        BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Log.e("AcsBaseDevice ", "Unable to initialize BluetoothManager.");

            callBack.onBtReaderStatusChanged(BluetoothReader.STATE_DISCONNECTED);

            return;
        }

        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.e("AcsBaseDevice ", "Unable to obtain a BluetoothAdapter.");
            callBack.onBtReaderStatusChanged(BluetoothReader.STATE_DISCONNECTED);

            return;
        }

        /*
         * Connect Device.
         */
        /* Clear old GATT connection. */
        if (mBluetoothGatt != null) {
            Log.e("AcsBaseDevice ", "Clear old GATT connection");

            mBluetoothGatt.disconnect();

            mBluetoothGatt.close();

            mBluetoothGatt = null;
        }

        /* Create a new connection. */
        final BluetoothDevice device = bluetoothAdapter
                .getRemoteDevice(mDeviceAddress);

        if (device == null) {
            Log.e("AcsBaseDevice ", "Device not found. Unable to connect.");

            callBack.onBtReaderStatusChanged(BluetoothReader.STATE_DISCONNECTED);

            return;
        }

        /* Connect to GATT server. */
        callBack.onBtReaderStatusChanged(BluetoothReader.STATE_CONNECTING);

        mBluetoothGatt = device.connectGatt(context, false, mGattCallback);
    }

    /* Update the display of Connection status string. */
    private void updateConnectionState(final int connectState) {
        mConnectState = connectState;

        if (connectState == BluetoothReader.STATE_CONNECTING) {
            Log.d("AcsBaseDevice ", "連線中");
        } else if (connectState == BluetoothReader.STATE_CONNECTED) {
            Log.d("AcsBaseDevice ", "已連線");
        } else if (connectState == BluetoothReader.STATE_DISCONNECTING) {
            Log.d("AcsBaseDevice ", "斷線中");
        } else {
            Log.d("AcsBaseDevice ", "已斷線");
        }
    }

    /* Disconnects an established connection. */
    public void disconnectReader() {
        if (mBluetoothGatt == null) {
            updateConnectionState(BluetoothReader.STATE_DISCONNECTED);
            return;
        }
        updateConnectionState(BluetoothReader.STATE_DISCONNECTING);

        mBluetoothGatt.disconnect();

        mBluetoothGatt.close();
    }

    /*
     * Update listener
     */
    private void setListener(BluetoothReader reader) {
        /* Update status change listener */
        if (mBluetoothReader instanceof Acr3901us1Reader) {
            ((Acr3901us1Reader) mBluetoothReader)
                    .setOnBatteryStatusChangeListener(new Acr3901us1Reader.OnBatteryStatusChangeListener() {

                        @Override
                        public void onBatteryStatusChange(
                                BluetoothReader bluetoothReader,
                                final int batteryStatus) {
                            Log.e("AcsBaseDevice ", "mBatteryStatusListener data: " + batteryStatus);
                        }

                    });
        } else if (mBluetoothReader instanceof Acr1255uj1Reader) {
            ((Acr1255uj1Reader) mBluetoothReader)
                    .setOnBatteryLevelChangeListener(new Acr1255uj1Reader.OnBatteryLevelChangeListener() {

                        @Override
                        public void onBatteryLevelChange(
                                BluetoothReader bluetoothReader,
                                final int batteryLevel) {
                            Log.e("AcsBaseDevice ", "mBatteryLevelListener data: " + batteryLevel);
                        }

                    });
        }
        mBluetoothReader
                .setOnCardStatusChangeListener(new BluetoothReader.OnCardStatusChangeListener() {

                    @Override
                    public void onCardStatusChange(
                            BluetoothReader bluetoothReader, final int sta) {
                        Log.e("AcsBaseDevice ", "mCardStatusListener c " + sta);

                        if (sta == BluetoothReader.CARD_STATUS_PRESENT) {
                            Log.d("AcsBaseDevice ", "sta = 2");

                            nowDecoderIndex = 0;

                            onPowerOn();
                        } else if (sta == BluetoothReader.CARD_STATUS_ABSENT) {
                            callBack.onCardRemove();
                        }

                        Log.d("AcsBaseDevice ", getCardStatusString(sta));
                    }

                });

        /* Wait for authentication completed. */
        mBluetoothReader.setOnAuthenticationCompleteListener(new BluetoothReader.OnAuthenticationCompleteListener() {
            @Override
            public void onAuthenticationComplete(BluetoothReader bluetoothReader, int errorCode) {
                if (errorCode == BluetoothReader.ERROR_SUCCESS) {
                    Log.d("AcsBaseDevice ", "Authentication Success!");

                    onPowerOn();
                } else {
                    Log.d("AcsBaseDevice ", "Authentication Failed!");
                }
            }
        });

        /* Wait for receiving ATR string. */
        mBluetoothReader.setOnAtrAvailableListener(new BluetoothReader.OnAtrAvailableListener() {
            @Override
            public void onAtrAvailable(BluetoothReader bluetoothReader, byte[] atr, int errorCode) {
                Log.d("AcsBaseDevice ", "onAtrAvailable");

                if (atr == null) {
                    if (errorCode == BluetoothReader.ERROR_AUTHENTICATION_REQUIRED) {
                        Log.d("AcsBaseDevice ", "onAtrAvailable2");

                        onAuth();
                    }

                    Log.d("AcsBaseDevice ", getErrorString(errorCode));
                } else {
                    Log.d("AcsBaseDevice ", BleACSUtils.toHexString(atr));

                    Log.d("AcsBaseDevice ", "Power On Card");

                    bleDecoder[nowDecoderIndex].onAtrAvailable(bluetoothReader);
                }
            }
        });


        /* Wait for power off response. */
        mBluetoothReader.setOnCardPowerOffCompleteListener(new BluetoothReader.OnCardPowerOffCompleteListener() {
            @Override
            public void onCardPowerOffComplete(BluetoothReader bluetoothReader, int i) {
                Log.d("AcsBaseDevice ", "onCardPowerOffComplete");

                Log.d("AcsBaseDevice ", getErrorString(i));
            }
        });

        /* Wait for response APDU. */
        mBluetoothReader
                .setOnResponseApduAvailableListener(
                        new BluetoothReader.OnResponseApduAvailableListener() {

                            @Override
                            public void onResponseApduAvailable(BluetoothReader bluetoothReader,
                                                                byte[] apdu, int errorCode) {
//                                if(bleDecoder[decoderIndex].decode(apdu) == null)
                                Log.d("AcsBaseDevice ", "onResponseApduAvailable");

                                Log.d("AcsBaseDevice ", getResponseString(
                                        apdu, errorCode));

                                final String decodeStr =
                                        bleDecoder[nowDecoderIndex].decode(bluetoothReader, apdu);

                                if (decodeStr == null) {
                                    Log.d("AcsBaseDevice ", "Read Fail");

                                    nowDecoderIndex = (nowDecoderIndex + 1 >= bleDecoder.length)
                                            ? 0 : nowDecoderIndex + 1;

                                    Log.d("AcsBaseDevice ", "Decoder Index : " + nowDecoderIndex);

                                    if (nowDecoderIndex != 0) {
                                        onPowerOn();
                                    }
                                } else if (decodeStr.equals("")) {
                                    Log.d("AcsBaseDevice ", "Next Step");
                                } else if (decodeStr.startsWith("{")) {
                                    Log.d("AcsBaseDevice ", "startsWith { ");

                                    AndroidSchedulers.mainThread().scheduleDirect(
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    callBack.onGetCardData(decodeStr);
                                                }
                                            }
                                    );
                                } else {
                                    Log.d("AcsBaseDevice ", "Not Catched Situation");
                                }
                            }
                        });

        /* Wait for escape command response. */
        mBluetoothReader.setOnEscapeResponseAvailableListener(new BluetoothReader.OnEscapeResponseAvailableListener() {
            @Override
            public void onEscapeResponseAvailable(BluetoothReader bluetoothReader, byte[] bytes, int i) {
                Log.d("AcsBaseDevice ", getResponseString(bytes, i));
                Log.d("AcsBaseDevice ", "onEscapeResponseAvailable");

            }
        });


//        /* Wait for device info available. */
//        mBluetoothReader
//                .setOnDeviceInfoAvailableListener(new BluetoothReader
//                .OnDeviceInfoAvailableListener() {
//
//                    @Override
//                    public void onDeviceInfoAvailable(
//                            BluetoothReader bluetoothReader, final int infoId,
//                            final Object o, final int status) {
//                       getActivity().runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                if (status != BluetoothGatt.GATT_SUCCESS) {
//                                    Toast.makeText(ReaderActivity.this,
//                                            "Failed to read device info!",
//                                            Toast.LENGTH_SHORT).show();
//                                    return;
//                                }
//                                switch (infoId) {
//                                    case BluetoothReader.DEVICE_INFO_SYSTEM_ID: {
//                                        mTxtSystemId.setText(CustomContextWrapper
//                                                .toHexString((byte[]) o));
//                                    }
//                                    break;
//                                    case BluetoothReader.DEVICE_INFO_MODEL_NUMBER_STRING:
//                                        mTxtModelNo.setText((String) o);
//                                        break;
//                                    case BluetoothReader.DEVICE_INFO_SERIAL_NUMBER_STRING:
//                                        mTxtSerialNo.setText((String) o);
//                                        break;
//                                    case BluetoothReader.DEVICE_INFO_FIRMWARE_REVISION_STRING:
//                                        mTxtFirmwareRev.setText((String) o);
//                                        break;
//                                    case BluetoothReader.DEVICE_INFO_HARDWARE_REVISION_STRING:
//                                        mTxtHardwareRev.setText((String) o);
//                                        break;
//                                    case BluetoothReader.DEVICE_INFO_MANUFACTURER_NAME_STRING:
//                                        mTxtManufacturerName.setText((String) o);
//                                        break;
//                                    default:
//                                        break;
//                                }
//                            }
//                        });
//                    }
//
//                });

        /* Wait for battery level available. */
        if (mBluetoothReader instanceof Acr1255uj1Reader) {
            ((Acr1255uj1Reader) mBluetoothReader)
                    .setOnBatteryLevelAvailableListener(new Acr1255uj1Reader.OnBatteryLevelAvailableListener() {
                        @Override
                        public void onBatteryLevelAvailable(BluetoothReader bluetoothReader, int batteryLevel,
                                                            int status) {
                            Log.e("AcsBaseDevice ",
                                    "mBatteryLevelListener data: " + getBatteryLevelString(batteryLevel));

                        }
                    });
        }

        /* Handle on battery status available. */
        if (mBluetoothReader instanceof Acr3901us1Reader) {
            ((Acr3901us1Reader) mBluetoothReader)
                    .setOnBatteryStatusAvailableListener(new Acr3901us1Reader.OnBatteryStatusAvailableListener() {
                        @Override
                        public void onBatteryStatusAvailable(BluetoothReader bluetoothReader, int i, int i1) {
                            Log.d(
                                    "AcsBaseDevice ", getBatteryStatusString(i));
                        }
                    });
        }

        /* Handle on slot status available. */
        mBluetoothReader
                .setOnCardStatusAvailableListener(new BluetoothReader.OnCardStatusAvailableListener() {
                    @Override
                    public void onCardStatusAvailable(BluetoothReader bluetoothReader, int cardStatus, int errorCode) {
                        Log.d("AcsBaseDevice ", "Card Status : " + cardStatus + " Error Code" + errorCode);
                        if (errorCode != BluetoothReader.ERROR_SUCCESS) {
                            Log.d("AcsBaseDevice ", getErrorString(errorCode));
                        } else {
                            Log.d("AcsBaseDevice ", getCardStatusString(cardStatus));
                        }
                    }
                });


        mBluetoothReader
                .setOnEnableNotificationCompleteListener(new BluetoothReader.OnEnableNotificationCompleteListener() {

                    @Override
                    public void onEnableNotificationComplete(
                            BluetoothReader bluetoothReader, final int result) {
                        if (result != BluetoothGatt.GATT_SUCCESS) {
                            /* Fail */
                            AndroidSchedulers.mainThread().scheduleDirect(new Runnable() {
                                @Override
                                public void run() {
                                    callBack.onGattStatusChanged(result);
                                }
                            });

                            Log.d("AcsBaseDevice ", "The device is unable to set notification!");
                        } else {
                            AndroidSchedulers.mainThread().scheduleDirect(new Runnable() {
                                @Override
                                public void run() {
                                    callBack.onGattStatusChanged(result);
                                }
                            });
                            Log.d("AcsBaseDevice ", "The device is ready to use!");
                        }
                    }

                });
        Log.d("AcsBaseDevice ", "set Listener Over");
    }

    /* Get the Battery status string. */
    private String getBatteryStatusString(int batteryStatus) {
        if (batteryStatus == BluetoothReader.BATTERY_STATUS_NONE) {
            return "No battery.";
        } else if (batteryStatus == BluetoothReader.BATTERY_STATUS_FULL) {
            return "The battery is full.";
        } else if (batteryStatus == BluetoothReader.BATTERY_STATUS_USB_PLUGGED) {
            return "The USB is plugged.";
        }
        return "The battery is low.";
    }

    /* Get the Battery level string. */
    private String getBatteryLevelString(int batteryLevel) {
        if (batteryLevel < 0 || batteryLevel > 100) {
            return "Unknown.";
        }
        return String.valueOf(batteryLevel) + "%";
    }

    /* Get the Card status string. */
    private String getCardStatusString(int cardStatus) {
        if (cardStatus == BluetoothReader.CARD_STATUS_ABSENT) {
            return "Absent.";
        } else if (cardStatus == BluetoothReader.CARD_STATUS_PRESENT) {
            return "Present.";
        } else if (cardStatus == BluetoothReader.CARD_STATUS_POWERED) {
            return "Powered.";
        } else if (cardStatus == BluetoothReader.CARD_STATUS_POWER_SAVING_MODE) {
            return "Power saving mode.";
        }
        return "The card status is unknown.";
    }

    /* Get the Error string. */
    private String getErrorString(int errorCode) {
        if (errorCode == BluetoothReader.ERROR_SUCCESS) {
            return "";
        } else if (errorCode == BluetoothReader.ERROR_INVALID_CHECKSUM) {
            return "The checksum is invalid.";
        } else if (errorCode == BluetoothReader.ERROR_INVALID_DATA_LENGTH) {
            return "The data length is invalid.";
        } else if (errorCode == BluetoothReader.ERROR_INVALID_COMMAND) {
            return "The command is invalid.";
        } else if (errorCode == BluetoothReader.ERROR_UNKNOWN_COMMAND_ID) {
            return "The command ID is unknown.";
        } else if (errorCode == BluetoothReader.ERROR_CARD_OPERATION) {
            return "The card operation failed.";
        } else if (errorCode == BluetoothReader.ERROR_AUTHENTICATION_REQUIRED) {
            return "Authentication is required.";
        } else if (errorCode == BluetoothReader.ERROR_LOW_BATTERY) {
            return "The battery is low.";
        } else if (errorCode == BluetoothReader.ERROR_CHARACTERISTIC_NOT_FOUND) {
            return "Error characteristic is not found.";
        } else if (errorCode == BluetoothReader.ERROR_WRITE_DATA) {
            return "Write command to reader is failed.";
        } else if (errorCode == BluetoothReader.ERROR_TIMEOUT) {
            return "Timeout.";
        } else if (errorCode == BluetoothReader.ERROR_AUTHENTICATION_FAILED) {
            return "Authentication is failed.";
        } else if (errorCode == BluetoothReader.ERROR_UNDEFINED) {
            return "Undefined error.";
        } else if (errorCode == BluetoothReader.ERROR_INVALID_DATA) {
            return "Received data error.";
        } else if (errorCode == BluetoothReader.ERROR_COMMAND_FAILED) {
            return "The command failed.";
        }
        return "Unknown error.";
    }

    /* Get the Response string. */
    private String getResponseString(byte[] response, int errorCode) {
        if (errorCode == BluetoothReader.ERROR_SUCCESS) {
            if (response != null && response.length > 0) {
//                switch (mType) {
//                    case 1:
//                        break;
//                    default:
//                        return CustomContextWrapper.toHexString(response);
//                }
                return BleACSUtils.toHexString(response);
            }
            return "";
        }
        return getErrorString(errorCode);
    }

    /* Start the process to enable the reader's notifications. */
    private void activateReader(BluetoothReader reader) {
        if (reader == null) {
            return;
        }

        if (reader instanceof Acr3901us1Reader) {
            /* Start pairing to the reader. */
            ((Acr3901us1Reader) mBluetoothReader).startBonding();
        } else if (mBluetoothReader instanceof Acr1255uj1Reader) {
            /* Enable notification. */
            mBluetoothReader.enableNotification(true);
        }
    }

    private void onPowerOn() {
        if (mBluetoothReader == null) {
            Log.d("AcsBaseDevice ", "card_reader_not_ready");

            return;
        }
        if (!mBluetoothReader.powerOnCard()) {
            Log.d("AcsBaseDevice ", "card_reader_not_ready");
        }
    }

    private void onAuth() {
        Log.d("AcsBaseDevice ", "onAuth");

        if (mBluetoothReader == null) {
            Log.d("AcsBaseDevice ", "card_reader_not_ready");
            return;
        }

        /* Retrieve master key from edit box. */
//        EditText editReadInput = new EditText(getContext());
//        editReadInput.setText("FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF");
        byte[] masterKey = BleACSUtils.getStringinHexBytes("FF FF FF FF FF FF FF FF FF FF FF FF " +
                "FF FF FF FF");

        if (masterKey != null && masterKey.length > 0) {
            /* Clear response field for the result of authentication. */
            Log.d("AcsBaseDevice ", "noData");

            Log.d("AcsBaseDevice ", "master key = " + new String(masterKey));

            /* Start authentication. */
            if (!mBluetoothReader.authenticate(masterKey)) {
                Log.d("AcsBaseDevice ", "card_reader_not_ready");
            } else {
                Log.d("AcsBaseDevice ", "Authenticating...");
            }
        } else {
            Log.d("AcsBaseDevice ", "Character format error!");
        }
    }

    private void onAPDU1() {
        Log.d("AcsBaseDevice ", "onAPDU1");

        /* Check for detected reader. */
        if (mBluetoothReader == null) {
            Log.d("AcsBaseDevice ", "card_reader_not_ready");
            return;
        }
        byte[] apduCommand = new byte[]{(byte) 0x00,
                (byte) 0xA4, (byte) 0x04, (byte) 0x00, (byte) 0x10,
                (byte) 0xD1, (byte) 0x58, (byte) 0x00, (byte) 0x00,
                (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x11, (byte) 0x00};
        if (apduCommand != null && apduCommand.length > 0) {
            /* Clear response field for result of APDU. */
            Log.d("AcsBaseDevice ", "noData");

            /* Transmit APDU command. */
            if (!mBluetoothReader.transmitApdu(apduCommand)) {
                Log.d("AcsBaseDevice ", "card_reader_not_ready");
            }
        } else {
            Log.d("AcsBaseDevice ", "Character format error!");
        }
    }

    private void onAPDU2() {
        Log.d("AcsBaseDevice ", "onAPDU2");

        /* Check for detected reader. */
        if (mBluetoothReader == null) {
            Log.d("AcsBaseDevice ", "card_reader_not_ready");
            return;
        }
        byte[] apduCommand = new byte[]{(byte) 0x00, (byte) 0xca, (byte) 0x11,
                (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x00};
        if (apduCommand != null && apduCommand.length > 0) {
            /* Clear response field for result of APDU. */
            Log.d("AcsBaseDevice ", "noData");

            /* Transmit APDU command. */
            if (!mBluetoothReader.transmitApdu(apduCommand)) {
                Log.d("AcsBaseDevice ", "card_reader_not_ready");
            }
        } else {
            Log.d("AcsBaseDevice ", "Character format error!");
        }
    }

    public BluetoothReader getmBluetoothReader() {
        return mBluetoothReader;
    }
}
