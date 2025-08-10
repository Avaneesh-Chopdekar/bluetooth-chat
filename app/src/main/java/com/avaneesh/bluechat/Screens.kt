package com.avaneesh.bluechat

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat


@Composable
fun RoleSelectionScreen(onHost: () -> Unit, onJoin: () -> Unit) {
    Column(modifier = Modifier
        .padding(16.dp)
        .navigationBarsPadding()
    ) {
        Button(onClick = onHost, modifier = Modifier.fillMaxWidth()) {
            Text("Host Chat (Make Discoverable)")
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onJoin, modifier = Modifier.fillMaxWidth()) {
            Text("Join Chat (Scan & Connect)")
        }
    }
}

@Composable
fun DeviceListScreen(devices: List<BluetoothDevice>, onDeviceClick: (BluetoothDevice) -> Unit) {
    Column(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
        Text("Nearby Devices", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn {
            items(devices) { device ->
                if (ActivityCompat.checkSelfPermission(
                        LocalContext.current,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return@items
                }
                Text(
                    text = device.name ?: device.address,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDeviceClick(device) }
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun ChatScreen(messages: List<String>, text: String, onTextChange: (String) -> Unit, onSend: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .navigationBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            reverseLayout = true,
            verticalArrangement = Arrangement.Bottom
        ) {
            items(messages.reversed()) { msg ->
                Text(msg)
            }
        }
        Row {
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = onSend, modifier = Modifier.padding(start = 8.dp)) {
                Text("Send")
            }
        }
    }
}