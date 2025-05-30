# 🔴 YouTube API Configuration Guide

## Step 1: Google Cloud Console Setup

1. **Acesse**: https://console.cloud.google.com/
2. **Crie um novo projeto** ou selecione um existente
3. **Nome sugerido**: "AutoClipster YouTube Integration"

## Step 2: Enable YouTube Data API v3

1. No painel esquerdo, vá em **"APIs & Services" > "Library"**
2. Procure por **"YouTube Data API v3"**
3. Clique em **"ENABLE"**

## Step 3: Create OAuth 2.0 Credentials

1. Vá em **"APIs & Services" > "Credentials"**
2. Clique em **"+ CREATE CREDENTIALS" > "OAuth client ID"**
3. Se for sua primeira vez, configure a **OAuth consent screen**:
   - User Type: **External**
   - App name: **AutoClipster**
   - User support email: seu email
   - Developer contact: seu email
   - Scopes: adicione **`https://www.googleapis.com/auth/youtube.upload`**

4. Criar OAuth client ID:
   - Application type: **Web application**
   - Name: **AutoClipster Web Client**
   - Authorized redirect URIs: **`http://localhost:8080/auth/youtube/callback`**

## Step 4: Configure Environment Variables

Após criar as credenciais, você receberá:
- **Client ID**: Algo como `123456789-abcdefg.apps.googleusercontent.com`
- **Client Secret**: Algo como `GOCSPX-abcdefg123456789`

Edite o arquivo `.env`:

```bash
# Substitua pelos seus valores reais
YOUTUBE_CLIENT_ID=sua_client_id_aqui
YOUTUBE_CLIENT_SECRET=seu_client_secret_aqui
YOUTUBE_REDIRECT_URI=http://localhost:8080/auth/youtube/callback
YOUTUBE_AUTO_UPLOAD=true  # ou false se quiser controle manual
```

## Step 5: Test the Configuration

1. Reinicie a aplicação
2. Acesse: http://localhost:8080/api/youtube/auth
3. Faça a autenticação com sua conta do YouTube
4. Teste um upload

---

# 🔵 N8N Integration Setup (Opcional)

N8N é uma ferramenta de automação de workflow. Se você quiser integrar:

## Option 1: Docker (Recomendado)

```bash
docker run -it --rm --name n8n -p 5678:5678 n8nio/n8n
```

## Option 2: NPM

```bash
npm install n8n -g
n8n start
```

## Configure N8N Webhook

1. Acesse: http://localhost:5678
2. Crie um novo workflow
3. Adicione um **Webhook Node**
4. Configure o webhook path: `/webhook/autoclipster`
5. Ative o workflow

## Update Environment Variables

```bash
N8N_WEBHOOK_URL=http://localhost:5678/webhook/autoclipster
N8N_ENABLED=true
```

---

## 🚀 Quick Start (Apenas YouTube)

Se você quiser começar apenas com YouTube (sem N8N):

```bash
# Manter N8N desabilitado
N8N_ENABLED=false

# Configurar apenas YouTube
YOUTUBE_CLIENT_ID=sua_client_id_real
YOUTUBE_CLIENT_SECRET=seu_client_secret_real
YOUTUBE_AUTO_UPLOAD=false
``` 