package org.newtglobal.dmapextension.utility;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Test {
	public static void main(String args[]) throws IOException {
//		String sourceJavaVersion = getSourceJavaVersion();
//		HashMap<String, String> upgradeDetails = calculateTargetJavaVersion(sourceJavaVersion);
//		System.out.println(upgradeDetails);

		getQueryWithoutExtraSpaces(
				"SELECT id  , firstName  ,   lastName     FROM  employee WHERE lastName LIKE  ' %sa';");
		 //getQueryWithoutExtraSpaces("select * 	from      emp;");
	}

	public static String getQueryWithoutExtraSpaces(String oracleQuery) {
		String item = "";

		
		String[] str1 = oracleQuery.split("\\s+");
		for (int i = 0; i < str1.length; i++) {
			if (i == str1.length - 1) {
				item = item + str1[i];
			} else {
				item = item + str1[i] + " ";
			}

		}
		System.out.println(item);
		System.out.println(item.length());
		return item;

	}

	public static String getSourceJavaVersion() throws IOException {
		String sourceJavaVersion = "";
		final DataInputStream input = new DataInputStream(new FileInputStream(
				"C:\\ArpitBhatnagar\\Material\\Atel- Full code base\\Atel\\IPFMDao\\bin\\th\\co\\ais\\ipfm\\dao\\PlanningDaoImpl.class"));
		// "C:\\ArpitBhatnagar\\DMAP_Extension_Workspace_And_Code\\DMAP_Extension_Code\\DMAP_Extension-Backend\\target\\classes\\org\\newtglobal\\dmapextension\\service\\Report.class"));
		input.skipBytes(4);
		final Integer minorVersion = input.readUnsignedShort();
		final Integer majorVersion = input.readUnsignedShort();
		HashMap<Integer, String> majorVersionToJdkVersion = new HashMap<Integer, String>();
		majorVersionToJdkVersion.put(45, "JDK 1.1");
		majorVersionToJdkVersion.put(46, "JDK 1.2");
		majorVersionToJdkVersion.put(47, "JDK 1.3");
		majorVersionToJdkVersion.put(48, "JDK 1.4");
		majorVersionToJdkVersion.put(49, "J2SE 5");
		majorVersionToJdkVersion.put(50, "Java SE 6");
		majorVersionToJdkVersion.put(51, "Java SE 7");
		majorVersionToJdkVersion.put(52, "Java SE 8");
		majorVersionToJdkVersion.put(53, "Java SE 9");
		majorVersionToJdkVersion.put(54, "Java SE 10");
		majorVersionToJdkVersion.put(55, "Java SE 11");
		majorVersionToJdkVersion.put(56, "Java SE 12");
		majorVersionToJdkVersion.put(57, "Java SE 13");
		majorVersionToJdkVersion.put(58, "Java SE 14");
		majorVersionToJdkVersion.put(59, "Java SE 15");
		majorVersionToJdkVersion.put(60, "Java SE 16");
		majorVersionToJdkVersion.put(61, "Java SE 17");
		majorVersionToJdkVersion.put(62, "Java SE 18");
		majorVersionToJdkVersion.put(63, "Java SE 19");
		for (Map.Entry m : majorVersionToJdkVersion.entrySet()) {
			if (m.getKey() == majorVersion) {

				sourceJavaVersion = m.getValue().toString();
			}
		}
		return sourceJavaVersion;
	}

	public static HashMap<String, String> calculateTargetJavaVersion(String sourceJavaVersion) {
		String targetJavaVersion = "";
		HashMap<String, String> hm = new HashMap<String, String>();
		hm.put("SourceJavaVersion", sourceJavaVersion);

		if (sourceJavaVersion.equals("JDK 1.1") || sourceJavaVersion.equals("JDK 1.2")
				|| sourceJavaVersion.equals("JDK 1.3") || sourceJavaVersion.equals("JDK 1.4")
				|| sourceJavaVersion.equals("J2SE 5") || sourceJavaVersion.equals("Java SE 6")
				|| sourceJavaVersion.equals("Java SE 7")) {
			hm.put("TargetJavaVersion", "Java SE 8");
			hm.put("Upgrade", "Required");
		} else if (sourceJavaVersion.equals("Java SE 8")) {
			hm.put("TargetJavaVersion", "Java SE 8");
			hm.put("Upgrade", "Not Required");
		} else if (sourceJavaVersion.equals("Java SE 9") || sourceJavaVersion.equals("Java SE 10")) {
			hm.put("TargetJavaVersion", "Java SE 11");
			hm.put("Upgrade", "Required");
		} else if (sourceJavaVersion.equals("Java SE 11")) {
			hm.put("TargetJavaVersion", "Java SE 11");
			hm.put("Upgrade", "Not Required");
		} else if (sourceJavaVersion.equals("Java SE 11") || sourceJavaVersion.equals("Java SE 12")
				|| sourceJavaVersion.equals("Java SE 13") || sourceJavaVersion.equals("Java SE 14")
				|| sourceJavaVersion.equals("Java SE 15") || sourceJavaVersion.equals("Java SE 16")) {
			hm.put("TargetJavaVersion", "Java SE 17");
			hm.put("Upgrade", "Required");
		} else if (sourceJavaVersion.equals("Java SE 17")) {
			hm.put("TargetJavaVersion", "Java SE 17");
			hm.put("Upgrade", "Not Required");
		}

		return hm;
	}

}