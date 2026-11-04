package dev.iori.colibriz4

import android.hardware.usb.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger

/** SCSI block I/O for the USB disk LUN; this is not SoC NAND/boot-ROM access. */
class UsbMassStorage(private val connection: UsbDeviceConnection, private val intf: UsbInterface) {
    data class Capacity(val lastLba: Long, val blockSize: Int) {
        val blocks: Long get() = lastLba + 1
        val bytes: Long get() = blocks * blockSize
    }
    private val input = (0 until intf.endpointCount).map { intf.getEndpoint(it) }
        .first { it.type == UsbConstants.USB_ENDPOINT_XFER_BULK && it.direction == UsbConstants.USB_DIR_IN }
    private val output = (0 until intf.endpointCount).map { intf.getEndpoint(it) }
        .first { it.type == UsbConstants.USB_ENDPOINT_XFER_BULK && it.direction == UsbConstants.USB_DIR_OUT }
    private val tags = AtomicInteger(0x5A340001)

    fun readCapacity(): Capacity {
        val d = command(byteArrayOf(0x25,0,0,0,0,0,0,0,0,0), 8, true)
        val b = ByteBuffer.wrap(d).order(ByteOrder.BIG_ENDIAN)
        val last = b.int.toLong() and 0xffffffffL
        val size = b.int
        require(last != 0xffffffffL) { "READ CAPACITY(16) required" }
        require(size in 128..65536) { "Unsupported block size: " + size }
        return Capacity(last, size)
    }

    fun readBlocks(lba: Long, count: Int, blockSize: Int): ByteArray {
        require(lba >= 0 && count in 1..32 && lba + count - 1 <= 0xffffffffL)
        val cdb = ByteArray(10)
        cdb[0] = 0x28
        putInt(cdb, 2, lba.toInt()); putShort(cdb, 7, count)
        return command(cdb, count * blockSize, true)
    }

    fun writeBlocks(lba: Long, data: ByteArray, blockSize: Int) {
        require(lba >= 0 && data.isNotEmpty() && data.size % blockSize == 0)
        val count = data.size / blockSize
        require(count in 1..32 && lba + count - 1 <= 0xffffffffL)
        val cdb = ByteArray(10)
        cdb[0] = 0x2A
        putInt(cdb, 2, lba.toInt()); putShort(cdb, 7, count)
        command(cdb, data.size, false, data)
    }

    private fun command(cdb: ByteArray, length: Int, inDirection: Boolean, payload: ByteArray? = null): ByteArray {
        val tag = tags.getAndIncrement()
        val cbw = ByteBuffer.allocate(31).order(ByteOrder.LITTLE_ENDIAN).apply {
            putInt(0x43425355); putInt(tag); putInt(length)
            put(if (inDirection) 0x80.toByte() else 0); put(0); put(cdb.size.toByte()); put(cdb)
            repeat(16 - cdb.size) { put(0) }
        }.array()
        transferOut(cbw)
        val result = if (inDirection) ByteArray(length).also { transferIn(it) } else ByteArray(0)
        if (!inDirection && payload != null) transferOut(payload)
        val csw = ByteArray(13); transferIn(csw)
        val b = ByteBuffer.wrap(csw).order(ByteOrder.LITTLE_ENDIAN)
        val sig = b.int; val returnedTag = b.int; val residue = b.int
        val status = csw[12].toInt() and 255
        check(sig == 0x53425355 && returnedTag == tag) { "Invalid BOT CSW" }
        check(status == 0 && residue == 0) { "SCSI failed: status=" + status + " residue=" + residue }
        return result
    }

    private fun transferOut(data: ByteArray) {
        var p = 0
        while (p < data.size) {
            val n = minOf(16 * 1024, data.size - p)
            val chunk = if (p == 0 && n == data.size) data else data.copyOfRange(p, p+n)
            val sent = connection.bulkTransfer(output, chunk, n, 10000)
            check(sent == n) { "USB OUT short transfer" }; p += sent
        }
    }
    private fun transferIn(data: ByteArray) {
        var p = 0
        while (p < data.size) {
            val n = minOf(16 * 1024, data.size - p)
            val chunk = ByteArray(n)
            val got = connection.bulkTransfer(input, chunk, n, 10000)
            check(got > 0) { "USB IN stalled" }
            System.arraycopy(chunk, 0, data, p, got); p += got
        }
    }
    private fun putInt(a: ByteArray, p: Int, v: Int) {
        a[p]=(v ushr 24).toByte(); a[p+1]=(v ushr 16).toByte()
        a[p+2]=(v ushr 8).toByte(); a[p+3]=v.toByte()
    }
    private fun putShort(a: ByteArray, p: Int, v: Int) {
        a[p]=(v ushr 8).toByte(); a[p+1]=v.toByte()
    }
}
