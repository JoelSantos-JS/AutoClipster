# AutoClipster

Sistema automático para baixar, analisar e processar clips da Twitch usando **Google Gemini AI**.

## 🚀 **TECNOLOGIAS UTILIZADAS**

- **Spring Boot 3.2.1**
- **Java 21**
- **Google Generative AI SDK 1.1.0** ✅ **(Totalmente Corrigido)**
- **Twitch API**
- **yt-dlp** (para download de clips)
- **H2 Database** (desenvolvimento)
- **Maven**
- **Lombok**
- **Jackson** (JSON processing)

---

## 🤖 **SISTEMA DE AUTOMAÇÃO COMPLETA**

### **🔄 Workflow Automático**

O AutoClipster agora possui um sistema de automação completa que executa o fluxo:

```
📺 Canal → 🎬 Clips → 📥 Download → 🤖 Análise Gemini → ✅ Processamento
```

### **📡 Endpoints de Automação**

#### **1. Executar Workflow Completo (Canal Único)**
```bash
POST /api/automation/execute
```

**Request:**
```json
{
  "channelName": "gaules",
  "clipLimit": 10,
  "daysBack": 7
}
```

#### **2. Executar Workflow Múltiplos Canais**
```bash
POST /api/automation/execute-multiple
```

**Request:**
```json
{
  "channelNames": ["gaules", "loud_coringa", "casimito"],
  "clipLimitPerChannel": 5,
  "daysBack": 3
}
```

#### **3. Status da Automação**
```bash
GET /api/automation/status
```

**Response:**
```json
{
  "totalClipsDownloaded": 25,
  "totalClipsProcessed": 23,
  "isProcessingActive": true,
  "pendingClips": 2,
  "successRate": 92.0,
  "lastUpdate": "2024-01-15T10:30:00"
}
```

#### **4. Reprocessar Clips que Falharam**
```bash
POST /api/automation/retry-failed
```

#### **5. Teste Rápido de Automação**
```bash
POST /api/automation/test
```

#### **6. Limpeza de Clips Antigos**
```bash
DELETE /api/automation/cleanup?daysToKeep=30
```

### **⚡ Processamento Automático**

#### **🎯 Características:**
- **Event-Driven**: Quando um clip é baixado, automaticamente dispara a análise
- **Assíncrono**: Processamento não bloqueia outras operações
- **Inteligente**: Filtra clips por qualidade (views, duração, conteúdo)
- **Resiliente**: Retry automático para clips que falharam
- **Scheduled**: Job automático a cada 5 minutos para clips pendentes

#### **🔍 Filtros de Qualidade:**
```java
// Configurável via application.properties
processing.min-viral-score=7.0
processing.min-duration=15
processing.max-duration=300
processing.min-views=100
```

#### **🤖 Análise Automática Inclui:**
- **Basic Analysis**: Título, descrição, tags otimizadas
- **Function Calling**: Análise estruturada com schema JSON
- **Google Search**: Trends e estratégias SEO em tempo real
- **Safety Settings**: Filtro de conteúdo inadequado
- **Multimodal**: Análise de thumbnail (quando disponível)

---

## 🛠 **GOOGLE GENERATIVE AI SDK - IMPLEMENTAÇÃO CORRIGIDA**

### **📋 Correções Implementadas:**

✅ **1. Métodos de API Corretos:**
```java
// ❌ ANTES (Incorreto)
GenerateContentResponse response = client.generateContent(content);

// ✅ DEPOIS (Correto - SDK 1.1.0)
GenerateContentResponse response = client.models.generateContent(
    "gemini-1.5-flash-8b", content);
```

✅ **2. JSON Parser Robusto:**
```java
// Implementação segura com Jackson
private final ObjectMapper objectMapper = new ObjectMapper();

private ClipAnalysis parseAnalysisResponse(String responseText) {
    try {
        return objectMapper.readValue(responseText, ClipAnalysis.class);
    } catch (JsonProcessingException e) {
        log.error("Erro ao fazer parse do JSON: {}", e.getMessage());
        return getFallbackAnalysis();
    }
}
```

