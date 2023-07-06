package com.ds.pulsar

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun EnsurePermissionsGranted( whenGrantedContend: @Composable () -> Unit ){
    val requiredPermissions = rememberMultiplePermissionsState(
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            listOf(
//                Manifest.permission.BLUETOOTH_SCAN,
//                Manifest.permission.BLUETOOTH_CONNECT,
//            )
//        } else {
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
//        }
    )

    if (!requiredPermissions.allPermissionsGranted) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally)
        {
            Text("The permissions are required for this app to function.", Modifier.padding(all = 10.dp))
            Button(onClick = { requiredPermissions.launchMultiplePermissionRequest() }) {
                Text(stringResource(R.string.request_permissions))
            }
        }
    }
    else{
        whenGrantedContend()
    }
}
