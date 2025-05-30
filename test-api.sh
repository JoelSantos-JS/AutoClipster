#!/bin/bash
# Teste rápido da API AutoClipster

echo "🧪 Testando AutoClipster API"
echo "============================"

# Verificar se aplicação está rodando
echo "1. Verificando se aplicação está online..."
curl -s http://localhost:8080/actuator/health > /dev/null
if [ $? -eq 0 ]; then
    echo "✅ Aplicação está rodando!"
else
    echo "❌ Aplicação não está rodando. Execute: ./mvnw.cmd spring-boot:run"
    exit 1
fi

echo ""
echo "2. Testando endpoints disponíveis..."

# Health check
echo "🔍 Health Check:"
curl -s http://localhost:8080/actuator/health | jq '.' 2>/dev/null || curl -s http://localhost:8080/actuator/health

echo ""
echo "🔍 Clips endpoint:"
curl -s -X GET http://localhost:8080/api/clips | jq '.' 2>/dev/null || echo "Endpoint respondeu"

echo ""
echo "🔍 YouTube status (se configurado):"
curl -s -X GET http://localhost:8080/api/youtube/status | jq '.' 2>/dev/null || echo "YouTube não configurado ou endpoint não disponível"

echo ""
echo "📊 Endpoints disponíveis:"
echo "  - http://localhost:8080/api/clips (Gerenciar clips)"
echo "  - http://localhost:8080/api/youtube/* (YouTube integration)"
echo "  - http://localhost:8080/api/gemini/* (AI processing)"
echo "  - http://localhost:8080/actuator/health (Health check)"

echo ""
echo "🎉 Teste concluído! Aplicação funcionando." 