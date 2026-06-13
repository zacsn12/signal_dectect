package org.zacsn.signal_dectect.data.scanner;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Build;
import android.util.Log;
import android.util.SparseArray;
import androidx.core.app.ActivityCompat;

import org.zacsn.signal_dectect.domain.model.DeviceType;
import org.zacsn.signal_dectect.domain.model.ManufacturerVerdict;
import org.zacsn.signal_dectect.domain.model.SignalDevice;
import org.zacsn.signal_dectect.util.DistanceUtils;
import org.zacsn.signal_dectect.util.MacVendorUtils;
import org.zacsn.signal_dectect.util.BluetoothManufacturerUtils;

import java.util.ArrayDeque;
import java.util.Locale;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Controller for Bluetooth (Classic and BLE) device scanning.
 */
@Singleton
public class BluetoothScanController {
    private static final String TAG = "BluetoothScanController";
    private static final long CLASSIC_DISCOVERY_RESTART_DELAY_MS = 1_000L;
    private static final long GATT_PROBE_TIMEOUT_MS = 10_000L;
    private static final int MAX_CONCURRENT_GATT_PROBES = 2;
    private static final int MIN_GATT_PROBE_RSSI_DBM = -85;
    private static final int DEFAULT_BLUETOOTH_TX_POWER_DBM = -59;
    private static final int MIN_REASONABLE_TX_POWER_DBM = -80;
    private static final int MAX_REASONABLE_TX_POWER_DBM = -35;
    private static final double BLUETOOTH_PATH_LOSS_EXPONENT = 2.5;
    private static final UUID DEVICE_INFORMATION_SERVICE_UUID =
        UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    private static final UUID MANUFACTURER_NAME_UUID =
        UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");
    private static final UUID MODEL_NUMBER_UUID =
        UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb");
    private static final UUID PNP_ID_UUID =
        UUID.fromString("00002a50-0000-1000-8000-00805f9b34fb");
    
    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private final ExternalRadioAdapterManager externalAdapterManager;
    private final ConcurrentHashMap<String, SignalDevice> deviceMap;
    private final java.util.Set<String> gattProbeStarted = ConcurrentHashMap.newKeySet();
    private final Queue<GattProbeRequest> pendingGattProbes = new ConcurrentLinkedQueue<>();
    private final java.util.Set<String> queuedGattProbes = ConcurrentHashMap.newKeySet();
    private final java.util.Set<BluetoothGatt> activeGatts = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<BluetoothGatt, GattProbeSession> gattProbeSessions = new ConcurrentHashMap<>();
    private final Handler handler;
    
    private BluetoothLeScanner bleScanner;
    private ScanCallback bleScanCallback;
    private BroadcastReceiver classicReceiver;
    private boolean isScanning = false;
    private ScanListener scanListener;
    
    public interface ScanListener {
        void onDeviceFound(SignalDevice device);
        void onScanError(String error);
    }

    
    @Inject
    public BluetoothScanController(
            @ApplicationContext Context context,
            ExternalRadioAdapterManager externalAdapterManager
    ) {
        this.context = context;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.externalAdapterManager = externalAdapterManager;
        this.deviceMap = new ConcurrentHashMap<>();
        this.handler = new Handler(Looper.getMainLooper());
    }
    
    public void setScanListener(ScanListener listener) {
        this.scanListener = listener;
    }
    
