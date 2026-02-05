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
    """Structured response model for educational content extraction"""
    title: str = Field(..., description="Video title or topic")
    subject_area: str = Field(..., description="Subject area (e.g., Mathematics, Physics, Computer Science)")
    difficulty_level: str = Field(..., description="Difficulty level: Beginner, Intermediate, or Advanced")
    key_concepts: list[str] = Field(..., description="Main concepts covered in the video")
    detailed_explanation: str = Field(..., description="Comprehensive explanation of what is taught in the video")
    learning_objectives: list[str] = Field(..., description="What students will learn after watching")
    examples_covered: list[str] = Field(..., description="Examples or case studies covered")
    prerequisites: list[str] = Field(..., description="What students should know before watching")
    key_takeaways: list[str] = Field(..., description="Important points to remember")
    summary: str = Field(..., description="Brief overview of the video content")
    success: bool = Field(True, description="Analysis success status")
    error: Optional[str] = Field(None, description="Error message if any")


class TranscriptExtractor:
    """Handles YouTube transcript extraction using yt-dlp"""
    
    @staticmethod
    def extract_transcript(youtube_url: str) -> tuple[str, int]:
        """
        Extract transcript and duration from YouTube video
        
        Args:
            youtube_url: The YouTube video URL
            
        Returns:
            Tuple of (transcript text, duration in seconds)
        """
        try:
            logger.info(f"Extracting transcript and metadata from: {youtube_url}")
            
            import sys
            # Command to get duration and try to get subs
            command = [
                sys.executable, "-m", "yt_dlp",
                "--skip-download",
                "--write-auto-subs",
                "--write-subs",
                "--sub-lang", "en",
                "--sub-format", "json3",
                "--output", "temp_transcript",
                "--print", "duration",
                "--quiet",
                "--no-warnings",
                youtube_url
            ]
            
            result = subprocess.run(
                command,
                capture_output=True,
                text=True,
                timeout=60
            )
            
            # Get duration from stdout
            try:
                duration_str = result.stdout.strip().split('\n')[-1] # Take last line in case of multiple
                duration = int(duration_str)
            except:
                logger.warning("Could not extract duration, defaulting to 0")
                duration = 0

            # Check for subtitle files
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
                for p in Path(".").glob("temp_transcript*"):
                    p.unlink(missing_ok=True)
                raise Exception("No captions/subtitles available for this video.")
            
            with open(subtitle_file, 'r', encoding='utf-8') as f:
                subtitle_data = json.load(f)
            
            transcript_parts = []
            if 'events' in subtitle_data:
                for event in subtitle_data['events']:
                    if 'segs' in event:
                        for seg in event['segs']:
                            if 'utf8' in seg:
                                transcript_parts.append(seg['utf8'])
            
            for p in Path(".").glob("temp_transcript*"):
                p.unlink(missing_ok=True)
            
            if not transcript_parts:
                raise Exception("Extracted subtitle file is empty")
            
            transcript = ' '.join(transcript_parts)
            logger.info(f"Extracted transcript: {len(transcript)} chars, Duration: {duration}s")
            
            return transcript, duration
            
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


