package org.newtglobal.dmapextension.controller;

import org.json.simple.JSONObject;
import org.newtglobal.dmapextension.service.ScanDirectory;
import org.newtglobal.dmapextension.service.ScanDirectoryForDiscovery;
import org.newtglobal.dmapextension.utility.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ResponseBody
@CrossOrigin(origins = "*")
public class DmapExtensionController {

	@Autowired
	private Utils utils;

	@Autowired
	private ScanDirectory scanDirectory;
	
	@Autowired
	private ScanDirectoryForDiscovery scanDirectoryForDiscovery;

	private static final Logger LOGGER = LoggerFactory.getLogger(DmapExtensionController.class);

	@RequestMapping(value = "/discovery", method = { RequestMethod.POST })
	public void startDiscovery(@RequestBody String appName) {
		try {
			System.out.println("Application passed for discovery --> " + appName);
			LOGGER.info("Application passed for discovery --> " + appName);
			if (!utils.isNull(appName)) {
				scanDirectoryForDiscovery.scanFoldersRecursively(appName);
			}
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}
	}

	@RequestMapping(value = "/assessment", method = { RequestMethod.POST })
	public JSONObject startAssessment(@RequestBody String appName) {
		JSONObject jsonObj = null;
		try {
			System.out.println("Application passed for assessment --> " + appName);
			LOGGER.info("Application passed for assessment --> " + appName);
			if (!utils.isNull(appName)) {
				jsonObj = scanDirectory.scanFoldersRecursively(appName);
			}
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}
		return jsonObj;
	}

}
