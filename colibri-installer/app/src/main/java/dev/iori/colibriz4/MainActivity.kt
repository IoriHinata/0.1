package dev.iori.colibriz4

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.*
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private lateinit var usb: UsbManager
    private lateinit var status: TextView
    private lateinit var details: TextView
    private var selectedName = "No image selected"
    private var selectedInfo = "No image selected"
    private var pendingReport = ""
    private val pickImage = 41
    private val exportReportCode = 42
    private val permissionAction by lazy { "${packageName}.USB_PERMISSION" }
    private var receiverRegistered = false

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                permissionAction -> {
                    val device = usbDeviceExtra(intent) ?: return
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        connectAndProbe(device)
                    } else {
                        status.text = "USB: permission denied"
                        appendDetails("Android USB permission was denied for ${device.deviceName}.")
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED,
                UsbManager.ACTION_USB_DEVICE_DETACHED -> inspectUsb()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usb = getSystemService(USB_SERVICE) as UsbManager
        render()
        val filter = IntentFilter().apply {
            addAction(permissionAction)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(usbReceiver, filter, RECEIVER_NOT_EXPORTED)
        else @Suppress("DEPRECATION") registerReceiver(usbReceiver, filter)
        receiverRegistered = true
        inspectUsb()
    }

    override fun onDestroy() {
        if (receiverRegistered) unregisterReceiver(usbReceiver)
        super.onDestroy()
    }

    private fun usbDeviceExtra(intent: Intent): UsbDevice? {
        return if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        else @Suppress("DEPRECATION") intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(0xFF101820.toInt())
        }
        fun heading(text: String, size: Float) = TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(0xFFF0F4F8.toInt())
        }
        root.addView(heading("COLIBRIOS / DIGMA Z4", 23f))
        root.addView(heading("PORT & DEVICE DIAGNOSTICS · 0.2.0", 12f))
        status = heading("USB: scanning…", 15f)
        root.addView(status, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 20 })
        details = heading("", 12f)
        root.addView(details, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = 12; bottomMargin = 12 })
        root.addView(Button(this).apply { text = "Scan USB device"; setOnClickListener { inspectUsb() } })
        root.addView(Button(this).apply { text = "Request access + read SCSI identity"; setOnClickListener { requestProbe() } })
        root.addView(Button(this).apply { text = "Select firmware / OS image"; setOnClickListener { chooseImage() } })
        root.addView(Button(this).apply { text = "Run installation simulation"; setOnClickListener { runInstallationSimulation() } })
        root.addView(Button(this).apply { text = "Export diagnostic report"; setOnClickListener { exportReport() } })
        root.addView(heading("Read-only mode. Only standard SCSI INQUIRY is sent to Mass Storage devices. No flash writes or vendor commands.", 12f),
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = 16 })
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun inspectUsb() {
        val devices = usb.deviceList.values.toList()
        if (devices.isEmpty()) {
            status.text = "USB: no device detected"
            details.text = "Connect DIGMA Z4 to this phone with a USB OTG adapter/cable. Some phones do not support USB host mode."
            return
        }
        val report = StringBuilder()
        devices.forEachIndexed { index, d ->
            report.append("Device #$index\nVID:PID %04X:%04X\n".format(d.vendorId, d.productId))
            report.append("Name: ${d.deviceName}\nManufacturer: ${d.manufacturerName ?: "?"}\nProduct: ${d.productName ?: "?"}\n")
            report.append("USB version: ${d.version}\nInterfaces: ${d.interfaceCount}\n")
            for (i in 0 until d.interfaceCount) {
                val intf = d.getInterface(i)
                report.append("Interface $i: class=${intf.interfaceClass}, subclass=${intf.interfaceSubclass}, protocol=${intf.interfaceProtocol}\n")
                for (e in 0 until intf.endpointCount) {
                    val ep = intf.getEndpoint(e)
                    report.append("  endpoint=0x%02X dir=%s type=%d maxPacket=%d\n".format(
                        ep.address, if (ep.direction == UsbConstants.USB_DIR_IN) "IN" else "OUT", ep.type, ep.maxPacketSize
                    ))
                }
            }
            val massStorage = (0 until d.interfaceCount).any { d.getInterface(it).interfaceClass == UsbConstants.USB_CLASS_MASS_STORAGE }
            report.append("Classification: ${if (massStorage) "USB Mass Storage" else "not Mass Storage"}\n")
            if (d.vendorId == 0x10D7 && d.productId == 0x1102 && massStorage) {
                report.append("Known profile: reported DIGMA Z4 normal USB identity. ADFU remains unconfirmed.\n")
            }
            report.append("Android permission: ${usb.hasPermission(d)}\n\n")
        }
        status.text = "USB: ${devices.size} device(s) detected"
        details.text = report.toString()
    }

    private fun requestProbe() {
        val device = usb.deviceList.values.firstOrNull { d ->
            (0 until d.interfaceCount).any { d.getInterface(it).interfaceClass == UsbConstants.USB_CLASS_MASS_STORAGE }
        }
        if (device == null) {
            status.text = "USB: no Mass Storage interface found"
            return
        }
        if (usb.hasPermission(device)) connectAndProbe(device) else {
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
            val permissionIntent = PendingIntent.getBroadcast(this, 0, Intent(permissionAction).setPackage(packageName), flags)
            usb.requestPermission(device, permissionIntent)
            status.text = "USB: waiting for Android permission"
        }
    }

    private fun connectAndProbe(device: UsbDevice) {
        status.text = "USB: running read-only SCSI INQUIRY…"
        Thread {
            val result = runCatching { scsiInquiry(device) }.fold(
                onSuccess = { it },
                onFailure = { "SCSI INQUIRY failed: ${it.javaClass.simpleName}: ${it.message}" }
            )
            runOnUiThread {
                appendDetails("\n--- READ-ONLY SCSI INQUIRY ---\n$result\n")
                status.text = if (result.startsWith("SCSI INQUIRY failed")) "USB: probe failed" else "USB: SCSI identity read"
            }
        }.start()
    }

    // Standard USB Mass Storage Bulk-Only Transport + SCSI INQUIRY (opcode 0x12).
    // This requests identification data only; it does not read or modify flash sectors.
    private fun scsiInquiry(device: UsbDevice): String {
        val intf = (0 until device.interfaceCount).map { device.getInterface(it) }
            .firstOrNull { it.interfaceClass == UsbConstants.USB_CLASS_MASS_STORAGE }
            ?: return "No Mass Storage interface."
        val bulkIn = (0 until intf.endpointCount).map { intf.getEndpoint(it) }
            .firstOrNull { it.type == UsbConstants.USB_ENDPOINT_XFER_BULK && it.direction == UsbConstants.USB_DIR_IN }
            ?: return "Bulk IN endpoint not found."
        val bulkOut = (0 until intf.endpointCount).map { intf.getEndpoint(it) }
            .firstOrNull { it.type == UsbConstants.USB_ENDPOINT_XFER_BULK && it.direction == UsbConstants.USB_DIR_OUT }
            ?: return "Bulk OUT endpoint not found."
        val connection = usb.openDevice(device) ?: return "Android could not open the USB device."
        try {
            if (!connection.claimInterface(intf, true)) return "Could not claim Mass Storage interface."
            val tag = 0x5A345A34
            val cbw = ByteBuffer.allocate(31).order(ByteOrder.LITTLE_ENDIAN).apply {
                putInt(0x43425355) // 'USBC'
                putInt(tag)
                putInt(36)
                put(0x80.toByte()) // device-to-host
                put(0) // LUN
                put(6) // CDB length
                put(0x12) // SCSI INQUIRY
                put(0) // EVPD
                put(0) // page code
                put(0) // reserved
                put(36) // allocation length
                put(0) // control
                repeat(10) { put(0) } // pad 6-byte CDB to 16 bytes
            }.array()
            if (connection.bulkTransfer(bulkOut, cbw, cbw.size, 3000) != cbw.size) return "Could not send SCSI command block."
            val data = ByteArray(36)
            val got = connection.bulkTransfer(bulkIn, data, data.size, 5000)
            if (got < 36) return "Short SCSI response ($got/36 bytes). Device may not implement standard BOT INQUIRY."
            val csw = ByteArray(13)
            val cswGot = connection.bulkTransfer(bulkIn, csw, csw.size, 3000)
            if (cswGot != 13) return "Missing BOT command status wrapper ($cswGot/13 bytes)."
            val cswBuffer = ByteBuffer.wrap(csw).order(ByteOrder.LITTLE_ENDIAN)
            val cswSignature = cswBuffer.int
            val cswTag = cswBuffer.int
            val residue = cswBuffer.int
            val statusByte = csw[12].toInt() and 0xFF
            if (cswSignature != 0x53425355 || cswTag != tag) return "Invalid BOT status wrapper (signature/tag mismatch)."
            if (statusByte != 0) return "SCSI command status=$statusByte (command failed or was rejected; residue=$residue)."
            fun ascii(start: Int, length: Int) = String(data, start, length, Charsets.US_ASCII).trim()
            return "Vendor: ${ascii(8, 8)}\nProduct: ${ascii(16, 16)}\nRevision: ${ascii(32, 4)}\nPeripheral type: ${data[0].toInt() and 0x1F}\nResponse bytes: $got\nBOT status: passed"
        } finally {
            runCatching { connection.releaseInterface(intf) }
            connection.close()
        }
    }

    private fun appendDetails(value: String) { details.text = details.text + value }

    private fun runInstallationSimulation() {
        val log = StringBuilder()
        log.append("COLIBRIOS / DIGMA Z4 — INSTALLATION READINESS\n")
        log.append("1. USB enumeration: ${if (usb.deviceList.isEmpty()) "NO DEVICE" else "DEVICE VISIBLE"}\n")
        log.append("2. SCSI INQUIRY: run the read-only probe to obtain target identity.\n")
        log.append("3. ADFU: NOT VERIFIED for VID:PID 10D7:1102; no mode-switch command sent.\n")
        log.append("4. NAND geometry and complete backup: NOT AVAILABLE.\n")
        log.append("5. CPU target: Actions ATJ2127 MIPS32/MIPS16; upstream KolibriOS is x86.\n")
        log.append("6. Firmware: ${if (selectedInfo == "No image selected") "not selected" else "selected and hashed; format still requires validation"}.\n")
        log.append("7. Flash: DISABLED until device-specific recovery and a tested MIPS payload exist.\n\n")
        log.append("This is a readiness report, not an installation. No device data was written.\n")
        details.text = log.toString()
        status.text = "Readiness check completed"
    }

    private fun chooseImage() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startActivityForResult(intent, pickImage)
    }

    @Deprecated("Legacy callback retained for broad Android compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        when (requestCode) {
            pickImage -> inspectImage(uri)
            exportReportCode -> contentResolver.openOutputStream(uri)?.use {
                it.write(pendingReport.toByteArray(Charsets.UTF_8))
                Toast.makeText(this, "Report exported", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun inspectImage(uri: Uri) {
        contentResolver.openInputStream(uri)?.use { input ->
            val digest = MessageDigest.getInstance("SHA-256")
            val first = ByteArray(64)
            var firstCount = 0
            val buffer = ByteArray(64 * 1024)
            var total = 0L
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                if (firstCount < first.size) {
                    val take = minOf(n, first.size - firstCount)
                    System.arraycopy(buffer, 0, first, firstCount, take)
                    firstCount += take
                }
                digest.update(buffer, 0, n)
                total += n
            }
            selectedName = uri.lastPathSegment ?: "selected image"
            val sha = digest.digest().joinToString("") { "%02x".format(it) }
            val magic = first.take(firstCount).joinToString(" ") { "%02X".format(it) }
            selectedInfo = "Name: $selectedName\nSize: $total bytes\nSHA-256: $sha\nFirst bytes: $magic\nInspected only; not validated as an ATJ2127 firmware image."
            appendDetails("\n--- SELECTED IMAGE ---\n$selectedInfo\n")
            Toast.makeText(this, "Image inspected (read-only)", Toast.LENGTH_LONG).show()
        } ?: run { Toast.makeText(this, "Could not open selected file", Toast.LENGTH_LONG).show() }
    }

    private fun exportReport() {
        pendingReport = """ColibriOS DIGMA Z4 Port Lab
Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}

USB devices:
${details.text}

Selected image:
$selectedInfo

Safety: only standard SCSI INQUIRY is implemented. No vendor-specific commands or flash writes.
"""
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "colibri-z4-report.txt")
        }
        startActivityForResult(intent, exportReportCode)
    }
}
