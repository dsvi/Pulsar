package com.ds.pulsar

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun EnsurePermissionsGranted(whenGranted: @Composable () -> Unit ){
    val context = LocalContext.current
    var askedBefore by remember { mutableStateOf(askedAboutPermissions) }
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
    ) {
        askedBefore = true
        askedAboutPermissions = true
    }
    AnimatedContent(targetState = requiredPermissions.allPermissionsGranted) { permissionsGranted ->
        if (!permissionsGranted) {
            val revoked = requiredPermissions.revokedPermissions
            val blocked = askedBefore &&
                    revoked.isNotEmpty() &&
                    revoked.none { it.status.shouldShowRationale }
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally)
            {
                Text(
                    modifier = Modifier.padding(all = 10.dp),
                    text = when {
                        blocked ->
                            "The permission is denied, and the system will not ask again.\n" +
                                    "Grant it on the app's settings page."
                        requiredPermissions.shouldShowRationale ->
                            "Scanning for bluetooth devices needs the location permission on this Android version.\n" +
                                    "It is used only to find the HRM BLE device."
                        else ->
                            "The permissions are required for this app to function."
                    }
                )
                ElevatedButton(onClick = {
                    if (blocked) {
                        // The only way left out of the blocked state is the app's own entry
                        // in the system settings.
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.fromParts("package", context.packageName, null))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } else {
                        requiredPermissions.launchMultiplePermissionRequest()
                    }
                }) {
                    Text(if (blocked) "Open the app settings" else "Grant permissions")
                }
            }
        }
        else{
            whenGranted()
        }
    }
}