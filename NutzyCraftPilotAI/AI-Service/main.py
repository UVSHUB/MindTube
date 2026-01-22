"""
NutzyCraft Pilot AI - Python AI Service
This service handles YouTube video analysis using Gemini AI
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, HttpUrl, Field
from typing import Optional, Dict, Any
import subprocess
import json
import re
import os
import logging
from pathlib import Path
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Initialize FastAPI app
app = FastAPI(
    title="NutzyCraft Pilot AI Service",
    description="AI-powered YouTube content analysis service",
    version="1.0.0"
)

# CORS middleware to allow Java backend to communicate
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # In production, replace with specific origins
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Pydantic models for request/response
class AnalysisRequest(BaseModel):
    """Request model for video analysis"""
    youtube_url: HttpUrl = Field(..., description="YouTube video URL to analyze")
    user_id: Optional[str] = Field(None, description="User ID for tracking (UUID)")

class AnalysisResponse(BaseModel):
    """Structured response model matching Gemini output"""
    hook_score: float = Field(..., ge=0, le=10, description="Hook effectiveness score (0-10)")
    retention_score: float = Field(..., ge=0, le=10, description="Retention potential score (0-10)")
    seo_score: float = Field(..., ge=0, le=10, description="SEO optimization score (0-10)")
    craft_score: float = Field(..., ge=0, le=10, description="Overall craft score (0-10)")
    strengths: list[str] = Field(..., description="Key strengths of the video")
    improvements: list[str] = Field(..., description="Suggested improvements")
    seo_keywords: list[str] = Field(..., description="Recommended SEO keywords")
    title_suggestions: list[str] = Field(..., description="Alternative title suggestions")
    summary: str = Field(..., description="Brief video summary")
    success: bool = Field(True, description="Analysis success status")
    error: Optional[str] = Field(None, description="Error message if any")


class TranscriptExtractor:
    """Handles YouTube transcript extraction using yt-dlp"""
    
    @staticmethod
    def extract_transcript(youtube_url: str) -> str:
        """
        Extract transcript from YouTube video without downloading the video
        
        Args:
            youtube_url: The YouTube video URL
            
        Returns:
            Raw transcript text
            
        Raises:
            Exception: If transcript extraction fails
        """
        try:
            logger.info(f"Extracting transcript from: {youtube_url}")
            
            # Try auto-generated captions first
            import sys
            command = [
                sys.executable, "-m", "yt_dlp",
                "--skip-download",
                "--write-auto-subs",
                "--write-subs",  # Also try manual subs
                "--sub-lang", "en",
                "--sub-format", "json3",
                "--output", "temp_transcript",
                "--quiet",  # Suppress warnings
                "--no-warnings",
                youtube_url
            ]
            
            result = subprocess.run(
                command,
                capture_output=True,
                text=True,
                timeout=60
            )
            
            # Check for subtitle files (try multiple patterns)
            subtitle_patterns = [
                "temp_transcript.en.json3",
                "temp_transcript.json3",
                "temp_transcript.en-US.json3",
                "temp_transcript.en-GB.json3"
            ]
            
            subtitle_file = None
            for pattern in subtitle_patterns:
                file_path = Path(pattern)
                if file_path.exists():
                    subtitle_file = file_path
                    break
            
            if not subtitle_file:
                # Clean up any partial files
                for p in Path(".").glob("temp_transcript*"):
                    p.unlink(missing_ok=True)
                raise Exception("No captions/subtitles available for this video. Please try a video with closed captions enabled.")
            
            # Parse the JSON subtitle file
            with open(subtitle_file, 'r', encoding='utf-8') as f:
                subtitle_data = json.load(f)
            
            # Extract text from subtitle events
            transcript_parts = []
            if 'events' in subtitle_data:
                for event in subtitle_data['events']:
                    if 'segs' in event:
                        for seg in event['segs']:
                            if 'utf8' in seg:
                                transcript_parts.append(seg['utf8'])
            
            # Clean up all temp files
            for p in Path(".").glob("temp_transcript*"):
                p.unlink(missing_ok=True)
            
            if not transcript_parts:
                raise Exception("Extracted subtitle file is empty")
            
            transcript = ' '.join(transcript_parts)
            logger.info(f"Extracted transcript: {len(transcript)} characters")
            
            return transcript
            
        except subprocess.TimeoutExpired:
            logger.error("Transcript extraction timed out")
            raise Exception("Transcript extraction timed out after 60 seconds")
        except Exception as e:
            logger.error(f"Transcript extraction error: {str(e)}")
            # Clean up any temp files on error
            for p in Path(".").glob("temp_transcript*"):
                p.unlink(missing_ok=True)
            raise Exception(f"Failed to extract transcript: {str(e)}")


class NLPPreprocessor:
    """Handles text preprocessing using spaCy to reduce tokens by 40%"""
    
    def __init__(self):
        """Initialize spaCy model (lazy loading)"""
        self.nlp = None
    
    def _load_spacy(self):
        """Load spaCy model on first use"""
        if self.nlp is None:
            try:
                import spacy
                self.nlp = spacy.load("en_core_web_sm")
                logger.info("spaCy model loaded successfully")
            except OSError:
                logger.warning("spaCy model not found, downloading...")
                import subprocess
                subprocess.run(["python", "-m", "spacy", "download", "en_core_web_sm"])
                import spacy
                self.nlp = spacy.load("en_core_web_sm")
    
    def clean_transcript(self, raw_transcript: str) -> str:
        """
        Clean and preprocess transcript to reduce token count by ~40%
        
        Preprocessing steps:
        1. Remove timestamps and speaker labels
        2. Remove repetitive filler words (um, uh, like, you know)
        3. Remove duplicate sentences
        4. Lemmatize and remove stop words while keeping context
        5. Remove excessive punctuation
        
        Args:
            raw_transcript: Raw transcript text
            
        Returns:
            Cleaned and optimized transcript
        """
        logger.info(f"Cleaning transcript: {len(raw_transcript)} characters")
        
        # Step 1: Remove timestamps (various formats)
        text = re.sub(r'\[\d+:\d+:\d+\]', '', raw_transcript)
        text = re.sub(r'\d+:\d+:\d+', '', text)
        text = re.sub(r'\d+:\d+', '', text)
        
        # Step 2: Remove speaker labels
        text = re.sub(r'^[A-Z\s]+:', '', text, flags=re.MULTILINE)
        text = re.sub(r'\[.*?\]', '', text)
        
        # Step 3: Remove common filler words (case-insensitive)
        filler_words = [
            r'\b(um+|uh+|er+|ah+)\b',
            r'\b(like|you know|i mean|basically|actually|literally)\b',
            r'\b(sort of|kind of)\b'
        ]
        for pattern in filler_words:
            text = re.sub(pattern, '', text, flags=re.IGNORECASE)
        
        # Step 4: Remove duplicate/repetitive phrases
        sentences = text.split('.')
        seen_sentences = set()
        unique_sentences = []
        for sentence in sentences:
            sentence_clean = sentence.strip().lower()
            if sentence_clean and sentence_clean not in seen_sentences and len(sentence_clean) > 10:
                seen_sentences.add(sentence_clean)
                unique_sentences.append(sentence.strip())
        text = '. '.join(unique_sentences)
        
        # Step 5: Clean up whitespace and punctuation
        text = re.sub(r'\s+', ' ', text)  # Multiple spaces to single
        text = re.sub(r'\.+', '.', text)  # Multiple periods to single
        text = re.sub(r',+', ',', text)   # Multiple commas to single
        
        # Step 6: Use spaCy for advanced preprocessing (optional, can be toggled)
        # This provides additional ~10% token reduction
        try:
            self._load_spacy()
            doc = self.nlp(text[:100000])  # Limit to avoid memory issues
            
            # Keep only meaningful tokens (remove pure stop words in non-critical positions)
            meaningful_tokens = []
            for sent in doc.sents:
                sent_tokens = []
                for token in sent:
                    # Keep tokens that are not pure stop words or have important POS
                    if not token.is_stop or token.pos_ in ['NOUN', 'VERB', 'ADJ', 'PROPN']:
                        sent_tokens.append(token.lemma_ if token.lemma_ != '-PRON-' else token.text)
                if sent_tokens:
                    meaningful_tokens.append(' '.join(sent_tokens))
            
            text = '. '.join(meaningful_tokens)
        except Exception as e:
            logger.warning(f"spaCy processing skipped: {str(e)}")
        
        text = text.strip()
        logger.info(f"Cleaned transcript: {len(text)} characters (reduction: {100 - (len(text)/len(raw_transcript)*100):.1f}%)")
        
        return text


class GeminiAnalyzer:
    """Handles content analysis using Google Gemini AI"""
    
    def __init__(self):
        """Initialize Gemini client"""
        self.api_key = os.getenv("GEMINI_API_KEY")
        if not self.api_key:
            logger.error("GEMINI_API_KEY environment variable not set")
        self.client = None
    
    def _initialize_client(self):
        """Initialize Gemini client on first use"""
        if self.client is None:
            try:
                from google import genai
                client = genai.Client(api_key=self.api_key)
                self.client = client
                logger.info("Gemini client initialized successfully")
            except Exception as e:
                logger.error(f"Failed to initialize Gemini: {str(e)}")
                raise
    
    def _get_system_instruction(self) -> str:
        """Define Gemini's persona and instructions"""
        return """You are a Senior Content Strategist and YouTube Analytics Expert with 10+ years of experience. 
        
Your role is to analyze YouTube video transcripts and provide actionable insights to help creators improve their content.

When analyzing videos, evaluate:
1. HOOK SCORE (0-10): How effectively does the video capture attention in the first 30 seconds?
2. RETENTION SCORE (0-10): How well does the content maintain viewer engagement throughout?
3. SEO SCORE (0-10): How well-optimized is the content for search and discovery?
4. CRAFT SCORE (0-10): Overall content quality, pacing, and production value

Provide your analysis in the following JSON structure:
{
    "hook_score": <float 0-10>,
    "retention_score": <float 0-10>,
    "seo_score": <float 0-10>,
    "craft_score": <float 0-10>,
    "strengths": [<list of 3-5 key strengths>],
    "improvements": [<list of 3-5 actionable improvements>],
    "seo_keywords": [<list of 5-10 relevant keywords>],
    "title_suggestions": [<list of 3 alternative title ideas>],
    "summary": "<2-3 sentence summary of the video content>"
}

Be specific, actionable, and encouraging in your feedback. Focus on practical improvements that creators can implement immediately.
"""
    
    def analyze_content(self, cleaned_transcript: str) -> Dict[str, Any]:
        """
        Analyze video content using Gemini AI
        
        Args:
            cleaned_transcript: Preprocessed transcript text
            
        Returns:
            Structured analysis results as dictionary
        """
        try:
            self._initialize_client()
            
            if not self.api_key:
                raise Exception("Gemini API key not configured")
            
            logger.info("Sending transcript to Gemini for analysis")
            
            # Construct the prompt
            prompt = f"""Analyze the following YouTube video transcript and provide a comprehensive content strategy analysis.

Transcript:
{cleaned_transcript[:15000]}

Remember to return your analysis in valid JSON format only, with no additional text."""
            
            # Generate content with Gemini using new API
            model_name = 'gemini-2.5-flash'
            logger.info(f"Using Gemini model: {model_name}")
            response = self.client.models.generate_content(
                model=model_name,
                contents=prompt,
                config={
                    'system_instruction': self._get_system_instruction(),
                    'temperature': 0.7,
                }
            )
            
            # Extract and parse JSON from response
            response_text = response.text
            
            # Try to extract JSON from response (handle markdown code blocks)
            json_match = re.search(r'```json\s*(.*?)\s*```', response_text, re.DOTALL)
            if json_match:
                response_text = json_match.group(1)
            else:
                # Try to find JSON object in response
                json_match = re.search(r'\{.*\}', response_text, re.DOTALL)
                if json_match:
                    response_text = json_match.group(0)
            
            # Parse JSON response
            analysis_result = json.loads(response_text)
            
            # Calculate craft score if not provided (average of other scores)
            if 'craft_score' not in analysis_result:
                analysis_result['craft_score'] = round(
                    (analysis_result['hook_score'] + 
                     analysis_result['retention_score'] + 
                     analysis_result['seo_score']) / 3,
                    2
                )
            
            logger.info(f"Analysis completed. Craft Score: {analysis_result.get('craft_score')}")
            
            return analysis_result
            
        except json.JSONDecodeError as e:
            logger.error(f"Failed to parse Gemini response as JSON: {str(e)}")
            logger.error(f"Raw response: {response_text[:500]}")
            raise Exception(f"Invalid JSON response from Gemini: {str(e)}")
        except Exception as e:
            logger.error(f"Gemini analysis error: {str(e)}")
            raise Exception(f"Content analysis failed: {str(e)}")


