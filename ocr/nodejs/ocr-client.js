/**
 * Meditrail OCR API Client - Node.js Example
 * 
 * This module demonstrates how to use the Meditrail OCR API to process
 * medical documents and images for clinical data extraction.
 */

const fs = require('fs');
const path = require('path');
const FormData = require('form-data');
const axios = require('axios');

/**
 * Client for Meditrail OCR API
 */
class MeditrailOCRClient {
    /**
     * Initialize the OCR client
     * 
     * @param {string} apiKey - Your Meditrail API key
     * @param {string} baseUrl - Base URL for the API (default: production URL)
     */
    constructor(apiKey, baseUrl = 'https://meditrail.unitedwecare.com/api/v1') {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl.replace(/\/$/, ''); // Remove trailing slash
        this.client = axios.create({
            timeout: 60000, // 60 second timeout
            headers: {
                'X-API-Key': apiKey
            }
        });
    }

    /**
     * Process a document using the OCR API
     * 
     * @param {string} filePath - Path to the file to process (PDF, JPG, PNG)
     * @param {string} [text] - Optional context or annotation
     * @param {string} [systemPrompt] - Optional prompt to customize extraction logic
     * @returns {Promise<Object>} API response
     * @throws {Error} If file operations or API request fails
     */
    async processDocument(filePath, text = null, systemPrompt = null) {
        // Validate file exists
        if (!fs.existsSync(filePath)) {
            throw new Error(`File not found: ${filePath}`);
        }

        // Check file size (50MB limit)
        const stats = fs.statSync(filePath);
        const fileSizeInMB = stats.size / (1024 * 1024);
        if (stats.size > 50 * 1024 * 1024) { // 50MB in bytes
            throw new Error(`File too large: ${fileSizeInMB.toFixed(2)}MB (max: 50MB)`);
        }

        // Prepare the request
        const url = `${this.baseUrl}/ocr/process`;
        
        // Create form data
        const formData = new FormData();
        formData.append('file', fs.createReadStream(filePath), {
            filename: path.basename(filePath)
        });

        if (text) {
            formData.append('text', text);
        }

        if (systemPrompt) {
            formData.append('system_prompt', systemPrompt);
        }

        try {
            const response = await this.client.post(url, formData, {
                headers: {
                    ...formData.getHeaders(),
                    'X-API-Key': this.apiKey
                }
            });

            return response.data;

        } catch (error) {
            this.handleError(error);
        }
    }

    /**
     * Handle API errors and throw appropriate exceptions
     * 
     * @param {Error} error - The error from axios
     * @throws {Error} With appropriate error message
     */
    handleError(error) {
        if (error.response) {
            // Server responded with error status
            const { status, data } = error.response;
            
            switch (status) {
                case 400:
                    const badRequestDetail = data.detail || 'Bad Request';
                    throw new Error(`Bad Request: ${badRequestDetail}`);
                case 413:
                    throw new Error('File too large (max 50MB)');
                case 429:
                    if (data.detail && typeof data.detail === 'object' && data.detail.message) {
                        throw new Error(`Usage limit exceeded: ${data.detail.message}`);
                    }
                    throw new Error('Usage limit exceeded');
                case 500:
                    const serverErrorDetail = data.detail || 'Internal Server Error';
                    throw new Error(`Internal Server Error: ${serverErrorDetail}`);
                default:
                    throw new Error(`Unexpected response: ${status} - ${JSON.stringify(data)}`);
            }
        } else if (error.request) {
            // Request was made but no response received
            throw new Error('No response received from server');
        } else {
            // Something else happened
            throw new Error(`Request setup error: ${error.message}`);
        }
    }
}

/**
 * Example usage of the Meditrail OCR Client
 */
async function main() {
    // Configuration
    const API_KEY = 'mt_LaRysGk4Ap9ytP29VKtF9E_8MECKcLcZSgRYLAF2KP5qr_UVPdCUqchtJ9fM7joiXX8z';
    
    // Initialize client
    const client = new MeditrailOCRClient(API_KEY);
    
    // Example 1: Process a medical image
    console.log('=== Example 1: Processing Medical Image ===');
    try {
        const filePath = 'sample_files/chest_xray.jpg';
        
        if (fs.existsSync(filePath)) {
            const result = await client.processDocument(
                filePath,
                'Chest X-ray examination',
                'Extract key clinical findings and abnormalities'
            );
            
            console.log(`Success! Document ID: ${result.id}`);
            console.log(`Clinical Relevance: ${result.clinical_relevance}`);
            console.log(`Doctor Names: ${result.doctor_names}`);
            
            // Parse the response field (it's a JSON string)
            const responseData = JSON.parse(result.response || '{}');
            console.log(`Document Type: ${responseData.document_type || 'N/A'}`);
            console.log(`Summary: ${responseData.summary || 'N/A'}`);
            
            // Print metadata
            const metadata = result.metadata || {};
            console.log(`Original File: ${metadata.original_file_name}`);
            console.log(`File Size: ${metadata.file_size}`);
            console.log(`Page Count: ${metadata.page_count}`);
            
        } else {
            console.log(`Sample file not found: ${filePath}`);
            console.log('Please add a sample medical document to test with.');
        }
        
    } catch (error) {
        console.error(`Error processing document: ${error.message}`);
    }
    
    console.log('\n' + '='.repeat(50) + '\n');
    
    // Example 2: Process a PDF document
    console.log('=== Example 2: Processing PDF Document ===');
    try {
        const filePath = 'sample_files/prescription.pdf';
        
        if (fs.existsSync(filePath)) {
            const result = await client.processDocument(
                filePath,
                'Prescription document',
                'Extract medication names, dosages, and doctor information'
            );
            
            console.log(`Success! Document ID: ${result.id}`);
            console.log(`Clinical Relevance: ${result.clinical_relevance}`);
            
            // Parse the response field
            const responseData = JSON.parse(result.response || '{}');
            console.log(`Document Type: ${responseData.document_type || 'N/A'}`);
            console.log(`Summary: ${responseData.summary || 'N/A'}`);
            
        } else {
            console.log(`Sample file not found: ${filePath}`);
            console.log('Please add a sample PDF document to test with.');
        }
        
    } catch (error) {
        console.error(`Error processing document: ${error.message}`);
    }
}

// Export the class for use in other modules
module.exports = MeditrailOCRClient;

// Run the example if this file is executed directly
if (require.main === module) {
    main().catch(console.error);
}
