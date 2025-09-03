package com.meditrail.ocr;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Meditrail OCR API Client - Java Example
 * 
 * This class demonstrates how to use the Meditrail OCR API to process
 * medical documents and images for clinical data extraction.
 */
public class MeditrailOCRClient {
    
    private final String apiKey;
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    /**
     * Initialize the OCR client
     * 
     * @param apiKey Your Meditrail API key
     * @param baseUrl Base URL for the API (default: production URL)
     */
    public MeditrailOCRClient(String apiKey, String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Initialize the OCR client with default production URL
     * 
     * @param apiKey Your Meditrail API key
     */
    public MeditrailOCRClient(String apiKey) {
        this(apiKey, "https://meditrail.unitedwecare.com/api/v1");
    }
    
    /**
     * Process a document using the OCR API
     * 
     * @param filePath Path to the file to process (PDF, JPG, PNG)
     * @param text Optional context or annotation
     * @param systemPrompt Optional prompt to customize extraction logic
     * @return API response as JsonNode
     * @throws IOException If file operations fail
     * @throws InterruptedException If the request is interrupted
     * @throws MeditrailAPIException If the API request fails
     */
    public JsonNode processDocument(String filePath, String text, String systemPrompt) 
            throws IOException, InterruptedException, MeditrailAPIException {
        
        // Validate file exists
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("File not found: " + filePath);
        }
        
        // Check file size (50MB limit)
        long fileSize = Files.size(path);
        if (fileSize > 50 * 1024 * 1024) { // 50MB in bytes
            throw new IllegalArgumentException(
                String.format("File too large: %.2fMB (max: 50MB)", fileSize / (1024.0 * 1024.0))
            );
        }
        
        // Prepare the request
        String url = baseUrl + "/ocr/process";
        
        // Create multipart form data
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        StringBuilder formData = new StringBuilder();
        
        // Add file
        formData.append("--").append(boundary).append("\r\n");
        formData.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                .append(path.getFileName()).append("\"\r\n");
        formData.append("Content-Type: application/octet-stream\r\n\r\n");
        
        // Add optional text parameter
        if (text != null && !text.trim().isEmpty()) {
            formData.append("--").append(boundary).append("\r\n");
            formData.append("Content-Disposition: form-data; name=\"text\"\r\n\r\n");
            formData.append(text).append("\r\n");
        }
        
        // Add optional system_prompt parameter
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            formData.append("--").append(boundary).append("\r\n");
            formData.append("Content-Disposition: form-data; name=\"system_prompt\"\r\n\r\n");
            formData.append(systemPrompt).append("\r\n");
        }
        
        formData.append("--").append(boundary).append("--\r\n");
        
        // Read file content
        byte[] fileContent = Files.readAllBytes(path);
        
        // Combine form data with file content
        ByteArrayOutputStream requestBody = new ByteArrayOutputStream();
        requestBody.write(formData.toString().getBytes());
        requestBody.write(fileContent);
        requestBody.write(("\r\n--" + boundary + "--\r\n").getBytes());
        
        // Create HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-API-Key", apiKey)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody.toByteArray()))
                .build();
        
        // Send request
        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
        
