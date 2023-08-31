package com.example.bluetooth_arduino.android

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.Message
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.logging.Handler

data class msgdata(var key:Int , var message: String)
class Connection(var context : Context) : ViewModel()
{
    lateinit var sendreceive1 : sendreceive

    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val _status = MutableLiveData<String?>("Disconnected")
    val status : LiveData<String?> get() = _status

    private val _paireddevices = MutableLiveData<List<BluetoothDevice?>>(emptyList())
    val paireddevices : LiveData<List<BluetoothDevice?>> get() = _paireddevices

    private val _allmessages = MutableLiveData<List<msgdata?>>(emptyList())
    val allmessages : LiveData<List<msgdata?>> get() = _allmessages
    private var STATE_LISTENING = 1
    private var STATE_CONNECTING = 2
    private var STATE_CONNECTED = 3
    private var STATE_CONNECTION_FAILED = 4
    private var STATE_MESSAGE_RECEIVED = 5

    @SuppressLint("MissingPermission")
    fun enablebluetooth()
    {
        if(!bluetoothAdapter.isEnabled)
        {
            var intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            context.startActivity(intent)
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchpaireddevices()
    {
        if(bluetoothAdapter != null)
        {
            _paireddevices.value = bluetoothAdapter.bondedDevices.toList()
            Toast.makeText(context, paireddevices.value?.map { it?.name }.toString(), Toast.LENGTH_SHORT).show()
        }
    }


    var handler = android.os.Handler{
        msg : Message ->
        when(msg.what)
        {
            STATE_LISTENING -> {
                _status.value = "Listening..."
                false
            }
            STATE_CONNECTING -> {
                _status.value = "Connecting..."
                false
            }
            STATE_CONNECTED -> {
                _status.value = "Connected"
                false
            }
            STATE_CONNECTION_FAILED -> {
                _status.value = "Connection Failed"
                false
            }
            STATE_MESSAGE_RECEIVED ->
            {
                val readbuff = msg.obj as ByteArray
                var data = String(readbuff,0,msg.arg1,Charsets.UTF_8)
                _allmessages.value = _allmessages.value?.plus(msgdata(1,data))
                Toast.makeText(context,data, Toast.LENGTH_SHORT).show()
                false
            }

            else ->
            {
                _status.value = "Error"
                false
            }
        }

    }



    inner class server() : Thread()
    {

        private lateinit var serversocket : BluetoothServerSocket

        init {
            serverclass()
        }

        @SuppressLint("MissingPermission")
        fun serverclass()
        {
            try {
                    serversocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(appname, uuid)
            }
            catch (e:IOException)
            {
                e.printStackTrace()
            }

        }
        override fun run() {
            super.run()
            var socket : BluetoothSocket? = null

            while (socket == null)
            {
                try {
                    var message = Message.obtain()
                    message.what = STATE_CONNECTING
                    handler.sendMessage(message)

                    socket = serversocket.accept()
                }
                catch (e:IOException)
                {
                    var message = Message.obtain()
                    message.what = STATE_CONNECTION_FAILED
                    handler.sendMessage(message)
                    e.printStackTrace()
                }

                if(socket != null)
                {
                    var message = Message.obtain()
                    message.what = STATE_CONNECTED
                    handler.sendMessage(message)

                    sendreceive1 = sendreceive(socket)
                    sendreceive1.start()
                }
            }

        }
    }

    inner class client(val device1 : BluetoothDevice) : Thread()
    {
        private lateinit var socket : BluetoothSocket
        private lateinit var device : BluetoothDevice

        init {
            clientclass(device1)
        }

        @SuppressLint("MissingPermission")
        fun clientclass(device1 : BluetoothDevice)
        {
            this.device = device1
            try {
                var hcdevice = bluetoothAdapter.getRemoteDevice(device.address)
                socket = hcdevice.createInsecureRfcommSocketToServiceRecord(uuid)
            }
            catch (e:IOException)
            {

                e.printStackTrace()
            }

        }

        @SuppressLint("MissingPermission")
        override fun run() {
            super.run()

            try {
                socket.connect()
                var message = Message.obtain()
                message.what = STATE_CONNECTED
                handler.sendMessage(message)

                sendreceive1 = sendreceive(socket)
                sendreceive1.start()
            }
            catch (e:IOException)
            {
                e.printStackTrace()
                var message = Message.obtain()
                message.what = STATE_CONNECTION_FAILED
                handler.sendMessage(message)
            }
        }


    }

    fun sharedsendreceive(): sendreceive {
        return sendreceive1
    }

    inner class sendreceive(socket: BluetoothSocket) : Thread() {
        private val bluetoothSocket: BluetoothSocket = socket
        private lateinit var inputStream: InputStream
        private lateinit var outputStream: OutputStream

        init {
            setupStreams()
        }

        private fun setupStreams() {
            var tempIn: InputStream? = null
            var tempOut: OutputStream? = null

            try {
                tempIn = bluetoothSocket.inputStream
                tempOut = bluetoothSocket.outputStream
            } catch (e: IOException) {
                e.printStackTrace()
            }

            inputStream = tempIn ?: return
            outputStream = tempOut ?: return
        }

        override fun run() {
            super.run()

            val buffer = ByteArray(1024)
            var bytes: Int

            while (true) {
                try {
                    bytes = inputStream.read(buffer)
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED, bytes, -1, buffer).sendToTarget()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        fun write(message: String) {

            var byteArray = message.toByteArray()
            try {
                outputStream.write(byteArray)
                _allmessages.value = _allmessages.value?.plus(msgdata(0,message))
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


}