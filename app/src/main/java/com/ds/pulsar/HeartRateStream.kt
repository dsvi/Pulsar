package com.ds.pulsar

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.*
import com.beepiz.bluetooth.gattcoroutines.GattConnection
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.lang.System.currentTimeMillis
import java.lang.Thread.sleep
import java.util.concurrent.TimeoutException

private const val MAC_ADDRESS = "dev_mac"

val heartRateStream: HeartRateStream by lazy{
    HeartRateStream()
}

class HeartRateStream {
    fun setSource( dev:  BluetoothDevice){
        assert(dev.address != null)
        devAddres = dev.address
        this.dev = dev
    }

    var devAddres: String by PreferenceDelegate("dev_addres", "")
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
    @SuppressLint("MissingPermission")
    @com.beepiz.bluetooth.gattcoroutines.ExperimentalBleGattCoroutinesCoroutinesApi
    suspend fun heartRateFlow() = flow<DataFromDevice> {
        val deviceConnection = GattConnection(bluetoothDevice = dev!!)
        var characHr: BluetoothGattCharacteristic? = null
        try {
            var lastTimeDataReceived = currentTimeMillis()
            CoroutineScope(currentCoroutineContext()).launch{
                while (true) {
                    delay(1_000)
                    if (currentTimeMillis() - lastTimeDataReceived > 5_000)
                        throw TimeoutException()
                }
            }
            deviceConnection.connect()
            //deviceConnection.requestMtu(256)
            deviceConnection.discoverServices()
            val serviceHrm =
                deviceConnection.getService("0000180d-0000-1000-8000-00805f9b34fb".uuid)
            characHr =
                serviceHrm?.getCharacteristic("00002a37-0000-1000-8000-00805f9b34fb".uuid)
            var characBattery: BluetoothGattCharacteristic? = null
            var lastTimeTriedToFindCharacBattery = 0L
            val tryToGetCharacBattery = suspend {
                val time = currentTimeMillis()
                if (time - lastTimeTriedToFindCharacBattery > 120_000) {
                    lastTimeTriedToFindCharacBattery = time
                    withTimeoutOrNull(500){
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
                        withTimeoutOrNull(500){
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
            if (characHr != null){
                deviceConnection.setCharacteristicNotificationsEnabledOnRemoteDevice(characHr, false)
                sleep(100) // let it go through before closing the connection
            }
            deviceConnection.close() // Also triggers disconnect by default.
            println("device collection finished")
        }
    }.conflate().flowOn(Dispatchers.IO)
}