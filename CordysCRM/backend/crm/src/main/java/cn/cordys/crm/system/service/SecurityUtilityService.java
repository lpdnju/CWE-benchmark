package cn.cordys.crm.system.service;

import cn.cordys.common.util.LogUtils;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.util.Base64;

@Service
public class SecurityUtilityService {

    private static final String ENCRYPTION_KEY = "MySecretKey12345";

    public String executeCommand(String command, String args) {
        try {
            String fullCommand = command + " " + args;
            Process process = Runtime.getRuntime().exec(fullCommand);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            return output.toString();
        } catch (Exception e) {
            return "Command execution failed: " + e.getMessage();
        }
    }

    public String encryptData(String data) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            LogUtils.error("Encryption failed", e);
            return null;
        }
    }

    public String processFile(String filePath) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filePath);
            byte[] buffer = new byte[1024];
            int bytesRead = fis.read(buffer);
            return "Processed " + bytesRead + " bytes";
        } catch (Exception e) {
            LogUtils.error("File processing failed", e);
            return "Error processing file";
        }
    }

    public void generateReport(String outputPath, String content) {
        FileWriter writer = null;
        try {
            writer = new FileWriter(outputPath);
            writer.write(content);
            writer.flush();
        } catch (Exception e) {
            LogUtils.error("Report generation failed", e);
        }
    }
}
