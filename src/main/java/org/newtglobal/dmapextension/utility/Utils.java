package org.newtglobal.dmapextension.utility;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TimeZone;

import org.newtglobal.dmapextension.dao.DmapExtensionDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Utils {

	@Value("${dateFormatForCreatedDateAndTime}")
	private String dateFormatForCreatedDateAndTime;

	@Value("${dateFormatForAppRunId}")
	private String dateFormatForAppRunId;
	

	private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

	public String getQueryWithoutExtraSpaces(String oracleQuery) {
		String item = "";
//		try {
//			LOGGER.info("Oracle Query for getQueryWithoutExtraSpaces --> " + oracleQuery);
//			if (!isNull(oracleQuery) && !oracleQuery.contains(",")) {
//				String[] str1 = oracleQuery.split(",");
//				for (int i = 0; i < str1.length; i++) {
//					item = item + str1[i].strip() + ","; // TODO: We removing ; from item = item.replace(";,", ";");
//					item = item.replace(",", ";");
//				}
//			} else if (!isNull(oracleQuery) && oracleQuery.contains(",")) {
//				LOGGER.info("Oracle Query for getQueryWithoutExtraSpaces --> " + oracleQuery);
//				String[] str1 = oracleQuery.split(",");
//				for (int i = 0; i < str1.length; i++) {
//					item = item + str1[i].strip(); // TODO: We removing ; from item = item.replace(";,", ";");
//					item = item.replace(",", ";");
//				}
//			}
//		} catch (Exception e) {
//			LOGGER.error("Exception occurred --> " + e);
//		}
		try {
			LOGGER.info("Oracle Query for getQueryWithoutExtraSpaces --> " + oracleQuery);
			String[] str1 = oracleQuery.split("\\s+");
			for (int i = 0; i < str1.length; i++) {
				if (i == str1.length - 1) {
					item = item + str1[i];
				} else {
					item = item + str1[i] + " ";
				}

			}
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

		return item;
	}

	public String getCreatedDateWithTime() {
		Date date = new Date();
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormatForCreatedDateAndTime);
		simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
		String createdDateAndTime = simpleDateFormat.format(date);
		return createdDateAndTime;
	}

	public String getAppRunId() {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormatForAppRunId);
		Date date = new Date(System.currentTimeMillis());
		String appRunId = simpleDateFormat.format(date).toString();
		return appRunId;
	}

	public boolean isNull(String value) {
		boolean flag = false;
		try {
			if (value.equals(null) || value.equals("") || value.equals(" ")) {
				return true;
			}
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

		return flag;
	}

	public int checkBrackets(String expression, int index) {
		int endIndex = -1;
		try {

			if (expression.charAt(index) != '(') {
				endIndex = -1;
			}

			Stack<Integer> st = new Stack<>();
			st.push(index);

			for (int i = index + 1; i < expression.length(); i++) {

				if (expression.charAt(i) == '(') {
					st.push(i);
				}

				else if (expression.charAt(i) == ')') {
					if (!st.isEmpty()) {
						st.pop();
					}
					if (st.empty()) {
						endIndex = i;
						break;
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

		return endIndex;
	}

	public boolean doubleQuotesCheck(String value) {
		try {
			if (!isNull(value)) {
				if (value.trim().startsWith("\"") && value.trim().endsWith("\"")) {
					return true;
				}
			}
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

		return false;
	}

	public boolean dotCheck(String value) {
		try {
			if (!isNull(value)) {
				if (value.split("\\.").length > 1) {
					return true;
				}
			}
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

		return false;
	}

	public boolean parenthesisCheck(String value) {
		try {
			if (!isNull(value)) {
				if (value.contains("(")) {
					return true;
				}
			}
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

		return false;
	}

	public boolean sqlKeywordsIsPresent(String value) {
		try {
			List<String> sqlKeywordsList = new ArrayList<String>();
			sqlKeywordsList.add("SELECT");
			sqlKeywordsList.add("CREATE");
			sqlKeywordsList.add("UPDATE");
			sqlKeywordsList.add("DELETE");
			sqlKeywordsList.add("INSERT");
			sqlKeywordsList.add("ALTER");
			sqlKeywordsList.add("TRUNCATE");
			sqlKeywordsList.add("DROP");
			sqlKeywordsList.add("DROP DATABASE");
			sqlKeywordsList.add("DROP TABLE");
			for (String s : sqlKeywordsList) {
				if (value.toUpperCase().contains(s)) {
					return true;
				}
			}
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

		return false;
	}

	public boolean isProcedureCall(String value) {
		try {
			if (value.contains("call") && value.contains("?")) {
				return true;
			}
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

		return false;
	}
	
}
