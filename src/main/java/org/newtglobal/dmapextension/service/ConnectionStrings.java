package org.newtglobal.dmapextension.service;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.newtglobal.dmapextension.dao.DmapExtensionDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConnectionStrings {

	@Autowired
	private DmapExtensionDao dmapExtensionDao;

	private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionStrings.class);

	public boolean isConnectionStringPresent(String appName) {
		boolean isConnectionStringPresent = false;
		try {
			List<Map<String, Object>> rs = dmapExtensionDao.isConnectionStringPresent(appName);
			if ((boolean) rs.get(0).get("connectiondetails")) {
				isConnectionStringPresent = true;
			}
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

		return isConnectionStringPresent;
	}

	public void remediateConnectionStrings(String dirPath, String appName) {
		String sourceUrl, sourceUser, sourcePwd = "";
		String targetUrl, targetUser, targetPwd = "";
		try {
			List<Map<String, Object>> rs = dmapExtensionDao.getStoredConnectionStrings(appName);
			sourceUrl = rs.get(0).get("sourceurl").toString();
			sourceUser = rs.get(0).get("sourceusername").toString();
			sourcePwd = rs.get(0).get("sourcepassword").toString();
			targetUrl = rs.get(0).get("targeturl").toString();
			targetUser = rs.get(0).get("targetusername").toString();
			targetPwd = rs.get(0).get("targetpassword").toString();

			Scanner sc = new Scanner(new File(dirPath));
			StringBuffer buffer = new StringBuffer();
			while (sc.hasNextLine()) {
				buffer.append(sc.nextLine() + System.lineSeparator());
			}
			String fileContents = buffer.toString();
			sc.close();
			fileContents = fileContents.replaceAll(sourceUrl, targetUrl);
			fileContents = fileContents.replaceAll(sourceUser, targetUser);
			fileContents = fileContents.replaceAll(sourcePwd, targetPwd);

			FileWriter writer = new FileWriter(dirPath);
			writer.append(fileContents);
			writer.flush();
			writer.close();
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

	}

}
