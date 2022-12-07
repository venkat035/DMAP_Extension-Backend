package org.newtglobal.dmapextension.service;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.restassured.RestAssured;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

@Component
public class GenerateReportThroughFlask {

	private static final Logger LOGGER = LoggerFactory.getLogger(GenerateReportThroughFlask.class);

	@Value("${flaskBaseURL}")
	private String flaskBaseUrl;

	public Response establishConnectionWithFlask(JSONObject jsonObj) {

		Response response = null;
		try {
			RestAssured.baseURI = flaskBaseUrl;
			RequestSpecification httpRequest = RestAssured.given();
			httpRequest.header("Content-Type", "application/json");
			httpRequest.body(jsonObj);
			response = httpRequest.request(Method.POST, "/generate_report");
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

		return response;
	}

}