✅ **3. Schema JSON Estruturado:**
```java
Schema responseSchema = Schema.object(
    Map.of(
        "optimizedTitle", Schema.string("Título otimizado para views"),
        "optimizedDescription", Schema.string("Descrição otimizada para SEO"),
        "tags", Schema.array(Schema.string("Tag relevante"))
    )
);
```

✅ **4. Function Calling Automático:**
```java
FunctionDeclaration analyzeClipFunction = FunctionDeclaration.newBuilder()
    .setName("analyze_gaming_clip")
    .setDescription("Analisa clips de gaming para otimização")
    .setParameters(responseSchema)
    .build();
```

✅ **5. Análise Assíncrona:**
```java
@Async
public CompletableFuture<ClipAnalysis> analyzeClipAsync(String title, String description) {
    return CompletableFuture.supplyAsync(() -> {
        // Análise Gemini
        return performAnalysis(title, description);
    });
}
```

✅ **6. Streaming Implementation:**
```java
public void analyzeClipStream(String title, Consumer<String> partialResponseConsumer) {
    GenerateContentStreamResponse streamResponse = 
        client.models.generateContentStream("gemini-1.5-flash-8b", content);
    
    streamResponse.forEach(chunk -> {
        String partialText = chunk.text();
        partialResponseConsumer.accept(partialText);
    });
}
```

✅ **7. Multimodal Input:**
```java
Content multimodalContent = Content.newBuilder()
    .setText("Analise este clip de gaming:")
    .addPart(FileData.fromUri(thumbnailUri, mimeType))
    .build();
```

✅ **8. Safety Settings:**
```java
SafetySetting hateSpeech = SafetySetting.newBuilder()
    .setCategory(HarmCategory.HARM_CATEGORY_HATE_SPEECH)
    .setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_HIGH_AND_ABOVE)
    .build();
```

✅ **9. Google Search Integration:**
```java
Tool googleSearchTool = Tool.newBuilder()
    .addFunctionDeclarations(googleSearchFunction)
    .build();

GenerationConfig config = GenerationConfig.newBuilder()
    .addTools(googleSearchTool)
    .setTemperature(0.7f)
    .build();
```

---

## 📊 **ENDPOINTS DISPONÍVEIS**

### **🤖 Gemini AI Analysis**
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/gemini/analyze` | Análise básica de clip |
| POST | `/api/gemini/analyze-advanced` | Análise com function calling |
| POST | `/api/gemini/analyze-stream` | Análise em tempo real (SSE) |
| POST | `/api/gemini/analyze-multimodal` | Análise com thumbnail |
| POST | `/api/gemini/analyze-search` | Análise com Google Search |
| GET | `/api/gemini/test` | Teste de integração |

### **📥 Download de Clips**
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/clips/download` | Download de clip específico |
| POST | `/api/clips/download-top` | Download dos melhores clips |
| GET | `/api/clips/downloaded` | Listar clips baixados |

### **📺 Twitch Integration**
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/twitch/user/{username}` | Buscar usuário |
| GET | `/api/twitch/clips/{userId}` | Buscar clips do canal |

### **🚀 Automação Completa**
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/automation/execute` | Workflow completo (canal único) |
| POST | `/api/automation/execute-multiple` | Workflow múltiplos canais |
| GET | `/api/automation/status` | Status da automação |
| POST | `/api/automation/retry-failed` | Reprocessar falhas |
| POST | `/api/automation/test` | Teste rápido |
| DELETE | `/api/automation/cleanup` | Limpeza de clips antigos |

---

## ⚙️ **CONFIGURAÇÃO**

### **📁 application.properties**
```properties
# Twitch API
twitch.client-id=YOUR_TWITCH_CLIENT_ID
twitch.client-secret=YOUR_TWITCH_CLIENT_SECRET

# Google AI
google.ai.api-key=YOUR_GOOGLE_AI_API_KEY

# Download
app.clips.download-path=./downloads
app.download.timeout=300

# Processamento Automático
processing.min-viral-score=7.0
processing.min-duration=15
processing.max-duration=300
processing.min-views=100
```

