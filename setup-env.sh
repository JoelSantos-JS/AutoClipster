#!/bin/bash
# AutoClipster Environment Setup Script

echo "🚀 AutoClipster Environment Setup"
echo "=================================="

# Criar backup do .env atual
cp .env .env.backup
echo "✅ Backup do .env criado (.env.backup)"

echo ""
echo "Escolha uma opção:"
echo "1) Configurar YouTube API (recomendado)"
echo "2) Usar sem YouTube (apenas download/análise)"
echo "3) Configuração completa (YouTube + N8N)"

read -p "Digite sua opção (1-3): " option

case $option in
    1)
        echo ""
        echo "📺 Configuração YouTube API"
        echo "=========================="
        echo "Primeiro, você precisa:"
        echo "1. Ir para: https://console.cloud.google.com/"
        echo "2. Criar projeto e ativar YouTube Data API v3"
        echo "3. Criar credenciais OAuth 2.0"
        echo ""
        
        read -p "Você já tem o CLIENT_ID? (s/n): " has_client_id
        
        if [ "$has_client_id" = "s" ]; then
            read -p "Digite seu YouTube CLIENT_ID: " youtube_client_id
            read -p "Digite seu YouTube CLIENT_SECRET: " youtube_client_secret
            
            # Atualizar .env
            sed -i "s/YOUTUBE_CLIENT_ID=.*/YOUTUBE_CLIENT_ID=$youtube_client_id/" .env
            sed -i "s/YOUTUBE_CLIENT_SECRET=.*/YOUTUBE_CLIENT_SECRET=$youtube_client_secret/" .env
            sed -i "s/YOUTUBE_AUTO_UPLOAD=.*/YOUTUBE_AUTO_UPLOAD=true/" .env
            
            echo "✅ YouTube configurado!"
        else
            echo "📖 Consulte o arquivo YOUTUBE_API_SETUP.md para instruções detalhadas"
        fi
        ;;
    2)
        echo "🔧 Configurando para uso sem YouTube..."
        sed -i "s/YOUTUBE_AUTO_UPLOAD=.*/YOUTUBE_AUTO_UPLOAD=false/" .env
        sed -i "s/N8N_ENABLED=.*/N8N_ENABLED=false/" .env
        echo "✅ Configurado para uso sem YouTube!"
        ;;
    3)
        echo "🔧 Configuração completa..."
        echo "Esta opção requer YouTube API + N8N running"
        echo "Consulte YOUTUBE_API_SETUP.md para instruções completas"
        ;;
    *)
        echo "❌ Opção inválida"
        exit 1
        ;;
esac

echo ""
echo "🎉 Configuração concluída!"
echo "Para aplicar as mudanças:"
echo "1. Reinicie a aplicação: ./mvnw.cmd spring-boot:run"
echo "2. Teste a API: http://localhost:8080/api/clips"
echo ""
echo "📚 Documentação completa: YOUTUBE_API_SETUP.md" 