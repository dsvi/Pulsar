package com.ds.pulsar

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.SharedPreferences

lateinit
var app: Pulsar
val preferences: SharedPreferences by lazy{ app.getSharedPreferences("prefs", Context.MODE_PRIVATE) }
val btAdapter: BluetoothAdapter by lazy{ (app.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).getAdapter() }

class Pulsar: Application() {
    override fun onCreate() {
        super.onCreate()
        app = this
    }
}