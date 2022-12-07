package org.newtglobal.dmapextension.controller;

import org.json.simple.JSONObject;
import org.newtglobal.dmapextension.service.JreUpgradation;
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
public class DmapExtensionJreUpgradeController {

	@Autowired
	private Utils utils;

	@Autowired
	private JreUpgradation jreUpgradation;

	private static final Logger LOGGER = LoggerFactory.getLogger(DmapExtensionJreUpgradeController.class);

	@RequestMapping(value = "/startJreUpgrade", method = { RequestMethod.POST })
	public JSONObject startJreUpgrade(@RequestBody String appName) throws Exception {
		JSONObject jsonObj = null;
		try {
			System.out.println("Application passed for JRE upgrade --> " + appName);
			LOGGER.info("Application passed for JRE upgrade --> " + appName);
			if (!utils.isNull(appName)) {
				jsonObj = jreUpgradation.scanFoldersRecursivelyForJreUpgradation(appName);
			}
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

		return jsonObj;
	}

}
