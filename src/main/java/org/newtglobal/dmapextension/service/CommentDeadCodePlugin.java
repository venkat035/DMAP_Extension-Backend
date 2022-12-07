package org.newtglobal.dmapextension.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class CommentDeadCodePlugin {
	public static void plugComments(String dirPath, int lineNo, String remediationMessage) {
		String dmapExtensionEngineMessage = "";

		try {
			if (remediationMessage.contains("Class") || remediationMessage.contains("Method")
					|| remediationMessage.contains("Field")) {
				dmapExtensionEngineMessage = " // DEX Message : Dead Code Detected - " + remediationMessage;
			}

			Path path = Paths.get(dirPath);

			String line = Files.readAllLines(path).get(lineNo).concat(dmapExtensionEngineMessage);
			List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

			lines.remove(Files.readAllLines(path).get(lineNo));
			lines.add(lineNo, line);
			Files.write(path, lines, StandardCharsets.UTF_8);
		} catch (IOException e) {
			System.out.println(e);
		}

	}
}
