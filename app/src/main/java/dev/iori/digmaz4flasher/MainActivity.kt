package dev.iori.digmaz4flasher

import android.app.Activity
import android.app.PendingIntent
import android.content.*
import android.hardware.usb.*
import android.os.Build
import android.os.Bundle
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var usb: UsbManager
    private lateinit var status: TextView
    private lateinit var details: TextView
    private lateinit var saveButton: Button
    private var report = "USB scan not performed."
    private var currentDevice: UsbDevice? = null
    private val permissionAction = "dev.iori.digmaz4flasher.USB_PERMISSION"
    private val createReportRequest = 4102

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
        root.addView(label("OS LAB  •  USB TOOLKIT  •  v0.1.1", 11f, Color.rgb(116, 180, 255)))
        val divider = View(this).apply { setBackgroundColor(Color.rgb(44, 58, 80)) }
        root.addView(divider, LinearLayout.LayoutParams(-1, 1).apply { topMargin = 16; bottomMargin = 18 })

        root.addView(label("Состояние подключения", 18f, Color.WHITE, true))
        status = label("Ищем USB-устройства…", 14f, Color.LTGRAY)
        root.addView(status)
        val scanButton = Button(this).apply {
            text = "Сканировать USB"
            setOnClickListener { scan() }
        }
        root.addView(scanButton, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 8 })

        root.addView(label("Данные устройства", 18f, Color.WHITE, true).apply {
            setPadding(0, 20, 0, 5)
        })
        details = label("Подключи плеер через OTG-адаптер с поддержкой передачи данных.", 13f, Color.rgb(190, 202, 220))
        root.addView(details)

        saveButton = Button(this).apply {
            text = "Сохранить диагностический отчёт"
            isEnabled = false
            setOnClickListener { saveReport() }
        }
        root.addView(saveButton, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 12 })

        root.addView(label("Что умеет эта версия", 18f, Color.WHITE, true).apply {
            setPadding(0, 22, 0, 5)
        })
        root.addView(label(
            "• Находит USB-устройства через Android USB Host\n" +
            "• Запрашивает разрешение на доступ\n" +
            "• Читает USB-дескрипторы, интерфейсы и endpoints\n" +
            "• Сохраняет отчёт в TXT для дальнейшего анализа\n" +
            "• Не отправляет неизвестные команды и не меняет память плеера",
            13f, Color.rgb(190, 202, 220)
        ))

        root.addView(label("Важно", 18f, Color.rgb(255, 190, 110), true).apply {
            setPadding(0, 20, 0, 5)
        })
        root.addView(label(
            "Это диагностическая версия, а не прошивальщик. Для установки ОС нужны проверенные данные о ревизии чипа, загрузчике, RAM, разметке flash и способе восстановления. Не используйте случайные firmware-файлы.",
            12f, Color.rgb(160, 174, 196)
        ))

        val scroll = ScrollView(this).apply { isFillViewport = true; addView(root) }
        setContentView(scroll)
    }

    private fun scan() {
        val devices = usb.deviceList.values.toList()
        if (devices.isEmpty()) {
            currentDevice = null
            status.text = "USB-устройства не обнаружены. Проверь OTG и кабель с передачей данных."
            details.text = "Подключи DIGMA Z4 включённым. Если он не появился, проверь, поддерживает ли телефон USB Host."
            report = "DIGMA Z4 OS Lab diagnostic report\nNo USB devices detected.\nDate: ${Date()}\n"
            saveButton.isEnabled = true
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
        out.append("Manufacturer: ${d.manufacturerName ?: "not provided by USB descriptor"}\n")
        out.append("Product: ${d.productName ?: "not provided by USB descriptor"}\n\n")
        for (i in 0 until d.interfaceCount) {
            val intf = d.getInterface(i)
            out.append("Interface $i\n")
            out.append("  class=${intf.interfaceClass}, subclass=${intf.interfaceSubclass}, protocol=${intf.interfaceProtocol}\n")
            out.append("  endpoints=${intf.endpointCount}\n")
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
                out.append("    endpoint address=0x${ep.address.toString(16)}, direction=$direction, type=$type, maxPacket=${ep.maxPacketSize}\n")
            }
        }
        out.append("\nSafety: no vendor commands sent; no flash or RAM writes performed.\n")
        report = out.toString()
        details.text = report
        status.text = if (d.vendorId == 0x10D6) {
            "Обнаружен USB VID Actions Semiconductor (0x10D6). Это ещё не подтверждает режим загрузчика или точную модель."
        } else {
            "USB-устройство обнаружено. VID:PID %04X:%04X".format(d.vendorId, d.productId)
        }
        saveButton.isEnabled = true
    }

    private fun saveReport() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "digma-z4-usb-report.txt")
        }
        startActivityForResult(intent, createReportRequest)
    }

    @Deprecated("Deprecated by Android; retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == createReportRequest && resultCode == RESULT_OK) {
            val uri: Uri = data?.data ?: return
            try {
                contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(report) }
                status.text = "Отчёт сохранён. Его можно отправить для анализа USB-протокола."
            } catch (e: Exception) {
                status.text = "Не удалось сохранить отчёт: ${e.message}"
            }
        }
    }

    override fun onDestroy() {
        try { unregisterReceiver(permissionReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }
}
