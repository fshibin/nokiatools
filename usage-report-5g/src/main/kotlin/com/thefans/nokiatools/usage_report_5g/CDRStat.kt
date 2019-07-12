package com.thefans.nokiatools.usage_report_5g;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;

private var needWarning = false;

fun main(args: Array<String>) {
	if (args.size < 1) {
		printUsage();
		System.exit(-1);
	}
	val headers = "MSISDN,chargingID,ECI,startTime,endTime,uplinkVolume,downlinkVolume";
	println(headers);

	for (i in 0 until args.size) {
		val f = File(args[i]);
		if (f.isFile()) processOneFile(f);
	}

	if (needWarning) println("Unknown CDR types found. This tool only works with PGW-CDRs.");
}

fun printUsage() {
	println("Usage: java -jar <path>" + System.getProperties().getProperty("file.separator") + "usgrep5g.jar files...");
	println("By Shibin FAN, copyright 2019-2025 Nokia Networks.");
}

fun processOneFile(f: File) {
	var fis: FileInputStream;
	try {
		fis = FileInputStream(f);
	} catch (e: FileNotFoundException) {
		System.err.println("Unable to open " + f.getAbsolutePath());
		return;
	}
	try {
		while (fis.available() > 0) {
			getASN1Tag(fis);
			val len = getASN1Len(fis);
			val buf = readBytes(fis, len);
			processOneCdr(buf);
		}
	} catch (e: Exception) {
		e.printStackTrace();
	} finally {
		try {
			fis.close();
		} catch (e: Exception) {
			e.printStackTrace();
		}
	}
}

fun processOneCdr(buf: ByteArray) {
	var recType: Int = 0; // tag 0
	var chargingId: Long = 0; // tag 5
	var msisdn: String = ""; // tag 22
	var uli = byteArrayOf(); // tag 32

	data class UsageReport5G (var startTime: String, var endTime: String, var uplinkVolume: Long, var downlinkVolume: Long);
	var ur5List = ArrayList<UsageReport5G>(); // tag 73

	val bais = ByteArrayInputStream(buf);
	while (bais.available() > 0) {
		try {
			var tag = getASN1Tag(bais);
			var len = getASN1Len(bais);
			var zhi = readBytes(bais, len);
			if (tag == 0) { // always the first field
				recType = toLong(zhi).toInt();
			} else {
				if (recType == 85) { // PGW-CDR
					if (tag == 5) {
						chargingId = toLong(zhi);
					} else if (tag == 22) { // servedMSISDN
						msisdn = swapMSISDN(toHexString(zhi).substring(2));
					} else if (tag == 32) { // userLocationInformation
						uli = zhi;
					} else if (tag == 73) { // listOfRANSecondaryRATUsageReports
						val bais2 = ByteArrayInputStream(zhi);

						while (bais2.available() > 0) {
							tag = getASN1Tag(bais2); // 16
							len = getASN1Len(bais2);
							zhi = readBytes(bais2, len);
							
							if (tag != 16) {
								System.err.println("Wrong tag instead of 16 - sequence of, is found.");
								System.exit(-1);
							}

							var startTime = ""; var endTime = "";
							var uplinkVolume = 0L; var downlinkVolume = 0L;

							val bais3 = ByteArrayInputStream(zhi);
							while (bais3.available() > 0) {
								tag = getASN1Tag(bais3);
								len = getASN1Len(bais3);
								zhi = readBytes(bais3, len);

								if (tag == 1) { // dataVolumeUplink
									uplinkVolume = toLong(zhi);
								} else if (tag == 2) { // dataVolumeDownlink
									downlinkVolume = toLong(zhi);
								} else if (tag == 3) { // rANStartTime
									startTime = formatTimeStamps(zhi);
								} else if (tag == 4) { // rANEndTime
									endTime = formatTimeStamps(zhi);
								}
							}
							val ur5 = UsageReport5G(startTime, endTime, uplinkVolume, downlinkVolume);
							ur5List.add(ur5);
						}
					}
				} else {
					needWarning = true;
				}
			}
		} catch (e: Exception) {
			e.printStackTrace();
		}
	}
	// TODO: extract ECI from uli
	for (ur5 in ur5List) {
		val S = ",";
		var out = "".plus(chargingId).plus(S).plus(msisdn).plus(S).plus(extractECIFromUserLocationInfo(uli)).plus(S);
		out = out.plus(ur5.startTime).plus(S).plus(ur5.endTime).plus(S).plus(ur5.uplinkVolume).plus(S).plus(ur5.downlinkVolume);
		System.out.println(out);
	}
}

fun swapMSISDN(ins: String): String {
	var out = "";
	var i = 0;
	while (i < ins.length) {
		if (i + 1 < ins.length) out += ins.substring(i + 1, i + 2);
		val tmp = ins.substring(i, i + 1);
		if (!"F".equals(tmp, true)) out += tmp;
		i = i + 2;
	}
	return out;
}

// TODO: further format time stamp according to requirement
fun formatTimeStamps(zhi: ByteArray): String {
	var ts = toHexString(zhi).substring(2);
	ts = ts.substring(0, ts.length - 6); // remove time zone like 2b0000
	return ts;
}

// TODO: further format ECI according to requirement
fun extractECIFromUserLocationInfo(uli: ByteArray): String {
	if (uli.size == 0) return "";
	var eci = "";
	// 1st byte: Geographic location type 
	var bit1 = (uli[0].toInt().and(0x01)) > 0; // CGI, 7 bytes
	var bit2 = (uli[0].toInt().and(0x02)) > 0; // SAI, 7 bytes
	var bit3 = (uli[0].toInt().and(0x04)) > 0; // RAI, 7 bytes
	var bit4 = (uli[0].toInt().and(0x08)) > 0; // TAI, 5 bytes
	var bit5 = (uli[0].toInt().and(0x10)) > 0; // ECGI, 7 bytes

	if (bit5) {
		var pos = 1; // from 2nd byte
		pos += (if (bit1) 7 else 0);
		pos += (if (bit2) 7 else 0);
		pos += (if (bit3) 7 else 0);
		pos += (if (bit4) 5 else 0);
		// ECGI = 7 bytes (3 bytes for location area and 4 bytes for ECI [actually 3 and a half...]). 
		pos += 3;
		eci = "" + toLong(uli, pos, pos + 4);
		// eci = toHexString(uli, pos, pos + 4).substring(2);
	}

	return eci;
}
