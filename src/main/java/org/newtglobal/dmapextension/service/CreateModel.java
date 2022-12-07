package org.newtglobal.dmapextension.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.newtglobal.dmapextension.utility.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import spoon.Launcher;
import spoon.compiler.Environment;
import spoon.reflect.CtModel;

@Component
public class CreateModel {

	@Autowired
	private Report report;

	@Autowired
	private Utils utils;

	@Autowired
	private StaticOrDynamicQueryCheck staticOrDynamicQueryCheck;

	private static final Logger LOGGER = LoggerFactory.getLogger(CreateModel.class);

	ArrayList<Map<String, String>> fileDetails = new ArrayList<Map<String, String>>();
	List<List<Map<String,String>>> conditionalQueryDetailsList = new ArrayList<List<Map<String,String>>>();
	List<HashMap<String, List<String>>> unUsedList = new ArrayList<HashMap<String, List<String>>>();
	int noOfFilesFound = 0;
	int noOfFilesFoundWithQueries = 0;
	int noofUnusedfiles=0;
	String applicationRunId = "";
	String reportDate = "";

	public void buildModelForEachFile(List<String> targetJavaFiles, String appName) {
		try {
			if (!targetJavaFiles.isEmpty()) {
				noOfFilesFound = targetJavaFiles.size();
			}
			staticOrDynamicQueryCheck.eachFileDetail.clear();
			fileDetails.clear();
			conditionalQueryDetailsList.clear();
			reportDate = utils.getAppRunId();

			if (!targetJavaFiles.isEmpty()) {
				ListIterator<String> it = targetJavaFiles.listIterator();
				while (it.hasNext()) {
					buildModel(it.next(),reportDate, appName);
				}
			}
			if (!utils.isNull(appName)) {
				report.prepareFileJson(appName, noOfFilesFound, fileDetails,noOfFilesFoundWithQueries,conditionalQueryDetailsList,unUsedList,noofUnusedfiles);
			}
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

	}

	public void buildModel(String dirPath, String applicationRunId,String appName) {
		try {
			Launcher launcher = new Launcher();
			launcher.addInputResource(dirPath);
			Environment e = launcher.getEnvironment();
			e.setCommentEnabled(false);
			e.setPrettyPrinterCreator(() -> new RoundBracketMinimiser(launcher.getEnvironment()));
			launcher.buildModel();
			CtModel model = launcher.getModel();
			
			staticOrDynamicQueryCheck.getVariables(model, launcher, dirPath,appName);
			Map<String, String> eachFileDetail = new HashMap<String, String>(
					staticOrDynamicQueryCheck.geteachFileDetail());
			if (eachFileDetail.size() != 0) {
				fileDetails.add(eachFileDetail);
				noOfFilesFoundWithQueries++;
			}
			
			DeadCodeUtility deadCodeUtility = new DeadCodeUtility();
			deadCodeUtility.getVariables(model, launcher, dirPath, applicationRunId, appName);
			HashMap<String, List<String>> unusedDetail = new HashMap<String, List<String>>(
					deadCodeUtility.geteachUnsedFileDetail());
			if (unusedDetail.size() != 0) {
				unUsedList.add(unusedDetail);
				noofUnusedfiles++;
			}
			List<Map<String,String>> eachFileconditionalQueryDetailsList = new ArrayList<Map<String,String>>(staticOrDynamicQueryCheck.getConditionalQueryDetailsList());
			conditionalQueryDetailsList.add(eachFileconditionalQueryDetailsList);
			staticOrDynamicQueryCheck.clearConditionalQueryDetailsList();
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

	}

}
