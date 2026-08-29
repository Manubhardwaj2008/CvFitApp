package com.manu.bsw017

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.manu.bsw017.MoyoungConstants.UUID_CHARACTERISTIC_BATTERY_LEVEL
import com.manu.bsw017.MoyoungConstants.UUID_CHARACTERISTIC_BODY_SENSOR_LOCATION
import com.manu.bsw017.MoyoungConstants.UUID_CHARACTERISTIC_HR_MEASUREMENT
import com.manu.bsw017.MoyoungConstants.UUID_CHARACTERISTIC_STEPS
import com.manu.bsw017.MoyoungConstants.UUID_DESCRIPTOR_CCCD
import com.manu.bsw017.MoyoungConstants.UUID_SERVICE_BATTERY
import com.manu.bsw017.MoyoungConstants.UUID_SERVICE_HEART_RATE
import com.manu.bsw017.MoyoungConstants.UUID_SERVICE_MOYOUNG
import com.manu.bsw017.network.HealthSample
import com.manu.bsw017.network.RetrofitClient
import kotlinx.coroutines.launch
import java.io.IOException

private const val TARGET_NAME = "BSW017"
private const val TARGET_MAC = "F8:0F:75:E0:06:AE"

class HealthMonitorViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        manager.adapter
    }
    private val scanner: BluetoothLeScanner? by lazy { bluetoothAdapter?.bluetoothLeScanner }
    private var gatt: BluetoothGatt? = null
    private var scanCallback: ScanCallback? = null

    val devices = mutableStateListOf<String>()
    val services = mutableStateListOf<String>()
    var status by mutableStateOf("Ready")
    var heartRate by mutableStateOf<Int?>(null)
    var battery by mutableStateOf<Int?>(null)
    var bodyLocation by mutableStateOf("Unknown")
    var steps by mutableIntStateOf(0)
    var calories by mutableIntStateOf(0)
    var distance by mutableIntStateOf(0)
    var feeaCharacteristicCount by mutableIntStateOf(0)
    var syncStatus by mutableStateOf("Not synced")
    var isSyncing by mutableStateOf(false)

    fun ensurePermissionsAndScan(context: Context, requestPermissions: (Array<String>) -> Unit) {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            status = "Turn Bluetooth on first"
            return
        }

        val missing = requiredPermissions().filter {
            context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            requestPermissions(missing.toTypedArray())
        } else {
            startScan()
        }
    }

    private fun requiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        val sc = scanner
        if (sc == null) {
            status = "BLE Scanner not available"
            return
        }
        scanCallback?.let { sc.stopScan(it) }
        devices.clear()
        services.clear()
        feeaCharacteristicCount = 0
        status = "Scanning for BSW017..."

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val name = result.scanRecord?.deviceName ?: device.name ?: "Unnamed"
                val isTarget = name.equals(TARGET_NAME, ignoreCase = true) ||
                    device.address.equals(TARGET_MAC, ignoreCase = true)

                if (isTarget) {
                    val display = "$name  ${device.address}"
                    if (!devices.contains(display)) devices.add(display)
                    connect(device)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                status = "BLE scan failed: $errorCode"
            }
        }

        sc.startScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    private fun connect(device: BluetoothDevice) {
        val sc = scanner
        scanCallback?.let { sc?.stopScan(it) }
        gatt?.close()
        gatt = null
        status = "Connecting to ${device.address}..."
        gatt = device.connectGatt(getApplication(), false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    private val gattCallback = object : BluetoothGattCallback() {

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, statusCode: Int, newState: Int) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                status = "Connected — discovering services"
                gatt.discoverServices()
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                status = "Disconnected"
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, statusCode: Int) {
            if (statusCode != BluetoothGatt.GATT_SUCCESS) {
                status = "Service discovery failed: $statusCode"
                return
            }

            services.clear()
            feeaCharacteristicCount = 0

            for (service in gatt.services) {
                services.add("Service: ${service.uuid}")
                for (characteristic in service.characteristics) {
                    services.add("  Characteristic: ${characteristic.uuid} [${properties(characteristic)}]")
                }

                if (service.uuid == UUID_SERVICE_MOYOUNG) {
                    feeaCharacteristicCount = service.characteristics.size
                }
            }

            status = "Services discovered"

            gatt.getService(UUID_SERVICE_HEART_RATE)
                ?.getCharacteristic(UUID_CHARACTERISTIC_HR_MEASUREMENT)
                ?.let { enableNotifications(gatt, it) }

            gatt.getService(UUID_SERVICE_MOYOUNG)
                ?.getCharacteristic(UUID_CHARACTERISTIC_STEPS)
                ?.let { enableNotifications(gatt, it) }

            gatt.getService(UUID_SERVICE_BATTERY)
                ?.getCharacteristic(UUID_CHARACTERISTIC_BATTERY_LEVEL)
                ?.let { gatt.readCharacteristic(it) }

            gatt.getService(UUID_SERVICE_HEART_RATE)
                ?.getCharacteristic(UUID_CHARACTERISTIC_BODY_SENSOR_LOCATION)
                ?.let { gatt.readCharacteristic(it) }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleCharacteristic(characteristic, value)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            statusCode: Int
        ) {
            if (statusCode == BluetoothGatt.GATT_SUCCESS) {
                handleCharacteristic(characteristic, value)
            }
        }
    }

    private fun handleCharacteristic(characteristic: BluetoothGattCharacteristic, value: ByteArray) {
        when (characteristic.uuid) {
            UUID_CHARACTERISTIC_HR_MEASUREMENT -> heartRate = parseHeartRate(value)
            UUID_CHARACTERISTIC_BATTERY_LEVEL -> if (value.isNotEmpty()) battery = value[0].toInt() and 0xFF
            UUID_CHARACTERISTIC_BODY_SENSOR_LOCATION -> if (value.isNotEmpty()) {
                bodyLocation = bodyLocationName(value[0].toInt() and 0xFF)
            }
            UUID_CHARACTERISTIC_STEPS -> {
                if (value.size >= 9) {
                    distance = (value[0].toInt() and 0xFF) or ((value[1].toInt() and 0xFF) shl 8) or ((value[2].toInt() and 0xFF) shl 16)
                    steps = (value[3].toInt() and 0xFF) or ((value[4].toInt() and 0xFF) shl 8) or ((value[5].toInt() and 0xFF) shl 16)
                    calories = (value[6].toInt() and 0xFF) or ((value[7].toInt() and 0xFF) shl 8) or ((value[8].toInt() and 0xFF) shl 16)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)
        characteristic.getDescriptor(UUID_DESCRIPTOR_CCCD)?.let { descriptor ->
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        }
    }

    private fun parseHeartRate(value: ByteArray): Int? {
        if (value.isEmpty()) return null
        val flags = value[0].toInt() and 0xFF
        return if ((flags and 0x01) == 0) {
            value.getOrNull(1)?.toInt()?.and(0xFF)
        } else if (value.size >= 3) {
            ((value[2].toInt() and 0xFF) shl 8) or (value[1].toInt() and 0xFF)
        } else {
            null
        }
    }

    private fun properties(characteristic: BluetoothGattCharacteristic): String {
        val p = characteristic.properties
        return buildList {
            if (p and BluetoothGattCharacteristic.PROPERTY_READ != 0) add("READ")
            if (p and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) add("WRITE")
            if (p and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) add("WRITE_NO_RESPONSE")
            if (p and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) add("NOTIFY")
            if (p and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) add("INDICATE")
        }.joinToString(", ")
    }

    private fun bodyLocationName(value: Int): String = when (value) {
        0 -> "Other"
        1 -> "Chest"
        2 -> "Wrist"
        3 -> "Finger"
        4 -> "Hand"
        5 -> "Ear Lobe"
        6 -> "Foot"
        else -> "Unknown ($value)"
    }

    fun syncToBackend() {
        if (isSyncing) return

        if (heartRate == null && battery == null) {
            syncStatus = "Nothing to sync yet — connect and wait for a reading"
            return
        }

        isSyncing = true
        syncStatus = "Syncing..."

        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.postSample(
                    HealthSample(
                        device_id = TARGET_NAME,
                        heart_rate = heartRate,
                        battery = battery,
                        steps = steps,
                        calories = calories,
                        distance = distance
                    )
                )

                syncStatus = if (response.isSuccessful) {
                    "Synced OK (${response.body()?.sample?.timestamp ?: "server time"})"
                } else {
                    "Sync failed: HTTP ${response.code()}"
                }
            } catch (e: IOException) {
                syncStatus = "Sync failed: can't reach backend (${e.message})"
            } catch (e: Exception) {
                syncStatus = "Sync failed: ${e.message}"
            } finally {
                isSyncing = false
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        status = "Disconnected"
    }

    override fun onCleared() {
        disconnect()
        super.onCleared()
    }
}
