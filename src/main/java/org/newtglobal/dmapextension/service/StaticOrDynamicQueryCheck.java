package org.newtglobal.dmapextension.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.newtglobal.dmapextension.dao.DmapExtensionDao;
import org.newtglobal.dmapextension.utility.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtTargetedExpression;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.reflect.visitor.filter.TypeFilter;

@Component
public class StaticOrDynamicQueryCheck {

	@Autowired
	private Utils utils;

	@Autowired
	private DmapCall dmapcall;

	@Autowired
	private DmapExtensionDao dmapExtensionDao;

	@Autowired
	private ConnectionStrings connectionStrings;

	private static final Logger LOGGER = LoggerFactory.getLogger(StaticOrDynamicQueryCheck.class);

	MultiValuedMap<String, List<String>> dynamicQueryPartsDetails = new ArrayListValuedHashMap<String, List<String>>();
	List<Map<String, String>> fileDetails = new ArrayList<Map<String, String>>();
	Map<String, String> eachFileDetail = new HashMap<String, String>();
	List<Map<String, String>> conditionalQueryDetailsList = new ArrayList<Map<String, String>>();

	int totalQueriesFound = 0;
	int totalQueriesRemediated = 0;
	int totalQueriesNotRemediated = 0;
	int totalQueriesNoRemediationRequired = 0;
	int totalProcedureCalls = 0;
	boolean fromPrepareCall = false;
	String currentFileName = "";
	CtModel javaFileModel;
	Launcher javaFileLauncher;
	Boolean isVariableQueryPart = false;
	String applicationName = "";
	Boolean isFunctionParametersUsedInQuery = false;

	public void getVariables(CtModel model, Launcher launcher, String dirPath, String appName) {
		eachFileDetail.clear();
		MultiValuedMap<String, List<String>> variableDetails = new ArrayListValuedHashMap<String, List<String>>();
		String fileName = "";
		javaFileModel = model;
		javaFileLauncher = launcher;
		applicationName = appName;

		try {
			for (CtField<?> ctField : model.getElements(new TypeFilter<>(CtField.class))) {
				String variableValue = "";
				String variablePath = ctField.getPath().toString();
				fileName = getFileName(variablePath);
				String variableName = ctField.getSimpleName();
				if (ctField.getAssignment() != null) {
					variableValue = ctField.getAssignment().toString();
				}
				String variableLineNumber = String.valueOf(ctField.getPosition().getLine());
				List<String> variableDetailsList = new ArrayList<String>();

				variableDetailsList.add(variableValue);
				variableDetailsList.add(variablePath);
				variableDetailsList.add(variableLineNumber);

				variableDetails.put(variableName, variableDetailsList);
			}

			for (CtLocalVariable<?> variable : model.getElements(new TypeFilter<>(CtLocalVariable.class))) {
				String variableValue = "";
				String variablePath = variable.getPath().toString();
				fileName = getFileName(variablePath);
				String methodName = getMethodName(variablePath);
				String variableName = variable.getSimpleName();
				if (variable.getAssignment() != null) {
					variableValue = variable.getAssignment().toString();
				}
				String variableLineNumber = String.valueOf(variable.getPosition().getLine());

				List<String> variableDetailsList = new ArrayList<String>();

				variableDetailsList.add(variableValue);
				variableDetailsList.add(variablePath);
				variableDetailsList.add(variableLineNumber);
				variableDetailsList.add(methodName);

				variableDetails.put(variableName, variableDetailsList);
			}

			for (CtAssignment<?, ?> variable : model.getElements(new TypeFilter<>(CtAssignment.class))) {
				String variableValue = "";
				String variablePath = variable.getPath().toString();
				fileName = getFileName(variablePath);
				String methodName = getMethodName(variablePath);
				String variableName = variable.getAssigned().toString();
				if (variable.getAssignment() != null) {
					variableValue = variable.getAssignment().toString();
				}
				String variableLineNumber = String.valueOf(variable.getPosition().getLine());

				List<String> variableDetailsList = new ArrayList<String>();

				variableDetailsList.add(variableValue);
				variableDetailsList.add(variablePath);
				variableDetailsList.add(variableLineNumber);
				variableDetailsList.add(methodName);

				variableDetails.put(variableName, variableDetailsList);
			}

			List<List<String>> sessionVariablesDetailsList = getSessionQueryContents(variableDetails);
			for (List<String> sessionVariableDetailsList : sessionVariablesDetailsList) {
				variableDetails.put("sessionQueries", sessionVariableDetailsList);
			}
			currentFileName = fileName.split("\\.")[0];

			if (connectionStrings.isConnectionStringPresent(appName)) {
				connectionStrings.remediateConnectionStrings(dirPath, appName);
			}
			getExecuteQueryContent(variableDetails, fileName, dirPath);

		} catch (Exception e) {
			System.out.println(e);
			LOGGER.error("Exception occurred --> " + e);
		}
	}

