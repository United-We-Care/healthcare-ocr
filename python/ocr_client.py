#!/usr/bin/env python3
"""
Meditrail OCR API Client - Python Example

This script demonstrates how to use the Meditrail OCR API to process
medical documents and images for clinical data extraction.
"""

import requests
import json
import os
import sys
from typing import Optional, Dict, Any
from pathlib import Path


class MeditrailOCRClient:
    """Client for Meditrail OCR API"""
    
    def __init__(self, api_key: str, base_url: str = "https://meditrail.unitedwecare.com/api/v1"):
        """
        Initialize the OCR client
        
        Args:
            api_key: Your Meditrail API key
            base_url: Base URL for the API (default: production URL)
        """
        self.api_key = api_key
        self.base_url = base_url.rstrip('/')
        self.headers = {
            'X-API-Key': api_key
        }
    
    def process_document(
        self, 
        file_path: str, 
        text: Optional[str] = None, 
        system_prompt: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Process a document using the OCR API
        
        Args:
            file_path: Path to the file to process (PDF, JPG, PNG)
            text: Optional context or annotation
            system_prompt: Optional prompt to customize extraction logic
            
        Returns:
            API response as dictionary
            
        Raises:
            FileNotFoundError: If the file doesn't exist
            requests.RequestException: If the API request fails
        """
        # Validate file exists
        if not os.path.exists(file_path):
            raise FileNotFoundError(f"File not found: {file_path}")
        
        # Check file size (50MB limit)
        file_size = os.path.getsize(file_path)
        if file_size > 50 * 1024 * 1024:  # 50MB in bytes
            raise ValueError(f"File too large: {file_size / (1024*1024):.2f}MB (max: 50MB)")
        
        # Prepare the request
        url = f"{self.base_url}/ocr/process"
        
        # Prepare multipart form data
        files = {
            'file': (os.path.basename(file_path), open(file_path, 'rb'))
        }
        
        data = {}
        if text:
            data['text'] = text
        if system_prompt:
            data['system_prompt'] = system_prompt
        
        try:
            response = requests.post(
                url,
                headers=self.headers,
                files=files,
                data=data,
                timeout=60  # 60 second timeout
            )
            
            # Close the file
            files['file'][1].close()
            
            # Handle different response status codes
            if response.status_code == 200:
                return response.json()
            elif response.status_code == 400:
                error_detail = response.json().get('detail', 'Bad Request')
                raise requests.RequestException(f"Bad Request: {error_detail}")
            elif response.status_code == 413:
                raise requests.RequestException("File too large (max 50MB)")
            elif response.status_code == 429:
                error_detail = response.json().get('detail', {})
                if isinstance(error_detail, dict):
                    message = error_detail.get('message', 'Usage limit exceeded')
                    raise requests.RequestException(f"Usage limit exceeded: {message}")
                else:
                    raise requests.RequestException("Usage limit exceeded")
            elif response.status_code == 500:
                error_detail = response.json().get('detail', 'Internal Server Error')
                raise requests.RequestException(f"Internal Server Error: {error_detail}")
            else:
                response.raise_for_status()
                
        except requests.exceptions.Timeout:
            raise requests.RequestException("Request timed out")
        except requests.exceptions.ConnectionError:
            raise requests.RequestException("Connection error")
        except json.JSONDecodeError:
            raise requests.RequestException("Invalid JSON response")
        finally:
            # Ensure file is closed even if an exception occurs
            if 'files' in locals() and files['file'][1]:
                files['file'][1].close()


def main():
    """Example usage of the Meditrail OCR Client"""
    
    # Configuration
    API_KEY = "mt_LaRysGk4Ap9ytP29VKtF9E_8MECKcLcZSgRYLAF2KP5qr_UVPdCUqchtJ9fM7joiXX8z"
    
    # Initialize client
    client = MeditrailOCRClient(API_KEY)
    
    # Example 1: Process a medical image
    print("=== Example 1: Processing Medical Image ===")
    try:
        # Replace with your actual file path
        file_path = "sample_files/chest_xray.jpg"
        
        if os.path.exists(file_path):
            result = client.process_document(
                file_path=file_path,
                text="Chest X-ray examination",
                system_prompt="Extract key clinical findings and abnormalities"
            )
            
            print(f"Success! Document ID: {result.get('id')}")
            print(f"Clinical Relevance: {result.get('clinical_relevance')}")
            print(f"Doctor Names: {result.get('doctor_names')}")
            
            # Parse the response field (it's a JSON string)
            response_data = json.loads(result.get('response', '{}'))
            print(f"Document Type: {response_data.get('document_type', 'N/A')}")
            print(f"Summary: {response_data.get('summary', 'N/A')}")
            
            # Print metadata
            metadata = result.get('metadata', {})
            print(f"Original File: {metadata.get('original_file_name')}")
            print(f"File Size: {metadata.get('file_size')}")
            print(f"Page Count: {metadata.get('page_count')}")
            
        else:
            print(f"Sample file not found: {file_path}")
            print("Please add a sample medical document to test with.")
            
    except Exception as e:
        print(f"Error processing document: {e}")
    
    print("\n" + "="*50 + "\n")
    
    # Example 2: Process a PDF document
    print("=== Example 2: Processing PDF Document ===")
    try:
        # Replace with your actual file path
        file_path = "sample_files/prescription.pdf"
        
        if os.path.exists(file_path):
            result = client.process_document(
                file_path=file_path,
                text="Prescription document",
                system_prompt="Extract medication names, dosages, and doctor information"
            )
            
            print(f"Success! Document ID: {result.get('id')}")
            print(f"Clinical Relevance: {result.get('clinical_relevance')}")
            
            # Parse the response field
            response_data = json.loads(result.get('response', '{}'))
            print(f"Document Type: {response_data.get('document_type', 'N/A')}")
            print(f"Summary: {response_data.get('summary', 'N/A')}")
            
        else:
            print(f"Sample file not found: {file_path}")
            print("Please add a sample PDF document to test with.")
            
    except Exception as e:
        print(f"Error processing document: {e}")


if __name__ == "__main__":
    main()
