# 🚀 Deployment Guide for NutzyCraft PilotAI

## Overview

- **Frontend**: Auto-deploys via Vercel (just push to GitHub)
- **Backend**: Requires manual deployment on DigitalOcean server

**Production URLs:**

- Frontend: https://mind-tube-gilt.vercel.app
- Backend API: https://pilotai-backend.nutzycraft.com/api/
- AI Service: https://pilotai-backend.nutzycraft.com/ai/

---

## Frontend Deployment (Automatic)

**Frontend files location:** `src/main/resources/static/`

```bash
# 1. Make your changes to HTML/CSS/JS files
# 2. Commit and push
git add .
git commit -m "Your change description"
git push origin main
```

✅ **Vercel automatically deploys** in 1-2 minutes!

---

## Backend Deployment (Manual)

### **Java Backend Changes** (`src/main/java/`)

```bash
# 1. On your local machine - commit and push
git add .
git commit -m "Backend: Your change description"
git push origin main

# 2. SSH into server
ssh root@209.97.161.131

# 3. Navigate to project
cd /var/www/pilotai/MindTube/NutzyCraftPilotAI

# 4. Pull latest code
git stash  # If you have local changes
git pull origin main

# 5. Rebuild the application
mvn clean package -DskipTests

# 6. Restart the backend service
systemctl restart pilotai-backend

# 7. Check if it's running
systemctl status pilotai-backend

# 8. Check logs for errors (optional)
journalctl -u pilotai-backend -n 50 --no-pager
```

---

### **Python AI Service Changes** (`AI-Service/`)

```bash
# 1. On your local machine - commit and push
git add AI-Service/
git commit -m "AI: Your change description"
git push origin main

# 2. SSH into server
ssh root@209.97.161.131

# 3. Navigate to AI service
cd /var/www/pilotai/MindTube/NutzyCraftPilotAI

# 4. Pull latest code
git stash  # If you have local changes
git pull origin main

# 5. If requirements.txt changed, update dependencies
cd AI-Service
source venv/bin/activate
pip install -r requirements.txt
deactivate

# 6. Restart the AI service
systemctl restart pilotai-ai

# 7. Check if it's running
systemctl status pilotai-ai

# 8. Check logs for errors (optional)
journalctl -u pilotai-ai -n 50 --no-pager
```

---

## Quick Reference Commands

### Check Service Status

```bash
# Backend (Java)
systemctl status pilotai-backend

# AI Service (Python)
systemctl status pilotai-ai

# Nginx
systemctl status nginx
```

### View Logs

```bash
# Backend logs
journalctl -u pilotai-backend -n 100 --no-pager

# AI service logs
journalctl -u pilotai-ai -n 100 --no-pager

# Nginx error logs
tail -n 50 /var/log/nginx/error.log
```

### Restart Services

```bash
# Restart backend
systemctl restart pilotai-backend

# Restart AI service
systemctl restart pilotai-ai

# Restart Nginx
systemctl restart nginx
```

---

## Typical Workflow Examples

### Example 1: Fix a Bug in Login (Java Backend)

```bash
# Local machine
git add src/main/java/com/nutzycraft/pilotai/controller/AuthController.java
git commit -m "Fix: Resolve login validation bug"
git push origin main

# SSH to server
ssh root@209.97.161.131
cd /var/www/pilotai/MindTube/NutzyCraftPilotAI
git pull origin main
mvn clean package -DskipTests
systemctl restart pilotai-backend
systemctl status pilotai-backend
```

---

### Example 2: Update AI Analysis Logic (Python)

```bash
# Local machine
git add AI-Service/main.py
git commit -m "AI: Improve transcript cleaning"
git push origin main

# SSH to server
ssh root@209.97.161.131
cd /var/www/pilotai/MindTube/NutzyCraftPilotAI
git pull origin main
systemctl restart pilotai-ai
systemctl status pilotai-ai
```

---

### Example 3: Update Frontend UI

```bash
# Local machine
git add src/main/resources/static/dashboard.html
git commit -m "UI: Improve dashboard layout"
git push origin main

# That's it! Vercel auto-deploys ✅
# No server commands needed
```

---

## Important Tips

1. **Always check service status** after restarting
2. **Check logs** if something doesn't work
3. **Frontend changes** deploy automatically via Vercel
4. **Backend changes** require:
   - Pull code
   - Rebuild (Java only)
   - Restart service
5. **If you change `requirements.txt`** or `pom.xml`, remember to reinstall dependencies

---

## Troubleshooting

**Service won't start?**

```bash
journalctl -u pilotai-backend -n 100 --no-pager
# or
journalctl -u pilotai-ai -n 100 --no-pager
```

**Git pull conflicts?**

