# OpenAI Controller - API Documentation

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Key Components](#key-components)
4. [API Endpoints](#api-endpoints)
5. [Technical Implementation](#technical-implementation)
6. [Data Models](#data-models)
7. [Configuration](#configuration)
8. [Usage Examples](#usage-examples)
9. [Error Handling](#error-handling)

---

## Overview

The **OpenAI Controller** is a Spring Boot REST controller that provides AI-powered locator healing capabilities for UI automation test failures. It leverages advanced Large Language Models (LLMs) through Groq and Gemini APIs, combined with RAG (Retrieval-Augmented Generation) techniques to intelligently analyze and heal failed test locators.

### Main Purpose
When UI automation tests fail due to broken locators (XPath, CSS selectors, resource IDs, etc.), this controller:
- Analyzes the failed locator against the current page source
- Uses AI to identify why the locator failed
- Provides a healed/corrected locator that works with the current page structure
- Tracks performance metrics and healing success rates

### Supported Automation Types
- **Web Automation** (Selenium, Playwright, Cypress)
- **Mobile Android** (Appium, UIAutomator)
- **Mobile iOS** (Appium, XCUITest)

### Base URL
```
/v1/openai/api
```

### Technology Stack
- **Framework**: Spring Boot 3.1.0
- **Language**: Java 17
- **Database**: MongoDB (for data persistence)
- **Storage**: GridFS (for image storage)
- **AI Models**: Groq (primary), Gemini (fallback)
- **RAG**: Custom implementation for context optimization

---

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      OpenAI Controller                          │
│                   (/v1/openai/api)                             │
└─────────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌──────────────┐    ┌──────────────────┐   ┌──────────────┐
│   Groq API   │    │   Gemini API     │   │  Local       │
│   (Primary)  │    │   (Fallback)     │   │  Healing     │
└──────────────┘    └──────────────────┘   └──────────────┘
        │                     │                     │
        └─────────────────────┼─────────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │   RAG Service    │
                    │  (Context Opt.)  │
                    └──────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌──────────────┐    ┌──────────────────┐   ┌──────────────┐
│  Context     │    │  Locator         │   │  Image       │
│  Analyzer    │    │  Matcher         │   │  Service     │
└──────────────┘    └──────────────────┘   └──────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │    MongoDB       │
                    │   (Persistence)  │
                    └──────────────────┘
```

### Request Flow

1. **Client** sends failed locator + page source
2. **LocatorMatcher** validates if locator is already present
3. **ContextAnalyzerService** analyzes page type (Web/Mobile)
4. **RagService** optimizes page source context (reduces tokens)
5. **AI API** (Groq/Gemini) analyzes and heals the locator
6. **Response Processing** validates and saves healed locator
7. **Performance Tracking** stores RAG and healing metrics

---

## Key Components

### 1. **RagService** (`onemg.analytics.dump.service.RagService`)
**Purpose**: Optimizes context sent to LLMs by extracting only relevant portions of page source

**Key Features**:
- Divides page source into 5 equal chunks
- Scores each chunk based on relevance to failed locator
- Returns the most relevant chunk (reduces token usage by ~80%)
- Maintains XML/HTML structure integrity

**Scoring Algorithm**:
```
- Direct locator match: +10.0 points
- Resource ID match: +8.0 points
- Resource ID parts: +5.0 points
- Package names: +3.0 points
- ID attributes: +6.0 points
- Text content: +4.0 points
- Class/tags: +2.0 points
- Mobile-specific content: +1.0 bonus
```

### 2. **ContextAnalyzerService** (`onemg.analytics.dump.service.ContextAnalyzerService`)
**Purpose**: Analyzes page structure and detects automation type

**Capabilities**:
- Detects automation type (Web/Android/iOS)
- Extracts form elements, interactive elements
- Identifies elements with IDs, classes
- Finds semantically similar elements

**Detection Logic**:
```java
- Android: Contains "android.widget", "android.view", "resource-id"
- iOS: Contains "XCUIElement", "UIAElement"
- Web: Contains HTML tags like "div", "button", "input"
```

### 3. **LocatorMatcher** (`onemg.analytics.dump.service.LocatorMatcher`)
**Purpose**: Validates locators and provides local healing without AI

**Methods**:
- `isLocatorPresent()`: Checks if locator exists in page source
- `ensureXPath()`: Converts any locator to valid XPath
- `healLocator()`: Attempts local healing using heuristics

### 4. **ImageService** (`onemg.analytics.dump.utils.ImageService`)
**Purpose**: Handles screenshot storage in GridFS

**Features**:
- Uploads screenshots (base64/multipart)
- Retrieves images by ID
- Links images with healed elements

---

## API Endpoints

### 1. POST `/get-healed-locator`
**Purpose**: Main endpoint to heal a failed locator using AI

**Request Body** (`HealingModel`):
```json
{
  "locator": "//button[@id='login']",
  "page_source": "<html>...</html>",
  "image_data_id": "optional-screenshot-id"
}
```

**Response** (Success - Already Present):
```json
{
  "id": "64f8a9b1c2d3e4f5a6b7c8d9",
  "locator": "//button[@id='login']",
  "healed_locator": "//button[@id='login']",
  "approach": "already_present",
  "confidence_score": 1.0,
  "model_name": "groq-llama3",
  "source": "groq",
  "rag_stats": {
    "original_length": 45000,
    "optimized_length": 8500,
    "compression_ratio": "81.11%",
    "processing_time_ms": 1250
  },
  "created_date": "2024-10-29T18:30:00.000Z"
}
```

**Response** (Success - Healing Required):
```json
{
  "id": "64f8a9b1c2d3e4f5a6b7c8d9",
  "locator": "//button[@id='login']",
  "healed_locator": "//button[@id='loginBtn']",
  "approach": "healing",
  "confidence_score": 0.95,
  "model_name": "groq-llama3",
  "source": "groq",
  "rag_stats": {
    "original_length": 45000,
    "optimized_length": 8500,
    "compression_ratio": "81.11%",
    "processing_time_ms": 1250
  },
  "created_date": "2024-10-29T18:30:00.000Z"
}
```

**Response** (No Match):
```json
{
  "id": "64f8a9b1c2d3e4f5a6b7c8d9",
  "locator": "//button[@id='login']",
  "healed_locator": "//button[@id='login']",
  "approach": "not_match",
  "confidence_score": 0.3,
  "model_name": "groq-llama3",
  "source": "groq",
  "rag_stats": {
    "original_length": 45000,
    "optimized_length": 8500,
    "compression_ratio": "81.11%",
    "processing_time_ms": 1250
  },
  "created_date": "2024-10-29T18:30:00.000Z"
}
```

**Error Response**:
```json
{
  "Status Code": 400,
  "Status Message": "locator and page_source are mandatory fields",
  "Reference-Id": "req-12345-67890"
}
```

**HTTP Status Codes**:
- `200 OK`: Locator healed successfully
- `400 Bad Request`: Missing required fields
- `500 Internal Server Error`: AI service failure (fallback applied)

**Technical Flow**:
1. Validates request (locator and page_source required)
2. Checks if locator is already present using `LocatorMatcher`
3. Retrieves similar examples from database (top 3 with confidence ≥ 0.80)
4. Calls Groq API with RAG-optimized context
5. On Groq failure, falls back to Gemini API
6. Processes AI response and extracts healed locator
7. Saves to `healed_element` collection
8. Records RAG performance metrics
9. Saves training data for future improvements

**AI Prompt Strategy**:
- System prompt defines healing rules and approach logic
- User prompt includes:
  - Failed locator
  - RAG-optimized page context (not full page source)
  - Similar successful healing examples
  - Special handling for OR conditions and text-based locators

---

### 2. POST `/is_healed`
**Purpose**: Check if a locator has been healed previously

**Request Body**:
```json
{
  "locator": "//button[@id='login']"
}
```

**Response** (Found):
```json
{
  "id": "64f8a9b1c2d3e4f5a6b7c8d9",
  "locator": "//button[@id='login']",
  "healed_locator": "//button[@id='loginBtn']",
  "approach": "healing",
  "confidence_score": 0.95,
  "model_name": "groq-llama3",
  "created_date": "2024-10-29T18:30:00.000Z"
}
```

**Response** (Not Found):
```json
{
  "Status Code": 400,
  "Status Message": "Locator not present in Healed DB : //button[@id='login']",
  "Reference-Id": "req-12345-67890"
}
```

**Logic**:
1. First searches for locator with confidence score ≥ 0.6
2. If not found, returns highest confidence score entry for that locator
3. Returns 400 if locator never healed before

---

### 3. POST `/set-locator-status`
**Purpose**: Update the status of a healed locator (mark as valid)

**Request Body**:
```json
{
  "locator": "//button[@id='login']",
  "page_source": "<html>...</html>"
}
```

**Response** (Success):
```json
{
  "id": "64f8a9b1c2d3e4f5a6b7c8d9",
  "locator": "//button[@id='login']",
  "healed_locator": "//button[@id='loginBtn']",
  "status": "Valid",
  "page_source": "<html>...</html>",
  "confidence_score": 0.95,
  "created_date": "2024-10-29T18:30:00.000Z"
}
```

**Use Case**: After manual verification of healed locator, mark it as valid for future reference

---

### 4. POST `/image/upload-image`
**Purpose**: Upload a screenshot for associating with failed locators

**Request**:
- Content-Type: `multipart/form-data`
- Parameter: `file` (image file)

**Response**:
```json
{
  "id": "64f8a9b1c2d3e4f5a6b7c8d9",
  "image_hex_string": "507f1f77bcf86cd799439011",
  "healed_element_id": null,
  "created_date": "2024-10-29T18:30:00.000Z"
}
```

**HTTP Status**:
- `201 Created`: Image uploaded successfully
- `400 Bad Request`: Upload failed

**Technical Details**:
- Images stored in GridFS (MongoDB binary storage)
- Returns GridFS file ID as `image_hex_string`
- Can be linked to healed element later

---

### 5. GET `/image/{id}`
**Purpose**: Download a screenshot by its GridFS ID

**Request**:
```
GET /v1/openai/api/image/507f1f77bcf86cd799439011
```

**Response**:
- Content-Type: `image/png` (or original type)
- Content-Disposition: `attachment; filename="screenshot.png"`
- Body: Binary image data

**HTTP Status**:
- `200 OK`: Image retrieved
- `404 Not Found`: Image ID doesn't exist

---

### 6. GET `/image/meta/{id}`
**Purpose**: Get metadata about a screenshot

**Request**:
```
GET /v1/openai/api/image/meta/507f1f77bcf86cd799439011
```

**Response**:
```json
{
  "file_name": "screenshot_20241029.png",
  "length": 245678,
  "content/type": "image/png",
  "base64": "data:image/png;base64,iVBORw0KGgoAAAA..."
}
```

---

### 7. GET `/rag/stats`
**Purpose**: Get overall RAG performance statistics

**Request**:
```
GET /v1/openai/api/rag/stats
```

**Response**:
```json
{
  "total_operations": 1250,
  "avg_compression_ratio": "81.23%",
  "avg_processing_time_ms": "1235.67",
  "avg_original_length": "42500",
  "avg_optimized_length": "7982",
  "avg_token_reduction": "34518",
  "automation_type_distribution": {
    "MOBILE_ANDROID": 850,
    "WEB": 320,
    "MOBILE_IOS": 80
  },
  "success_rate": "94.40%"
}
```

**Metrics Explained**:
- **total_operations**: Number of successful RAG operations
- **avg_compression_ratio**: Average context size reduction percentage
- **avg_processing_time_ms**: Average time for RAG processing
- **avg_original_length**: Average original page source length
- **avg_optimized_length**: Average optimized context length
- **avg_token_reduction**: Average characters/tokens saved
- **automation_type_distribution**: Breakdown by automation type
- **success_rate**: Percentage of successful RAG operations

---

### 8. GET `/rag/performance/{locator}`
**Purpose**: Get RAG performance metrics for a specific locator

**Request**:
```
GET /v1/openai/api/rag/performance/%2F%2Fbutton%5B%40id%3D'login'%5D
```
*(Note: URL-encode the locator)*

**Response**:
```json
{
  "total_attempts": 5,
  "successful_attempts": 4,
  "success_rate": "80.00%",
  "best_performance": {
    "compression_ratio": "85.50%",
    "processing_time_ms": 980,
    "optimized_length": 6525,
    "original_length": 45000
  },
  "recent_performances": [
    {
      "id": "64f8a9b1c2d3e4f5a6b7c8d9",
      "locator": "//button[@id='login']",
      "compression_ratio": 85.5,
      "processing_time_ms": 980,
      "success": true,
      "automation_type": "WEB",
      "created_date": "2024-10-29T18:30:00.000Z"
    }
  ]
}
```

**HTTP Status**:
- `200 OK`: Performance data found
- `404 Not Found`: No data for this locator

---

### 9. GET `/rag/health`
**Purpose**: Health check for RAG services

**Request**:
```
GET /v1/openai/api/rag/health
```

**Response** (Healthy):
```json
{
  "rag_enabled": true,
  "configuration_loaded": true,
  "timestamp": 1698604800000,
  "status": "RAG services are operational"
}
```

**Response** (Unhealthy):
```json
{
  "status": "RAG services unavailable",
  "error": "RagConfiguration bean not found"
}
```

**HTTP Status**:
- `200 OK`: Services healthy
- `503 Service Unavailable`: Services down

---

### 10. POST `/rag/test`
**Purpose**: Test RAG extraction for debugging purposes

**Request Body**:
```json
{
  "locator": "//button[@id='login']",
  "page_source": "<html>...</html>",
  "debug": "true"
}
```

**Response**:
```json
{
  "validation_passed": true,
  "context_length": 8500,
  "context_empty": false,
  "original_length": 45000,
  "compression_ratio": "81.11%",
  "extracted_context": "<button id='login'>...</button>",
  "stats": {
    "originalLength": 45000,
    "optimizedLength": 8500,
    "compressionRatio": 81.11
  },
  "locator_analysis": {
    "original_locator": "//button[@id='login']",
    "is_xpath_format": true,
    "is_resource_id_format": false,
    "contains_resource_id": false
  },
  "chunk_analysis": {
    "total_chunks": 5,
    "chunk_size_avg": 9000,
    "chunks": [
      {
        "chunk_number": 1,
        "start_position": 0,
        "end_position": 9000,
        "length": 9000,
        "preview": "<html><head>...",
        "contains_locator": false,
        "contains_resource_id_parts": false
      }
    ]
  },
  "target_element_found_in_source": true
}
```

**Debug Mode**:
- Set `debug: "true"` to get detailed chunk analysis
- Shows how RAG divides and scores page source
- Useful for troubleshooting RAG issues

---

### 11. POST `/rag/analyze-context`
**Purpose**: Analyze page structure and context

**Request Body**:
```json
{
  "page_source": "<html>...</html>",
  "locator": "//button[@id='login']"
}
```

**Response**:
```json
{
  "page_analysis": {
    "automation_type": "WEB",
    "form_elements_count": 12,
    "interactive_elements_count": 45,
    "elements_with_ids_count": 78,
    "elements_with_classes_count": 156,
    "text_content_count": 234
  },
  "rag_optimization": {
    "original_length": 45000,
    "optimized_length": 8500,
    "compression_achieved": "81.11%",
    "relevant_context": "<button id='login'>...</button>"
  },
  "similar_elements": [
    "<button id='loginBtn'>Login</button>",
    "<button class='login-button'>Sign In</button>",
    "<a id='login-link'>Login</a>"
  ]
}
```

**Use Cases**:
- Understand page structure before healing
- Find similar elements for manual inspection
- Validate RAG optimization quality

---

### 12. POST `/is_already_present`
**Purpose**: Check if a locator exists in the page source (local validation)

**Request Body**:
```json
{
  "locator": "//button[@id='login']",
  "page_source": "<html>...</html>"
}
```

**Response**:
```json
{
  "Status Code": 200,
  "Status Message": "Locator not present in Page Source : true",
  "Reference-Id": "req-12345-67890"
}
```

**Technical Details**:
- Uses `LocatorMatcher.isLocatorPresent()`
- Validates XPath, CSS selectors, resource IDs
- No AI involved - pure local validation
- Returns boolean indicating presence

---

### 13. POST `/heal_locally`
**Purpose**: Heal locator using local heuristics (no AI)

**Request Body**:
```json
{
  "locator": "//button[@id='login']",
  "page_source": "<html>...</html>"
}
```

**Response**:
```json
{
  "healed_locator": "//button[@id='loginBtn']",
  "confidence": 0.85,
  "approach": "id_similarity",
  "original_locator": "//button[@id='login']"
}
```

**Use Case**:
- Fast healing without AI latency
- Offline healing capability
- Fallback when AI services unavailable

---

## Technical Implementation

### AI Integration

#### 1. **Groq API (Primary)**
- **Model**: `llama3-70b-8192` (configurable)
- **Temperature**: 0.05 (low for consistency)
- **Max Tokens**: 5000
- **Response Format**: JSON
- **Authentication**: Bearer token

**Request Format**:
```json
{
  "model": "llama3-70b-8192",
  "temperature": 0.05,
  "max_tokens": 5000,
  "response_format": { "type": "json_object" },
  "messages": [
    {
      "role": "system",
      "content": "You are a UI automation locator healing expert..."
    },
    {
      "role": "user",
      "content": "TASK: Analyze and heal the failed locator\n\nFailed Locator: //button[@id='login']\n..."
    }
  ]
}
```

#### 2. **Gemini API (Fallback)**
- **Model**: `gemini-1.5-pro` (configurable)
- **Temperature**: 0.05
- **Max Output Tokens**: 5000
- **Response MIME Type**: `application/json`
- **Authentication**: API key in query parameter

**Request Format**:
```json
{
  "contents": [
    {
      "role": "user",
      "parts": [
        { "text": "TASK: Analyze and heal..." }
      ]
    }
  ],
  "systemInstruction": {
    "parts": [
      { "text": "You are a UI automation locator healing expert..." }
    ]
  },
  "generationConfig": {
    "temperature": 0.05,
    "maxOutputTokens": 5000,
    "responseMimeType": "application/json"
  }
}
```

---

### AI Prompt Engineering

#### System Prompt Components

1. **Core Instructions**:
   - Role definition: "UI automation locator healing expert"
   - Analysis process: Parse page source, check OR conditions, validate text
   - Confidence scoring rules

2. **Critical Rules**:
   - **Text Matching**: Never modify text unless absent
   - **OR Conditions**: Return original if any condition matches
   - **Healing Thresholds**: 
     - ≥60% = healing
     - <60% = not_match
     - 100% = already_present

3. **Output Format**:
   - JSON only: `{ locator, approach, confidence_score }`
   - Always return XPath format
   - Avoid coordinates, bounds, encoded characters

4. **Examples**:
   - 6 example scenarios covering:
     - OR condition matches
     - Simple matches
     - Healing required
     - Complex OR conditions
     - Text healing
     - Case-insensitive text

#### User Prompt Construction

```
TASK: Analyze and heal the failed locator

Failed Locator: {locator}

[OR Condition Warning if applicable]
[Text-Based Locator Warning if applicable]

SIMILAR SUCCESSFUL CASES:
- Original: //button[@id='old'] → Healed: //button[@id='new'] (confidence: 0.95)

Relevant Page Context (RAG-optimized):
{optimized_page_source}
```

---

### RAG (Retrieval-Augmented Generation)

#### Purpose
- Reduce token usage by 80%+ (45,000 → 8,500 chars typical)
- Faster API response times
- Lower costs
- Improved accuracy (less noise)

#### Algorithm

1. **Chunking Strategy**:
   ```
   - Divide page source into exactly 5 equal chunks
   - Each chunk size = total_length / 5
   - Maintain XML/HTML structure integrity
   - Break at complete tags when possible
   ```

2. **Relevance Scoring**:
   ```
   For each chunk:
     score = 0
     
     if contains_full_locator: score += 10.0
     if contains_resource_id: score += 8.0
     if contains_resource_id_parts: score += 5.0
     if contains_package_name: score += 3.0
     if contains_id_attribute: score += 6.0
     if contains_text_content: score += 4.0
     if contains_class_or_tag: score += 2.0
     if is_mobile_content: score += 1.0
     
     return score
   ```

3. **Selection Logic**:
   ```
   - Select chunk with highest score
   - If all scores = 0, use middle chunk (index 2)
   - If middle chunk empty, use first non-empty chunk
   ```

4. **Performance Tracking**:
   ```
   - Original page source length
   - Optimized context length
   - Compression ratio (%)
   - Processing time (ms)
   - Automation type
   - Success/failure
   ```

---

### Locator Validation

#### LocatorMatcher Logic

**XPath Validation**:
```java
1. Parse page source as XML document
2. Compile XPath expression
3. Evaluate against document
4. Return true if nodes found
```

**Web Locator Validation**:
```java
- By ID: doc.select("#" + locator)
- By name: doc.select("[name=" + locator + "]")
- By CSS: doc.select(locator)
```
