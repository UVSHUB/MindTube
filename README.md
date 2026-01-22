# MindTube - NutzyCraft Pilot AI

AI-powered YouTube content analysis platform that helps creators optimize their videos for better engagement, retention, and SEO.

## 🚀 Features

- **Smart Transcript Extraction**: Extract YouTube transcripts without downloading videos using yt-dlp
- **AI-Powered Analysis**: Advanced content analysis using Google Gemini AI
- **Token Optimization**: 40% token reduction through intelligent NLP preprocessing
- **Comprehensive Scoring**: Hook, Retention, SEO, and overall Craft scores
- **Actionable Insights**: Specific recommendations for content improvement
- **SEO Keywords**: Automated keyword extraction and title suggestions
- **Full-Stack Integration**: Java Spring Boot backend + Python FastAPI AI service

## 📁 Project Structure

```
NutzyCraftPilotAI/
├── AI-Service/                 # Python FastAPI AI Service
│   ├── main.py                # Main AI service with Gemini integration
│   ├── requirements.txt       # Python dependencies
│   ├── .env.example          # Environment configuration template
│   ├── test_service.py       # Service testing script
│   └── README.md             # AI service documentation
├── src/                       # Java Spring Boot Application
│   └── main/
│       ├── java/
│       │   └── com/nutzycraft/pilotai/
│       │       ├── controller/    # REST API controllers
│       │       ├── entity/        # JPA entities
│       │       ├── repository/    # Data repositories
│       │       └── service/       # Business logic
│       └── resources/
│           ├── application.properties
│           └── static/            # Frontend HTML/CSS/JS
├── database/                  # Database schemas and scripts
├── docker-compose.yml        # Docker orchestration
├── pom.xml                   # Maven configuration
└── SETUP_GUIDE.md           # Complete setup instructions

```

## 🛠️ Technology Stack

### Backend (Java)
- Spring Boot 3.x
- PostgreSQL (Supabase)
- JPA/Hibernate
- RestTemplate for microservice communication

### AI Service (Python)
- FastAPI - Modern, high-performance web framework
- Google Gemini AI - Advanced language model for content analysis
- yt-dlp - YouTube transcript extraction
- spaCy - NLP preprocessing and token optimization
- Pydantic - Data validation and serialization

## 📋 Prerequisites

- Java 17+
- Maven 3.6+
- Python 3.9+
- PostgreSQL (or Supabase account)
- Google Gemini API key ([Get one here](https://makersuite.google.com/app/apikey))

## ⚡ Quick Start

### 1. Clone the Repository
```bash
git clone <repository-url>
cd NutzyCraftPilotAI
```

### 2. Set Up Python AI Service
```bash
cd AI-Service

# Create virtual environment
python -m venv venv

# Activate (Windows)
venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Download spaCy model
python -m spacy download en_core_web_sm

# Configure API key
copy .env.example .env
# Edit .env and add your GEMINI_API_KEY

# Run the service
python main.py
```

Service will be available at http://localhost:8000

### 3. Set Up Java Backend
```bash
cd ..

# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

Application will be available at http://localhost:8080

### 4. Test the Application

Open your browser and navigate to:
```
http://localhost:8080/static/dashboard.html
```

Submit a YouTube URL and get AI-powered analysis!

## 📚 Detailed Documentation

For complete setup instructions, see [SETUP_GUIDE.md](NutzyCraftPilotAI/SETUP_GUIDE.md)

For AI service details, see [AI-Service/README.md](NutzyCraftPilotAI/AI-Service/README.md)

## 🔄 How It Works

1. **User submits YouTube URL** through the web interface
2. **Java backend** receives the request and forwards it to Python AI service
3. **Python AI service** performs:
   - Transcript extraction using yt-dlp (no video download)
   - NLP preprocessing with spaCy (40% token reduction)
   - Content analysis using Gemini AI
4. **Structured JSON response** is returned to Java backend
5. **Analysis is saved** to PostgreSQL database
6. **Results are displayed** to the user with actionable insights

## 🎯 Key Features Explained

### A. YouTube Transcript Extraction
- Uses yt-dlp with `--skip-download` flag
- Extracts auto-generated or manual captions
- No heavy video file downloads
- Fast and efficient processing

### B. NLP Token Optimization
Achieves ~40% token reduction through:
- Timestamp removal (5%)
- Filler word elimination (15%)
- Duplicate sentence removal (10%)
- Smart stop word filtering (10%)

This significantly reduces Gemini API costs while maintaining content quality.

### C. Gemini AI Analysis
- Persona: "Senior Content Strategist"
- Structured JSON output for easy parsing
- Comprehensive scoring system:
  - **Hook Score**: Opening effectiveness
  - **Retention Score**: Engagement potential
  - **SEO Score**: Search optimization
  - **Craft Score**: Overall quality
- Actionable recommendations
- SEO keyword extraction
- Title suggestions

## 🧪 Testing

### Test Python Service
```bash
cd AI-Service
python test_service.py
```

### Test Java Backend
```bash
mvn test
```

### Manual API Testing
```bash
# Test Python service
curl -X POST "http://localhost:8000/analyze" ^
  -H "Content-Type: application/json" ^
  -d "{\"youtube_url\": \"https://www.youtube.com/watch?v=VIDEO_ID\"}"

# Test Java backend
curl -X POST "http://localhost:8080/api/analysis" ^
  -H "Content-Type: application/json" ^
  -d "{\"userId\": \"USER_UUID\", \"url\": \"https://www.youtube.com/watch?v=VIDEO_ID\"}"
```

## 🔧 Configuration

### Python Service (.env)
```env
GEMINI_API_KEY=your_gemini_api_key_here
SERVICE_PORT=8000
LOG_LEVEL=INFO
```

### Java Application (application.properties)
```properties
server.port=8080
ai.service.url=http://localhost:8000
spring.datasource.url=jdbc:postgresql://...
```

## 🐛 Troubleshooting

### "Python service not responding"
- Ensure Python service is running on port 8000
- Check `ai.service.url` in application.properties

### "Gemini API error"
- Verify API key in .env file
- Check quota at Google AI Studio
- Ensure environment variables are loaded

### "No subtitles available"
- Video must have captions enabled
- Try a different video with auto-generated captions

## 🚀 Deployment

### Using Docker Compose
```bash
docker-compose up
```

### Manual Deployment
1. Deploy Python service to cloud platform (AWS, GCP, Azure)
2. Update Java configuration with production Python service URL
3. Deploy Java application
4. Configure environment variables securely

## 📊 API Endpoints

### Python AI Service (Port 8000)
- `GET /` - Health check
- `GET /health` - Detailed status
- `POST /analyze` - Analyze YouTube video
- `GET /docs` - Swagger documentation

### Java Backend (Port 8080)
- `POST /api/analysis` - Submit video for analysis
- `GET /api/analysis?userId={id}` - Get user's analysis history
- `DELETE /api/analysis/{id}` - Delete analysis

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📝 License

This project is part of the NutzyCraft ecosystem.

## 🙏 Acknowledgments

- Google Gemini AI for content analysis
- yt-dlp for transcript extraction
- spaCy for NLP preprocessing
- FastAPI for Python web framework
- Spring Boot for Java backend

---

Built with ❤️ by NutzyCraft Team