	public void getExecuteQueryContent(MultiValuedMap<String, List<String>> variableDetails, String fileName, String dirPath) {
		String queryMethodContent = "";
		String variableValue = "";
		String variableLineNumber = "";
		String queryMethodFunctionName = "";
		try {
			for (Map.Entry<String, List<String>> variableDetail : variableDetails.entries()) {
				fromPrepareCall = false;
				isFunctionParametersUsedInQuery = false;
				if (variableDetail.getValue().size() == 4) {
					variableValue = variableDetail.getValue().get(0);
					variableLineNumber = variableDetail.getValue().get(2);
					queryMethodFunctionName = variableDetail.getValue().get(3);
				} else {
					variableValue = variableDetail.getValue().get(0);
					variableLineNumber = variableDetail.getValue().get(2);
				}

				if (variableValue.contains(".execute(")) {
					String queryMethod = ".execute(";
					queryMethodContent = getQueryMethodBracketContents(variableValue, queryMethod);
				} else if (variableValue.contains(".createStatement(")) {
					String queryMethod = ".createStatement(";
					queryMethodContent = getQueryMethodBracketContents(variableValue, queryMethod);
				} else if (variableValue.contains(".prepareStatement(")) {
					String queryMethod = ".prepareStatement(";
					queryMethodContent = getQueryMethodBracketContents(variableValue, queryMethod);
				} else if (variableValue.contains(".prepareCall(")) {
					String queryMethod = ".prepareCall(";
					fromPrepareCall = true;
					queryMethodContent = getQueryMethodBracketContents(variableValue, queryMethod);
				} else if (variableValue.contains(".executeQuery(")) {
					String queryMethod = ".executeQuery(";
					queryMethodContent = getQueryMethodBracketContents(variableValue, queryMethod);
				} else if (variableValue.contains("@Query")) {
					String queryMethod = "@Query";
					queryMethodContent = getQueryMethodBracketContents(variableValue, queryMethod);
				} else if (variableValue.contains(".save(")) {
					String queryMethod = ".save(";
					queryMethodContent = getQueryMethodBracketContents(variableValue, queryMethod);
				} else if (variableValue.contains(".delete(")) {
					String queryMethod = ".delete(";
					queryMethodContent = getQueryMethodBracketContents(variableValue, queryMethod);
				} else if (variableValue.contains(".update(")) {
					String queryMethod = ".update(";
					queryMethodContent = getQueryMethodBracketContents(variableValue, queryMethod);
				} else if (variableValue.contains(".createQuery(")) {
					String queryMethod = ".createQuery(";
					queryMethodContent = getQueryMethodBracketContents(variableValue, queryMethod);
				} else if (variableValue.contains(".createSQLQuery(")) {
					String queryMethod = ".createSQLQuery(";
					queryMethodContent = getQueryMethodBracketContents(variableValue, queryMethod);
				} else if (variableValue.contains(".executeUpdate(")) {
					String queryMethod = ".executeUpdate(";
					queryMethodContent = getQueryMethodBracketContents(variableValue, queryMethod);
				}
				if (!queryMethodContent.isBlank() && !queryMethodContent.isEmpty()) {
					if (queryMethodContent.indexOf(',') != -1) {
						if (utils.sqlKeywordsIsPresent(queryMethodContent)) {
							getStaticOrDynamicQuery(queryMethodContent, variableDetails, fileName,
									dirPath, variableLineNumber, queryMethodFunctionName);
							queryMethodContent = "";
						} else if (utils.doubleQuotesCheck(queryMethodContent)
								&& utils.isProcedureCall(queryMethodContent) || fromPrepareCall) {
							getStaticOrDynamicQuery(queryMethodContent, variableDetails, fileName,
									dirPath, variableLineNumber, queryMethodFunctionName);
							queryMethodContent = "";
						}

					} else if (queryMethodContent.indexOf(',') == -1) {
						getStaticOrDynamicQuery(queryMethodContent, variableDetails, fileName,
								dirPath, variableLineNumber, queryMethodFunctionName);
						queryMethodContent = "";
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

	}

	public String getQueryMethodBracketContents(String variableValue, String queryMethod) {
		int startIndex;
		String queryMethodContent = "";
		try {
			startIndex = variableValue.indexOf(queryMethod) + queryMethod.length() - 1;
			int endIndex = utils.checkBrackets(variableValue, startIndex);
			if (checkStaticOrDynamicQuery(
					variableValue.substring(startIndex + 1, endIndex)) == "inline_static_query_found") {
				queryMethodContent = variableValue.substring(startIndex + 1, endIndex);
			} else {
				queryMethodContent = variableValue.substring(startIndex + 1, endIndex);
			}
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

		return queryMethodContent;
	}

	public String checkStaticOrDynamicQuery(String queryMethodContent) {
		String queryTypeFlag = "";
		try {
			String[] executeQueryContentArr = queryMethodContent.split("\\+");
			if (executeQueryContentArr.length == 1) {
				if (utils.doubleQuotesCheck(executeQueryContentArr[0])) {
					queryTypeFlag = "inline_static_query_found";
				} else if (utils.dotCheck(executeQueryContentArr[0])) {
					queryTypeFlag = "stringBufferOrBuilder_found";
				} else {
					queryTypeFlag = "variable_found";
				}
			} else {
				queryTypeFlag = "dynamic_query_found";
			}
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

		return queryTypeFlag;
	}

	public void getStaticOrDynamicQuery(String queryMethodContent, MultiValuedMap<String, List<String>> variableDetails,
			String fileName, String dirPath, String variableLineNumber,
			String queryMethodFunctionName) {
		String query = "";
		List<Map<String, Object>> rs = dmapExtensionDao.getAppRunIdFromDiscoveryTable(applicationName);
		try {
			String queryTypeFlag = checkStaticOrDynamicQuery(queryMethodContent);

			if (queryTypeFlag.equals("inline_static_query_found")) {
				query = queryMethodContent.replaceAll("\"", "");
				if (utils.isProcedureCall(query) || fromPrepareCall) {
					dmapExtensionDao.insertProcedureCallDetails(rs.get(0).get("applicationrunid").toString(), fileName, variableLineNumber,
							queryMethodContent, utils.getCreatedDateWithTime());
				}
				System.out.println("inline_static_query_found =====> " + query + " in =====> " + variableLineNumber);
			} else if (queryTypeFlag.equals("variable_found")) {

				query = getStaticQueryFromVariable(queryMethodContent, variableDetails, queryMethodFunctionName);
				if (utils.isProcedureCall(query) || fromPrepareCall) {
					dmapExtensionDao.insertProcedureCallDetails(rs.get(0).get("applicationrunid").toString(), fileName, variableLineNumber, query,
							utils.getCreatedDateWithTime());
				}
				System.out.println("variable_query_found =====> " + query);
			} else if (queryTypeFlag.equals("stringBufferOrBuilder_found")) {
				String[] stringBufferVariableName = queryMethodContent.split("\\.");
				String stringBufferOrBuilderFound = getStaticQueryFromVariable(stringBufferVariableName[0],
						variableDetails, queryMethodFunctionName);
				if (stringBufferOrBuilderFound.length() > 0) {
					// System.out.println("Query part with string buffer present --------- " +
					// stringBufferOrBuilderFound);
					query = getStringBufferContents(stringBufferVariableName[0], queryMethodFunctionName,
							variableDetails);
				} else {
					// System.out.println("Query part is calling another method from different java
					// file");
				}
				System.out.println("string_buffer_query_found =====> " + query + "in =====> " + variableLineNumber);
			} else if (queryTypeFlag.equals("dynamic_query_found")) {
				query = getDynamicQuery(queryMethodContent, variableDetails, queryMethodFunctionName);
				System.out.println("dynamic_query_found =====> " + query + "in =====> " + variableLineNumber);

			}
			if (!utils.isNull(query)) {
				getReportJson(query, fileName, dirPath, variableLineNumber, dynamicQueryPartsDetails);
			}
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

	}

	public String getDynamicQueryFromVariable(String executeQueryContent,
			MultiValuedMap<String, List<String>> variableDetails, String queryMethodFunctionName) {
		String query = "";
		String variableFunctionName = "";
		try {
			for (Map.Entry<String, List<String>> variableDetail : variableDetails.entries()) {
				List<String> queryPartDetailsList = new ArrayList<String>();
				if (!queryMethodFunctionName.isBlank() && !queryMethodFunctionName.isEmpty()
						&& variableDetail.getValue().size() == 4) {
					variableFunctionName = variableDetail.getValue().get(3);
					if (executeQueryContent.trim().equalsIgnoreCase(variableDetail.getKey().trim())
							&& variableFunctionName.equalsIgnoreCase(queryMethodFunctionName)) {
						query = variableDetail.getValue().get(0).replaceAll("\"", "");
						queryPartDetailsList.add(query);
						queryPartDetailsList.add(variableDetail.getValue().get(2));
						dynamicQueryPartsDetails.put(variableDetail.getKey(), queryPartDetailsList);
						break;

					}
				} else {
					if (executeQueryContent.trim().equalsIgnoreCase(variableDetail.getKey().trim())) {
						query = variableDetail.getValue().get(0).replaceAll("\"", "");
						queryPartDetailsList.add(query);
						queryPartDetailsList.add(variableDetail.getValue().get(2));
						dynamicQueryPartsDetails.put(variableDetail.getKey(), queryPartDetailsList);
						break;

					}

				}
			}

		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}
		if (query.isBlank() || query.isEmpty()) {
			return executeQueryContent + " ";
		} else {
			return query + " ";
		}

	}

	public String getStaticQueryFromVariable(String executeQueryContent,
			MultiValuedMap<String, List<String>> variableDetails, String queryMethodFunctionName) {
		String query = "";
		String variableFunctionName = "";
		isVariableQueryPart = true;
		try {
			for (Map.Entry<String, List<String>> variableDetail : variableDetails.entries()) {
				if (!queryMethodFunctionName.isBlank() && !queryMethodFunctionName.isEmpty()
						&& variableDetail.getValue().size() == 4) {
					variableFunctionName = variableDetail.getValue().get(3);
					if (executeQueryContent.trim().equalsIgnoreCase(variableDetail.getKey().trim())
							&& variableFunctionName.equalsIgnoreCase(queryMethodFunctionName)) {
						query = variableDetail.getValue().get(0);
						if (utils.isNull(query)) {
							continue;
						} else if (!utils.sqlKeywordsIsPresent(query)) {
							continue;
						} else {
							break;
						}

					}
				} else {
					if (executeQueryContent.trim().equalsIgnoreCase(variableDetail.getKey().trim())) {
						query = variableDetail.getValue().get(0);
						if (utils.isNull(query)) {
							continue;
						} else if (!utils.sqlKeywordsIsPresent(query)) {
							continue;
						} else {
							break;
						}

					}

				}

			}
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

		query = getDynamicQuery(query, variableDetails, queryMethodFunctionName);
		if (query.isBlank() || query.isEmpty()) {
			if (isFunctionParametersUsedInQuery(queryMethodFunctionName, executeQueryContent)) {
				isFunctionParametersUsedInQuery = true;
			}
		}

		return query + " ";
	}

	// Method to extract or identify if function parameters used in query

	public boolean isFunctionParametersUsedInQuery(String queryMethodFunctionName, String executeQueryContent) {
		try {
			List<CtType<?>> pack = javaFileLauncher.getFactory().Type().getAll();
			CtType<Object> type = pack.get(0).getTopLevelType();
			List<CtMethod<?>> methods = type.getMethodsByName(queryMethodFunctionName);
			List<CtParameter<?>> functionParametersList = methods.get(0).getParameters();
			for (CtParameter<?> parameter : functionParametersList) {
				String parameterName = parameter.getSimpleName();
				if (parameterName.equals(executeQueryContent)) {
					return true;
				}
			}

		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}
		return false;

	}

	// TODO : Check for concat() method
	public String getDynamicQuery(String executeQueryContent, MultiValuedMap<String, List<String>> variableDetails,
			String queryMethodFunctionName) {
		String resultQuery = "";
		try {
			// TODO : We need to remove this IF and need to write generic logic ( if + is
			// inside any static query)
			if (executeQueryContent.contains("NVLMAXSEQ,0+1") || executeQueryContent.contains("NVL(MAX(SEQ),0)+1")) {
				return executeQueryContent;
			}
			String[] executeQueryContentArray = executeQueryContent.split("\\+");

			for (String eachQueryPart : executeQueryContentArray) {
				// 1st check - String (with quotes)
				if (utils.doubleQuotesCheck(eachQueryPart)) {
					// some inline query part
					// System.out.println("Query part with string query");
					resultQuery = resultQuery + (eachQueryPart.replaceAll("\"", ""));
				} else {
					// 2nd check - Method call from different file
					// 3rd check - string buffer or builder
					if (utils.dotCheck(eachQueryPart)) {
						// it can be sb.toString() or constants.getTable()
						// System.out.println("Query part with string buffer or referring from another
						// file");
						String[] eachQueryPartContent = eachQueryPart.split("\\.");
						String stringBufferOrBuilderFound = getStaticQueryFromVariable(eachQueryPartContent[0],
								variableDetails, queryMethodFunctionName);
						if (stringBufferOrBuilderFound.strip().length() > 0) {
							// System.out.println("Query part with string buffer present --------- " +
							// stringBufferOrBuilderFound);
							resultQuery = resultQuery + getStringBufferContents(eachQueryPartContent[0],
									queryMethodFunctionName, variableDetails);
						} else {
							// System.out.println("Query part is calling another method from different java
							// file");
						}
					} else {
						// 4th check - For variable
						// 5th check - Method call from same file
						// query variable part, can be query1 or gettable()
						if (utils.parenthesisCheck(eachQueryPart)) { // TODO: need to find a way to remove only extra
																		// brackets
							// System.out.println("Query part with method call");
							resultQuery = resultQuery + eachQueryPart.replaceAll("\\(\\)", "");
						} else {
							// System.out.println("Query part with a single variable");
							resultQuery = resultQuery + getDynamicQueryFromVariable(eachQueryPart, variableDetails,
									queryMethodFunctionName).replaceAll("\"", "");

						}
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

		return resultQuery;

	}

	public String getStringBufferContents(String stringBufferVariableName, String queryMethodFunctionName,
			MultiValuedMap<String, List<String>> variableDetails) {
		String query = "";
		List<CtMethod<?>> requiredMethods = null;
		try {
			requiredMethods = findMethodsWithStringBuffer(javaFileModel, javaFileLauncher.getFactory());

			if (requiredMethods != null) {
				for (CtMethod<?> requiredMethod : requiredMethods) {
					String lineNumber = "";
					if (queryMethodFunctionName.equalsIgnoreCase(requiredMethod.getSimpleName().toString())) {
						List<CtElement> requiredCtElements = requiredMethod.getElements(null).stream()
								.filter(st -> st instanceof CtInvocation && ((CtInvocation<?>) st).getTarget()
										.toString().equalsIgnoreCase(stringBufferVariableName.strip()))
								.collect(Collectors.toList());
						List<CtElement> requiredConditionalElements = null;
						List<CtStatement> statements = requiredMethod.getBody().getStatements();
						for (CtStatement statement : statements) {
							if (statement.toString().contains("if")) {
								requiredConditionalElements = statement.getElements(null);
								List<CtElement> requiredConditionalCtElements = requiredConditionalElements.stream()
										.filter(st -> st instanceof CtInvocation && ((CtInvocation<?>) st).getTarget()
												.toString().equalsIgnoreCase(stringBufferVariableName.strip()))
										.collect(Collectors.toList());
								for (CtElement sbStatement : requiredConditionalCtElements) {
									CtInvocation<?> sbInvocation1 = (CtInvocation<?>) sbStatement;
									if (!sbInvocation1.toString().contains("toString()")) {
										Map<String, String> conditionalQueryDetailsMap = new HashMap<String, String>();
										String conditionalQueryLineNumber = String
												.valueOf(sbInvocation1.getPosition().getLine());
										// String conditionalQuery = sbInvocation1.getArguments().get(0).toString();
										conditionalQueryDetailsMap.put("filename", currentFileName + ".java");
										conditionalQueryDetailsMap.put("lineno", conditionalQueryLineNumber);
										conditionalQueryDetailsMap.put("methodname", queryMethodFunctionName);
										conditionalQueryDetailsList.add(conditionalQueryDetailsMap);
									}

								}
							}

						}

						for (CtElement sbStatement : requiredCtElements) {
							List<String> queryPartDetailsList = new ArrayList<String>();
							CtInvocation<?> sbInvocation = (CtInvocation<?>) sbStatement;
							if (!sbInvocation.toString().contains("toString()")) {
								String stringBufferQueryLineageFinder1 = getDynamicQuery(
										sbInvocation.getArguments().get(0).toString(), variableDetails,
										queryMethodFunctionName);
								queryPartDetailsList.add(stringBufferQueryLineageFinder1.replaceAll("\"", ""));
								System.out.println(stringBufferQueryLineageFinder1);
								query = query + " " + stringBufferQueryLineageFinder1.replaceAll("\"", "");
								lineNumber = String.valueOf(sbInvocation.getPosition().getLine());
								queryPartDetailsList.add(lineNumber);
								dynamicQueryPartsDetails.put(sbStatement.toString(), queryPartDetailsList);
							}

						}
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}
		return query.trim();
	}

	public List<CtMethod<?>> findMethodsWithStringBuffer(CtModel model, Factory factory) {
		ArrayList<CtMethod<?>> al = new ArrayList<CtMethod<?>>();
		try {
			List<CtType<?>> pack = javaFileLauncher.getFactory().Type().getAll();
			CtType<Object> type = pack.get(0).getTopLevelType();
//			CtType<?> type = model.getRootPackage().getType(currentFileName);
			Set<CtMethod<?>> methods = type.getMethods();
			for (CtMethod<?> method : methods) {
				List<CtStatement> statements = method.getBody().getStatements();
				for (CtStatement statement : statements) {
					if (statement.getReferencedTypes().contains(factory.createCtTypeReference(StringBuffer.class))) {
						al.add(method);
						break;
					}

				}

			}
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

		return al;
	}

	public List<List<String>> getSessionQueryContents(MultiValuedMap<String, List<String>> variableDetails) {
		List<CtMethod<?>> requiredMethods = null;
		List<List<String>> variableDetailsList = new ArrayList<List<String>>();
		try {
			requiredMethods = findMethodsWithSession(javaFileModel, javaFileLauncher.getFactory());

			if (requiredMethods != null) {
				for (CtMethod<?> requiredMethod : requiredMethods) {
					boolean isPresent = false;
					String lineNumber = "";
					List<CtElement> requiredCtElements = requiredMethod.getElements(null).stream()
							.filter(st -> st instanceof CtInvocation && ((CtInvocation<?>) st).getTarget().toString()
									.contains("session.createSQLQuery("))
							.collect(Collectors.toList());

//					List<CtStatement> sbStatements = requiredMethod.getBody().getStatements().stream()
//							.filter(st -> st instanceof CtInvocation && ((CtInvocation<?>) st).getTarget().toString()
//									.equalsIgnoreCase(stringBufferVariableName.strip()))
//							.collect(Collectors.toList());
					if (requiredCtElements.size() > 0) {
						CtElement sbStatement = requiredCtElements.get(0);
						CtInvocation<?> sbInvocation = (CtInvocation<?>) sbStatement;
						List<String> eachVariableDetailsList = new ArrayList<String>();
						for (Map.Entry<String, List<String>> variableDetail : variableDetails.entries()) {
							if (variableDetail.getValue().get(0).equals(sbInvocation.toString()))
								isPresent = true;
						}
						if (!isPresent) {
							eachVariableDetailsList.add(sbInvocation.toString()); // variable value
							eachVariableDetailsList.add(sbInvocation.getPath().toString()); // path
							lineNumber = String.valueOf(sbInvocation.getPosition().getLine()); // line no
							eachVariableDetailsList.add(lineNumber);
							eachVariableDetailsList.add(getMethodName(sbInvocation.getPath().toString()));// method Name
							variableDetailsList.add(eachVariableDetailsList);
						}

					} else {
						continue;
					}

				}

			}
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

		return variableDetailsList;
	}

	public List<CtMethod<?>> findMethodsWithSession(CtModel model, Factory factory) {
		ArrayList<CtMethod<?>> al = new ArrayList<CtMethod<?>>();
		try {
			List<CtType<?>> pack = javaFileLauncher.getFactory().Type().getAll();
			CtType<Object> type = pack.get(0).getTopLevelType();
//			CtType<?> type = model.getRootPackage().getType(currentFileName);
			Set<CtMethod<?>> methods = type.getMethods();
			for (CtMethod<?> method : methods) {
				List<CtStatement> statements = method.getBody().getStatements();
				for (CtStatement statement : statements) {
					if (statement.toString().contains("org.hibernate.Session")) {
						al.add(method);
						break;
					}

				}

			}
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

		return al;
	}

	public String getFileName(String variablePath) {

		String fileName = "";
		int index1 = 0;
		try {
			int index = variablePath.indexOf("#containedType[name=");
			if (variablePath.contains("]#method[")) {
				index1 = variablePath.indexOf("]#method[");
			} else if (variablePath.contains("]#field[")) {
				index1 = variablePath.indexOf("]#field[");
			} else if (variablePath.contains("]#annonymousExecutable[")) {
				index1 = variablePath.indexOf("]#annonymousExecutable[");
			} else if (variablePath.contains("]#constructor[")) {
				index1 = variablePath.indexOf("]#constructor[");
			} else if (variablePath.contains("]#value[")) {
				index1 = variablePath.indexOf("]#value[");
			}
			if (index != -1 && index1 != -1) {
				if (index1 == 0) {
					System.out.println("index1 0 found");
				}
				String val = variablePath.substring(index, index1);
				String[] arr = val.split("=");
				fileName = arr[1] + ".java";
			}
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

		return fileName;
	}

	public String getMethodName(String variablePath) {

		String methodName = "";
		int index1;
		try {
			int index = variablePath.indexOf("#method[signature=");
			index1 = variablePath.indexOf("(");

			if (index != -1 && index1 != -1) {
				String val = variablePath.substring(index, index1);
				String[] arr = val.split("=");
				methodName = arr[1];
			}
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

		return methodName;
	}

	public void getReportJson(String query, String fileName, String dirPath,
			String variableLineNumber, MultiValuedMap<String, List<String>> dynamicQueryPartsDetails) {
		Map<String, String> queryWithFileName = new HashMap<String, String>();
		Map<String, String> singleQueryStats = new HashMap<String, String>();
		String oracleQuery = "";
		queryWithFileName.put("fileName", fileName);
		queryWithFileName.put("query", query);
		if (fromPrepareCall) {
			oracleQuery = "Procedure_call";
		} else {
			oracleQuery = query;
		}
		try {
			singleQueryStats = dmapcall.getSqlStats(oracleQuery, dirPath, fileName, applicationName,
					variableLineNumber, dynamicQueryPartsDetails,isFunctionParametersUsedInQuery);
			getFileDetailsMap(queryWithFileName, singleQueryStats);
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

		dynamicQueryPartsDetails.clear();

	}

	public void getFileDetailsMap(Map<String, String> queryWithFileName, Map<String, String> singleQueryStats) {
		try {
			if (!queryWithFileName.get("fileName").toString().equals(null)
					&& eachFileDetail.get("fileName") != queryWithFileName.get("fileName")) {
				eachFileDetail.put("fileName", queryWithFileName.get("fileName").toString());
				totalQueriesFound = 0;
				totalQueriesRemediated = 0;
				totalQueriesNotRemediated = 0;
				totalQueriesNoRemediationRequired = 0;
				totalProcedureCalls = 0;

				if (utils.isProcedureCall(singleQueryStats.get("convertedQuery")) || fromPrepareCall) {
					totalProcedureCalls = totalProcedureCalls + 1;
				} else {
					totalQueriesFound = totalQueriesFound + 1;
					if (singleQueryStats.get("queryRemediatedButFoundSame").toString().equals("true")) {
						totalQueriesNoRemediationRequired = totalQueriesNoRemediationRequired + 1;
					} else if (singleQueryStats.get("queryRemediated").toString().equals("true")) {
						totalQueriesRemediated = totalQueriesRemediated + 1;
					} else if (singleQueryStats.get("queryNotRemediated").toString().equals("true")) {
						totalQueriesNotRemediated = totalQueriesNotRemediated + 1;
					}
				}

				String totalQueriesFoundString = Integer.toString(totalQueriesFound);
				String totalQueriesRemediatedString = Integer.toString(totalQueriesRemediated);
				String totalQueriesNotRemediatedString = Integer.toString(totalQueriesNotRemediated);
				String totalQueriesNoRemediationRequiredString = Integer.toString(totalQueriesNoRemediationRequired);
				String totalProcedureCallsString = Integer.toString(totalProcedureCalls);

				eachFileDetail.put("totalQueriesFound", totalQueriesFoundString);
				eachFileDetail.put("totalQueriesRemediated", totalQueriesRemediatedString);
				eachFileDetail.put("totalQueriesNotRemediated", totalQueriesNotRemediatedString);
				eachFileDetail.put("totalQueriesNoRemediationRequired", totalQueriesNoRemediationRequiredString);
				eachFileDetail.put("totalProcedureCalls", totalProcedureCallsString);

			} else if (eachFileDetail.get("fileName").equals(queryWithFileName.get("fileName"))) {

				if (utils.isProcedureCall(singleQueryStats.get("convertedQuery")) || fromPrepareCall) {
					totalProcedureCalls = totalProcedureCalls + 1;
				} else {
					totalQueriesFound = totalQueriesFound + 1;
					if (singleQueryStats.get("queryRemediatedButFoundSame").toString().equals("true")) {
						totalQueriesNoRemediationRequired = totalQueriesNoRemediationRequired + 1;
					} else if (singleQueryStats.get("queryRemediated").toString().equals("true")) {
						totalQueriesRemediated = totalQueriesRemediated + 1;
					} else if (singleQueryStats.get("queryNotRemediated").toString().equals("true")) {
						totalQueriesNotRemediated = totalQueriesNotRemediated + 1;
					}
				}

				String totalQueriesFoundString = Integer.toString(totalQueriesFound);
				String totalQueriesRemediatedString = Integer.toString(totalQueriesRemediated);
				String totalQueriesNotRemediatedString = Integer.toString(totalQueriesNotRemediated);
				String totalQueriesNoRemediationRequiredString = Integer.toString(totalQueriesNoRemediationRequired);
				String totalProcedureCallsString = Integer.toString(totalProcedureCalls);

				eachFileDetail.put("totalQueriesFound", totalQueriesFoundString);
				eachFileDetail.put("totalQueriesRemediated", totalQueriesRemediatedString);
				eachFileDetail.put("totalQueriesNotRemediated", totalQueriesNotRemediatedString);
				eachFileDetail.put("totalQueriesNoRemediationRequired", totalQueriesNoRemediationRequiredString);
				eachFileDetail.put("totalProcedureCalls", totalProcedureCallsString);
			}
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

	}

	public Map<String, String> geteachFileDetail() {
		return eachFileDetail;
	}

	public List<Map<String, String>> getConditionalQueryDetailsList() {
		return conditionalQueryDetailsList;
	}

	public void clearConditionalQueryDetailsList() {
		conditionalQueryDetailsList.clear();
	}

}
