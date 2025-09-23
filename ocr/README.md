
# 🏥 MediScan OCR API – Healthcare Document & Clinical Data Extraction

The **MediScan OCR API** processes medical documents (PDFs and images) for **OCR transcription** and **structured clinical extraction**, making them EMR/EHR-ready.

✅ **HIPAA & SOC 2 Compliant**  
✅ **Supports Handwriting, Medical Scans, Prescriptions**  
✅ **API-first** with ready examples in **Python, Java, and Node.js**

---

## 🔍 API Overview

- **Endpoint:**  
  `POST https://meditrail.unitedwecare.com/api/v1/ocr/process`

- **Authentication:**  
  API Key via request header  
  ```http
  X-API-Key: your_api_key_here
```
**Supported File Types:** PDF, JPG, JPEG, PNG (max 50MB)

### 🚀 Features

- ✅ OCR transcription of medical documents and images
- ✅ Automatic extraction of clinical data, labs, medications, diagnoses
- ✅ Support for multi-page PDFs & medical image formats
- ✅ Custom system prompts for contextual extraction
- ✅ Built-in error handling & usage limits

## ⚡ Quick Start

### Python Example

```bash
cd ocr/python
pip install -r requirements.txt
python ocr_client.py
```

### Java Example

```bash
cd ocr/java
mvn clean compile exec:java
```

### Node.js Example

```bash
cd ocr/nodejs
npm install
npm start
```

## API Key Configuration

Replace the API key in each example with your actual Meditrail API key:

```python
# Python
API_KEY = "your_actual_api_key_here"
```

```java
// Java
String apiKey = "your_actual_api_key_here";
```

```javascript
// Node.js
const API_KEY = 'your_actual_api_key_here';
```

## File Structure

```
ocr/
├── README.md
├── python/
│   ├── ocr_client.py
│   └── requirements.txt
├── java/
│   ├── pom.xml
│   └── src/main/java/com/meditrail/ocr/
│       └── MeditrailOCRClient.java
├── nodejs/
│   ├── ocr-client.js
│   └── package.json
└── sample_files/
    ├── chest_xray.jpg
    ├── MRI_CT_Scan.pdf
    └── hw_prescription.jpg
```

## API Response Format

### Success Response (200)

```json
{
  "id": "19973f06-32e9-41f9-81b1-bb3b757e39f8",
  "response": "{\"document_type\": \"Chest X-ray\", \"summary\": \"...\"}",
  "clinical_relevance": true,
  "doctor_names": "N/A",
  "metadata": {
    "original_file_name": "chest-1.jpg",
    "new_file_name": "medical-image_2025-01-22T10:00:00Z.jpg",
    "file_type": "jpg",
    "file_size": "71.98KB",
    "text": "string",
    "category_name": "medical image",
    "doctor_name": "N/A",
    "page_count": 1
  }
}
```

### Error Responses

| Status | Reason | Description |
|--------|--------|-------------|
| 400 | Bad Request | Unsupported file type or invalid request |
| 413 | File Too Large | File exceeds 50MB limit |
| 429 | Usage Limit Exceeded | Daily usage limit reached |
| 500 | Internal Server Error | Server-side processing error |

## Usage Examples

### Basic Document Processing

```python
# Python
client = MeditrailOCRClient(API_KEY)
result = client.process_document("path/to/document.pdf")
```

```java
// Java
MeditrailOCRClient client = new MeditrailOCRClient(apiKey);
JsonNode result = client.processDocument("path/to/document.pdf");
```

```javascript
// Node.js
const client = new MeditrailOCRClient(API_KEY);
const result = await client.processDocument("path/to/document.pdf");
```

### Advanced Processing with Context

```python
# Python
result = client.process_document(
    file_path="chest_xray.jpg",
    text="Chest X-ray examination",
    system_prompt="Extract key clinical findings and abnormalities"
)
```

```java
// Java
JsonNode result = client.processDocument(
    "chest_xray.jpg",
    "Chest X-ray examination",
    "Extract key clinical findings and abnormalities"
);
```

```javascript
// Node.js
const result = await client.processDocument(
    "chest_xray.jpg",
    "Chest X-ray examination",
    "Extract key clinical findings and abnormalities"
);
```

## Error Handling

All examples include comprehensive error handling for:

- File not found errors
- File size validation (50MB limit)
- Network timeouts
- API rate limiting (429 errors)
- Server errors (500 errors)
- Invalid responses

## Requirements

### Python
- Python 3.6+
- requests >= 2.31.0

### Java
- Java 11+
- Maven 3.6+
- Jackson 2.15.2

### Node.js
- Node.js 14.0+
- axios ^1.6.0
- form-data ^4.0.0

## 🧪 Sample Files

Use the provided test files in /sample_files/:

- `chest_xray.jpg` → medical image
- `MRI_CT_Scan.pdf` → MRI/CT scan report
- `hw_prescription.jpg` → handwritten prescription


## API Limits

- **File Size:** Maximum 50MB per request
- **File Types:** PDF, JPG, JPEG, PNG
- **Usage Limits:** Based on your plan's daily limit
- **Timeout:** 60 seconds per request

## Billing

- **PDFs:** Billed by actual page count
- **Images:** Counted as 1 page each

## Support

For API support and questions:
- Check the error responses for detailed information
- Monitor your usage limits to avoid 429 errors
- Ensure files are in supported formats and under size limits

## Explore & Access

- **Get API Access:** Contact [contactus@unitedwecare.com](mailto:contactus@unitedwecare.com) to get access to the MediScan OCR API
