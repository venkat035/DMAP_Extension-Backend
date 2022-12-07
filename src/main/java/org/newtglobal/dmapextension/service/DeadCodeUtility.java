package org.newtglobal.dmapextension.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.visitor.filter.TypeFilter;

public class DeadCodeUtility {
	private static final Logger LOGGER = LoggerFactory.getLogger(DeadCodeUtility.class);
	String currentFileName = "";
	HashMap<String, List<String>> DeadCode = new HashMap<String, List<String>>();
	
	public void getVariables(CtModel model, Launcher launcher, String dirPath, String applicationRunId,
			String appName) throws IOException {
		
		CtModel javaFileModel;
		Launcher javaFileLauncher;
		String fileName = "";
		javaFileModel = model;
		javaFileLauncher = launcher;
		long lines = 0;
		
		List<String> fileNames = new ArrayList<String>();
		List<String> unusedLineNumbers = new ArrayList<String>();
		List<String> unusedDescription = new ArrayList<String>();
		List<String> filepath = new ArrayList<String>();
		List<String> noofLines = new ArrayList<String>();
		
		List<CtClass> newClass = model.getElements(new TypeFilter<>(CtClass.class));
		List<CtField> field = model.getElements(new TypeFilter<>(CtField.class));
		List<CtMethod> methods = model.getElements(new TypeFilter<>(CtMethod.class));
		
		List<CtExecutableReference> executableReferences = model
				.getElements(new TypeFilter<>(CtExecutableReference.class));
		
		List<CtFieldReference> FieldexecutableReferences = model.getElements(new TypeFilter<>(CtFieldReference.class));
		File file = new File(dirPath);
		String path = file.getPath();
		filepath.add(path);
		String filename = file.getName();
		fileNames.add(filename);
		Path path_val = Paths.get(dirPath);
		lines = Files.lines(path_val).count();
		String s=String.valueOf(lines); 
		noofLines.add(s);
		System.out.println("Each file lines" +lines);
		
		
		
		try {
			for (CtExecutableReference execReference : executableReferences) {
				CtExecutable declaration = execReference.getExecutableDeclaration();
				if (declaration != null && declaration instanceof CtClass) {
					CtClass methodOfExec = (CtClass) declaration;
					for (CtClass method : new ArrayList<>(newClass)) {
						if (method.equals(methodOfExec)) {
							methods.remove(method);
							break;
						}
					}
					
				}
				if (declaration != null && declaration instanceof CtMethod) {
					CtMethod methodOfExec = (CtMethod) declaration;
					for (CtMethod method : new ArrayList<>(methods)) {
						if (method.equals(methodOfExec)) {
							methods.remove(method);
							break;
						}
					}
				}
				if (declaration != null && declaration instanceof CtField) {
					CtField methodOfExec = (CtField) declaration;
					for (CtField method : new ArrayList<>(field)) {
						if (method.equals(methodOfExec)) {
							methods.remove(method);
							break;
						}
					}
				}
			}
			
			if (methods.isEmpty()) {
				System.out.println("There is no dead code from methods & Fields & Classes!");
			} else {
				for (CtClass method : newClass) {
					String classvalue = String.valueOf(method.getSimpleName());
					String variableLineNumber = String.valueOf(method.getPosition().getLine());
					unusedLineNumbers.add(variableLineNumber);
					unusedDescription.add(classvalue);
					String variableValue = "";
					String variablePath = method.getPath().toString();
					DeadCode.put("description", unusedDescription);
					DeadCode.put("deadCodeLineNum", unusedLineNumbers);
					DeadCode.put("fileName", fileNames);
					DeadCode.put("filePath", filepath);	
					DeadCode.put("noOfLines", noofLines);
					String remediationMessage = "The Following Class has no reference " + classvalue;
				
					CommentDeadCodePlugin.plugComments(path, Integer.parseInt(variableLineNumber) - 1, remediationMessage);
				}
				for (CtMethod method : methods) {
					List<String> MethodWarning = new ArrayList<String>();
					String methodvalue = String.valueOf(method.getSimpleName());
					String variableLineNumber = String.valueOf(method.getPosition().getLine());
					unusedLineNumbers.add(variableLineNumber);
					unusedDescription.add(methodvalue);
					String variableValue = "";
					String variablePath = method.getPath().toString();
					DeadCode.put("description", unusedDescription);
					DeadCode.put("deadCodeLineNum", unusedLineNumbers);
					DeadCode.put("fileName", fileNames);
					DeadCode.put("filePath", filepath);	
					DeadCode.put("noOfLines", noofLines);
					String remediationMessage = "The Following Method has no reference " + methodvalue;
					CommentDeadCodePlugin.plugComments(path, Integer.parseInt(variableLineNumber) - 1, remediationMessage);
				}
				for (CtField method : field) {
					List<String> FieldDesription = new ArrayList<String>();
					String methodvalue = String.valueOf(method.getSimpleName());
					String variableLineNumber = String.valueOf(method.getPosition().getLine());
					unusedDescription.add(methodvalue);
					unusedLineNumbers.add(variableLineNumber);
					DeadCode.put("description", unusedDescription);
					DeadCode.put("deadCodeLineNum", unusedLineNumbers);
					DeadCode.put("fileName", fileNames);
					DeadCode.put("filePath", filepath);	
					DeadCode.put("noOfLines", noofLines);
					String variableValue = "";
					String variablePath = method.getPath().toString();
					String remediationMessage = "The Following Field has no reference " + methodvalue;
					CommentDeadCodePlugin.plugComments(path, Integer.parseInt(variableLineNumber) - 1, remediationMessage);
				}
				
			}
		}
		catch (Exception e) {
			System.out.println(e);
			LOGGER.error("Exception occurred --> " + e);
		}
	}
	public HashMap<String, List<String>> geteachUnsedFileDetail() {
		return DeadCode;
	}
}