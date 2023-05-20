package com.ds.pulsar

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanCallback.*
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.bluetooth.le.ScanSettings.MATCH_MODE_STICKY
import android.os.ParcelUuid
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow

sealed interface SearchResult{
}

class Failure(val why: String) : SearchResult

data class DevInfo(
    val dev: BluetoothDevice,
) : SearchResult {

}


val btScanErrors = mapOf<Int, String>(
    SCAN_FAILED_ALREADY_STARTED to "Fails to start scan as BLE scan with the same settings is already started by the app.",
    SCAN_FAILED_APPLICATION_REGISTRATION_FAILED to "Fails to start scan as app cannot be registered.",
    SCAN_FAILED_FEATURE_UNSUPPORTED to "Fails to start power optimized scan as this feature is not supported.",
    SCAN_FAILED_INTERNAL_ERROR to "Fails to start scan due an internal error.",
    //TODO: fix it when move to new API lvl
//    SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES to "Fails to start scan as it is out of hardware resources.",
//    SCAN_FAILED_SCANNING_TOO_FREQUENTLY to "Fails to start scan as application tries to scan too frequently.",
)

@SuppressLint("MissingPermission")
fun searchForBleDevices() : Flow<SearchResult> {
    return callbackFlow<SearchResult> {
        val btLeScanner = btAdapter.bluetoothLeScanner
        val scanSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setMatchMode(MATCH_MODE_STICKY).build()
        val filters = listOf( ScanFilter.Builder().setServiceUuid(
            ParcelUuid.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        ).build() )
        val leScanCallback: ScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                val dev = result.device
                trySendBlocking(DevInfo(dev))
            }
            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                if (errorCode == SCAN_FAILED_ALREADY_STARTED)
                    return
                trySendBlocking(Failure(btScanErrors[errorCode] ?: """¯\(°_o)/¯"""))
            }
        }
        btLeScanner.startScan(filters,scanSettings,leScanCallback)

        awaitClose {
            if (btAdapter.isEnabled) // might be called when user had turned off BT
                btLeScanner.stopScan(leScanCallback)
        }
    }.buffer(Channel.UNLIMITED)
}



