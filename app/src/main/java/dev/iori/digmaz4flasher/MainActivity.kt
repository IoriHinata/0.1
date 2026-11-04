package dev.iori.digmaz4flasher

import android.app.Activity
import android.app.PendingIntent
import android.content.*
import android.hardware.usb.*
import android.os.Build
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private lateinit var usb: UsbManager
    private lateinit var status: TextView
    private lateinit var details: TextView
    private lateinit var scanButton: Button
    private var selected: UsbDevice? = null
    private val permissionAction = "dev.iori.digmaz4flasher.USB_PERMISSION"

    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != permissionAction) return
            val device = getUsbDevice(intent)
            if (device != null && usb.hasPermission(device)) inspect(device)
            else status.text = "USB permission was not granted. Nothing was written to the player."
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
            setPadding(24, 34, 24, 24)
            setBackgroundColor(Color.rgb(13, 18, 29))
        }
        fun text(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
            text = value; textSize = size; setTextColor(color)
            if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
        }
        root.addView(text("DIGMA Z4", 27f, Color.WHITE, true))
        root.addView(text("OS LAB  •  USB DIAGNOSTICS  •  v0.1.0", 11f, Color.rgb(116, 180, 255)))
        val line = View(this).apply { setBackgroundColor(Color.rgb(44, 58, 80)) }
        root.addView(line, LinearLayout.LayoutParams(-1, 1).apply { topMargin = 20; bottomMargin = 20 })
        root.addView(text("Подключение плеера", 18f, Color.WHITE, true))
        status = text("Ищем USB-устройства…", 14f, Color.LTGRAY)
        root.addView(status, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 12 })
        scanButton = Button(this).apply { text = "Сканировать USB"; setOnClickListener { scan() } }
        root.addView(scanButton, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 16 })
        root.addView(text("Информация об устройстве", 18f, Color.WHITE, true), LinearLayout.LayoutParams(-1, -2).apply { topMargin = 24 })
        details = text("Пока нет данных. Подключи плеер напрямую через OTG-адаптер.", 13f, Color.rgb(190, 202, 220))
        root.addView(details, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 10 })
        val spacer = Space(this)
        root.addView(spacer, LinearLayout.LayoutParams(1, 0, 1f))
        root.addView(text("БЕЗОПАСНЫЙ РЕЖИМ", 12f, Color.rgb(103, 220, 170), true))
        root.addView(text("Версия 0.1 только определяет USB-устройство и читает его дескрипторы. Запись прошивки отключена: сначала требуется проверить точную ревизию, снять резервную копию и подтвердить формат образа.", 12f, Color.rgb(160, 174, 196)))
        setContentView(root)
    }

    private fun scan() {
        val devices = usb.deviceList.values.toList()
        if (devices.isEmpty()) {
            selected = null
            status.text = "USB-устройства не обнаружены. Нужен Android с USB-host/OTG и OTG-кабель."
            details.text = "Подключи DIGMA Z4 включённым. Если он не появится, попробуй другой OTG-переходник или кабель с передачей данных."
            return
        }
        val target = devices.firstOrNull { it.vendorId == 0x10D6 && (it.productId == 0x1101 || it.productId == 0x10D6) }
        val device = target ?: devices.first()
        selected = device
        status.text = if (target != null) "Найдено устройство Actions Semiconductor — возможный режим DIGMA Z4." else "Найдено USB-устройство. Проверяем дескрипторы…"
        if (usb.hasPermission(device)) inspect(device) else {
            val intent = PendingIntent.getBroadcast(this, 0, Intent(permissionAction).setPackage(packageName), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
            usb.requestPermission(device, intent)
        }
    }

    private fun inspect(d: UsbDevice) {
        val out = StringBuilder()
        out.append("USB VID:PID  %04X:%04X\n".format(d.vendorId, d.productId))
        out.append("Имя: ${d.deviceName}\nВерсия USB: ${d.version}\nКласс: ${d.deviceClass}\nИнтерфейсов: ${d.interfaceCount}\n\n")
        for (i in 0 until d.interfaceCount) {
            val intf = d.getInterface(i)
            out.append("Интерфейс $i: class=${intf.interfaceClass}, subclass=${intf.interfaceSubclass}, protocol=${intf.interfaceProtocol}\n")
            for (e in 0 until intf.endpointCount) {
                val ep = intf.getEndpoint(e)
                out.append("  Endpoint ${ep.address}: ${if (ep.direction == UsbConstants.USB_DIR_IN) "IN" else "OUT"}, type=${ep.type}, maxPacket=${ep.maxPacketSize}\n")
            }
        }
        details.text = out.toString()
        status.text = "Устройство определено. Никакие команды записи или изменения памяти не выполнялись."
    }

    override fun onDestroy() {
        try { unregisterReceiver(permissionReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }
}
