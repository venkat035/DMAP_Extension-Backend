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
public class CreateModelForDiscovery {

	@Autowired
	private GetDiscoveryDetails getDiscoveryDetails;

	@Autowired
	private Utils utils;

	@Autowired
	private StaticOrDynamicQueryCheck staticOrDynamicQueryCheck;

	private static final Logger LOGGER = LoggerFactory.getLogger(CreateModelForDiscovery.class);

	ArrayList<Map<String, String>> fileDetails = new ArrayList<Map<String, String>>();
	List<List<Map<String, String>>> conditionalQueryDetailsList = new ArrayList<List<Map<String, String>>>();
	int noOfFilesFound = 0;
	String applicationRunId = "";
	String reportDate = "";
	int noOfFilesWithQueries = 0;

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
					buildModel(it.next(), reportDate, appName);
				}
			}

		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

	}

	public void buildModel(String dirPath, String applicationRunId, String appName) {
		try {
			LOGGER.info("Application run ID at build model time --> " + applicationRunId);
			Launcher launcher = new Launcher();
			launcher.addInputResource(dirPath);
			Environment e = launcher.getEnvironment();
			e.setPrettyPrinterCreator(() -> new RoundBracketMinimiser(launcher.getEnvironment()));
			launcher.buildModel();
			CtModel model = launcher.getModel();
			boolean queryFound = getDiscoveryDetails.getVariables(model, launcher, dirPath, applicationRunId, appName);
			if(queryFound) {
				noOfFilesWithQueries++;
			}

		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

	}

}
