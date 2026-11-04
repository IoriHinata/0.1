package dev.iori.colibriz4

import android.app.Activity
import android.content.Intent
import android.hardware.usb.*
import android.os.Bundle
import android.widget.*
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usb = getSystemService(USB_SERVICE) as UsbManager
        render()
        inspectUsb()
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
        root.addView(heading("PORT & INSTALLER LAB · 0.1.0", 12f))
        status = heading("USB: scanning…", 15f)
        root.addView(status, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 20 })
        details = heading("", 12f)
        root.addView(details, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = 12; bottomMargin = 12 })
        root.addView(Button(this).apply { text = "Scan USB device"; setOnClickListener { inspectUsb() } })
        root.addView(Button(this).apply { text = "Select OS / firmware image"; setOnClickListener { chooseImage() } })
        root.addView(Button(this).apply { text = "Export diagnostic report"; setOnClickListener { exportReport() } })
        root.addView(Button(this).apply {
            text = "INSTALL COLIBRIOS — NOT AVAILABLE"
            isEnabled = false
        })
        root.addView(heading("Safety: this build is read-only. Stock x86 ColibriOS cannot run on the ATJ2127 MIPS SoC. Installation remains locked until a compatible MIPS port, verified boot protocol, full backup and recovery path exist.", 12f),
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = 16 })
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun inspectUsb() {
        val devices = usb.deviceList.values
        if (devices.isEmpty()) {
            status.text = "USB: no device detected (connect player through OTG)"
            details.text = "No USB devices are currently visible to Android."
            return
        }
        val report = StringBuilder()
        devices.forEachIndexed { index, d ->
            val isMassStorage = (0 until d.interfaceCount).any {
                d.getInterface(it).interfaceClass == UsbConstants.USB_CLASS_MASS_STORAGE
            }
            report.append("Device #$index\nVID:PID %04X:%04X\n".format(d.vendorId, d.productId))
            report.append("Manufacturer: ${d.manufacturerName ?: "?"}\nProduct: ${d.productName ?: "?"}\n")
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
            report.append("Classification: ${if (isMassStorage) "USB Mass Storage" else "not identified as Mass Storage"}\n\n")
            if (d.vendorId == 0x10D7 && d.productId == 0x1102 && isMassStorage) {
                report.append("WARNING: matches the reported 10D7:1102 Mass Storage identity; NOT confirmed ADFU.\n")
            }
        }
        status.text = "USB: ${devices.size} device(s) detected"
        details.text = report.toString()
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

    private fun inspectImage(uri: android.net.Uri) {
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
            selectedInfo = "Name: $selectedName\nSize: $total bytes\nSHA-256: $sha\nFirst bytes: $magic\n\nInspected only; not validated as an ATJ2127 image."
            details.text = selectedInfo + "\n\n" + details.text
            Toast.makeText(this, "Image inspected (read-only)", Toast.LENGTH_LONG).show()
        }
    }

    private fun exportReport() {
        pendingReport = """ColibriOS DIGMA Z4 Port Lab
Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}

USB:
${details.text}

Selected image:
$selectedInfo

Safety: no flash writes or vendor-specific USB commands were sent.
"""
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "colibri-z4-report.txt")
        }
        startActivityForResult(intent, exportReportCode)
    }
}
