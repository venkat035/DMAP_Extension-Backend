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
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.reflect.visitor.filter.TypeFilter;

@Component
public class GetDiscoveryDetails {
	
	@Autowired
	private DmapExtensionDao dmapExtensionDao;
	
	@Autowired
	private Utils utils;

	private static final Logger LOGGER = LoggerFactory.getLogger(GetDiscoveryDetails.class);

	Map<String, String> eachFileDetail = new HashMap<String, String>();

	CtModel javaFileModel;
	Launcher javaFileLauncher;
	boolean fromPrepareCall = false;
	boolean queryFound = false;
	String currentFileName = "";

	public boolean getVariables(CtModel model, Launcher launcher, String dirPath, String applicationRunId,
			String appName) {
		eachFileDetail.clear();
		MultiValuedMap<String, List<String>> variableDetails = new ArrayListValuedHashMap<String, List<String>>();
		String fileName = "";
		javaFileModel = model;
		javaFileLauncher = launcher;
		

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

			getExecuteQueryContent(variableDetails, fileName, dirPath, appName);

		} catch (Exception e) {
			System.out.println(e);
			LOGGER.error("Exception occurred --> " + e);
		}
		return queryFound;
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

	public void getExecuteQueryContent(MultiValuedMap<String, List<String>> variableDetails,
			String fileName, String dirPath, String appName) {
		String queryMethodContent = "";
		String variableValue = "";
		String variableLineNumber = "";
		String queryMethodFunctionName = "";
		queryFound = false;
		try {
			for (Map.Entry<String, List<String>> variableDetail : variableDetails.entries()) {
				fromPrepareCall = false;
				if (variableDetail.getValue().size() == 4) {
					variableValue = variableDetail.getValue().get(0);
					variableLineNumber = variableDetail.getValue().get(2);
					queryMethodFunctionName = variableDetail.getValue().get(3);
				} else {
					variableValue = variableDetail.getValue().get(0);
					variableLineNumber = variableDetail.getValue().get(2);
				}


				if (variableValue.contains(".execute(") || variableValue.contains(".createStatement(")
						|| variableValue.contains(".prepareStatement(") || variableValue.contains(".prepareCall(")
						|| variableValue.contains(".executeQuery(") || variableValue.contains("@Query")
						|| variableValue.contains(".save(") || variableValue.contains(".delete(")
						|| variableValue.contains(".createQuery(") || variableValue.contains(".createSQLQuery(")
						|| variableValue.contains(".executeUpdate(") || variableValue.contains(".update("))
						{
					queryFound = true;
					List<Map<String, Object>> rs = dmapExtensionDao.getAppRunIdFromDiscoveryTable(appName);
					dmapExtensionDao.insertFileDetails(appName, rs.get(0).get("applicationrunid").toString(), fileName ,null, utils.getCreatedDateWithTime());
				}

			}
			System.out.println("currentFileName -----> " + currentFileName);
			
			
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

	}

}
