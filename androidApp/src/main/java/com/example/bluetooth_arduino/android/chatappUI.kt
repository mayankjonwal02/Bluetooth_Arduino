package com.example.bluetooth_arduino.android

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
val appname = "Bluetooth_App"

@SuppressLint("MissingPermission")
@Composable
fun chatapp(connection: Connection) {

    var permissionlauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestMultiplePermissions(), onResult = {})
    LaunchedEffect(Unit){
        permissionlauncher.launch(
            arrayOf(
                android.Manifest.permission.BLUETOOTH,
                android.Manifest.permission.BLUETOOTH_ADMIN,
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_ADVERTISE,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }


    var status = connection.status.observeAsState("")
    var paireddevices = connection.paireddevices.observeAsState(emptyList<BluetoothDevice>())
    var allmessages = connection.allmessages.observeAsState(emptyList())
    var ninja = listOf<Int?>(1, 2, 3, 4)
    var message by remember {
        mutableStateOf("")
    }
    var context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Magenta.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .background(Color.Transparent),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(5.dp))
                Text(text = "Status : ${status.value}", color = Color.Black)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Button(
                        onClick = { connection.enablebluetooth() },
                        colors = ButtonDefaults.buttonColors(
                            contentColor = Color.Blue,
                            backgroundColor = Color.Transparent
                        )
                    ) {
                        Text(text = "Enable BT", modifier = Modifier.background(Color.Transparent))
                    }
                    Button(
                        onClick = {
                            connection.fetchpaireddevices()
                        },
                        colors = ButtonDefaults.buttonColors(
                            contentColor = Color.Blue,
                            backgroundColor = Color.Transparent
                        )
                    ) {
                        Text(text = "Devices", modifier = Modifier.background(Color.Transparent))
                    }
                    Button(
                        onClick = {
                            connection.server().start()
                        },
                        colors = ButtonDefaults.buttonColors(
                            contentColor = Color.Blue,
                            backgroundColor = Color.Transparent
                        )
                    ) {
                        Text(text = "Listen", modifier = Modifier.background(Color.Transparent))
                    }
                }

            }


            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(10.dp)
                    .background(color = Color.White)
            ) {
                items(paireddevices.value) { device ->

                    device?.name?.let {
                        Text(
                            text = it,
                            color = Color.Black,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    connection
                                        .client(device)
                                        .start()
                                }
                                .padding(10.dp)
                                .background(Color.Magenta.copy(alpha = 0.2f))
                                .padding(2.dp)
                        )
                    }

                }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .padding(10.dp)
                    .background(color = Color.White),
                state = rememberLazyListState()
            ) {
                items(allmessages.value) { msg ->

                    var data = msg?.message
                    var key = msg?.key
                    if (data != null) {
                        Text(
                            text = data,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Transparent)
                                .padding(5.dp),
                            textAlign = if (key == 1) {
                                TextAlign.Start
                            } else {
                                TextAlign.End
                            },
                            color = if (key == 1) {
                                Color.Red
                            } else {
                                Color.Blue
                            }
                        )
                    }

                }

            }


        }

        TextField(value = message, onValueChange = { message = it },
            placeholder = { Text(text = "Enter a Message") },

            label = { Text(text = "Message") },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "",
                    modifier = Modifier.clickable {
                        if(message.isNotEmpty()){
                            connection.sendreceive1.write(message)
                            message = ""
                        }
                    else
                        {
                            Toast.makeText(context,"Field Empty",Toast.LENGTH_SHORT).show()
                        }})
            }
        )

    }
}
