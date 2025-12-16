package com.withintent.samplebleapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.polidea.multiplatformbleadapter.BleAdapter
import com.polidea.multiplatformbleadapter.BleAdapterFactory
import com.polidea.multiplatformbleadapter.ConnectionOptions
import com.polidea.multiplatformbleadapter.ScanResult
import com.polidea.multiplatformbleadapter.utils.Base64Converter
import com.polidea.multiplatformbleadapter.utils.ByteUtils
import com.polidea.multiplatformbleadapter.utils.Constants
import com.withintent.samplebleapp.ui.theme.AndroidTheme
import okio.Buffer
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest


class MainActivity : ComponentActivity(), EasyPermissions.PermissionCallbacks {
    private val TAG = "FS"

    private val MTU = 256
    private val ANDROID_PACKET_LENGTH = 192

//    private val serviceUuid = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
//    private val writeUuid = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
//    private val notifyUuid = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"
    private val serviceUuid = "00000001-0000-1000-8000-00805f9b34fb"
    private val writeUuid = "00000002-0000-1000-8000-00805f9b34fb"
    private val notifyUuid = "00000003-0000-1000-8000-00805f9b34fb"

    companion object {
        private const val RC_BLUETOOTH_PERM = 1
    }

    private lateinit var bleAdapter: BleAdapter
    private var buffer: Buffer = Buffer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, ">>> onCreate")


        setContent {
            Log.i(TAG, ">>> onCreate - setContent")
            bleAdapter = BleAdapterFactory.getNewAdapter(this)
            bleAdapter.logLevel = Constants.BluetoothLogLevel.VERBOSE
            val bleState = remember { mutableStateOf(bleAdapter.currentState) }

            bleAdapter.createClient("SampleBleApp",
                    {
                        bleState.value = bleAdapter.currentState;
                    },
                    {
                        Log.i(TAG, "onStateRestored $it")
                    }
            )

            AndroidTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val devices = remember { mutableStateOf(mapOf<String, ScanResult>()) }
                    val scanStarted = remember { mutableStateOf(false) }
                    val enableResponse = remember { mutableStateOf("No response") }

                    Column {
                        Text(
                                text = "Ble Sample APP", style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                                text = "BLE State ${bleAdapter.currentState}"
                        )

                        if (bleState.value == "PoweredOff") {
                            Button(onClick = {
                                bleAdapter.enable("test", { bleState.value = bleAdapter.currentState }, { enableResponse.value = "Error" });
                            }) {
                                Text(text = "Enable BLE")
                            }
                        } else {
                            Button(onClick = {
                                bleAdapter.disable("test", { enableResponse.value = "Done" }, { enableResponse.value = "Error" });
                            }) {
                                Text(text = "Disable BLE")
                            }
                        }
                        Text(
                                text = "Response from ble enable/disable ${enableResponse.value}", style = MaterialTheme.typography.bodyMedium
                        )


                        if (bleAdapter.currentState == "PoweredOn") {
                            if (!scanStarted.value) {
                                Button(onClick = {
                                    devices.value = emptyMap()
                                    scanStarted.value = true
                                    requestBluetoothPermissions()
                                }) {
                                    Text(text = "Start scan")
                                }
                            } else {
                                Button(onClick = {
                                    scanStarted.value = false
                                    bleAdapter.stopDeviceScan()
                                }) {
                                    Text(text = "Stop scan")
                                }
                            }
                        }
                        Card(modifier = Modifier.padding(8.dp)) {
                            val sorted = devices.value.values.sortedBy { it.deviceId }
                            LazyColumn {
                                items(sorted.size) { index ->
                                    val item: ScanResult = sorted.get(index)
                                    Text(text = "${item.deviceId} - ${item.deviceName}", modifier = Modifier.padding(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun requestBluetoothPermissions() {
        val perms = arrayOf(
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_CONNECT,
        )
        if (EasyPermissions.hasPermissions(this, *perms)) {
            // 权限已授予，开始扫描
            startBleScan()
        } else {
            // 请求权限
            EasyPermissions.requestPermissions(
                    PermissionRequest.Builder(this, RC_BLUETOOTH_PERM, *perms)
                            .setRationale("需要蓝牙权限来扫描和连接设备")
                            .setPositiveButtonText("确定")
                            .setNegativeButtonText("取消")
                            .build()
            )
        }
    }

    private fun startBleScan() {
        // 实现蓝牙扫描逻辑
        bleAdapter.startDeviceScan(
                null,
                0,
                1,
                scanCallback@ {
//                    devices.value = devices.value.plus(it.deviceId to it)
                    Log.i(TAG, "OnScanResultCallback $it")
//                    if (it.deviceName?.startsWith("HyperMateMax E0CB") != true) {
                    if (it.deviceName?.startsWith("Nordic_UART") != true) {
                        return@scanCallback
                    }
                    Log.i(TAG, "找到设备")
                    bleAdapter.stopDeviceScan()
                    val options = ConnectionOptions(false, MTU, null, null, 0, true)
                    // 给超时时间会崩溃
//                    val options = ConnectionOptions(false, ANDROID_PACKET_LENGTH, null, 3000, 0)
                    bleAdapter.connectToDevice(it.deviceId, options,
                            {
                                Log.i(TAG, "connectToDevice - OnSuccessCallback $it")
                                discoverServices(it.id)
                            },
                            {
                                Log.i(TAG, "connectToDevice - OnEventCallback $it")
                            },
                            {
                                Log.i(TAG, "connectToDevice - OnErrorCallback $it")
                            }
                    )
                },
                {
                    Log.i(TAG, "OnErrorCallback $it")
                }
        )
    }

    private fun discoverServices(deviceId: String) {
        bleAdapter.discoverAllServicesAndCharacteristicsForDevice(
                deviceId,
                "discoverService",
                {
                    Log.i(TAG, "discover - OnSuccessCallback $it")
//                    val value = "3f232300000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
//                    writeCharacteristic(it.id, serviceUuid, writeUuid, value)
                    notifyCharacteristic(it.id, serviceUuid, notifyUuid)
                },
                {
                    Log.i(TAG, "discover - OnErrorCallback $it")
                }
        )
    }

    var msgDataLen = 0L
    var packetSize = 0L

    private fun notifyCharacteristic(deviceId: String, serviceId: String, characteristicId: String) {
        buffer.clear()
        bleAdapter.monitorCharacteristicForDevice(
                deviceId,
                serviceId,
                characteristicId,
                "monitorCharacteristic",
                {
                    Log.i(TAG, "notify - OnEventCallback $it")
                },
                {
                    Log.i(TAG, "notify - OnSuccessCallback $it")
                },
                {
                    Log.i(TAG, "notify - OnErrorCallback $it")
                }
        )
        Log.d(TAG, ">>> 开始写数据")
        val command = "3f232300000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
        val cmdBytes = ByteUtils.hexToBytes(command)
        val list = Protocol.slice(cmdBytes)
        for (data in list) {
            val cmdBase64 = Base64Converter.encode(data)
            writeCharacteristic(deviceId, serviceUuid, writeUuid, cmdBase64)
        }
    }

    private fun writeCharacteristic(deviceId: String, serviceId: String, characteristicId: String, value: String) {
        bleAdapter.writeCharacteristicForDevice(
                deviceId,
                serviceId,
                characteristicId,
                value,
                false,
                "writeValue",
                {
                    Log.i(TAG, "write - OnSuccessCallback: ${ByteUtils.bytesToHex(it.value)}")

                },
                {
                    Log.i(TAG, "write - OnErrorCallback")
                }
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, ">>> onRequestPermissionsResult: " + permissions.joinToString())
        // 将权限请求结果传递给 EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        Log.d(TAG, ">>> onPermissionsGranted: " + perms.joinToString())
        if (requestCode == RC_BLUETOOTH_PERM) {
            if (perms.containsAll(listOf(
                            android.Manifest.permission.BLUETOOTH_SCAN,
                            android.Manifest.permission.BLUETOOTH_CONNECT
                    ))) {
                // 所有蓝牙权限都已授予，开始扫描
                startBleScan()
            }
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        Log.d(TAG, ">>> onPermissionsDenied")
    }

}