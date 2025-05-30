# 🔧 AutoClipster - Configuração de Environment

Este guia explica como configurar as variáveis de ambiente para o AutoClipster.

## 📋 Pré-requisitos

Antes de começar, você precisa obter credenciais das seguintes APIs:

### 1. 🟣 Twitch API (OBRIGATÓRIO)
- Acesse: https://dev.twitch.tv/console/apps
- Crie uma nova aplicação
- Anote o `Client ID` e `Client Secret`

### 2. 🤖 Gemini AI (OBRIGATÓRIO)
- Acesse: https://makersuite.google.com/app/apikey
- Crie uma nova API Key
- Anote a chave gerada

### 3. 🗄️ PostgreSQL Database (OBRIGATÓRIO)
- Tenha uma instância do PostgreSQL rodando
- Crie um banco de dados chamado `autoclipster`
- Anote as credenciais de conexão

### 4. 📺 YouTube API (OPCIONAL)
- Acesse: https://console.cloud.google.com/
- Ative a YouTube Data API v3
- Crie credenciais OAuth 2.0
- Anote o `Client ID` e `Client Secret`

### 5. 🔗 N8N (OPCIONAL)
- Se usar N8N para automação
- Configure um webhook endpoint

## 🚀 Configuração Rápida

### 1. Copie o arquivo de exemplo:
```bash
cp .env.example .env
```

### 2. Edite o arquivo `.env` com suas credenciais:
```bash
# No Windows
notepad .env

# No Linux/Mac
nano .env
```

### 3. Preencha as variáveis obrigatórias:
```env
# OBRIGATÓRIAS - substitua pelos valores reais
TWITCH_CLIENT_ID=seu_client_id_twitch_aqui
TWITCH_CLIENT_SECRET=seu_client_secret_twitch_aqui
GEMINI_API_KEY=sua_api_key_gemini_aqui
DATABASE_PASSWORD=sua_senha_postgres_aqui
```

## 📖 Variáveis Detalhadas

### 🔑 Obrigatórias
| Variável | Descrição | Onde obter |
|----------|-----------|------------|
| `TWITCH_CLIENT_ID` | ID da aplicação Twitch | [Twitch Console](https://dev.twitch.tv/console/apps) |
| `TWITCH_CLIENT_SECRET` | Secret da aplicação Twitch | [Twitch Console](https://dev.twitch.tv/console/apps) |
| `GEMINI_API_KEY` | Chave da API Gemini | [Google AI Studio](https://makersuite.google.com/app/apikey) |
| `DATABASE_URL` | URL do banco PostgreSQL | Configuração local |
| `DATABASE_USERNAME` | Usuário do banco | Configuração local |
| `DATABASE_PASSWORD` | Senha do banco | Configuração local |

### ⚙️ Configuráveis
| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `CLIPS_DOWNLOAD_PATH` | `./downloads` | Pasta para salvar clipes |
| `DOWNLOAD_TIMEOUT` | `300` | Timeout em segundos |
| `AI_BATCH_SIZE` | `5` | Quantos clipes processar por vez |
| `AI_ENABLED` | `true` | Ativar processamento IA |

### 📺 Opcionais (YouTube)
| Variável | Descrição |
|----------|-----------|
| `YOUTUBE_CLIENT_ID` | Para upload automático no YouTube |
| `YOUTUBE_CLIENT_SECRET` | Para upload automático no YouTube |

### 🔗 Opcionais (N8N)
| Variável | Descrição |
|----------|-----------|
| `N8N_WEBHOOK_URL` | Para integração com workflows N8N |
| `N8N_ENABLED` | Ativar integração N8N |

## 🛡️ Segurança

⚠️ **IMPORTANTE**: 
- **NUNCA** commite o arquivo `.env` no Git
- O arquivo `.env` já está no `.gitignore`
- Use senhas fortes para o banco de dados
- Mantenha suas API keys seguras

## ✅ Validação

Quando você iniciar a aplicação, ela automaticamente:

1. ✅ Verificará se todas as variáveis obrigatórias estão definidas
2. ⚠️ Alertará sobre variáveis opcionais ausentes
3. 📊 Mostrará um resumo da configuração
4. ❌ Falhará se houver variáveis obrigatórias ausentes

### Exemplo de log na inicialização:
```
🔍 Validating environment variables...
📊 Current configuration:
  ├── Twitch API: ✅ Configured
  ├── Gemini AI: ✅ Configured  
  ├── Database: ✅ Configured
  ├── YouTube API: ⚠️  Optional
  ├── N8N Integration: ⚠️  Optional
  ├── AI Processing: ✅ Enabled
  └── Download Path: ./downloads
✅ Environment validation completed successfully!
```

## 🐛 Troubleshooting

### Erro: "Missing required environment variables"
- Verifique se o arquivo `.env` existe na raiz do projeto
- Confirme se todas as variáveis obrigatórias estão preenchidas
- Verifique se não há espaços extras nos valores

### Erro de conexão com banco
- Confirme se o PostgreSQL está rodando
- Teste a conexão manualmente
- Verifique usuário, senha e nome do banco

### Erro na API Twitch/Gemini
- Verifique se as credenciais estão corretas
- Teste as APIs usando Postman ou curl
- Confirme se as APIs estão ativas

## 🆘 Suporte

Se precisar de ajuda:
1. Verifique os logs da aplicação
2. Confirme que seguiu todos os passos
3. Teste as credenciais manualmente
4. Consulte a documentação das APIs 