package com.salesapp.android.bluetooth

import com.salesapp.android.data.entities.ProfileEntity
import com.salesapp.android.data.entities.Store
import com.salesapp.android.data.entities.TransactionEntity
import com.salesapp.android.data.entities.TxItem
import com.salesapp.android.util.Fmt
import java.io.ByteArrayOutputStream

/**
 * Build ESC/POS bytes for a 58mm thermal receipt.
 * 58mm paper fits about 32 chars at Font A.
 */
object ReceiptBuilder {
    private const val WIDTH = 32

    private val ESC: Byte = 0x1B
    private val GS: Byte = 0x1D
    private val INIT = byteArrayOf(ESC, 0x40)
    private val ALIGN_LEFT   = byteArrayOf(ESC, 0x61, 0x00)
    private val ALIGN_CENTER = byteArrayOf(ESC, 0x61, 0x01)
    private val BOLD_ON  = byteArrayOf(ESC, 0x45, 0x01)
    private val BOLD_OFF = byteArrayOf(ESC, 0x45, 0x00)
    private val LF = byteArrayOf(0x0A)
    private val FEED3 = byteArrayOf(0x0A, 0x0A, 0x0A)
    private val CUT = byteArrayOf(GS, 0x56, 0x01)

    fun build(
        tx: TransactionEntity,
        store: Store?,
        items: List<TxItem>,
        profile: ProfileEntity?
    ): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(INIT)
        out.write(ALIGN_CENTER)
        out.write(BOLD_ON)
        out.write(line("PT PINUS MERAH ABADI"))
        out.write(BOLD_OFF)
        out.write(line("SALES APP - STRAC"))
        out.write(line(sep('=')))
        out.write(ALIGN_LEFT)

        out.write(line(twoCols("Faktur:", shortId(tx.id))))
        out.write(line(twoCols("Tanggal:", Fmt.datetime(tx.date))))
        if (store != null) {
            out.write(line("Toko  : ${store.name}"))
            if (store.address.isNotBlank()) out.write(line("Alamat: ${trimTo(store.address, WIDTH-8)}"))
        }
        if (profile != null && (profile.name.isNotBlank() || profile.code.isNotBlank())) {
            out.write(line("Sales : ${profile.code} ${profile.name}".trim()))
        }
        out.write(line(sep('-')))

        for (it in items) {
            out.write(line(trimTo(it.name + if (it.discountPerCtn > 0) " *" else "", WIDTH)))
            val qty = qtyLabel(it.ctn, it.box, it.pcs)
            val hargaSub = twoCols("$qty @${Fmt.rupiah(it.price)}", Fmt.rupiah(it.itemFinalNet))
            out.write(line(hargaSub))
        }
        out.write(line(sep('-')))
        out.write(line(twoCols("Total Bruto", Fmt.rupiah(tx.totalBruto))))
        if (tx.totalReguler > 0) out.write(line(twoCols("Disk Reguler", "-" + Fmt.rupiah(tx.totalReguler))))
        if (tx.totalStrata > 0) out.write(line(twoCols("Disk Strata",  "-" + Fmt.rupiah(tx.totalStrata))))
        if (tx.codDiscount > 0) out.write(line(twoCols("Disk COD",     "-" + Fmt.rupiah(tx.codDiscount))))
        out.write(BOLD_ON)
        out.write(line(twoCols("TOTAL BERSIH", Fmt.rupiah(tx.netTotal))))
        out.write(BOLD_OFF)
        out.write(line(sep('=')))
        out.write(ALIGN_CENTER)
        out.write(line("Terima Kasih"))
        out.write(line("Printed by SalesApp"))
        out.write(FEED3)
        out.write(CUT)
        return out.toByteArray()
    }

    fun buildPreviewText(
        tx: TransactionEntity, store: Store?, items: List<TxItem>, profile: ProfileEntity?
    ): String {
        val sb = StringBuilder()
        sb.appendLine("PT PINUS MERAH ABADI".center())
        sb.appendLine("SALES APP - STRAC".center())
        sb.appendLine(sep('='))
        sb.appendLine(twoCols("Faktur:", shortId(tx.id)))
        sb.appendLine(twoCols("Tanggal:", Fmt.datetime(tx.date)))
        store?.let {
            sb.appendLine("Toko  : ${it.name}")
            if (it.address.isNotBlank()) sb.appendLine("Alamat: ${trimTo(it.address, WIDTH-8)}")
        }
        profile?.let {
            if (it.name.isNotBlank() || it.code.isNotBlank())
                sb.appendLine("Sales : ${it.code} ${it.name}".trim())
        }
        sb.appendLine(sep('-'))
        for (it in items) {
            sb.appendLine(trimTo(it.name + if (it.discountPerCtn > 0) " *" else "", WIDTH))
            sb.appendLine(twoCols("${qtyLabel(it.ctn, it.box, it.pcs)} @${Fmt.rupiah(it.price)}", Fmt.rupiah(it.itemFinalNet)))
        }
        sb.appendLine(sep('-'))
        sb.appendLine(twoCols("Total Bruto", Fmt.rupiah(tx.totalBruto)))
        if (tx.totalReguler > 0) sb.appendLine(twoCols("Disk Reguler", "-" + Fmt.rupiah(tx.totalReguler)))
        if (tx.totalStrata > 0) sb.appendLine(twoCols("Disk Strata", "-" + Fmt.rupiah(tx.totalStrata)))
        if (tx.codDiscount > 0) sb.appendLine(twoCols("Disk COD", "-" + Fmt.rupiah(tx.codDiscount)))
        sb.appendLine(twoCols("TOTAL BERSIH", Fmt.rupiah(tx.netTotal)))
        sb.appendLine(sep('='))
        sb.appendLine("Terima Kasih".center())
        sb.appendLine("Printed by SalesApp".center())
        return sb.toString()
    }

    private fun line(s: String): ByteArray = (s + "\n").toByteArray(Charsets.ISO_8859_1)
    private fun sep(c: Char): String = String(CharArray(WIDTH) { c })
    private fun trimTo(s: String, max: Int): String = if (s.length <= max) s else s.substring(0, max)
    private fun shortId(id: String) = id.take(8).uppercase()
    private fun twoCols(left: String, right: String): String {
        val spaces = WIDTH - left.length - right.length
        return if (spaces > 0) left + " ".repeat(spaces) + right
        else trimTo(left, WIDTH - right.length - 1) + " " + right
    }
    private fun qtyLabel(c: Int, b: Int, p: Int): String {
        val parts = mutableListOf<String>()
        if (c > 0) parts += "${c}CTN"
        if (b > 0) parts += "${b}BOX"
        if (p > 0) parts += "${p}PCS"
        return parts.joinToString("+").ifEmpty { "0" }
    }
    private fun String.center(): String {
        if (this.length >= WIDTH) return this.substring(0, WIDTH)
        val pad = (WIDTH - this.length) / 2
        return " ".repeat(pad) + this
    }
}
