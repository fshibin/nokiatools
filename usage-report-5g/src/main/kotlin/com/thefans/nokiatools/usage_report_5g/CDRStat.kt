package com.thefans.nokiatools.usage_report_5g

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class CDRStat {
	static boolean needWarning = false;
	static Set<String> subs = new HashSet<String>();
	static FileOutputStream fos = null;
	static long totUplink, totDownlink;

	public static void main(String[] args) {
		if (args.length < 1) {
			printUsage();
			System.exit(0);
		}

		try {
			fos = new FileOutputStream("bjasn1stat.csv");
			String out = "msisdn" + "\n";
			try {
				fos.write(out.getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		System.out.println("统计PGW话单中都有哪些用户（去重后）并把用户列表写入文件");

		for (int i = 0; i < args.length; i++) {
			File f = new File(args[i]);
			if (f.isFile()) {
				System.out.print("Decoding " + args[i] + "...");
				statCdr(f);
				System.out.println("done");
			}
		}
		System.out.print("Writing subscriber list into bjasn1stat.csv...");
		for (String out : subs) {
			out += "\n";
			try {
				fos.write(out.getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("done");
		System.out.println("Subscriber#: " + subs.size() + ", totUplink: " + totUplink + ", totDownlink: " + totDownlink);

		if (needWarning) {
			System.out.println("Unknown CDR types found. This tool can only support PGW-CDRs.");
		}
	}

	public static void statCdr(File f) {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(f);
		} catch (FileNotFoundException e) {
			System.out.println("Unable to open " + f.getAbsolutePath());
			return;
		}
		try {
			while (fis.available() > 0) {
				Common.getASN1Tag(fis);
				int len = Common.getASN1Len(fis);
				byte[] buf = Common.readBytes(fis, len);
				statCdr(buf);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				fis.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void statCdr(byte[] buf) {
		long recType = 0;
		ByteArrayInputStream bais = new ByteArrayInputStream(buf);
		while (bais.available() > 0) {
			try {
				int tag = Common.getASN1Tag(bais);
				int len = Common.getASN1Len(bais);
				byte[] val = Common.readBytes(bais, len);
				if (tag == 0) {
					recType = Common.toLong(val);
				} else {
					if (recType == 85) { // PGW-CDR
						if (tag == 22) { // servedMSISDN
							String msisdn = swapMSISDN(Common.toHexString(val).substring(2));
							subs.add(msisdn);
						} else if (tag == 34) { // listOfServiceData
							ByteArrayInputStream bais5 = new ByteArrayInputStream(val);

							long uplink = 0, downlink = 0;

							while (bais5.available() > 0) {
								tag = Common.getASN1Tag(bais5); // 16
								len = Common.getASN1Len(bais5);
								val = Common.readBytes(bais5, len);

								ByteArrayInputStream bais6 = new ByteArrayInputStream(val);
								while (bais6.available() > 0) {
									tag = Common.getASN1Tag(bais6);
									len = Common.getASN1Len(bais6);
									val = Common.readBytes(bais6, len);

									if (tag == 12) { // uplinkVolume
										uplink += Common.toLong(val);
									}
									if (tag == 13) { // downlinkVolume
										downlink += Common.toLong(val);
									}
								}
							}
							totUplink += uplink;
							totDownlink += downlink;
						}
					} else {
						needWarning = true;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	static String swapMSISDN(String in) {
		String out = "";
		int i = 0;
		while (i < in.length()) {
			if (i + 1 < in.length()) out += in.substring(i + 1, i + 2);
			String tmp = in.substring(i, i + 1);
			if (!"F".equalsIgnoreCase(tmp)) out += tmp;
			i = i + 2;
		}
		return out;
	}

	public static void printUsage() {
		System.out.println("Usage: java -jar <path>" + System.getProperties().getProperty("file.separator") + "bjasn1stat.jar files...");
		System.out.println("By Shipley FAN, copyright 2014-2019 Nokia Networks.");
	}
}

