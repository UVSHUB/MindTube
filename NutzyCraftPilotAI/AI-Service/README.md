# NutzyCraft Pilot AI - Python AI Service

AI-powered YouTube content analysis service using FastAPI, Gemini AI, and advanced NLP.

## Features

- **YouTube Transcript Extraction**: Extract video transcripts without downloading using yt-dlp
- **Smart NLP Preprocessing**: Reduce token count by ~40% using spaCy
- **Gemini AI Analysis**: Get professional content strategy insights
- **Structured JSON Output**: Easy integration with Java backend
- **RESTful API**: FastAPI with automatic OpenAPI documentation

## Quick Start

### 1. Install Dependencies

```bash
# Create virtual environment
python -m venv venv

# Activate virtual environment
# Windows:
venv\Scripts\activate
# Linux/Mac:
source venv/bin/activate

# Install packages
pip install -r requirements.txt

# Download spaCy model
python -m spacy download en_core_web_sm
```

### 2. Configure Environment

```bash
# Copy the example env file
copy .env.example .env

# Edit .env and add your Gemini API key
# Get your key from: https://makersuite.google.com/app/apikey
GEMINI_API_KEY=your_actual_api_key_here
```

### 3. Run the Service

```bash
# Development mode (auto-reload)
python main.py

# Or with uvicorn directly
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

The service will be available at:
- API: http://localhost:8000
- Interactive Docs: http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc

## API Endpoints

### POST /analyze

Analyze a YouTube video and get content strategy insights.

**Request:**
```json
{
    "youtube_url": "https://www.youtube.com/watch?v=VIDEO_ID",
    "user_id": 123
}
```

**Response:**
```json
{
    "hook_score": 8.5,
    "retention_score": 7.8,
    "seo_score": 6.9,
    "craft_score": 7.7,
    "strengths": [
        "Strong opening hook that immediately captures attention",
        "Clear and engaging storytelling throughout",
        "Good pacing with natural transitions"
    ],
    "improvements": [
        "Add more specific keywords in the first 30 seconds",
        "Include call-to-action earlier in the video",
        "Improve thumbnail-title alignment"
    ],
    "seo_keywords": [
        "content strategy",
        "youtube growth",
        "video optimization",
        "audience retention",
        "engagement tactics"
    ],
    "title_suggestions": [
        "How to 10X Your YouTube Views with This Simple Strategy",
        "The Secret to Viral YouTube Content (Revealed)",
        "YouTube Algorithm Hacks Every Creator Should Know"
    ],
    "summary": "This video explores effective content strategies for YouTube creators. It covers audience retention techniques and SEO optimization methods.",
    "success": true,
    "error": null
}
```

### GET /health

Health check endpoint to verify service status.

### POST /test-transcript

Test endpoint to verify transcript extraction and preprocessing without running full analysis.

## Architecture

### 1. TranscriptExtractor
- Uses yt-dlp to extract YouTube transcripts
- No video download required (--skip-download flag)
- Handles automatic subtitles
- Fallback mechanisms for different subtitle formats

### 2. NLPPreprocessor
- Removes timestamps and speaker labels
- Eliminates filler words (um, uh, like, you know)
- Removes duplicate sentences
- Uses spaCy for advanced preprocessing
- Achieves ~40% token reduction

### 3. GeminiAnalyzer
- Configures Gemini as "Senior Content Strategist" persona
- Sends cleaned transcript for analysis
- Enforces structured JSON output
- Returns actionable insights

## Token Reduction Strategy

The preprocessing pipeline reduces tokens by approximately 40% through:

1. **Timestamp Removal** (~5% reduction)
2. **Filler Word Elimination** (~15% reduction)
3. **Duplicate Sentence Removal** (~10% reduction)
4. **Stop Word Filtering** (~10% reduction)

This significantly reduces API costs while maintaining content quality.

## Integration with Java Backend

Your Java `AnalysisController` should make HTTP POST requests to this service:

```java
@PostMapping("/api/analysis/analyze")
public ResponseEntity<?> analyzeVideo(@RequestBody AnalysisRequest request) {
    // 1. Call Python AI Service
    RestTemplate restTemplate = new RestTemplate();
    String pythonServiceUrl = "http://localhost:8000/analyze";
    
    // 2. Send request
    AnalysisResponse response = restTemplate.postForObject(
        pythonServiceUrl,
        request,
        AnalysisResponse.class
    );
    
    // 3. Save to database
    Analysis analysis = new Analysis();
    analysis.setCraftScore(response.getCraftScore());
    // ... set other fields
    analysisRepository.save(analysis);
    
    return ResponseEntity.ok(analysis);
}
```

## Development

### Run Tests
```bash
pytest tests/
```

### Format Code
```bash
black main.py
```

### Lint
```bash
pylint main.py
```

## Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| GEMINI_API_KEY | Google Gemini API key | Yes |
| SERVICE_PORT | Port to run service on | No (default: 8000) |
| LOG_LEVEL | Logging level | No (default: INFO) |

## Error Handling

The service includes comprehensive error handling:
- Invalid YouTube URLs
- Missing transcripts/captions
- Gemini API failures
- Network timeouts
- JSON parsing errors

All errors return proper HTTP status codes and descriptive messages.

## Performance

- Average processing time: 5-10 seconds per video
- Concurrent request handling via FastAPI
- Efficient memory usage with streaming
- Token-optimized for cost reduction

## Troubleshooting

### "yt-dlp not found"
Install yt-dlp: `pip install yt-dlp`

### "spaCy model not found"
Download model: `python -m spacy download en_core_web_sm`

### "Gemini API error"
Check your API key in .env file and verify quota at Google AI Studio

### "No subtitles available"
The video must have captions enabled (auto-generated or manual)

## License

Part of NutzyCraft Pilot AI project.
