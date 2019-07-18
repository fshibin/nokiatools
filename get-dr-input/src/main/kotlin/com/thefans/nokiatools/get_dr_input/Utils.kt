package com.thefans.nokiatools.get_dr_input

import java.io.InputStream;

fun readBytes(ins: InputStream, len: Int): ByteArray {
	var b = ByteArray(len) { _ -> 0 };
	var rd: Int = 0;
	while (rd < len) {
		val t = ins.read(b, rd, len - rd);
		if (t < 0) throw Exception("$len bytes expected, $rd bytes got.");
		rd += t;
	}
	return b;
}

fun toShort(b: ByteArray): Int {
	var i: Int = 0;
	if (0 < b.size) {
		i = i.or(b[0].toInt().and(0xFF));
	}
	if (1 < b.size) {
		i = i.shl(8);
		i = i.or(b[1].toInt().and(0xFF));
	}
	return i;
}

fun toHexString(ba: ByteArray): String {
	return toHexString(ba, 0, ba.size);
}

fun toHexString(ba: ByteArray, start: Int, end: Int): String {
	var ret = "0x";
	for (i in start until end) ret = ret.plus(String.format("%02X", ba[i]));
	return ret;
}
