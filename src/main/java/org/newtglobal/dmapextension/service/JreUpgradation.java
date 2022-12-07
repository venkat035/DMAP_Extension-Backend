package org.newtglobal.dmapextension.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Set;
import java.io.PrintStream;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONObject;
import org.newtglobal.dmapextension.utility.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

@Component
public class JreUpgradation {

	@Autowired
	Utils utils;

	@Autowired
	private ScanDirectory scanDirectory;

	@Value("${jre_log_files_by_code}")
	private String jre_log_files_by_code;

	JSONObject jsonObj = new JSONObject();
	Set<String> targetJavaFilesForJreUpgradation = new HashSet<String>();
	static Set<String> targetJavaFilesWarnings = new HashSet<String>();
	List<String> displaylist = new ArrayList<String>();
	static List<HashMap<String, List<String>>> data = new ArrayList<HashMap<String, List<String>>>();

	public JSONObject scanFoldersRecursivelyForJreUpgradation(String appName) throws Exception {
		File dir = scanDirectory.getPath(appName);
		scanFoldersRecursively(dir,appName);
		String reportDate = "";

		reportDate = utils.getAppRunId();

		List<HashMap<String, List<String>>> filterData = data.stream().distinct().collect(Collectors.toList());

		jsonObj.put("ApplicationRunId", reportDate);
		jsonObj.put("ApplicationRunDate", utils.getCreatedDateWithTime()+ " IST");
		jsonObj.put("ApplicationName", appName);
		jsonObj.put("Java_Version", "1.6");
		System.out.println("data =====> " + data);
		jsonObj.put("Data", filterData);
		jsonObj.put("TotalJavaFilesScanned", targetJavaFilesForJreUpgradation.size());
		jsonObj.put("TotalWarningsFound", targetJavaFilesWarnings.size());
	String outfile = jre_log_files_by_code + appName + "_" + reportDate + ".txt";
		
		FileOutputStream stream = new FileOutputStream(outfile);
		System.out.println("ApplicationName=" + appName);
		System.setOut(new PrintStream(stream));
		System.out.println("ApplicationRunId=" + reportDate);
		System.out.println("ApplicationRunDate=" + utils.getCreatedDateWithTime() + " IST");
		System.out.println("Data=" + filterData);
		System.out.println("TotalJavaFilesScanned=" + targetJavaFilesForJreUpgradation.size());
		System.out.println("TotalWarningsFound=" + targetJavaFilesWarnings.size()+" "+"\n");
		System.out.println("Filteres the Warnings/Errors:-");
		List<String> li=Files.lines(Paths.get(outfile))
		.filter(t ->t.contains(",") && t.contains("[") && t.contains("]"))
		.map(t ->t.replaceAll(",", "\n"))
					.collect(Collectors.toList());
		System.out.println(li);
		String filename = jre_log_files_by_code + appName + "_" + reportDate + ".json";
	try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonParser jp = new JsonParser();
            JsonElement je = jp.parse(jsonObj.toJSONString());
            String prettyJsonString = gson.toJson(je);                  
            out.write(prettyJsonString);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return jsonObj;
	}

	public Integer runProcess(String command, String filePath,String appName) throws Exception {
		String line = null;
		StringBuffer output = new StringBuffer();
		Process pro = Runtime.getRuntime().exec(command);
		printLines(command + " stderr:", pro.getErrorStream(), filePath,appName);
		pro.waitFor();
		Integer result = pro.exitValue();
		return result;
	}

	public void printLines(String cmd, InputStream ins, String filePath,String appName) throws Exception {
		List<String> resultedErrorsInFile = new ArrayList<String>();
		List<String> resultedErrorsInFileLineNumbers = new ArrayList<String>();
		List<String> filePathList = new ArrayList<String>();
		List<String> resultedWarningsInFile = new ArrayList<String>();
		List<String> countOccur = new ArrayList<String>();
		HashMap<String, List<String>> eachFileDetails = new HashMap<String, List<String>>();
		String line = null;
		BufferedReader in = new BufferedReader(new InputStreamReader(ins));
		String fileName = FilenameUtils.getName(filePath);
		filePathList.add(fileName);
		String reportDate = "";
		reportDate = utils.getAppRunId();
		while ((line = in.readLine()) != null) {
			if (line.contains("error:") && !line.contains("error: cannot find symbol")
					&& !line.contains("does not exist")) {
				resultedErrorsInFile.add(line);
			} else if (line.contains("warning:") && !line.contains("import") && !line.contains(".class")) {
				resultedWarningsInFile.add(line);
			}
		}

		for (String s : resultedWarningsInFile) {
			String lineNumber = "";
			int index = s.indexOf(".java:");
			int index1 = s.indexOf(": warning: [removal]");
			if (index != -1 && index1 != -1) {
				String val = s.substring(index, index1);
				String[] arr = val.split(":");
				lineNumber = arr[1];
				resultedErrorsInFileLineNumbers.add(lineNumber);
			}
		}

		String occurencesCount = Integer.toString(resultedErrorsInFileLineNumbers.size());
		countOccur.add(occurencesCount);
		if (!resultedWarningsInFile.isEmpty()) {
			if (fileName.endsWith(".java")) {
				eachFileDetails.put("fileName", filePathList);
				eachFileDetails.put("warning", resultedWarningsInFile);
				eachFileDetails.put("lineNumbers", resultedErrorsInFileLineNumbers);
				eachFileDetails.put("occurences", countOccur);
				targetJavaFilesWarnings.addAll(resultedWarningsInFile);
				System.out.println("eachFileDetails" + eachFileDetails);
				data.add(eachFileDetails);
			}
		}
	}

	public JSONObject scanFoldersRecursively(File dir,String appName) throws Exception {
		String fileExtension1 = ".java";
		List<File> totalfilesFound = findFiles(dir, fileExtension1);
		for (File file : totalfilesFound) {
			targetJavaFilesForJreUpgradation.add(file.getAbsolutePath());
		}
		if (!targetJavaFilesForJreUpgradation.isEmpty()) {
			Iterator<String> it = targetJavaFilesForJreUpgradation.iterator();
			while (it.hasNext()) {
				String javaFile = it.next();
				Integer result = runProcess("javac " + javaFile, javaFile,appName);
			}
		}
		return jsonObj;
	}

	public List<File> findFiles(File filePath, String fileExtension1) {
		File[] files = filePath.listFiles(f -> f.isDirectory() || f.getName().toLowerCase().endsWith(fileExtension1));
		ArrayList<File> result = new ArrayList<>();
		if (files != null) {
			for (File file : files) {

				if (file.isDirectory()) {
					result.addAll(findFiles(file, fileExtension1));
				} else {
					result.add(file);
				}

			}
		}
		return result;
	}
}
