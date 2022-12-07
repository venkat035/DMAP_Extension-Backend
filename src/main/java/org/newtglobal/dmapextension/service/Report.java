package org.newtglobal.dmapextension.service;

import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.simple.JSONObject;
import org.springframework.stereotype.Component;
import org.newtglobal.dmapextension.controller.DmapExtensionController;
import org.newtglobal.dmapextension.dao.DmapExtensionDao;
import org.newtglobal.dmapextension.utility.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class Report {

	@Autowired
	private Utils utils;
	
	@Autowired
	private DmapExtensionDao dmapExtensionDao;

	JSONObject jsonObj = new JSONObject();

	private static final Logger LOGGER = LoggerFactory.getLogger(Report.class);

	public void prepareFileJson(String appName, int noOfFilesFound,
			ArrayList<Map<String, String>> fileDetails, int noOfFilesFoundWithQueries,List<List<Map<String,String>>> conditionalQueryDetailsList,List<HashMap<String, List<String>>> unUsedList,int noofUnusedfiles) {
		try {
			LOGGER.info("File detail JSON before report --> " + fileDetails);
			String noOfFilesFoundString = Integer.toString(noOfFilesFound);
			List<Map<String, Object>> rs = dmapExtensionDao.getAppRunIdFromDiscoveryTable(appName);
			jsonObj.put("applicationRunId", rs.get(0).get("applicationrunid").toString());
			jsonObj.put("applicationRunDate", utils.getCreatedDateWithTime() + " IST");
			jsonObj.put("applicationName", appName);
			jsonObj.put("javaVersion", "8"); // TODO: Need to make this dynamic
			jsonObj.put("fileDetails", fileDetails);
			jsonObj.put("Data",unUsedList);
			jsonObj.put("noOfFilesFound", noOfFilesFoundString);
			jsonObj.put("noOfFilesFoundWithDeadcode", noofUnusedfiles);
			jsonObj.put("noOfFilesFoundWithQueries", noOfFilesFoundWithQueries);
			jsonObj.put("conditionalquerydetails", conditionalQueryDetailsList);
			LOGGER.info("File detail JSON before report --> " + noOfFilesFoundString);
			LOGGER.info("File unUsedList --> " + unUsedList);
			LOGGER.info("Final JSON report --> " + jsonObj);
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

	}

}
