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
    """Handles YouTube transcript extraction using youtube-transcript-api"""
    
    @staticmethod
    def extract_video_id(youtube_url: str) -> str:
        """
        Extract video ID from various YouTube URL formats
        
        Args:
            youtube_url: The YouTube video URL
            
        Returns:
            Video ID string
        """
        import re
        
        # Patterns for different YouTube URL formats
        patterns = [
            r'(?:youtube\.com\/watch\?v=|youtu\.be\/)([a-zA-Z0-9_-]{11})',
            r'youtube\.com\/embed\/([a-zA-Z0-9_-]{11})',
            r'youtube\.com\/v\/([a-zA-Z0-9_-]{11})',
        ]
        
        for pattern in patterns:
            match = re.search(pattern, youtube_url)
            if match:
                return match.group(1)
        
        raise ValueError(f"Could not extract video ID from URL: {youtube_url}")
    
    @staticmethod
    def extract_transcript(youtube_url: str) -> tuple[str, int]:
        """
        Extract transcript and duration from YouTube video using YouTube Data API v3
        
        Args:
            youtube_url: The YouTube video URL
            
        Returns:
            Tuple of (transcript text, duration in seconds)
        """
        try:
            logger.info(f"Extracting transcript from: {youtube_url}")
            
            # Extract video ID
            video_id = TranscriptExtractor.extract_video_id(youtube_url)
            logger.info(f"Video ID: {video_id}")
            
            # Get API key from environment
            api_key = os.getenv('YOUTUBE_API_KEY')
            
            # Try Method 1: youtube-transcript-api with cookies (fastest)
            try:
                from youtube_transcript_api import YouTubeTranscriptApi
                from youtube_transcript_api._errors import TranscriptsDisabled, NoTranscriptFound
                
                logger.info("Attempting transcript extraction via youtube-transcript-api")
                
                # Try to get English transcript first
                try:
                    transcript_list = YouTubeTranscriptApi.get_transcript(
                        video_id, 
                        languages=['en', 'en-US', 'en-GB']
                    )
                except NoTranscriptFound:
                    # If no English, try auto-generated
                    logger.warning("No manual transcript found, trying auto-generated")
                    transcript_list = YouTubeTranscriptApi.get_transcript(video_id)
                
                # Combine all transcript segments
                transcript_parts = [entry['text'] for entry in transcript_list]
                transcript = ' '.join(transcript_parts)
                
                # Calculate duration from last timestamp
                duration = 0
                if transcript_list:
                    last_entry = transcript_list[-1]
                    duration = int(last_entry['start'] + last_entry.get('duration', 0))
                
                logger.info(f"✅ Successfully extracted via youtube-transcript-api: {len(transcript)} chars")
                return transcript, duration
                
            except (TranscriptsDisabled, NoTranscriptFound, Exception) as api_error:
                logger.warning(f"youtube-transcript-api failed: {api_error}")
                
                # Method 2: Fallback to YouTube Data API v3 (if API key available)
                if api_key:
                    logger.info("Attempting fallback to YouTube Data API v3")
                    return TranscriptExtractor._extract_via_youtube_api(video_id, api_key)
                else:
                    logger.error("No YouTube API key configured for fallback")
                    raise Exception("No captions/subtitles available for this video. Please configure YOUTUBE_API_KEY in .env for better reliability.")
            
        except ValueError as e:
            logger.error(f"Invalid YouTube URL: {e}")
            raise Exception(f"Invalid YouTube URL: {str(e)}")
        except Exception as e:
            logger.error(f"Transcript extraction error: {str(e)}")
            raise Exception(f"Failed to extract transcript: {str(e)}")
    
    @staticmethod
    def _extract_via_youtube_api(video_id: str, api_key: str) -> tuple[str, int]:
        """
        Extract transcript using official YouTube Data API v3
        
        Args:
            video_id: YouTube video ID
            api_key: YouTube Data API key
            
        Returns:
            Tuple of (transcript text, duration in seconds)
        """
        try:
            from googleapiclient.discovery import build
            from googleapiclient.errors import HttpError
            
            # Build YouTube API client
            youtube = build('youtube', 'v3', developerKey=api_key)
            
            # Get video details for duration
            video_response = youtube.videos().list(
                part='contentDetails,snippet',
                id=video_id
            ).execute()
            
            if not video_response['items']:
                raise Exception("Video not found")
            
            # Extract duration (in ISO 8601 format like PT15M33S)
            duration_iso = video_response['items'][0]['contentDetails']['duration']
            duration = TranscriptExtractor._parse_iso_duration(duration_iso)
            
            # Get captions list
            captions_response = youtube.captions().list(
                part='snippet',
                videoId=video_id
            ).execute()
            
            if not captions_response.get('items'):
                raise Exception("No captions available for this video")
            
            # Find English caption track
            caption_id = None
            for caption in captions_response['items']:
                if caption['snippet']['language'] in ['en', 'en-US', 'en-GB']:
                    caption_id = caption['id']
                    break
            
            if not caption_id:
                # Fallback to first available caption
                caption_id = captions_response['items'][0]['id']
            
            # Download caption
            caption_data = youtube.captions().download(
                id=caption_id,
                tfmt='srt'  # SubRip format
            ).execute()
            
            # Parse SRT format to plain text
            transcript = TranscriptExtractor._parse_srt(caption_data)
            
            logger.info(f"✅ Successfully extracted via YouTube Data API: {len(transcript)} chars")
            return transcript, duration
            
        except HttpError as e:
            logger.error(f"YouTube API error: {e}")
            if e.resp.status == 403:
                raise Exception("YouTube API quota exceeded or invalid API key")
            else:
                raise Exception(f"YouTube API error: {str(e)}")
        except Exception as e:
            logger.error(f"YouTube API extraction failed: {e}")
            raise Exception(f"Failed to extract captions via YouTube API: {str(e)}")
    
    @staticmethod
    def _parse_iso_duration(duration_iso: str) -> int:
        """Parse ISO 8601 duration (e.g., PT15M33S) to seconds"""
        import re
        match = re.match(r'PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?', duration_iso)
        if not match:
            return 0
        hours = int(match.group(1) or 0)
        minutes = int(match.group(2) or 0)
        seconds = int(match.group(3) or 0)
        return hours * 3600 + minutes * 60 + seconds
    
    @staticmethod
    def _parse_srt(srt_content: str) -> str:
        """Parse SRT subtitle format to plain text"""
        import re
        # Remove subtitle numbers and timestamps
        text = re.sub(r'\d+\n\d{2}:\d{2}:\d{2},\d{3} --> \d{2}:\d{2}:\d{2},\d{3}\n', '', srt_content)
        # Remove empty lines
        text = '\n'.join([line for line in text.split('\n') if line.strip()])
        return text.replace('\n', ' ').strip()


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

