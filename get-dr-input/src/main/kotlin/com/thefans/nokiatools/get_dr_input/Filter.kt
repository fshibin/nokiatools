package com.thefans.nokiatools.get_dr_input

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

var filtered: Int = 0;
var outputted: Int = 0;
var outputCMG: Boolean = false;
var outputFNG: Boolean = false;
var outputFNS: Boolean = false;

fun printUsage() {
	println("Usage: java -jar <path>" + System.getProperties().getProperty("file.separator") + "getdrinput.jar [-cmg] [-fng] [-fns] RawDataFile");
	println("This tool extracts CDRs CMCC/CUC expected by Data Refinery EPC streams with GTP' headers removed.");
	println("-cmg: only CMG CDRs (SGW-CDR, PGW-CDR) will be extracted.");
	println("-fng: only FNG CDRs (SGW-CDR, PGW-CDR) will be extracted.");
	println("-fns: only FNS CDRs (SGSN-CDR) will be extracted.");
	println("If none is specified, use -cmg by default.");
	println("By Shibin FAN, copyright 2019-2025 Nokia Networks.");
}

fun main(args: Array<String>) {
	if (args.size < 1 || args.size > 4) {
		printUsage();
		System.exit(0);
	}

	var li: Int = args.size - 1;

	if ("-cmg".equals(args[li], true)) {
		printUsage();
		System.exit(0);
	}

	if ("-fng".equals(args[li], true)) {
		printUsage();
		System.exit(0);
	}

	if ("-fns".equals(args[li], true)) {
		printUsage();
		System.exit(0);
	}

	if (args.size == 1) outputCMG = true;
	else {
		for (i in 0 until li) {
			if ("-cmg".equals(args[i], true)) outputCMG = true;
			else if ("-fng".equals(args[i], true)) outputFNG = true;
			else if ("-fns".equals(args[i], true)) outputFNS = true;
			else {
				printUsage();
				System.exit(0);
			}
		}
	}

	val f: File = File(args[li]);
	if (f.isFile()) filterCdrs(f);
	println(String.format("%s: total %8d CDRs, filtered %8d, outputted %8d.", args[li], (filtered + outputted), filtered, outputted));
}

fun filterCdrs(f: File) {
	var fis: FileInputStream;
	var fos: FileOutputStream? = null;
	var fos2: FileOutputStream? = null;
	try {
		fis = FileInputStream(f);
	} catch (e: FileNotFoundException) {
		println("Unable to open " + f.getAbsolutePath());
		return;
	}
	
	try {
		while (fis.available() > 0) {

			readBytes(fis, 1); // flags
			readBytes(fis, 1); // msgType
			readBytes(fis, 2); // msgLength; TODO: 1
			readBytes(fis, 2); // seqNumber
			readBytes(fis, 2); // transferCmd

			readBytes(fis, 1); // dummy
			readBytes(fis, 2); // length; TODO: 2
			val recordNumber = readBytes(fis, 1);
			val recordFormat = readBytes(fis, 1);
			val formatVersion = readBytes(fis, 2);


			for (i in 0 until recordNumber[0]) {
				val cdrLength = readBytes(fis, 2); // TODO: 3
				val cdr = readBytes(fis, toShort(cdrLength));

				var shouldOutput: Boolean = false;

				if (recordFormat[0] == 0x01.toByte()) { // ASN.1
					if (outputCMG && toHexString(formatVersion).equals("0x1b09", true) // -	cMG: 0x1b09 (release 11.8.0)
							&& ((cdr[0] == 0xbf.toByte() && cdr[1] == 0x4e.toByte())
									|| (cdr[0] == 0xbf.toByte() && cdr[1] == 0x4f.toByte()))) // SGW-CDR or PGW-CDR
						shouldOutput = true;
					if (outputCMG && toHexString(formatVersion).equals("0x1d07", true) // -	cMG: 0x0d07 (release 13.6.0)
							&& ((cdr[0] == 0xbf.toByte() && cdr[1] == 0x4e.toByte())
									|| (cdr[0] == 0xbf.toByte() && cdr[1] == 0x4f.toByte()))) // SGW-CDR or PGW-CDR
						shouldOutput = true;
					if (outputFNG && toHexString(formatVersion).equals("0x1b0a", true) // -	Flexi NG: 0x1b0a (release 11.9.0)
							&& ((cdr[0] == 0xbf.toByte() && cdr[1] == 0x4e.toByte())
									|| (cdr[0] == 0xbf.toByte() && cdr[1] == 0x4f.toByte()))) // SGW-CDR or PGW-CDR
						shouldOutput = true;
					if (outputFNS && toHexString(formatVersion).equals("0x1705", true) // -	Flexi NS: 0x1705 (release 7.4.0)
							&& cdr[0] == 0xb4.toByte()) // SGSN-CDR
						shouldOutput = true;
				}

				if (shouldOutput) {
					if (fos == null) {
						fos = FileOutputStream(f.getAbsolutePath().plus(".ber"));
					}
					fos.write(cdr);
					outputted++;
				} else {
					if (fos2 == null) {
						fos2 = FileOutputStream(f.getAbsolutePath().plus(".ber.filtered"));
					}
					fos2.write(cdr);
					filtered++;
				}
			}

		}
	} catch (e: Exception) {
		e.printStackTrace();
	} finally {
		try {
			fis.close();
			if (fos != null) fos.close();
			if (fos2 != null) fos2.close();
		} catch (e: Exception) {
			e.printStackTrace();
		}
	}
}
