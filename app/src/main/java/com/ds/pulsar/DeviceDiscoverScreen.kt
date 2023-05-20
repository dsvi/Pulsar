package com.ds.pulsar

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
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

        if (macAddress != other.macAddress) return false

        return true
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

//class DeviceDiscoverVM : ViewModel(){
//    SnapshotStateList<DevInfo>
//}


@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DeviceDiscoverScreen() {
    WhenBtIsReady {
        Column(
            modifier = Modifier.padding(vertical=15.dp),
            horizontalAlignment = Alignment.CenterHorizontally)
        {
            Column(horizontalAlignment = Alignment.CenterHorizontally){
                Text(text = "Looking for heart rate monitors around", style=MaterialTheme.typography.h6)
                Text(text = "click on a found device to use it", style=MaterialTheme.typography.subtitle1 )
            }
            val error = remember{ mutableStateOf("") }
            val devList = remember{ mutableStateListOf<DeviceListElement>() }
            LaunchedEffect(devList){
                searchForBleDevices().collect {
                    when (it) {
                        is DevInfo -> {
                            var name = it.dev.name?: "???"
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
                Text(error.value, color= MaterialTheme.colors.error, modifier= Modifier.padding(vertical=5.dp))
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DevList(devList: SnapshotStateList<DeviceListElement>){
    LazyColumn(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 5.dp)){
        items( items = devList, key = {it.macAddress} ) { dev ->
            Box(modifier = Modifier
                .padding(3.dp)
                .animateItemPlacement())
            {
                DevListItem(dev)
            }
        }
    }
}


@Composable
fun DevListItem(info: DeviceListElement){
    Card(
        elevation=2.dp,
        modifier = Modifier
            .clickable { info.onClick() }
            .fillMaxWidth())
    {
        Column(Modifier.padding(5.dp)) {
            Text(info.name, fontWeight = FontWeight.Bold, style=MaterialTheme.typography.subtitle1)
            Text(info.macAddress, style=MaterialTheme.typography.subtitle2)
        }
    }
}