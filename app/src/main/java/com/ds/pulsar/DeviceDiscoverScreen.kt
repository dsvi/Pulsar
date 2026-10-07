package com.ds.pulsar

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.ds.pulsar.ui.theme.PulsarTheme


class DeviceListElement(
    val name: String,
    val macAddress: String,
    val onClick: () -> Unit = fun (){}
){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DeviceListElement

        return macAddress == other.macAddress
    }

    override fun hashCode(): Int {
        return macAddress.hashCode()
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview(){
    PulsarTheme {
        DevList(remember{
            mutableStateListOf(
                DeviceListElement("name1", "1"),
                DeviceListElement("name2", "2")
            )
        })
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DeviceDiscoverScreen() {
    WhenBtIsReady {
        Column(
            modifier = Modifier.padding(vertical=15.dp),
            horizontalAlignment = Alignment.CenterHorizontally)
        {
            Column(horizontalAlignment = Alignment.CenterHorizontally){
                Text(text = "Looking for heart rate monitors around", style=MaterialTheme.typography.titleLarge)
                Text(text = "click on a found device to use it", style=MaterialTheme.typography.titleMedium )
            }
            val error = remember{ mutableStateOf("") }
            val devList = remember{ mutableStateListOf<DeviceListElement>() }
            LaunchedEffect(devList){
                searchForBleDevices().collect {
                    when (it) {
                        is DevInfo -> {
                            val name = it.dev.name?: "???"
                            val dev = DeviceListElement(name, it.dev.address){
                                heartRateStream.setSource(it.dev)
                                navController.popBackStack()
                            }
                            if (!devList.contains(dev))
                                devList.add(dev)
                        }
                        is Failure ->
                            error.value = "Error while looking for a heart rate monitor: " + it.why
                    }
                }
            }
            DevList(devList = devList)
            if (error.value.isNotEmpty())
                Text(error.value, color= MaterialTheme.colorScheme.error, modifier= Modifier.padding(vertical=5.dp))
        }
    }
}


@Composable
fun DevList(devList: SnapshotStateList<DeviceListElement>){
    LazyColumn(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 5.dp)){
        items( items = devList, key = {it.macAddress} ) { dev ->
            Box(modifier = Modifier
                .padding(3.dp)
                .animateItem())
            {
                DevListItem(dev)
            }
        }
    }
}


@Composable
fun DevListItem(info: DeviceListElement){
    Card(
        elevation = CardDefaults.cardElevation(
            defaultElevation = 5.dp
        ),
        modifier = Modifier
            .clickable { info.onClick() }
            .fillMaxWidth())
    {
        Column(Modifier.padding(5.dp)) {
            Text(info.name, fontWeight = FontWeight.Bold, style=MaterialTheme.typography.titleMedium)
            Text(info.macAddress, style=MaterialTheme.typography.titleSmall)
        }
    }
}