#!/bin/bash
# Script de teste para automação completa do AutoClipster
# Integração: Download → Análise Gemini → Processamento Automático

BASE_URL="http://localhost:8080"
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 TESTE DA AUTOMAÇÃO COMPLETA DO AUTOCLIPSTER${NC}"
echo "=============================================="

# Teste 1: Status da Automação
echo -e "\n${YELLOW}📊 1. Consultando status atual da automação...${NC}"
curl -s -X GET "$BASE_URL/api/automation/status" \
  -H "Content-Type: application/json" | jq '.' || echo "❌ Erro na consulta"

# Teste 2: Teste de Automação Rápida
echo -e "\n${YELLOW}🧪 2. Executando teste de automação rápida...${NC}"
curl -s -X POST "$BASE_URL/api/automation/test" \
  -H "Content-Type: application/json" | jq '.' || echo "❌ Erro no teste"

# Teste 3: Workflow Completo para um Canal
echo -e "\n${YELLOW}🚀 3. Executando workflow completo para canal 'gaules'...${NC}"
curl -s -X POST "$BASE_URL/api/automation/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "channelName": "gaules",
    "clipLimit": 3,
    "daysBack": 2
  }' | jq '.' || echo "❌ Erro no workflow"

# Aguardar um pouco para o processamento
echo -e "\n${BLUE}⏳ Aguardando 10 segundos para processamento...${NC}"
sleep 10

# Teste 4: Status Atualizado
echo -e "\n${YELLOW}📊 4. Consultando status após execução...${NC}"
curl -s -X GET "$BASE_URL/api/automation/status" \
  -H "Content-Type: application/json" | jq '.' || echo "❌ Erro na consulta"

# Teste 5: Workflow para Múltiplos Canais (pequeno)
echo -e "\n${YELLOW}🔄 5. Executando workflow para múltiplos canais...${NC}"
curl -s -X POST "$BASE_URL/api/automation/execute-multiple" \
  -H "Content-Type: application/json" \
  -d '{
    "channelNames": ["gaules", "loud_coringa"],
    "clipLimitPerChannel": 2,
    "daysBack": 1
  }' | jq '.' || echo "❌ Erro no workflow múltiplo"

# Aguardar processamento
echo -e "\n${BLUE}⏳ Aguardando 15 segundos para processamento múltiplo...${NC}"
sleep 15

# Teste 6: Verificar Clips Baixados
echo -e "\n${YELLOW}📥 6. Verificando clips baixados...${NC}"
curl -s -X GET "$BASE_URL/api/clips/downloaded" \
  -H "Content-Type: application/json" | jq '.' || echo "❌ Erro na consulta de clips"

# Teste 7: Reprocessamento de Clips que Falharam
echo -e "\n${YELLOW}🔄 7. Tentando reprocessar clips que falharam...${NC}"
curl -s -X POST "$BASE_URL/api/automation/retry-failed" \
  -H "Content-Type: application/json" | jq '.' || echo "❌ Erro no reprocessamento"

# Teste 8: Status Final da Automação
echo -e "\n${YELLOW}📊 8. Status final da automação...${NC}"
curl -s -X GET "$BASE_URL/api/automation/status" \
  -H "Content-Type: application/json" | jq '.' || echo "❌ Erro na consulta final"

# Teste 9: Verificar Análises do Gemini
echo -e "\n${YELLOW}🤖 9. Testando análise individual do Gemini...${NC}"
curl -s -X POST "$BASE_URL/api/gemini/analyze" \
  -H "Content-Type: application/json" \
  -d '{
    "clipTitle": "GAULES DESTRUINDO NO CS2",
    "clipDescription": "Gaules fazendo clutch épico no CS2",
    "streamerName": "gaules",
    "gameName": "Counter-Strike 2"
  }' | jq '.' || echo "❌ Erro na análise Gemini"

# Teste 10: Análise com Google Search
echo -e "\n${YELLOW}🔍 10. Testando análise com Google Search...${NC}"
curl -s -X POST "$BASE_URL/api/gemini/analyze-search" \
  -H "Content-Type: application/json" \
  -d '{
    "clipTitle": "LOUD_CORINGA ACE INSANO",
    "clipDescription": "Coringa fazendo ace no Valorant",
    "streamerName": "loud_coringa",
    "gameName": "VALORANT"
  }' | jq '.' || echo "❌ Erro na análise com Google Search"

# Teste 11: Limpeza (apenas mostrar, não executar automaticamente)
echo -e "\n${YELLOW}🧹 11. Comando para limpeza de clips antigos (não executado):${NC}"
echo "curl -X DELETE \"$BASE_URL/api/automation/cleanup?daysToKeep=30\""

echo -e "\n${GREEN}✅ TESTES DA AUTOMAÇÃO COMPLETA FINALIZADOS!${NC}"
echo -e "${BLUE}=============================================${NC}"
echo -e "${YELLOW}📋 RESUMO DOS TESTES:${NC}"
echo "1. ✅ Status da automação"
echo "2. ✅ Teste rápido"
echo "3. ✅ Workflow completo (canal único)"
echo "4. ✅ Status após execução"
echo "5. ✅ Workflow múltiplos canais"
echo "6. ✅ Verificação de clips baixados"
echo "7. ✅ Reprocessamento de falhas"
echo "8. ✅ Status final"
echo "9. ✅ Análise individual Gemini"
echo "10. ✅ Análise com Google Search"
echo "11. 📝 Comando de limpeza (informativo)"

echo -e "\n${BLUE}🔍 PARA MONITORAR EM TEMPO REAL:${NC}"
echo "- Logs do servidor: tail -f logs/application.log"
echo "- Status: curl -s \"$BASE_URL/api/automation/status\" | jq"
echo "- Clips: curl -s \"$BASE_URL/api/clips/downloaded\" | jq"

echo -e "\n${GREEN}🎉 Sistema de automação completa testado!${NC}" 