```bash
git stash
git pull origin main
```

**Need to roll back?**

```bash
git log --oneline -n 10  # See recent commits
git checkout <commit-hash>  # Roll back to specific commit
systemctl restart pilotai-backend
```

---

## SSL Certificate Renewal

Your SSL certificate from Let's Encrypt is set to auto-renew every 90 days. To manually renew if needed:

```bash
ssh root@209.97.161.131
certbot renew
systemctl reload nginx
```

---

## Server Information

- **Server IP:** 209.97.161.131
- **OS:** Ubuntu 22.04 LTS
- **Domain:** pilotai-backend.nutzycraft.com
- **Services:**
  - Java Backend: Port 8080 (internal)
  - Python AI Service: Port 8000 (internal)
  - Nginx: Port 80 & 443 (public)

---

## Environment Variables

### Production .env File Location

**AI Service:**

```
/var/www/pilotai/MindTube/NutzyCraftPilotAI/AI-Service/.env
```

**Required variables:**

```env
GROQ_API_KEY=your_groq_api_key
RAPIDAPI_KEY=your_rapidapi_key
```

### Java Backend Environment Variables

**Location:** `/etc/systemd/system/pilotai-backend.service`

The backend uses environment variables defined in the systemd service file.

**Key variables:**

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://...
SPRING_DATASOURCE_USERNAME=postgres.xxxxx
SPRING_DATASOURCE_PASSWORD=xxxxx

# SendGrid Email (HTTP API - SMTP is blocked by DigitalOcean)
SENDGRID_API_KEY=SG.xxxxx

# Cloudinary
CLOUDINARY_CLOUD_NAME=xxxxx
CLOUDINARY_API_KEY=xxxxx
CLOUDINARY_API_SECRET=xxxxx
```

**⚠️ Important:** DigitalOcean blocks SMTP ports (25, 587, 465) to prevent spam. We use **SendGrid HTTP API** instead of SMTP.

---

## 📧 SendGrid Email Configuration

### Why SendGrid?

DigitalOcean blocks all SMTP ports on droplets. SendGrid's HTTP API uses HTTPS (port 443) which is not blocked.

### Setup Instructions

1. **Create SendGrid Account**
   - Sign up at https://signup.sendgrid.com/
   - Free tier: 100 emails/day (perfect for verification emails)

2. **Verify Single Sender** (No domain needed)
   - Go to **Settings** → **Sender Authentication**
   - Click **"Verify a Single Sender"**
   - Use: `nutzycraft@gmail.com` (or your email)
   - Check your email and verify

3. **Create API Key**
   - Go to **Settings** → **API Keys**
   - Create new key with **Full Access** or **Mail Send** permission
   - Copy the API key (starts with `SG.`)

4. **Update Server**

   ```bash
   ssh root@209.97.161.131
   nano /etc/systemd/system/pilotai-backend.service
   ```

   Add this line:

   ```
   Environment="SENDGRID_API_KEY=SG.your_api_key_here"
   ```

5. **Reload and Restart**
   ```bash
   systemctl daemon-reload
   systemctl restart pilotai-backend
   systemctl status pilotai-backend
   ```

### Testing Email

```bash
# Watch logs while signing up
journalctl -u pilotai-backend -f

# You should see:
# ✅ Email sent successfully via SendGrid to: user@example.com
```

### Troubleshooting Email Issues

**Emails not sending?**

```bash
# Check logs for SendGrid errors
journalctl -u pilotai-backend -n 100 --no-pager | grep -i sendgrid
```

**Common issues:**

- API key not set in systemd service
- Sender email not verified in SendGrid
- SendGrid API quota exceeded (free tier: 100/day)

---

## Database (Supabase)

Your PostgreSQL database is hosted on Supabase. No manual deployment needed, but connection details are in:

- `.env` files (local)
- Server environment variables (production)

---

## Emergency Contacts

- **GitHub Repo:** https://github.com/UVSHUB/MindTube
- **Vercel Dashboard:** https://vercel.com
- **DigitalOcean:** https://cloud.digitalocean.com
- **Google Cloud Console:** https://console.cloud.google.com

---

## Complete Deployment Checklist

### After Making Code Changes:

- [ ] Test changes locally
- [ ] Commit changes with descriptive message
- [ ] Push to GitHub (`git push origin main`)
- [ ] If backend changed:
  - [ ] SSH to server
  - [ ] Pull latest code
  - [ ] Rebuild (Java) or update deps (Python)
  - [ ] Restart services
  - [ ] Check service status
  - [ ] Test in production
- [ ] If frontend changed:
  - [ ] Wait 1-2 minutes for Vercel deploy
  - [ ] Test in production

---

**Last Updated:** February 6, 2026