        // Handle response
        return handleResponse(response);
    }
    
    /**
     * Process a document with only file path (no optional parameters)
     * 
     * @param filePath Path to the file to process
     * @return API response as JsonNode
     * @throws IOException If file operations fail
     * @throws InterruptedException If the request is interrupted
     * @throws MeditrailAPIException If the API request fails
     */
    public JsonNode processDocument(String filePath) 
            throws IOException, InterruptedException, MeditrailAPIException {
        return processDocument(filePath, null, null);
    }
    
    /**
     * Handle API response and throw appropriate exceptions
     * 
     * @param response HTTP response
     * @return Parsed JSON response
     * @throws MeditrailAPIException If the API request fails
     */
    private JsonNode handleResponse(HttpResponse<String> response) throws MeditrailAPIException {
        int statusCode = response.statusCode();
        String responseBody = response.body();
        
        try {
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            
            switch (statusCode) {
                case 200:
                    return jsonResponse;
                case 400:
                    String detail = jsonResponse.has("detail") ? 
                            jsonResponse.get("detail").asText() : "Bad Request";
                    throw new MeditrailAPIException("Bad Request: " + detail, statusCode);
                case 413:
                    throw new MeditrailAPIException("File too large (max 50MB)", statusCode);
                case 429:
                    if (jsonResponse.has("detail")) {
                        JsonNode detailNode = jsonResponse.get("detail");
                        if (detailNode.isObject() && detailNode.has("message")) {
                            throw new MeditrailAPIException(
                                "Usage limit exceeded: " + detailNode.get("message").asText(), 
                                statusCode
                            );
                        }
                    }
                    throw new MeditrailAPIException("Usage limit exceeded", statusCode);
                case 500:
                    String errorDetail = jsonResponse.has("detail") ? 
                            jsonResponse.get("detail").asText() : "Internal Server Error";
                    throw new MeditrailAPIException("Internal Server Error: " + errorDetail, statusCode);
                default:
                    throw new MeditrailAPIException(
                        "Unexpected response: " + statusCode + " - " + responseBody, 
                        statusCode
                    );
            }
        } catch (IOException e) {
            throw new MeditrailAPIException("Invalid JSON response: " + responseBody, statusCode);
        }
    }
    
    /**
     * Custom exception for Meditrail API errors
     */
    public static class MeditrailAPIException extends Exception {
        private final int statusCode;
        
        public MeditrailAPIException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }
        
        public int getStatusCode() {
            return statusCode;
        }
    }
    
    /**
     * Example usage of the Meditrail OCR Client
     */
    public static void main(String[] args) {
        // Configuration
        String apiKey = "mt_LaRysGk4Ap9ytP29VKtF9E_8MECKcLcZSgRYLAF2KP5qr_UVPdCUqchtJ9fM7joiXX8z";
        
        // Initialize client
        MeditrailOCRClient client = new MeditrailOCRClient(apiKey);
        
        // Example 1: Process a medical image
        System.out.println("=== Example 1: Processing Medical Image ===");
        try {
            String filePath = "sample_files/chest_xray.jpg";
            
            if (Files.exists(Paths.get(filePath))) {
                JsonNode result = client.processDocument(
                    filePath,
                    "Chest X-ray examination",
                    "Extract key clinical findings and abnormalities"
                );
                
                System.out.println("Success! Document ID: " + result.get("id").asText());
                System.out.println("Clinical Relevance: " + result.get("clinical_relevance").asBoolean());
                System.out.println("Doctor Names: " + result.get("doctor_names").asText());
                
                // Parse the response field (it's a JSON string)
                JsonNode responseData = objectMapper.readTree(result.get("response").asText());
                System.out.println("Document Type: " + responseData.get("document_type").asText());
                System.out.println("Summary: " + responseData.get("summary").asText());
                
                // Print metadata
                JsonNode metadata = result.get("metadata");
                System.out.println("Original File: " + metadata.get("original_file_name").asText());
                System.out.println("File Size: " + metadata.get("file_size").asText());
                System.out.println("Page Count: " + metadata.get("page_count").asInt());
                
            } else {
                System.out.println("Sample file not found: " + filePath);
                System.out.println("Please add a sample medical document to test with.");
            }
            
        } catch (Exception e) {
            System.err.println("Error processing document: " + e.getMessage());
        }
        
        System.out.println("\n" + "=".repeat(50) + "\n");
        
        // Example 2: Process a PDF document
        System.out.println("=== Example 2: Processing PDF Document ===");
        try {
            String filePath = "sample_files/prescription.pdf";
            
            if (Files.exists(Paths.get(filePath))) {
                JsonNode result = client.processDocument(
                    filePath,
                    "Prescription document",
                    "Extract medication names, dosages, and doctor information"
                );
                
                System.out.println("Success! Document ID: " + result.get("id").asText());
                System.out.println("Clinical Relevance: " + result.get("clinical_relevance").asBoolean());
                
                // Parse the response field
                JsonNode responseData = objectMapper.readTree(result.get("response").asText());
                System.out.println("Document Type: " + responseData.get("document_type").asText());
                System.out.println("Summary: " + responseData.get("summary").asText());
                
            } else {
                System.out.println("Sample file not found: " + filePath);
                System.out.println("Please add a sample PDF document to test with.");
            }
            
        } catch (Exception e) {
            System.err.println("Error processing document: " + e.getMessage());
        }
    }
}