# Initialize service components
transcript_extractor = TranscriptExtractor()
nlp_preprocessor = NLPPreprocessor()
gemini_analyzer = GeminiAnalyzer()


@app.get("/")
async def root():
    """Health check endpoint"""
    return {
        "service": "NutzyCraft Pilot AI",
        "status": "running",
        "version": "1.0.0"
    }


@app.get("/health")
async def health_check():
    """Detailed health check"""
    return {
        "status": "healthy",
        "gemini_configured": bool(os.getenv("GEMINI_API_KEY")),
        "timestamp": "2026-01-20"
    }


@app.post("/analyze", response_model=AnalysisResponse)
async def analyze_video(request: AnalysisRequest):
    """
    Main endpoint to analyze YouTube video content
    
    This endpoint orchestrates the complete analysis pipeline:
    1. Extract transcript using yt-dlp
    2. Clean and preprocess text using spaCy
    3. Analyze content using Gemini AI
    4. Return structured results
    
    Args:
        request: AnalysisRequest containing YouTube URL
        
    Returns:
        AnalysisResponse with detailed content analysis
    """
    try:
        logger.info(f"Starting analysis for: {request.youtube_url}")
        
        # Step 1: Extract transcript
        raw_transcript = transcript_extractor.extract_transcript(str(request.youtube_url))
        
        if not raw_transcript or len(raw_transcript) < 100:
            raise HTTPException(
                status_code=400,
                detail="Transcript too short or unavailable. Video may not have captions enabled."
            )
        
        # Step 2: Clean and preprocess
        cleaned_transcript = nlp_preprocessor.clean_transcript(raw_transcript)
        
        # Step 3: Analyze with Gemini
        analysis_result = gemini_analyzer.analyze_content(cleaned_transcript)
        
        # Step 4: Return structured response
        response = AnalysisResponse(
            **analysis_result,
            success=True
        )
        
        logger.info(f"Analysis completed successfully for user: {request.user_id}")
        
        return response
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Analysis failed: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Analysis failed: {str(e)}"
        )


@app.post("/test-transcript")
async def test_transcript_extraction(request: AnalysisRequest):
    """Test endpoint to verify transcript extraction only"""
    try:
        raw_transcript = transcript_extractor.extract_transcript(str(request.youtube_url))
        cleaned_transcript = nlp_preprocessor.clean_transcript(raw_transcript)
        
        return {
            "success": True,
            "raw_length": len(raw_transcript),
            "cleaned_length": len(cleaned_transcript),
            "reduction_percent": round(100 - (len(cleaned_transcript)/len(raw_transcript)*100), 2),
            "preview": cleaned_transcript[:500]
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    import uvicorn
    
    # Run the FastAPI server
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=8000,
        log_level="info"
    )