CRITICAL: You MUST include ALL fields in your JSON response. Every field listed above is REQUIRED, including summary, title, subject_area, difficulty_level, key_concepts, detailed_explanation, learning_objectives, examples_covered, prerequisites, and key_takeaways. Do not omit any field.

Write the detailed_explanation in a way that teaches the material effectively. 

CRITICAL FORMATTING RULES FOR PDF AND WEB READABILITY:
1. Use PURE PLAIN TEXT only. Absolutely NO Markdown (#, **, etc.) and NO HTML tags.
2. For each major section, follow this EXACT structure:
   - Write the section heading in ALL CAPS
   - Add ONE blank line after the heading
   - Write the content paragraphs
   - Add ONE blank line before the next section heading
3. Within a section, separate paragraphs with a single blank line.
4. For lists, format like this:
   - Start a new line after intro text
   - Type a dash and space, then the list item
   - Each list item on its own line
   - Add a blank line after the entire list
5. Keep paragraphs focused and clear (4-6 sentences).
6. Use simple, direct language that students can easily understand.
7. Break complex topics into multiple short paragraphs rather than one long paragraph.

EXAMPLE OF CORRECT FORMAT:

INTRODUCTION TO THE TOPIC

This is the first paragraph explaining the main concept. It should be clear and easy to understand. Each paragraph focuses on one key idea. This helps students learn step by step.

This is the second paragraph providing more specific details. Notice there is one blank line between paragraphs. This makes the text easy to read on both web and PDF.

Key points to remember:
- First important point explained clearly
- Second important point with context
- Third important point with examples

NEXT MAJOR TOPIC

This section starts immediately after the heading with just one blank line. This prevents large gaps in the PDF while keeping the structure clear.

The content flows naturally from one paragraph to the next. Each paragraph builds on the previous one to create a complete learning experience.


The length and depth of the explanation MUST be proportional to the length and complexity of the video content. For longer, more technical videos, provide an exhaustive breakdown so a student can learn the material in depth without needing to watch the video.
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

CRITICAL FORMATTING FOR PDF COMPATIBILITY: 
- Use PURE PLAIN TEXT only
- ALL CAPS headings on their own line
- ONE blank line after each heading (not more)
- ONE blank line between paragraphs
- ONE blank line between sections
- Use dash (-) for list items
- Keep paragraphs clear and focused (4-6 sentences)
- Break topics into multiple short paragraphs for better flow
- NO Markdown symbols, NO HTML tags

IMPORTANT: Include ALL required JSON fields in your response (title, subject_area, difficulty_level, key_concepts, detailed_explanation, learning_objectives, examples_covered, prerequisites, key_takeaways, and summary). Do not omit any field.

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