    /**
     * Start Bluetooth scanning (both BLE and Classic).
     */
    public void startScan() {
        boolean frameworkAvailable = bluetoothAdapter != null && bluetoothAdapter.isEnabled();
        ScanSourceSelection scanSource = externalAdapterManager.selectBluetoothSource(frameworkAvailable);
        Log.i(TAG, scanSource.getMessage());

        if (!scanSource.canUseAndroidFramework()) {
            if (scanListener != null) {
                scanListener.onScanError(scanSource.getMessage());
            }
            return;
        }
        
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) 
                != PackageManager.PERMISSION_GRANTED) {
            if (scanListener != null) {
                scanListener.onScanError("Bluetooth scan permission not granted");
            }
            return;
        }
        
        isScanning = true;
        deviceMap.clear();
        gattProbeStarted.clear();
        
        startBleScan();
        startClassicScan();
    }

    
    /**
     * Start BLE scanning.
     */
    private void startBleScan() {
        bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bleScanner == null) return;
        
        bleScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                if (ActivityCompat.checkSelfPermission(context, 
                        Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                handleBleScanResult(result);
            }

            @Override
            public void onBatchScanResults(java.util.List<ScanResult> results) {
                if (ActivityCompat.checkSelfPermission(context,
                        Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                if (results == null) {
                    return;
                }
                for (ScanResult result : results) {
                    handleBleScanResult(result);
                }
            }
            
            @Override
            public void onScanFailed(int errorCode) {
                if (scanListener != null) {
                    scanListener.onScanError("BLE scan failed with error code: " + errorCode);
                }
            }
        };
        
        try {
            ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
                .setReportDelay(0)
                .build();
            bleScanner.startScan(null, settings, bleScanCallback);
        } catch (SecurityException e) {
            if (scanListener != null) {
                scanListener.onScanError("BLE scan permission error: " + e.getMessage());
            }
        }
    }

    private void handleBleScanResult(ScanResult result) {
        if (result == null) {
            return;
        }

        BluetoothDevice device = result.getDevice();
        if (device == null) {
            return;
        }

        String macAddress = device.getAddress();
        if (macAddress == null || macAddress.isEmpty()) {
            return;
        }
        String deviceKey = macAddress.toUpperCase(Locale.US);
        SignalDevice previous = deviceMap.get(deviceKey);
        int rssi = SignalDeviceStabilizer.smoothSignalStrength(previous, result.getRssi());

        ScanRecord scanRecord = result.getScanRecord();

        int txPower = DEFAULT_BLUETOOTH_TX_POWER_DBM;
        if (scanRecord != null && scanRecord.getTxPowerLevel() != Integer.MIN_VALUE) {
            txPower = normalizeTxPower(scanRecord.getTxPowerLevel());
        }

        String deviceName = scanRecord != null ? scanRecord.getDeviceName() : null;
        if (!isUsefulDeviceName(deviceName)) {
            deviceName = getBluetoothDeviceName(device);
        }
        ManufacturerEvidence manufacturerEvidence = evaluatePassiveManufacturer(
            deviceKey,
            scanRecord,
            deviceName,
            DeviceType.BLUETOOTH_LE
        );
        deviceName = resolveBluetoothDeviceName(
            deviceName,
            manufacturerEvidence.manufacturer,
            DeviceType.BLUETOOTH_LE
        );

        long now = System.currentTimeMillis();
        SignalDevice signalDevice = new SignalDevice(
            deviceKey,
            deviceName,
            DeviceType.BLUETOOTH_LE,
            "未确认",
            manufacturerEvidence.manufacturer,
            manufacturerEvidence.source,
            manufacturerEvidence.confidence,
            manufacturerEvidence.verdict,
            manufacturerEvidence.evidence,
            rssi,
            null,
            calculateDistance(rssi, txPower),
            now,
            now,
            false,
            false,
            false
        );
        signalDevice = SignalDeviceStabilizer.merge(previous, signalDevice);

        deviceMap.put(deviceKey, signalDevice);
        if (scanListener != null) {
            scanListener.onDeviceFound(signalDevice);
        }
        enqueueGattManufacturerProbe(device, deviceKey, rssi);
    }

    
    /**
     * Start Classic Bluetooth discovery.
     */
    private void startClassicScan() {
        classicReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    if (ActivityCompat.checkSelfPermission(context, 
                            Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    short rssiShort = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                    int rssi = (int) rssiShort;
                    
                    if (device != null) {
                        String macAddress = device.getAddress();
                        if (macAddress == null || macAddress.isEmpty()) {
                            return;
                        }
                        String deviceKey = macAddress.toUpperCase(Locale.US);
                        SignalDevice previous = deviceMap.get(deviceKey);
                        if (rssiShort == Short.MIN_VALUE) {
                            rssi = previous != null ? previous.getSignalStrength() : -100;
                        } else {
                            rssi = SignalDeviceStabilizer.smoothSignalStrength(previous, rssi);
                        }
                        
                        String deviceName = getBluetoothDeviceName(device);
                        ManufacturerEvidence manufacturerEvidence = evaluatePassiveManufacturer(
                            deviceKey,
                            null,
                            deviceName,
                            DeviceType.BLUETOOTH_CLASSIC
                        );
                        deviceName = resolveBluetoothDeviceName(
                            deviceName,
                            manufacturerEvidence.manufacturer,
                            DeviceType.BLUETOOTH_CLASSIC
                        );
                        
                        long now = System.currentTimeMillis();
                        SignalDevice signalDevice = new SignalDevice(
                            deviceKey,
                            deviceName,
                            DeviceType.BLUETOOTH_CLASSIC,
                            "未确认",
                            manufacturerEvidence.manufacturer,
                            manufacturerEvidence.source,
                            manufacturerEvidence.confidence,
                            manufacturerEvidence.verdict,
                            manufacturerEvidence.evidence,
                            rssi,
                            null,
                            calculateDistance(rssi, DEFAULT_BLUETOOTH_TX_POWER_DBM),
                            now,
                            now,
                            false,
                            false,
                            false
                        );
                        signalDevice = SignalDeviceStabilizer.merge(previous, signalDevice);
                        
                        deviceMap.put(deviceKey, signalDevice);
                        if (scanListener != null) {
                            scanListener.onDeviceFound(signalDevice);
                        }
                    }
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action) && isScanning) {
                    handler.postDelayed(() -> startClassicDiscovery(), CLASSIC_DISCOVERY_RESTART_DELAY_MS);
                }
            }
        };
        
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        context.registerReceiver(classicReceiver, filter);

        startClassicDiscovery();
    }

    private void startClassicDiscovery() {
        try {
            if (!isScanning || bluetoothAdapter == null) {
                return;
            }
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            bluetoothAdapter.startDiscovery();
        } catch (SecurityException e) {
            if (scanListener != null) {
                scanListener.onScanError("Classic Bluetooth scan permission error: " + e.getMessage());
            }
        }
    }

    private void enqueueGattManufacturerProbe(BluetoothDevice device, String deviceKey, int rssi) {
        if (!isScanning || device == null || deviceKey == null
                || rssi < MIN_GATT_PROBE_RSSI_DBM
                || gattProbeStarted.contains(deviceKey)
                || !queuedGattProbes.add(deviceKey)) {
            return;
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            queuedGattProbes.remove(deviceKey);
            return;
        }

        pendingGattProbes.offer(new GattProbeRequest(device, deviceKey));
        drainGattProbeQueue();
    }

    private void drainGattProbeQueue() {
        if (!isScanning || activeGatts.size() >= MAX_CONCURRENT_GATT_PROBES) {
            return;
        }

        GattProbeRequest request;
        while (activeGatts.size() < MAX_CONCURRENT_GATT_PROBES
                && (request = pendingGattProbes.poll()) != null) {
            queuedGattProbes.remove(request.deviceKey);
            if (gattProbeStarted.add(request.deviceKey)) {
                startGattManufacturerProbe(request.device, request.deviceKey);
            }
        }
    }

    private void startGattManufacturerProbe(BluetoothDevice device, String deviceKey) {
        if (!isScanning || device == null || deviceKey == null) {
            return;
        }

        try {
            BluetoothGattCallback callback = new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                        try {
                            gatt.discoverServices();
                        } catch (SecurityException e) {
                            closeGatt(gatt);
                        }
                    } else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                        closeGatt(gatt);
                    }
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        closeGatt(gatt);
                        return;
                    }

                    android.bluetooth.BluetoothGattService service =
                        gatt.getService(DEVICE_INFORMATION_SERVICE_UUID);
                    if (service == null) {
                        closeGatt(gatt);
                        return;
                    }

                    GattProbeSession session = new GattProbeSession(deviceKey);
                    addReadableCharacteristic(session, service.getCharacteristic(MANUFACTURER_NAME_UUID));
                    addReadableCharacteristic(session, service.getCharacteristic(MODEL_NUMBER_UUID));
                    addReadableCharacteristic(session, service.getCharacteristic(PNP_ID_UUID));

                    if (session.pendingReads.isEmpty()) {
                        closeGatt(gatt);
                        return;
                    }

                    gattProbeSessions.put(gatt, session);
                    if (!readNextGattCharacteristic(gatt, session)) {
                        closeGatt(gatt);
                    }
                }

                @Override
                public void onCharacteristicRead(
                        BluetoothGatt gatt,
                        BluetoothGattCharacteristic characteristic,
                        int status
                ) {
                    byte[] value = null;
                    if (status == BluetoothGatt.GATT_SUCCESS && characteristic != null) {
                        value = characteristic.getValue();
                    }
                    handleGattCharacteristicRead(gatt, characteristic, value, status);
                }

                @Override
                public void onCharacteristicRead(
                        BluetoothGatt gatt,
                        BluetoothGattCharacteristic characteristic,
                        byte[] value,
                        int status
                ) {
                    handleGattCharacteristicRead(gatt, characteristic, value, status);
                }
            };

            BluetoothGatt gatt;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE);
            } else {
                gatt = device.connectGatt(context, false, callback);
            }
            if (gatt != null) {
                activeGatts.add(gatt);
                handler.postDelayed(() -> closeGatt(gatt), GATT_PROBE_TIMEOUT_MS);
            } else {
                gattProbeStarted.remove(deviceKey);
                drainGattProbeQueue();
            }
        } catch (SecurityException e) {
            gattProbeStarted.remove(deviceKey);
            drainGattProbeQueue();
        }
    }

    private void addReadableCharacteristic(
            GattProbeSession session,
            BluetoothGattCharacteristic characteristic
    ) {
        if (session == null || characteristic == null) {
            return;
        }
        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) == 0) {
            return;
        }
        session.pendingReads.offer(characteristic);
    }

    private boolean readNextGattCharacteristic(BluetoothGatt gatt, GattProbeSession session) {
        if (gatt == null || session == null) {
            return false;
        }

        BluetoothGattCharacteristic next = session.pendingReads.poll();
        if (next == null) {
            finishGattManufacturerProbe(session);
            return false;
        }

        try {
            return gatt.readCharacteristic(next);
        } catch (SecurityException e) {
            return false;
        }
    }

    private void handleGattCharacteristicRead(
            BluetoothGatt gatt,
            BluetoothGattCharacteristic characteristic,
            byte[] value,
            int status
    ) {
        GattProbeSession session = gattProbeSessions.get(gatt);
        if (session == null) {
            closeGatt(gatt);
            return;
        }

        if (status == BluetoothGatt.GATT_SUCCESS && characteristic != null && value != null) {
            UUID uuid = characteristic.getUuid();
            if (MANUFACTURER_NAME_UUID.equals(uuid)) {
                session.manufacturerName = readGattString(value);
            } else if (MODEL_NUMBER_UUID.equals(uuid)) {
                session.modelNumber = readGattString(value);
            } else if (PNP_ID_UUID.equals(uuid)) {
                session.pnpId = value.clone();
            }
        }

        if (!readNextGattCharacteristic(gatt, session)) {
            closeGatt(gatt);
        }
    }

    private void finishGattManufacturerProbe(GattProbeSession session) {
        if (session == null) {
            return;
        }
        if (session.finished) {
            return;
        }
        session.finished = true;

        SignalDevice current = deviceMap.get(session.deviceKey);
        if (current == null) {
            return;
        }

        ManufacturerEvidence evidence = evaluateGattManufacturer(
                current,
                session.manufacturerName,
                session.modelNumber,
                session.pnpId
        );
        if (evidence.verdict == ManufacturerVerdict.UNKNOWN || !isKnownManufacturer(evidence.manufacturer)) {
            return;
        }

        SignalDevice confirmed = new SignalDevice(
            current.getMacAddress(),
            shouldReplaceNameWithConfirmedManufacturer(current.getDeviceName(), current.getCandidateManufacturer())
                    ? evidence.manufacturer
                    : current.getDeviceName(),
            current.getDeviceType(),
            evidence.verdict == ManufacturerVerdict.CONFIRMED ? evidence.manufacturer : "未确认",
            evidence.manufacturer,
            evidence.source,
            evidence.confidence,
            evidence.verdict,
            evidence.evidence,
            current.getSignalStrength(),
            current.getFrequency(),
            current.getDistance(),
            current.getFirstSeen(),
            System.currentTimeMillis(),
            current.isFocused(),
            current.isBlacklisted(),
            current.isWhitelisted()
        );

        deviceMap.put(session.deviceKey, confirmed);
        if (scanListener != null) {
            scanListener.onDeviceFound(confirmed);
        }
    }

    private String readGattString(byte[] value) {
        if (value == null || value.length == 0) {
            return null;
        }
        return new String(value, java.nio.charset.StandardCharsets.UTF_8).trim();
    }

    private void closeGatt(BluetoothGatt gatt) {
        if (gatt == null) {
            return;
        }

        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED) {
                gatt.disconnect();
            }
        } catch (Exception e) {
            // Ignore cleanup failures.
        }

        try {
            gatt.close();
        } catch (Exception e) {
            // Ignore cleanup failures.
        }
        GattProbeSession session = gattProbeSessions.remove(gatt);
        finishGattManufacturerProbe(session);
        activeGatts.remove(gatt);
        drainGattProbeQueue();
    }

    
    /**
     * Stop Bluetooth scanning.
     */
    public void stopScan() {
        if (!isScanning) return;
        
        isScanning = false;
        for (BluetoothGatt gatt : new java.util.ArrayList<>(activeGatts)) {
            closeGatt(gatt);
        }
        pendingGattProbes.clear();
        queuedGattProbes.clear();
        handler.removeCallbacksAndMessages(null);
        
        // Stop BLE scan
        if (bleScanner != null && bleScanCallback != null) {
            try {
                if (ActivityCompat.checkSelfPermission(context, 
                        Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                    bleScanner.stopScan(bleScanCallback);
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        
        // Stop Classic Bluetooth discovery
        if (bluetoothAdapter != null) {
            try {
                if (ActivityCompat.checkSelfPermission(context, 
                        Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                    bluetoothAdapter.cancelDiscovery();
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        
        // Unregister receiver
        if (classicReceiver != null) {
            try {
                context.unregisterReceiver(classicReceiver);
            } catch (Exception e) {
                // Ignore if not registered
            }
            classicReceiver = null;
        }
    }
    
    /**
     * Calculate an approximate Bluetooth distance from RSSI.
     *
     * BLE advertisements can expose unrealistic TxPower values. Feeding those
     * directly into an exponential model can produce absurd distances, so the
     * value is normalized before this method is called and the result is capped
     * to the app's reliable display range.
     */
    private double calculateDistance(int rssi, int txPower) {
        if (rssi >= 0 || rssi < -120) {
            return DistanceUtils.UNKNOWN_DISTANCE;
        }

        double distance = Math.pow(
            10.0,
            (normalizeTxPower(txPower) - rssi) / (10.0 * BLUETOOTH_PATH_LOSS_EXPONENT)
        );
        return DistanceUtils.sanitize(distance);
    }

    private int normalizeTxPower(int txPower) {
        if (txPower < MIN_REASONABLE_TX_POWER_DBM || txPower > MAX_REASONABLE_TX_POWER_DBM) {
            return DEFAULT_BLUETOOTH_TX_POWER_DBM;
        }
        return txPower;
    }

    private String resolveBluetoothDeviceName(
            String broadcastName,
            String candidateManufacturer,
            DeviceType deviceType
    ) {
        if (isUsefulDeviceName(broadcastName)) {
            return broadcastName.trim();
        }
        if (isKnownManufacturer(candidateManufacturer)) {
            return candidateManufacturer.trim();
        }
        return deviceType == DeviceType.BLUETOOTH_LE ? "未知BLE设备" : "未知经典蓝牙设备";
    }

    private String getBluetoothDeviceName(BluetoothDevice device) {
        if (device == null) {
            return null;
        }
        try {
            return device.getName();
        } catch (SecurityException e) {
            return null;
        }
    }

    private boolean shouldReplaceNameWithConfirmedManufacturer(String deviceName, String candidateManufacturer) {
        if (!isUsefulDeviceName(deviceName)) {
            return true;
        }
        if (isUnknownBluetoothName(deviceName)) {
            return true;
        }
        return isKnownManufacturer(candidateManufacturer)
                && deviceName.trim().equalsIgnoreCase(candidateManufacturer.trim());
    }

    private boolean isUsefulDeviceName(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        return !isUnknownBluetoothName(value);
    }

    private boolean isUnknownBluetoothName(String value) {
        if (value == null) {
            return true;
        }
        String normalized = value.trim().toLowerCase(Locale.US);
        return normalized.equals("unknown")
            || normalized.equals("unknown device")
            || normalized.equals("未知")
            || normalized.equals("未知ble设备")
            || normalized.equals("未知经典蓝牙设备");
    }
    
    public boolean isScanning() {
        return isScanning;
    }

    private boolean isKnownManufacturer(String manufacturer) {
        return manufacturer != null
            && !manufacturer.equals("未知")
            && !manufacturer.equals("未知厂商")
            && !manufacturer.equals("随机地址");
    }
    
    public ConcurrentHashMap<String, SignalDevice> getDeviceMap() {
        return deviceMap;
    }

    private ManufacturerEvidence evaluatePassiveManufacturer(
            String macAddress,
            ScanRecord scanRecord,
            String deviceName,
            DeviceType deviceType
    ) {
        ManufacturerEvidence best = ManufacturerEvidence.unknown();

        String macManufacturer = MacVendorUtils.getVendor(macAddress);
        if (isKnownManufacturer(macManufacturer)) {
            boolean isClassicBluetooth = deviceType == DeviceType.BLUETOOTH_CLASSIC;
            int confidence = isClassicBluetooth ? 80 : 45;
            String source = isClassicBluetooth ? "classic_mac_oui" : "ble_mac_oui";
            ManufacturerEvidence macEvidence = isClassicBluetooth
                    ? ManufacturerEvidence.likely(
                            macManufacturer,
                            source,
                            confidence,
                            "经典蓝牙MAC OUI命中: " + macManufacturer
                    )
                    : ManufacturerEvidence.possible(
                    macManufacturer,
                    source,
                    confidence,
                    "MAC OUI命中: " + macManufacturer
            );
            best = best.better(macEvidence);
        }

        if (scanRecord != null) {
            SparseArray<byte[]> manufacturerData = scanRecord.getManufacturerSpecificData();
            if (manufacturerData != null && manufacturerData.size() > 0) {
                for (int i = 0; i < manufacturerData.size(); i++) {
                    int companyId = manufacturerData.keyAt(i);
                    byte[] data = manufacturerData.valueAt(i);
                    String btManufacturer = BluetoothManufacturerUtils.getReliableManufacturer(companyId, data);
                    if (btManufacturer != null) {
                        best = best.better(ManufacturerEvidence.possible(
                                btManufacturer,
                                "ble_company_id",
                                60,
                                "BLE广播Company ID " + BluetoothManufacturerUtils.getCompanyIdHex(companyId)
                                        + ": " + btManufacturer
                        ));
                    }
                }
            }
        }

        String nameManufacturer = BluetoothManufacturerUtils.inferManufacturerFromName(deviceName);
        if (nameManufacturer != null) {
            int confidence = deviceType == DeviceType.BLUETOOTH_CLASSIC ? 75 : 70;
            String source = "device_name";
            String evidence = "设备名称特征: " + deviceName;
            if (deviceType == DeviceType.BLUETOOTH_CLASSIC
                    && isKnownManufacturer(macManufacturer)
                    && BluetoothManufacturerUtils.isSameManufacturer(macManufacturer, nameManufacturer)) {
                confidence = 85;
                source = "classic_mac_name_match";
                evidence = "经典蓝牙MAC OUI与设备名称品牌一致: "
                        + macManufacturer + " / " + deviceName;
            }
            best = best.better(ManufacturerEvidence.likely(nameManufacturer, source, confidence, evidence));
        }

        return best;
    }

    private ManufacturerEvidence evaluateGattManufacturer(
            SignalDevice current,
            String manufacturerName,
            String modelNumber,
            byte[] pnpId
    ) {
        ManufacturerEvidence best = ManufacturerEvidence.unknown();
        String evidence = "";
        if (isKnownManufacturer(manufacturerName)) {
            best = ManufacturerEvidence.confirmed(
                    manufacturerName.trim(),
                    "gatt_device_info",
                    96,
                    "GATT设备信息服务 Manufacturer Name: " + manufacturerName.trim()
            );
            evidence = best.evidence;
        }

        PnpId parsedPnp = parsePnpId(pnpId);
        if (parsedPnp != null) {
            String pnpManufacturer = BluetoothManufacturerUtils.getPnpManufacturer(
                    parsedPnp.vendorIdSource,
                    parsedPnp.vendorId
            );
            String pnpEvidence = "GATT PnP ID " + parsedPnp.describe();
            if (isKnownManufacturer(pnpManufacturer)) {
                if (best.verdict == ManufacturerVerdict.CONFIRMED
                        && BluetoothManufacturerUtils.isSameManufacturer(best.manufacturer, pnpManufacturer)) {
                    best = ManufacturerEvidence.confirmed(
                            best.manufacturer,
                            "gatt_device_info+pnp_id",
                            100,
                            evidence + "；" + pnpEvidence + "，厂商一致"
                    );
                } else if (best.verdict == ManufacturerVerdict.UNKNOWN) {
                    best = ManufacturerEvidence.confirmed(
                            pnpManufacturer,
                            "gatt_pnp_id",
                            98,
                            pnpEvidence + ": " + pnpManufacturer
                    );
                } else {
                    best = ManufacturerEvidence.likely(
                            best.manufacturer,
                            "gatt_device_info_conflict",
                            85,
                            evidence + "；" + pnpEvidence + ": " + pnpManufacturer + "，线索不一致"
                    );
                }
            } else if (best.verdict == ManufacturerVerdict.CONFIRMED) {
                best = ManufacturerEvidence.confirmed(
                        best.manufacturer,
                        best.source,
                        best.confidence,
                        evidence + "；" + pnpEvidence + "，VID未收录"
                );
            }
        }

        if (best.verdict == ManufacturerVerdict.UNKNOWN && current != null) {
            return new ManufacturerEvidence(
                    current.getCandidateManufacturer(),
                    current.getManufacturerSource(),
                    current.getManufacturerConfidence(),
                    current.getManufacturerVerdict(),
                    current.getManufacturerEvidence()
            );
        }

        if (isUsefulDeviceName(modelNumber)) {
            best = new ManufacturerEvidence(
                    best.manufacturer,
                    best.source,
                    best.confidence,
                    best.verdict,
                    appendEvidence(best.evidence, "型号: " + modelNumber.trim())
            );
        }
        return best;
    }

    private PnpId parsePnpId(byte[] value) {
        if (value == null || value.length < 7) {
            return null;
        }
        int vendorIdSource = value[0] & 0xFF;
        int vendorId = ((value[2] & 0xFF) << 8) | (value[1] & 0xFF);
        int productId = ((value[4] & 0xFF) << 8) | (value[3] & 0xFF);
        int productVersion = ((value[6] & 0xFF) << 8) | (value[5] & 0xFF);
        return new PnpId(vendorIdSource, vendorId, productId, productVersion);
    }

    private String appendEvidence(String first, String second) {
        if (first == null || first.trim().isEmpty()) {
            return second;
        }
        if (second == null || second.trim().isEmpty()) {
            return first;
        }
        return first + "；" + second;
    }

    private static final class GattProbeRequest {
        private final BluetoothDevice device;
        private final String deviceKey;

        private GattProbeRequest(BluetoothDevice device, String deviceKey) {
            this.device = device;
            this.deviceKey = deviceKey;
        }
    }

    private static final class GattProbeSession {
        private final String deviceKey;
        private final ArrayDeque<BluetoothGattCharacteristic> pendingReads = new ArrayDeque<>();
        private String manufacturerName;
        private String modelNumber;
        private byte[] pnpId;
        private boolean finished;

        private GattProbeSession(String deviceKey) {
            this.deviceKey = deviceKey;
        }
    }

    private static final class ManufacturerEvidence {
        private final String manufacturer;
        private final String source;
        private final int confidence;
        private final ManufacturerVerdict verdict;
        private final String evidence;

        private ManufacturerEvidence(
                String manufacturer,
                String source,
                int confidence,
                ManufacturerVerdict verdict,
                String evidence
        ) {
            this.manufacturer = manufacturer;
            this.source = source;
            this.confidence = confidence;
            this.verdict = verdict;
            this.evidence = evidence;
        }

        private static ManufacturerEvidence unknown() {
            return new ManufacturerEvidence("未确认", "unknown", 0, ManufacturerVerdict.UNKNOWN, "");
        }

        private static ManufacturerEvidence possible(
                String manufacturer,
                String source,
                int confidence,
                String evidence
        ) {
            return new ManufacturerEvidence(manufacturer, source, confidence, ManufacturerVerdict.POSSIBLE, evidence);
        }

        private static ManufacturerEvidence likely(
                String manufacturer,
                String source,
                int confidence,
                String evidence
        ) {
            return new ManufacturerEvidence(manufacturer, source, confidence, ManufacturerVerdict.LIKELY, evidence);
        }

        private static ManufacturerEvidence confirmed(
                String manufacturer,
                String source,
                int confidence,
                String evidence
        ) {
            return new ManufacturerEvidence(manufacturer, source, confidence, ManufacturerVerdict.CONFIRMED, evidence);
        }

        private ManufacturerEvidence better(ManufacturerEvidence other) {
            if (other == null) {
                return this;
            }
            if (other.confidence > confidence) {
                return other;
            }
            return this;
        }
    }

    private static final class PnpId {
        private final int vendorIdSource;
        private final int vendorId;
        private final int productId;
        private final int productVersion;

        private PnpId(int vendorIdSource, int vendorId, int productId, int productVersion) {
            this.vendorIdSource = vendorIdSource;
            this.vendorId = vendorId;
            this.productId = productId;
            this.productVersion = productVersion;
        }

        private String describe() {
            return "VID来源=" + BluetoothManufacturerUtils.getPnpVendorSourceLabel(vendorIdSource)
                    + ", VID=" + BluetoothManufacturerUtils.getCompanyIdHex(vendorId)
                    + ", PID=" + BluetoothManufacturerUtils.getCompanyIdHex(productId)
                    + ", 版本=" + BluetoothManufacturerUtils.getCompanyIdHex(productVersion);
        }
    }
}
