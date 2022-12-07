package org.newtglobal.dmapextension.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;
import org.newtglobal.dmapextension.dao.DmapExtensionDao;
import org.newtglobal.dmapextension.utility.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ScanDirectory {

	@Autowired
	private Utils utils;

	@Autowired
	private Report report;

	@Autowired
	private CreateModel createModel;


	@Autowired
	private GenerateReportThroughFlask generateReportThroughFlask;

	private static final Logger LOGGER = LoggerFactory.getLogger(ScanDirectory.class);

	@Value("${project_upload_path}")
	private String project_upload_path;

	JSONObject jsonObj = new JSONObject();
	String appNameForJson = "";
	List<String> targetJavaFiles = new ArrayList<String>();

	public File getPath(String appName) {
		String appPath = "";
		File filePath = null;
		try {
			if (!utils.isNull(appName)) {
				appNameForJson = appName;
				appPath = project_upload_path + appName;
			}
			filePath = new File(appPath);
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

		return filePath;
	}

	public List<File> findFiles(File filePath, String fileExtension1, String fileExtension2, String fileExtension3) {
		ArrayList<File> result = new ArrayList<>();
		try {
			File[] files = filePath.listFiles(f -> f.isDirectory() || f.getName().toLowerCase().endsWith(fileExtension1)
					|| f.getName().toLowerCase().endsWith(fileExtension2)
					|| f.getName().toLowerCase().endsWith(fileExtension3));

			if (files != null) {
				for (File file : files) {

					if (file.isDirectory()) {
						result.addAll(findFiles(file, fileExtension1, fileExtension2, fileExtension3));
					} else {
						result.add(file);
					}

				}
			}
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

		return result;
	}

	public JSONObject scanFoldersRecursively(String appName) {

		String fileExtension1 = ".java";
		String fileExtension2 = ".properties";
		String fileExtension3 = ".xml";
		try {
			File dir = getPath(appName);
			List<File> totalfilesFound = findFiles(dir, fileExtension1, fileExtension2, fileExtension3);
			for (File file : totalfilesFound) {
				targetJavaFiles.add(file.getAbsolutePath());
			}

			if (!targetJavaFiles.isEmpty()) {
				createModel.noOfFilesFoundWithQueries = 0;
				createModel.buildModelForEachFile(targetJavaFiles, appNameForJson);
				generateReportThroughFlask.establishConnectionWithFlask(report.jsonObj);

			}
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}
		targetJavaFiles.clear();
		return report.jsonObj;
	}
}
