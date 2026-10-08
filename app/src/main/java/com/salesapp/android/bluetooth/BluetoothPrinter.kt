package com.salesapp.android.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID

/**
 * Classic Bluetooth ESC/POS printer helper for 58mm thermal printer.
 * Compatible with common Chinese BT thermal printers (RPP02, Zjiang, MTP-II, Goojprt, etc.)
 */
object BluetoothPrinter {
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var socket: BluetoothSocket? = null
    private var output: OutputStream? = null
    var connectedName: String? = null
        private set

    @SuppressLint("MissingPermission")
    fun pairedDevices(context: Context): List<BluetoothDevice> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()
        return try { adapter.bondedDevices?.toList() ?: emptyList() } catch (_: SecurityException) { emptyList() }
    }

    @SuppressLint("MissingPermission")
    suspend fun connect(device: BluetoothDevice): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            disconnect()
            val s = device.createRfcommSocketToServiceRecord(SPP_UUID)
            BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery()
            s.connect()
            socket = s
            output = s.outputStream
            connectedName = try { device.name } catch (_: SecurityException) { device.address }
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    fun isConnected(): Boolean = socket?.isConnected == true

    fun disconnect() {
        try { output?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        output = null; socket = null; connectedName = null
    }

    suspend fun print(bytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val out = output ?: return@withContext Result.failure(IllegalStateException("Printer belum terhubung"))
            out.write(bytes); out.flush()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }
}
