package com.ds.pulsar

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun EnsureBtIsOn(whenBtIsOn: @Composable ()->Unit ) {
    val context = LocalContext.current

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // Only claim the adapter when the user really accepted the request. If they
            // dismissed the dialog, we never turned Bluetooth on, so the exit dialog must not
            // offer to turn it off for them.
            mayDisableBt = (result.resultCode == Activity.RESULT_OK) && btAdapter.isEnabled
        }

    val isBluetoothEnabled = remember{ mutableStateOf( btAdapter.isEnabled ) }

    // If context changes, or isBluetoothEnabled recreated, unregister and register again
    DisposableEffect(context, isBluetoothEnabled) {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    when (intent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR
                    )) {
                        BluetoothAdapter.STATE_OFF -> {
                            isBluetoothEnabled.value = false
                            println("Bluetooth is OFF")
                        }
                        BluetoothAdapter.STATE_ON -> {
                            isBluetoothEnabled.value = true
                            println("Bluetooth is ON")
                        }
                    }

                }
            }
        }

        context.registerReceiver(receiver, intentFilter)
        // When the effect leaves the Composition, remove the callback
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    AnimatedContent(
        targetState = isBluetoothEnabled.value,
    ) { isBtEnabled ->
        if (!isBtEnabled) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ){
                Text("Got to enable Bluetooth", Modifier.padding(all = 10.dp))
                ElevatedButton(
                    onClick = {
                        launcher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                    }
                ){
                    Text("Aye, let's do it")
                }
            }
        } else {
            whenBtIsOn()
        }
    }
}