package com.avaneesh.bluechat

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class MainActivity : ComponentActivity() {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val devices = mutableStateListOf<BluetoothDevice>()

    private var chatSocket: BluetoothSocket? = null
    private var chatOutput: OutputStream? = null
    private var chatInput: InputStream? = null

    private var receiveJob: Job? = null
    private var serverJob: Job? = null

    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.all { it }) {
            // Continue after permission granted
        } else {
            Toast.makeText(this, "Bluetooth permissions are required!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)

        setContent {
            MaterialTheme {
                var role by remember { mutableStateOf<String?>(null) }
                var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
                var messages by remember { mutableStateOf(listOf<String>()) }
                var text by remember { mutableStateOf("") }
                val keyboardController = LocalSoftwareKeyboardController.current

                when {
                    role == null -> {
                        RoleSelectionScreen(
                            onHost = {
                                if (hasBluetoothPermissions()) {
                                    enableDiscoverableAndListen {
                                        role = "host"
                                    }
                                } else {
                                    requestPermissions.launch(requiredPermissions)
                                }
                            },
                            onJoin = {
                                if (hasBluetoothPermissions()) {
                                    startDiscovery()
                                    role = "join"
                                } else {
                                    requestPermissions.launch(requiredPermissions)
                                }
                            }
                        )
                    }

                    role == "join" && selectedDevice == null -> {
                        DeviceListScreen(devices) { device ->
                            connectToDevice(device) {
                                selectedDevice = device
                            }
                        }
                    }

                    selectedDevice != null || role == "host" -> {
                        ChatScreen(
                            messages = messages,
                            text = text,
                            onTextChange = { text = it },
                            onSend = {
                                sendMessage(text)
                                messages = messages + "Me: $text"
                                text = ""
                                keyboardController?.hide()
                            }
                        )

                        // Start receiving in background
                        LaunchedEffect(Unit) {
                            receiveJob = CoroutineScope(Dispatchers.IO).launch {
                                val buffer = ByteArray(1024)
                                while (true) {
                                    try {
                                        val bytes = chatInput?.read(buffer) ?: break
                                        if (bytes > 0) {
                                            val msg = String(buffer, 0, bytes)
                                            withContext(Dispatchers.Main) {
                                                messages = messages + "Them: $msg"
                                            }
                                        }
                                    } catch (e: IOException) {
                                        break
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!hasBluetoothPermissions()) {
            requestPermissions.launch(requiredPermissions)
        }
    }

    private fun hasBluetoothPermissions(): Boolean {
        return requiredPermissions.all { perm ->
            ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startDiscovery() {
        if (bluetoothAdapter?.isEnabled == true) {
            devices.clear()

            // Show paired devices instantly
            val pairedDevices = if (ActivityCompat.checkSelfPermission(
                    this,
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
                return
            } else {
                bluetoothAdapter?.bondedDevices
            }
            pairedDevices?.forEach { device ->
                if (!devices.contains(device)) devices.add(device)
            }

            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }
            bluetoothAdapter?.startDiscovery()
        } else {
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_SHORT).show()
        }
    }

    private fun connectToDevice(device: BluetoothDevice, onConnected: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val socket = if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
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
                    return@launch
                } else {
                    device.createRfcommSocketToServiceRecord(MY_UUID)
                }
                bluetoothAdapter?.cancelDiscovery()
                socket.connect()
                chatSocket = socket
                chatOutput = socket.outputStream
                chatInput = socket.inputStream
                withContext(Dispatchers.Main) { onConnected() }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun startServer(onConnected: () -> Unit) {
        serverJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val serverSocket = if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
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
                    return@launch
                } else {
                    bluetoothAdapter
                        ?.listenUsingRfcommWithServiceRecord("BTChat", MY_UUID)
                }
                val socket = serverSocket?.accept()
                serverSocket?.close()

                socket?.let {
                    chatSocket = it
                    chatOutput = it.outputStream
                    chatInput = it.inputStream
                    withContext(Dispatchers.Main) { onConnected() }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun enableDiscoverableAndListen(onConnected: () -> Unit) {
        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        }
        startActivity(discoverableIntent)
        startServer(onConnected)
    }

    private fun sendMessage(msg: String) {
        CoroutineScope(Dispatchers.IO).launch {
            chatOutput?.write(msg.toByteArray())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        receiveJob?.cancel()
        serverJob?.cancel()
        chatSocket?.close()
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (BluetoothDevice.ACTION_FOUND == intent?.action) {
                val device: BluetoothDevice? =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                device?.let {
                    if (!devices.any { d -> d.address == it.address }) {
                        devices.add(it)
                    }
                }
            }
        }
    }

    companion object {
        private val MY_UUID: UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // SPP UUID
    }
}