### **🔧 Maven Dependencies**
```xml
<dependency>
    <groupId>com.google.ai.generativelanguage</groupId>
    <artifactId>google-ai-generativelanguage</artifactId>
    <version>1.1.0</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

### **🔨 Build Configuration**
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <source>21</source>
                <target>21</target>
            </configuration>
        </plugin>
    </plugins>
</build>
```

---

## 🧪 **TESTANDO A APLICAÇÃO**

### **1. 🤖 Testes do Gemini**
```bash
chmod +x test-gemini-endpoints.sh
./test-gemini-endpoints.sh
```

### **2. 🚀 Testes de Automação Completa**
```bash
chmod +x test-automation-endpoints.sh
./test-automation-endpoints.sh
```

### **3. 📊 Monitoramento em Tempo Real**
```bash
# Status da automação
curl -s "http://localhost:8080/api/automation/status" | jq

# Clips baixados
curl -s "http://localhost:8080/api/clips/downloaded" | jq

# Logs em tempo real
tail -f logs/application.log
```

---

## 🔄 **EXEMPLO DE USO DA AUTOMAÇÃO COMPLETA**

### **1. Executar Automação para Canal:**
```bash
curl -X POST "http://localhost:8080/api/automation/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "channelName": "gaules",
    "clipLimit": 5,
    "daysBack": 3
  }'
```

### **2. Resultado Esperado:**
- ✅ Canal encontrado: "gaules"
- ✅ 5 clips dos últimos 3 dias baixados
- ✅ Análise automática com Gemini iniciada
- ✅ Clips processados e otimizados
- ✅ Relatório de qualidade gerado

### **3. Fluxo Completo Executado:**
```
🔍 Buscar Canal → 🎬 Obter Clips → 📥 Download Automático 
    ↓
📡 Evento Publicado → 🤖 Análise Gemini → ✅ Processamento
    ↓
🎯 Filtros de Qualidade → 📊 Estatísticas → 💾 Persistência
```

---

## 🎯 **MELHORIAS IMPLEMENTADAS**

✅ **Robust JSON Parsing** - Parser seguro com fallback  
✅ **Error Handling** - Tratamento completo de erros  
✅ **Rate Limiting** - Controle de requisições  
✅ **Async Processing** - Processamento assíncrono  
✅ **Event-Driven Architecture** - Arquitetura baseada em eventos  
✅ **Comprehensive Logging** - Logs detalhados  
✅ **Memory Management** - Gestão eficiente de memória  
✅ **Safety Settings** - Filtros de segurança  
✅ **Google Search Integration** - Integração com Google Search  
✅ **Multimodal Analysis** - Análise de imagem e texto  
✅ **Automated Workflow** - Fluxo completamente automatizado  

---

## 📈 **STATUS DA IMPLEMENTAÇÃO**

🎉 **100% Compatível com Google Generative AI SDK 1.1.0**

A implementação está **totalmente corrigida** e segue rigorosamente a documentação oficial do SDK, incluindo:

- ✅ Métodos de API corretos (`client.models.generateContent`)
- ✅ Parsing JSON robusto e seguro
- ✅ Function calling implementado
- ✅ Streaming de respostas
- ✅ Análise multimodal
- ✅ Configurações de segurança
- ✅ Integração com Google Search
- ✅ Sistema de automação completa
- ✅ Processamento assíncrono e event-driven

## 🚀 **PRÓXIMOS PASSOS**

1. **🎬 Upload Automático para YouTube** - Integração com YouTube API
2. **📊 Dashboard de Monitoramento** - Interface web para acompanhar o progresso
3. **🔔 Notificações** - Webhooks e notificações push
4. **📈 Analytics Avançados** - Métricas de performance e ROI
5. **🤖 IA Preditiva** - Machine Learning para prever clips virais

---

💡 **O AutoClipster agora é uma solução completa de automação para criadores de conteúdo!**
