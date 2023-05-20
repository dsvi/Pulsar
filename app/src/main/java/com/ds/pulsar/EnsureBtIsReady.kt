package com.ds.pulsar

import androidx.compose.runtime.Composable
import com.ds.pulsar.EnsureBtIsOn

@Composable
fun WhenBtIsReady(whenBtIsReady: @Composable ()->Unit){
    EnsurePermissionsGranted {
        EnsureBtIsOn {
            whenBtIsReady()
        }
    }
}