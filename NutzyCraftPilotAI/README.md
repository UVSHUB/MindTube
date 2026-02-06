# 🎬 NutzyCraft PilotAI - AI-Powered YouTube Content Analysis

AI-powered YouTube educational content analysis platform that extracts transcripts and provides comprehensive analysis using advanced AI models.

[![Production](https://img.shields.io/badge/Production-Live-success)](https://mind-tube-gilt.vercel.app)
[![Backend](https://img.shields.io/badge/Backend-HTTPS-blue)](https://pilotai-backend.nutzycraft.com)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

## 🌟 Live Demo

- **Frontend**: https://mind-tube-gilt.vercel.app
- **Backend API**: https://pilotai-backend.nutzycraft.com/api/
- **AI Service**: https://pilotai-backend.nutzycraft.com/ai/

## ✨ Features

### Core Features

- 🎥 **Smart Transcript Extraction** - Extract YouTube transcripts using RapidAPI (no cookies needed!)
- 🤖 **AI-Powered Analysis** - Advanced content analysis using Groq AI (LLaMA 3.1 70B)
- 📊 **Educational Content Focus** - Specialized analysis for learning videos
- 📝 **Comprehensive Reports** - Subject area, difficulty level, key concepts, and detailed explanations
- 💾 **Save & Export** - Export to PDF or save directly to Google Drive
- 🔐 **Google Authentication** - Secure OAuth 2.0 sign-in
- 📱 **Responsive Design** - Beautiful, modern UI with glassmorphism effects

### Technical Features

- ⚡ **HTTPS Security** - Full SSL/TLS encryption with Let's Encrypt
- 🚀 **CORS Enabled** - Cross-origin support for frontend-backend communication
- 📦 **Token Optimization** - Smart preprocessing reduces API costs
- 🔄 **Auto-Deployment** - Frontend auto-deploys via Vercel on push

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Frontend (Vercel)                        │
│           https://mind-tube-gilt.vercel.app                 │
│                                                              │
│  • HTML/CSS/JavaScript (Vanilla)                           │
│  • Google Sign-In Integration                              │
│  • Google Drive API Integration                            │
│  • PDF Export (jsPDF)                                      │
└──────────────────┬──────────────────────────────────────────┘
                   │ HTTPS
                   ▼
┌─────────────────────────────────────────────────────────────┐
│              Backend (DigitalOcean VPS)                     │
│        https://pilotai-backend.nutzycraft.com               │
│                                                              │
│  ┌─────────────────────┐    ┌──────────────────────┐      │
│  │  Java Spring Boot   │    │   Python FastAPI     │      │
│  │    (Port 8080)      │◄───┤    (Port 8000)       │      │
│  │                     │    │                       │      │
│  │  • REST API         │    │  • Transcript Extract│      │
│  │  • Auth Service     │    │  • AI Analysis       │      │
│  │  • Database Layer   │    │  • NLP Processing    │      │
│  └─────────┬───────────┘    └──────────────────────┘      │
│            │                                                │
│            │ HTTPS (Nginx Reverse Proxy)                   │
│            ▼                                                │
│  ┌─────────────────────┐                                   │
│  │   PostgreSQL DB     │                                   │
│  │    (Supabase)       │                                   │
│  └─────────────────────┘                                   │
└─────────────────────────────────────────────────────────────┘
                   │
                   ├──► RapidAPI (YouTube Transcripts)
                   ├──► Groq AI (Content Analysis)
                   └──► Google APIs (Auth, Drive)
```

## 📁 Project Structure

```
NutzyCraftPilotAI/
├── AI-Service/                    # Python FastAPI AI Service
│   ├── main.py                   # Main AI service with Groq integration
│   ├── requirements.txt          # Python dependencies
│   ├── .env                      # Environment variables (not in git)
│   └── cookies.txt               # YouTube cookies (optional, deprecated)
│
├── src/                          # Java Spring Boot Application
│   └── main/
│       ├── java/com/nutzycraft/pilotai/
│       │   ├── controller/       # REST API controllers
│       │   ├── entity/           # JPA entities
│       │   ├── repository/       # Data repositories
│       │   └── service/          # Business logic
│       └── resources/
│           ├── application.properties
│           └── static/           # Frontend Files
│               ├── index.html
│               ├── dashboard.html
│               ├── login.html
│               ├── signup.html
│               ├── settings.html
│               ├── css/
│               └── js/
│                   ├── config.js  # API configuration
│                   ├── dashboard.js
│                   └── auth.js
│
├── DEPLOYMENT_GUIDE.md          # Production deployment guide
├── FRONTEND_UPDATE_GUIDE.md     # Frontend development guide
├── pom.xml                       # Maven configuration
└── README.md                     # This file
```

## 🛠️ Technology Stack

### Frontend

- **Framework**: Vanilla JavaScript (ES6+)
- **Styling**: Custom CSS with Glassmorphism
- **Deployment**: Vercel (Auto-deploy from GitHub)
- **APIs**:
  - Google Sign-In (OAuth 2.0)
  - Google Drive API
  - jsPDF for PDF generation

### Backend (Java)

- **Framework**: Spring Boot 3.x
- **Database**: PostgreSQL (Supabase)
- **ORM**: JPA/Hibernate
- **Server**: Embedded Tomcat
- **API Communication**: RestTemplate

### AI Service (Python)

- **Framework**: FastAPI
- **AI Model**: Groq (LLaMA 3.1 70B via API)
- **Transcript**: RapidAPI YouTube Transcript Service
- **NLP**: spaCy (preprocessing)
- **Server**: Uvicorn (ASGI)

### Infrastructure

- **VPS**: DigitalOcean (Ubuntu 22.04)
- **Web Server**: Nginx (Reverse Proxy)
- **SSL**: Let's Encrypt (Certbot)
- **Domain**: Cloudflare DNS

## 🚀 Quick Start (Local Development)

### Prerequisites

- Java 17+
- Maven 3.6+
- Python 3.10+
- PostgreSQL or Supabase account
- API Keys:
  - Groq API Key ([Get free at groq.com](https://console.groq.com))
  - RapidAPI Key ([YouTube Transcript API](https://rapidapi.com/antonthedev4/api/youtube-transcript3))
  - Google OAuth Credentials ([Google Cloud Console](https://console.cloud.google.com))

### 1. Clone Repository

```bash
git clone https://github.com/UVSHUB/MindTube.git
cd MindTube/NutzyCraftPilotAI
```

### 2. Set Up Python AI Service

```bash
cd AI-Service

# Create virtual environment
python -m venv venv

# Activate (Windows)
venv\Scripts\activate
# Activate (Linux/Mac)
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt

# Download spaCy model
python -m spacy download en_core_web_sm

# Configure environment
# Create .env file with:
GROQ_API_KEY=your_groq_api_key
RAPIDAPI_KEY=your_rapidapi_key

# Run service
python main.py
```

Service runs on: http://localhost:8000

### 3. Set Up Java Backend

```bash
cd ..

# Configure application.properties
# Update database URL and credentials

# Build
mvn clean install

# Run
mvn spring-boot:run
```

Backend runs on: http://localhost:8080

### 4. Configure Frontend

Update `src/main/resources/static/js/config.js`:

```javascript
const API_BASE_URL = "http://localhost:8080";
```

### 5. Test

Open browser: http://localhost:8080/index.html

## 📚 API Documentation

### Python AI Service Endpoints

#### `POST /analyze`

Analyze YouTube video content.

**Request:**

```json
{
  "youtube_url": "https://www.youtube.com/watch?v=VIDEO_ID"
}
```

**Response:**

```json
{
  "title": "Video Title",
  "subject_area": "Computer Science",
  "difficulty_level": "Intermediate",
  "key_concepts": ["Concept 1", "Concept 2"],
  "detailed_explanation": "...",
  "learning_objectives": ["Objective 1"],
  "examples_covered": ["Example 1"],
  "prerequisites": ["Prerequisite 1"],
  "key_takeaways": ["Takeaway 1"],
  "summary": "...",
  "duration": 600
}
```

#### `GET /health`

Health check endpoint.

### Java Backend Endpoints

#### `POST /api/analysis`

Submit video for analysis (requires authentication).

**Request:**

```json
{
  "userId": "user-uuid",
  "url": "https://www.youtube.com/watch?v=VIDEO_ID"
}
```

#### `GET /api/analysis?userId={uuid}`

Get user's analysis history.

#### `DELETE /api/analysis/{id}`

Delete specific analysis.

## 🔧 Configuration

### Environment Variables

#### Python AI Service (.env)

```env
GROQ_API_KEY=gsk_...
RAPIDAPI_KEY=9ff0e...
```

#### Java Backend (application.properties)

```properties
server.port=8080
ai.service.url=http://localhost:8000

# Database (Supabase)
spring.datasource.url=jdbc:postgresql://...
spring.datasource.username=postgres
spring.datasource.password=...

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
```

#### Frontend (config.js)

```javascript
const API_BASE_URL = "https://pilotai-backend.nutzycraft.com";
```

### Google OAuth Setup

1. **Go to**: [Google Cloud Console](https://console.cloud.google.com)
2. **Enable APIs**:
   - Google+ API
   - Google Drive API
3. **Create OAuth 2.0 Client ID**
4. **Add authorized origins**:
   - `http://localhost:8080`
   - `https://mind-tube-gilt.vercel.app`
5. **Add redirect URIs**:
   - `http://localhost:8080/dashboard.html`
   - `https://mind-tube-gilt.vercel.app/dashboard.html`

## 🚀 Production Deployment

See [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) for complete production deployment instructions.

### Quick Deployment Summary

**Frontend (Vercel):**

1. Connect GitHub repository
2. Set build settings to `src/main/resources/static/`
3. Auto-deploys on push to main

**Backend (DigitalOcean):**

```bash
# SSH to server
ssh root@209.97.161.131

# Navigate to project
cd /var/www/pilotai/MindTube/NutzyCraftPilotAI

# Pull latest code
git pull origin main

# Restart services
systemctl restart pilotai-backend  # Java
systemctl restart pilotai-ai       # Python

# Check status
systemctl status pilotai-backend
systemctl status pilotai-ai
```

## 🧪 Testing

### Test AI Service

```bash
cd AI-Service
source venv/bin/activate

# Test with sample video
curl -X POST "http://localhost:8000/analyze" \
  -H "Content-Type: application/json" \
  -d '{"youtube_url": "https://www.youtube.com/watch?v=8mAITcNt710"}'
```

### Test Backend

```bash
mvn test
```

## 🐛 Troubleshooting

### Common Issues

**"Analysis failed: RAPIDAPI_KEY not configured"**

- Add RAPIDAPI_KEY to AI-Service/.env file
- Restart AI service

**"Mixed Content Error"**

- Ensure using HTTPS URLs in production
- Check config.js has correct API_BASE_URL

**"YouTube transcript not available"**

- Video must have captions enabled
- Try a different video with auto-generated captions
- Check RapidAPI quota (500/month on free tier)

**"Google Sign-In not working"**

- Verify OAuth credentials in Google Cloud Console
- Ensure authorized origins and redirect URIs are correct
- Check browser console for CORS errors

### Logs

**Python AI Service:**

```bash
journalctl -u pilotai-ai -n 100 --no-pager
```

**Java Backend:**

```bash
journalctl -u pilotai-backend -n 100 --no-pager
```

**Nginx:**

```bash
tail -f /var/log/nginx/error.log
```

## 📊 Performance

- **Transcript Extraction**: ~3-5 seconds via RapidAPI
- **AI Analysis**: ~10-30 seconds (depends on video length)
- **Total Processing**: ~15-35 seconds per video
- **API Costs**:
  - RapidAPI: 500 free requests/month
  - Groq: Generous free tier
  - Total: ~$0 for moderate usage

## 🔐 Security

- ✅ HTTPS enforced (Let's Encrypt SSL)
- ✅ CORS properly configured
- ✅ OAuth 2.0 authentication
- ✅ Environment variables for secrets
- ✅ Input validation and sanitization
- ✅ SQL injection prevention (JPA)

## 🤝 Contributing

Contributions welcome! Please:

1. Fork the repository
2. Create feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open Pull Request

## 📝 License

This project is licensed under the MIT License.

## 🙏 Acknowledgments

- **Groq** for lightning-fast AI inference
- **RapidAPI** for reliable YouTube transcript extraction
- **Vercel** for seamless frontend hosting
- **DigitalOcean** for robust VPS hosting
- **Google** for OAuth and Drive APIs
- **Let's Encrypt** for free SSL certificates

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/UVSHUB/MindTube/issues)
- **Docs**: Check DEPLOYMENT_GUIDE.md and FRONTEND_UPDATE_GUIDE.md
- **Website**: https://nutzycraft.com

---

**Built with ❤️ by NutzyCraft Team**

_Last Updated: February 6, 2026_
