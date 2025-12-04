package com.salesmanager.core.business.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("systemUtils")
public class SystemUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(SystemUtils.class);

	public String getSystemInfo(String command) {
		StringBuilder output = new StringBuilder();
		try {
			Process process = Runtime.getRuntime().exec(command);
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				output.append(line).append("\n");
			}
			process.waitFor();
		} catch (IOException | InterruptedException e) {
			LOGGER.error("Error executing system command", e);
		}
		return output.toString();
	}

	public String checkDiskSpace(String path) {
		String command = "df -h " + path;
		return getSystemInfo(command);
	}

	public String searchFiles(String directory, String filename) {
		List<String> args = new ArrayList<>();
		args.add("find");
		args.add(directory);
		args.add("-name");
		args.add(filename);
		try {
			ProcessBuilder pb = new ProcessBuilder(args);
			Process process = pb.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			StringBuilder output = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				output.append(line).append("\n");
			}
			process.waitFor();
			return output.toString();
		} catch (IOException | InterruptedException e) {
			LOGGER.error("Error searching files", e);
			return "";
		}
	}
}
