package com.thefans.nokiatools.usage_report_5g

import java.io.InputStream;

var buf : ByteArray = byteArrayOf();

fun catBytes(b1: ByteArray, b2: ByteArray) : ByteArray = b1.plus(b2);

fun getASN1Tag(ins: InputStream) : Int {
	var r : Int = 0;
	// var b = ins.readBytes(1); // This reads all content instead of just 1 byte
	var b = ByteArray(1);
	ins.read(b);
	buf = b;
	if (b[0].toInt().and(0x1F) != 0x1F) return b[0].toInt().and(0x1F);
	else {
		b[0] = 0x80.toByte();
		while(b[0].toInt() != 0) {
			// b = ins.readBytes(1);
			ins.read(b);
			buf = buf.plus(b);
			r = r.shl(7) + b[0].toInt().and(0x7F);
			b[0] = b[0].toInt().and(0x80).toByte();
		}
	} 
	return r;
}

fun getASN1Len(ins: InputStream) : Int {
	var r : Int = 0;
	// var b = ins.readBytes(1); // This reads all content instead of just 1 byte
	var b = ByteArray(1);
	ins.read(b);
	buf = b;
	if (b[0].toInt().and(0x80) == 0) return b[0].toInt();
	else {
		val l: Int = b[0].toInt().and(0x7F);
		for (i in 0 until l) {
			// b = ins.readBytes(1);
			ins.read(b);
			buf = buf.plus(b);
			r = r.shl(8) + b[0].toInt().and(0xFF);
		}
	}
	return r;
}

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

fun toHexString(ba: ByteArray): String {
	var ret = "0x";
    for (b in ba) ret = ret.plus(String.format("%02X", b));
	return ret;
}

fun toHexString(ba: ByteArray, start: Int, end: Int): String {
	var ret = "0x";
    for (i in start until end) ret = ret.plus(String.format("%02X", ba[i]));
	return ret;
}

fun toLong(ba: ByteArray): Long {
	var l: Long = 0;
	l = l.or(ba[0].toLong().and(0xFF));
	for (i in 1 until ba.size) {
		l = l.shl(8);
		l = l.or(ba[i].toLong().and(0xFF));
	}
	return l;
}

fun toLong(ba: ByteArray, start: Int, end: Int): Long {
	var l: Long = 0;
	l = l.or(ba[start].toLong().and(0xFF));
	for (i in start + 1 until end) {
		l = l.shl(8);
		l = l.or(ba[i].toLong().and(0xFF));
	}
	return l;
}
