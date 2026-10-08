package com.salesapp.android.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object Fmt {
    private val rp = NumberFormat.getNumberInstance(Locale("in", "ID"))
    fun rupiah(v: Long): String = "Rp " + rp.format(v)
    fun rupiah(v: Double): String = "Rp " + rp.format(v.toLong())
    fun number(v: Long): String = rp.format(v)
    fun number(v: Double): String = rp.format(v)

    private val tgl = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("in", "ID"))
    fun datetime(ms: Long): String = tgl.format(Date(ms))

    private val tglShort = SimpleDateFormat("dd/MM/yyyy", Locale("in", "ID"))
    fun dateShort(ms: Long): String = tglShort.format(Date(ms))

    val months = listOf("Jan","Feb","Mar","Apr","Mei","Jun","Jul","Agu","Sep","Okt","Nov","Des")
    fun ymLabel(ym: String): String {
        val (y, m) = ym.split("-").let { Pair(it[0], it[1].toInt()) }
        return "${months[m-1]} $y"
    }
    fun currentYm(): String {
        val c = Calendar.getInstance()
        val m = (c.get(Calendar.MONTH)+1).toString().padStart(2,'0')
        return "${c.get(Calendar.YEAR)}-$m"
    }
    fun ymList(backMonths: Int = 11, forwardMonths: Int = 1): List<String> {
        val c = Calendar.getInstance()
        c.add(Calendar.MONTH, -backMonths)
        val list = mutableListOf<String>()
        repeat(backMonths + forwardMonths + 1) {
            val y = c.get(Calendar.YEAR)
            val m = (c.get(Calendar.MONTH)+1).toString().padStart(2,'0')
            list.add("$y-$m")
            c.add(Calendar.MONTH, 1)
        }
        return list.reversed()
    }
    fun ymOf(ms: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = ms }
        val m = (c.get(Calendar.MONTH)+1).toString().padStart(2,'0')
        return "${c.get(Calendar.YEAR)}-$m"
    }
}

val categories = listOf("KSNI", "Simba", "Hello Panda", "Amo")