class GroqAnalyzer:
    """Handles content analysis using Groq AI (Free & Fast)"""
    
    def __init__(self):
        """Initialize Groq client"""
        self.api_key = os.getenv("GROQ_API_KEY")
        if not self.api_key:
            logger.error("GROQ_API_KEY environment variable not set")
        self.client = None
    
    def _initialize_client(self):
        """Initialize Groq client on first use"""
        if self.client is None:
            try:
                from groq import Groq
                self.client = Groq(api_key=self.api_key)
                logger.info("Groq client initialized successfully")
            except Exception as e:
                logger.error(f"Failed to initialize Groq: {str(e)}")
                raise
    
    def _get_system_instruction(self) -> str:
        """Define AI's persona and instructions"""
        return """You are an Expert Educational Content Analyst and Learning Facilitator with deep expertise in pedagogy and knowledge extraction.
        
Your role is to analyze educational video transcripts and extract learning content in a way that helps university students understand and master the material.

When analyzing educational videos, extract:
1. SUBJECT AREA: The academic field or discipline
2. DIFFICULTY LEVEL: Beginner, Intermediate, or Advanced
3. KEY CONCEPTS: Main ideas and principles taught
4. DETAILED EXPLANATION: Clear, comprehensive explanation of the content that enables learning
5. LEARNING OBJECTIVES: What students will be able to do after watching
6. EXAMPLES: Specific examples, case studies, or demonstrations covered
7. PREREQUISITES: Background knowledge needed to understand the material
8. KEY TAKEAWAYS: Most important points to remember

Provide your analysis in the following JSON structure:
{
    "title": "<video title or main topic>",
    "subject_area": "<academic subject>",
    "difficulty_level": "<Beginner|Intermediate|Advanced>",
    "key_concepts": [<comprehensive list of all main concepts mentioned>],
    "detailed_explanation": "<extremely detailed, multi-paragraph explanation of EVERYTHING taught; if the video is long, this section should be very extensive, covering every nuance in a clear educational style>",
    "learning_objectives": [<full list of learning outcomes based on the video's depth>],
    "examples_covered": [<all examples, case studies, or demonstrations mentioned>],
    "prerequisites": [<all necessary background knowledge needed>],
    "key_takeaways": [<comprehensive list of all important points to remember>],
    "summary": "<overview of the video content and its value>"
}

Write the detailed_explanation in a way that teaches the material effectively. Use clear language, define terms, and explain concepts thoroughly. The length and depth of the explanation MUST be proportional to the length and complexity of the video content. For longer, more technical videos, provide an exhaustive breakdown so a student can learn the material in depth without needing to watch the video.
"""
    
    def analyze_content(self, cleaned_transcript: str, duration_seconds: int = 0) -> Dict[str, Any]:
        """
        Analyze video content using Groq AI
        
        Args:
            cleaned_transcript: Preprocessed transcript text
            duration_seconds: Length of the video in seconds
            
        Returns:
            Structured analysis results as dictionary
        """
        try:
            self._initialize_client()
            
            if not self.api_key:
                raise Exception("Groq API key not configured")
            
            logger.info("Sending transcript to Groq for educational content extraction")
            
            duration_minutes = duration_seconds // 60
            
            # Construct the prompt
            prompt = f"""Extract educational content from the following video transcript.
            
VIDEO DURATION: {duration_minutes} minutes

Transcript:
{cleaned_transcript[:30000]}

Analyze this educational content thoroughly and provide a comprehensive, deep-dive learning guide. 
IMPORTANT: The 'detailed_explanation' must be proportional to the video duration ({duration_minutes} minutes). 
- If the video is short (e.g., < 5 mins), a concise but thorough explanation is fine.
- If the video is long (e.g., > 15 mins), you MUST provide a very lengthy, multi-section, and exhaustive 'detailed_explanation' that covers every major point and nuance mentioned in the transcript.

Focus on explaining what is taught, the key concepts, examples, and learning outcomes in great detail. 

Remember to return your analysis in valid JSON format only, with no additional text."""
            
            # Generate content with Groq
            # Available models: llama-3.3-70b-versatile, llama-3.1-8b-instant, mixtral-8x7b-32768
            model_name = 'llama-3.3-70b-versatile'  # Fast and powerful
            logger.info(f"Using Groq model: {model_name}")
            
            response = self.client.chat.completions.create(
                model=model_name,
                messages=[
                    {"role": "system", "content": self._get_system_instruction()},
                    {"role": "user", "content": prompt}
                ],
                temperature=0.7,
                max_tokens=4096,
                response_format={"type": "json_object"}  # Force JSON output
            )
            
            # Extract response text
            response_text = response.choices[0].message.content
            
            # Parse JSON response
            analysis_result = json.loads(response_text)
            
            # Ensure all required fields are present
            if 'title' not in analysis_result:
                analysis_result['title'] = "Educational Video Content"
            if 'subject_area' not in analysis_result:
                analysis_result['subject_area'] = "General"
            if 'difficulty_level' not in analysis_result:
                analysis_result['difficulty_level'] = "Intermediate"
            
            logger.info(f"Educational content extracted: {analysis_result.get('title')}")
            
            return analysis_result
            
        except json.JSONDecodeError as e:
            logger.error(f"Failed to parse Groq response as JSON: {str(e)}")
            logger.error(f"Raw response: {response_text[:500]}")
            raise Exception(f"Invalid JSON response from Groq: {str(e)}")
        except Exception as e:
            logger.error(f"Groq analysis error: {str(e)}")
            raise Exception(f"Content analysis failed: {str(e)}")


# Initialize service components
transcript_extractor = TranscriptExtractor()
nlp_preprocessor = NLPPreprocessor()
groq_analyzer = GroqAnalyzer()


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
        "groq_configured": bool(os.getenv("GROQ_API_KEY")),
        "timestamp": "2026-01-28"
    }


@app.post("/analyze", response_model=AnalysisResponse)
async def analyze_video(request: AnalysisRequest):
    """
    Main endpoint to analyze YouTube video content
    
    This endpoint orchestrates the complete analysis pipeline:
    1. Extract transcript using yt-dlp
    2. Clean and preprocess text using spaCy
    3. Analyze content using Groq AI
    4. Return structured results
    
    Args:
        request: AnalysisRequest containing YouTube URL
        
    Returns:
        AnalysisResponse with detailed content analysis
    """
    try:
        logger.info(f"Starting analysis for: {request.youtube_url}")
        
        # Step 1: Extract transcript and duration
        raw_transcript, duration = transcript_extractor.extract_transcript(str(request.youtube_url))
        
        if not raw_transcript or len(raw_transcript) < 100:
            raise HTTPException(
                status_code=400,
                detail="Transcript too short or unavailable. Video may not have captions enabled."
            )
        
        # Step 2: Clean and preprocess
        cleaned_transcript = nlp_preprocessor.clean_transcript(raw_transcript)
        
        # Step 3: Analyze with Groq
        analysis_result = groq_analyzer.analyze_content(cleaned_transcript, duration)
        
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
