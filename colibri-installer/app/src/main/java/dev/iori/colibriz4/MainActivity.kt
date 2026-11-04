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
        root.addView(Button(this).apply { text = "Run installation simulation"; setOnClickListener { runInstallationSimulation() } })
        root.addView(Button(this).apply { text = "Export diagnostic report"; setOnClickListener { exportReport() } })
        root.addView(Button(this).apply {
            text = "INSTALL TO PLAYER — SIMULATION ONLY"
            setOnClickListener { runInstallationSimulation() }
        })
        root.addView(heading("Simulation mode: models the DIGMA Z4 install pipeline. No flash writes are performed.", 12f),
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

    private fun runInstallationSimulation() {
        val log = StringBuilder()
        log.append("COLIBRIOS / DIGMA Z4 — INSTALLATION SIMULATION\n")
        log.append("Mode: DRY RUN / NO DEVICE COMMANDS / NO FLASH WRITES\n\n")
        log.append("[1/8] USB DISCOVERY ..................... OK\n")
        log.append("Replaying reported profile VID:PID 10D7:1102\n")
        log.append("USB Mass Storage / SCSI transparent / Bulk-Only\n")
        log.append("This is a saved diagnostic profile, not proof of a live connection.\n\n")
        log.append("[2/8] TARGET IDENTIFICATION ............ OK (PROFILE)\n")
        log.append("DIGMA Z4 / Actions ATJ2127 / MIPS32-MIPS16 profile\n\n")
        log.append("[3/8] IMAGE INSPECTION ................. ")
        if (selectedInfo == "No image selected") {
            log.append("WAITING FOR IMAGE\nSelect an image to calculate SHA-256 and inspect its header.\n")
        } else {
            log.append("HASHED — FORMAT NOT YET VALIDATED\n")
            log.append(selectedInfo.replace("\n", "\n  "))
            log.append("\n")
        }
        log.append("\n[4/8] ADFU HANDSHAKE ................... NOT RUN\n")
        log.append("10D7:1102 is currently identified as Mass Storage, not confirmed ADFU.\n")
        log.append("No vendor-specific command was sent.\n\n")
        log.append("[5/8] NAND MAP / GEOMETRY .............. UNKNOWN\n")
        log.append("No verified read-only NAND dump or board-specific layout is available.\n\n")
        log.append("[6/8] BACKUP / RESTORE CHECK ........... NOT AVAILABLE\n")
        log.append("No full backup and tested recovery image are available.\n\n")
        log.append("[7/8] KERNEL COMPATIBILITY ............. BLOCKED\n")
        log.append("Stock KolibriOS is x86-compatible; the target profile is MIPS.\n")
        log.append("A MIPS kernel/loader and Z4-specific drivers must be built first.\n\n")
        log.append("[8/8] FLASH / REBOOT ................... SKIPPED\n")
        log.append("No data was written. The player remains unchanged.\n\n")
        log.append("RESULT: SIMULATION COMPLETED; INSTALLATION BLOCKED AT CPU COMPATIBILITY.\n")
        log.append("Next: inspect a genuine factory firmware dump, map the boot chain, then test a minimal MIPS payload.\n")
        details.text = log.toString()
        status.text = "SIMULATION: completed — no writes"
        Toast.makeText(this, "Simulation complete: player unchanged", Toast.LENGTH_LONG).show()
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
