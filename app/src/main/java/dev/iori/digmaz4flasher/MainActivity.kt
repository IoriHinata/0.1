package dev.iori.digmaz4flasher

import android.app.Activity
import android.app.PendingIntent
import android.content.*
import android.hardware.usb.*
import android.os.Build
import android.os.Bundle
import android.graphics.Color
import android.net.Uri
import android.provider.OpenableColumns
import android.view.View
import android.widget.*
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var usb: UsbManager
    private lateinit var status: TextView
    private lateinit var details: TextView
    private lateinit var imageDetails: TextView
    private lateinit var saveButton: Button
    private var report = "USB scan not performed."
    private var imageReport = "No image selected."
    private var selectedImageUri: Uri? = null
    private var currentDevice: UsbDevice? = null
    private val permissionAction = "dev.iori.digmaz4flasher.USB_PERMISSION"
    private val createReportRequest = 4102
    private val selectImageRequest = 4103

    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != permissionAction) return
            val device = getUsbDevice(intent)
            if (device != null && usb.hasPermission(device)) inspect(device)
            else status.text = "USB permission was not granted. The player was not modified."
        }
    }

    @Suppress("DEPRECATION")
    private fun getUsbDevice(intent: Intent): UsbDevice? =
        if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        else intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usb = getSystemService(USB_SERVICE) as UsbManager
        window.statusBarColor = Color.rgb(13, 18, 29)
        window.navigationBarColor = Color.rgb(13, 18, 29)
        buildUi()
        val filter = IntentFilter(permissionAction)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(permissionReceiver, filter, RECEIVER_NOT_EXPORTED)
        else registerReceiver(permissionReceiver, filter)
        scan()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 28, 22, 24)
            setBackgroundColor(Color.rgb(13, 18, 29))
        }
        fun label(value: String, size: Float, color: Int, bold: Boolean = false) =
            TextView(this).apply {
                text = value
                textSize = size
                setTextColor(color)
                if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 5, 0, 5)
            }

        root.addView(label("DIGMA Z4", 28f, Color.WHITE, true))
        root.addView(label("OS LAB  •  IMAGE INSPECTOR  •  v0.2.0", 11f, Color.rgb(116, 180, 255)))
        val divider = View(this).apply { setBackgroundColor(Color.rgb(44, 58, 80)) }
        root.addView(divider, LinearLayout.LayoutParams(-1, 1).apply { topMargin = 16; bottomMargin = 18 })

        root.addView(label("1. Подключение плеера", 18f, Color.WHITE, true))
        status = label("Ищем USB-устройства…", 14f, Color.LTGRAY)
        root.addView(status)
        root.addView(Button(this).apply {
            text = "Сканировать USB"
            setOnClickListener { scan() }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 8 })

        root.addView(label("Данные устройства", 16f, Color.WHITE, true).apply { setPadding(0, 14, 0, 5) })
        details = label("Подключи плеер через OTG-адаптер с поддержкой передачи данных.", 13f, Color.rgb(190, 202, 220))
        root.addView(details)

        root.addView(label("2. Образ операционной системы", 18f, Color.WHITE, true).apply { setPadding(0, 22, 0, 5) })
        root.addView(label("Выбери файл прошивки или образ диска из памяти телефона.", 13f, Color.rgb(190, 202, 220)))
        root.addView(Button(this).apply {
            text = "Выбрать образ (.bin, .img, .fw, .zip и др.)"
            setOnClickListener { chooseImage() }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 8 })
        imageDetails = label("Образ не выбран.", 13f, Color.rgb(190, 202, 220))
        root.addView(imageDetails)

        root.addView(label("3. Установка", 18f, Color.WHITE, true).apply { setPadding(0, 22, 0, 5) })
        root.addView(label(
            "ЗАПИСЬ ЗАБЛОКИРОВАНА: для DIGMA Z4 пока не подтверждены точная ревизия платы, протокол загрузчика, карта flash и способ восстановления. Приложение не будет стирать или записывать память наугад.",
            13f, Color.rgb(255, 190, 110)
        ))
        root.addView(Button(this).apply {
            text = "Установка ОС пока недоступна — нет проверенного протокола"
            isEnabled = false
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 8 })

        saveButton = Button(this).apply {
            text = "Сохранить диагностический отчёт"
            isEnabled = false
            setOnClickListener { saveReport() }
        }
        root.addView(saveButton, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 12 })

        root.addView(label("Безопасность", 16f, Color.WHITE, true).apply { setPadding(0, 22, 0, 5) })
        root.addView(label(
            "Выбор файла не означает, что он совместим с плеером. Расширение и контрольная сумма SHA-256 помогают идентифицировать файл, но не доказывают, что образ загрузочный или предназначен для этой платы. Никакие данные в плеер не отправляются.",
            12f, Color.rgb(160, 174, 196)
        ))

        setContentView(ScrollView(this).apply { isFillViewport = true; addView(root) })
    }

    private fun chooseImage() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/octet-stream", "application/zip", "application/x-iso9660-image",
                "application/x-raw-disk-image", "application/x-firmware", "application/gzip"
            ))
        }
        startActivityForResult(intent, selectImageRequest)
    }

    private fun inspectImage(uri: Uri) {
        selectedImageUri = uri
        imageDetails.text = "Читаю файл и считаю SHA-256…"
        Thread {
            try {
                val name = queryDisplayName(uri) ?: "unknown"
                val size = querySize(uri)
                val digest = MessageDigest.getInstance("SHA-256")
                val head = ByteArray(16)
                var headCount = 0
                var total = 0L
                contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "Не удалось открыть файл" }
                    val buffer = ByteArray(1024 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (headCount < head.size) {
                            val copy = minOf(count, head.size - headCount)
                            System.arraycopy(buffer, 0, head, headCount, copy)
                            headCount += copy
                        }
                        digest.update(buffer, 0, count)
                        total += count
                    }
                }
                val sha = digest.digest().joinToString("") { "%02x".format(it) }
                val type = detectType(head.copyOf(headCount))
                val sizeText = if (size >= 0) "$size bytes (read: $total)" else "$total bytes"
                val result = "Файл: $name\nРазмер: $sizeText\nТип по сигнатуре: $type\nSHA-256: $sha\n\nПроверка: файл прочитан полностью. Совместимость с DIGMA Z4 не подтверждена."
                imageReport = result
                runOnUiThread {
                    imageDetails.text = result
                    saveButton.isEnabled = true
                    status.text = "Образ проверен на чтение. Запись в плеер не выполнялась."
                }
            } catch (e: Exception) {
                runOnUiThread {
                    imageReport = "Image inspection failed: ${e.message}"
                    imageDetails.text = imageReport
                    status.text = "Не удалось прочитать выбранный образ."
                }
            }
        }.start()
    }

    private fun queryDisplayName(uri: Uri): String? =
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }

    private fun querySize(uri: Uri): Long {
        return contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else -1L
        } ?: -1L
    }

    private fun detectType(bytes: ByteArray): String {
        fun starts(vararg values: Int) = bytes.size >= values.size && values.indices.all { (bytes[it].toInt() and 0xff) == values[it] }
        return when {
            starts(0x7f, 0x45, 0x4c, 0x46) -> "ELF executable"
            starts(0x50, 0x4b, 0x03, 0x04) -> "ZIP archive"
            starts(0x1f, 0x8b) -> "GZIP archive"
            starts(0x3a, 0xff, 0x26, 0xed) -> "Android sparse image"
            starts(0x4d, 0x5a) -> "DOS/PE executable header"
            starts(0x53, 0x51, 0x53, 0x48) -> "SquashFS filesystem"
            else -> "Unknown/raw binary (signature not recognized)"
        }
    }

    private fun scan() {
        val devices = usb.deviceList.values.toList()
        if (devices.isEmpty()) {
            currentDevice = null
            status.text = "USB-устройства не обнаружены. Проверь OTG и кабель с передачей данных."
            details.text = "Подключи DIGMA Z4 включённым. Если он не появился, проверь поддержку USB Host телефоном."
            report = "DIGMA Z4 OS Lab diagnostic report\nNo USB devices detected.\nDate: ${Date()}\n"
            saveButton.isEnabled = selectedImageUri != null
            return
        }
        val device = devices.firstOrNull { it.vendorId == 0x10D6 } ?: devices.first()
        currentDevice = device
        if (usb.hasPermission(device)) inspect(device) else {
            status.text = "Найдено USB-устройство. Запрашиваем разрешение Android…"
            val intent = PendingIntent.getBroadcast(
                this, 0, Intent(permissionAction).setPackage(packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            usb.requestPermission(device, intent)
        }
    }

    private fun inspect(d: UsbDevice) {
        val out = StringBuilder()
        out.append("DIGMA Z4 OS Lab — USB diagnostic report\n")
        out.append("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}\n\n")
        out.append("USB VID:PID: %04X:%04X\n".format(d.vendorId, d.productId))
        out.append("Device path: ${d.deviceName}\n")
        out.append("USB version: ${d.version}\n")
        out.append("Device class: ${d.deviceClass}\n")
        out.append("Device subclass: ${d.deviceSubclass}\n")
        out.append("Device protocol: ${d.deviceProtocol}\n")
        out.append("Interface count: ${d.interfaceCount}\n")
        out.append("Manufacturer: ${d.manufacturerName ?: "not provided"}\n")
        out.append("Product: ${d.productName ?: "not provided"}\n\n")
        for (i in 0 until d.interfaceCount) {
            val intf = d.getInterface(i)
            out.append("Interface $i: class=${intf.interfaceClass}, subclass=${intf.interfaceSubclass}, protocol=${intf.interfaceProtocol}, endpoints=${intf.endpointCount}\n")
            for (e in 0 until intf.endpointCount) {
                val ep = intf.getEndpoint(e)
                val direction = if (ep.direction == UsbConstants.USB_DIR_IN) "IN" else "OUT"
                val type = when (ep.type) {
                    UsbConstants.USB_ENDPOINT_XFER_CONTROL -> "CONTROL"
                    UsbConstants.USB_ENDPOINT_XFER_ISOC -> "ISOCHRONOUS"
                    UsbConstants.USB_ENDPOINT_XFER_BULK -> "BULK"
                    UsbConstants.USB_ENDPOINT_XFER_INT -> "INTERRUPT"
                    else -> "UNKNOWN"
                }
                out.append("  endpoint=0x${ep.address.toString(16)}, $direction, $type, maxPacket=${ep.maxPacketSize}\n")
            }
        }
        out.append("\nSafety: no vendor commands; no flash/RAM writes.\n")
        report = out.toString()
        details.text = report
        status.text = if (d.vendorId == 0x10D6) {
            "USB VID Actions Semiconductor (0x10D6) найден. Режим загрузчика и модель не подтверждены."
        } else "USB-устройство найдено: %04X:%04X".format(d.vendorId, d.productId)
        saveButton.isEnabled = true
    }

    private fun saveReport() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "digma-z4-os-lab-report.txt")
        }
        startActivityForResult(intent, createReportRequest)
    }

    @Deprecated("Deprecated by Android; retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        when (requestCode) {
            selectImageRequest -> {
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {}
                inspectImage(uri)
            }
            createReportRequest -> {
                try {
                    val combined = report + "\n\nIMAGE REPORT\n" + imageReport + "\n"
                    contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(combined) }
                    status.text = "Диагностический отчёт сохранён."
                } catch (e: Exception) {
                    status.text = "Не удалось сохранить отчёт: ${e.message}"
                }
            }
        }
    }

    override fun onDestroy() {
        try { unregisterReceiver(permissionReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }
}
