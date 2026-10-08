@file:OptIn(ExperimentalBleGattCoroutinesCoroutinesApi::class)

package com.ds.pulsar

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.*
import com.beepiz.bluetooth.gattcoroutines.ConnectionClosedException
import com.beepiz.bluetooth.gattcoroutines.ExperimentalBleGattCoroutinesCoroutinesApi
import com.beepiz.bluetooth.gattcoroutines.GattConnection
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.shareIn
import java.lang.System.currentTimeMillis
import java.util.concurrent.TimeoutException
import kotlin.time.Duration.Companion.milliseconds

val heartRateStream: HeartRateStream by lazy{
    HeartRateStream()
}

class HeartRateStream {
    private var devAddres: String by PreferenceDelegate("dev_addres", "")
    var lastScanTime: Long = 0
    fun setSource( dev:  BluetoothDevice){
        assert(dev.address != null)
        devAddres = dev.address
        this.dev = dev
    }

    private var dev: BluetoothDevice? = null
    get() {
        if ( field == null) {
            assert( devAddres.isNotEmpty() )
            field = btAdapter.getRemoteDevice(devAddres)
        }
        return field
    }

    val requiresSearchForDevice: Boolean
    get() = devAddres.isEmpty()

    data class DataFromDevice(
        var pulse: UInt = 0u,
        var batteryLevel: UInt? = null
    )
    // check requiresSearchForDevice before use
    // Hot stream: a single connection is shared between all collectors; the latest
    // value is replayed to new collectors, and it automatically reconnects on error.
    @SuppressLint("MissingPermission")
    val data: SharedFlow<DataFromDevice> = upstreamFlow()
        .retryWhen { e, _ ->
            if (e is TimeoutException || e is ConnectionClosedException) {
                println("device stream failed: $e; retrying to connect")
                delay(200.milliseconds)
                true
            } else false
        }
        .shareIn(
            CoroutineScope(SupervisorJob() + Dispatchers.IO),
            SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            replay = 1
        )

    @SuppressLint("MissingPermission")
    private fun upstreamFlow() = flow<DataFromDevice> {
        val deviceConnection = GattConnection(bluetoothDevice = dev!!)
        try {
            var lastTimeDataReceived = currentTimeMillis()
            CoroutineScope(currentCoroutineContext()).launch {
                val timeoutMillis: Long = 7_000
                val currTime = currentTimeMillis()
                if (currTime - lastScanTime > 10_000) {
                    lastScanTime = currTime
                    launch {
                        withTimeoutOrNull(timeoutMillis.milliseconds) {
                            searchForBleDevices().collect()
                        }
                    }
                }
                while (true) {
                    delay(1_000)
                    if (currentTimeMillis() - lastTimeDataReceived > timeoutMillis)
                        throw TimeoutException()
                }
            }
            deviceConnection.connect()
            //deviceConnection.requestMtu(256)
            deviceConnection.discoverServices()
            val serviceHrm =
                deviceConnection.getService("0000180d-0000-1000-8000-00805f9b34fb".uuid)
            val characHr =
                serviceHrm?.getCharacteristic("00002a37-0000-1000-8000-00805f9b34fb".uuid)
            var characBattery: BluetoothGattCharacteristic? = null
            var lastTimeTriedToFindCharacBattery = 0L
            val tryToGetCharacBattery = suspend {
                val time = currentTimeMillis()
                if (time - lastTimeTriedToFindCharacBattery > 120_000) {
                    lastTimeTriedToFindCharacBattery = time
                    withTimeoutOrNull(500.milliseconds){
                        val serviceBattery =
                            deviceConnection.getService("0000180f-0000-1000-8000-00805f9b34fb".uuid)
                        characBattery =
                            serviceBattery?.getCharacteristic("00002a19-0000-1000-8000-00805f9b34fb".uuid)
                        if (characBattery?.properties?.and(PROPERTY_READ) == 0)
                            characBattery = null
                    }
                }
            }
            tryToGetCharacBattery()
            if (characHr != null){
                val f = deviceConnection.notifications(characHr)
                deviceConnection.setCharacteristicNotificationsEnabledOnRemoteDevice(
                    characHr,
                    true
                )
                var lastTimeHeardFromBattery = 0L
                val data = DataFromDevice()
                f.conflate().collect { heartRate ->
                    val flags = heartRate.getIntValue(FORMAT_UINT8, 0)
                    val is8bit = flags and 1 == 0
                    val hr = if (is8bit)
                        heartRate.getIntValue(FORMAT_UINT8, 1)
                    else
                        heartRate.getIntValue(FORMAT_UINT16, 1)
                    data.pulse = hr.toUInt()
                    println("♥ ${data.pulse} ♥")
                    if (characBattery != null) {
                        withTimeoutOrNull(500.milliseconds){
                            val time = currentTimeMillis()
                            if (time - lastTimeHeardFromBattery > 60_000){
                                lastTimeHeardFromBattery = time
                                deviceConnection.readCharacteristic(characBattery!!)
                                val value = characBattery!!.value
                                if (value.isNotEmpty()) {
                                    data.batteryLevel = value[0].toUInt()
                                    println("\uD83D\uDD0B ${data.batteryLevel} \uD83D\uDD0B")
                                }
                            }
                        }
                    }
                    else{
                        tryToGetCharacBattery()
                    }
                    lastTimeDataReceived = currentTimeMillis()
                    emit(data)
                }
            }
        }
        finally {
            deviceConnection.close() // Also triggers disconnect by default.
            println("device collection finished")
        }
    }.conflate().flowOn(Dispatchers.IO)
}