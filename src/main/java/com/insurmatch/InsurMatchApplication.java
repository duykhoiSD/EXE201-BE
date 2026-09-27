package com.insurmatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

@SpringBootApplication
public class InsurMatchApplication {

    public static void main(String[] args) {
        loadEnvFile();
        SpringApplication.run(InsurMatchApplication.class, args);
    }

    /**
     * Tự động đọc file .env từ thư mục gốc nếu có (Local dev)
     * mà không cần cài thêm thư viện bên ngoài.
     */
    private static void loadEnvFile() {
        File envFile = new File(".env");
        if (!envFile.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eqIdx = line.indexOf('=');
                if (eqIdx > 0) {
                    String key = line.substring(0, eqIdx).trim();
                    String value = line.substring(eqIdx + 1).trim();

                    // Loại bỏ quotes nếu có (" hoặc ')
                    if ((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }

                    // Riêng mật khẩu app Gmail: loại bỏ khoảng trắng nếu người dùng copy giữ nguyên định dạng 4 ký tự
                    if ("MAIL_PASSWORD".equals(key)) {
                        value = value.replace(" ", "");
                    }

                    // Chỉ set nếu môi trường hệ thống chưa có (ưu tiên biến môi trường thực tế)
                    if (System.getProperty(key) == null && System.getenv(key) == null) {
                        System.setProperty(key, value);
                    }
                }
            }
            System.out.println("[InsurMatch] Successfully loaded environment variables from .env");
        } catch (IOException e) {
            System.err.println("[InsurMatch] Warning: Could not read .env file: " + e.getMessage());
        }
    }
}
