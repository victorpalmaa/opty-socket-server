Escopo Técnico: Opty - Servidor de Mensageria
Versão: 1.0

1. Visão Geral
Este escopo define os requisitos para o serviço de comunicação em tempo real do Opty. O serviço será um servidor WebSocket construído em Java com Spring Boot, responsável por gerenciar conexões persistentes com os clientes.

2. Arquitetura
Endpoint: O servidor exporá um único endpoint WebSocket em /chat.
Handler de Mensagens: Uma classe ChatWebSocketHandler estenderá TextWebSocketHandler para gerenciar o ciclo de vida da conexão (afterConnectionEstablished, handleTextMessage, afterConnectionClosed).
Autenticação: A autenticação será feita via um HandshakeInterceptor. Ele irá interceptar a requisição de handshake, extrair o token JWT do query parameter token e validá-lo. Se o token for inválido, a conexão será rejeitada.
Formato das Mensagens: A comunicação ocorrerá via mensagens de texto no formato JSON.

3. Fluxo de Conexão
O cliente (frontend) tenta estabelecer uma conexão em ws://localhost:8080/chat?token=SEU_JWT.
O HandshakeInterceptor intercepta a requisição.
O token é extraído, validado (assinatura e expiração).
Se válido, a conexão é estabelecida e o userId é associado à sessão.
Se inválido, a conexão é fechada com status 1008 (Policy Violation).

4. Payloads de Mensagem
Cliente → Servidor: { "type": "user_message", "content": "string" }
Servidor → Cliente: { "type": "server_response", "content": "string", "timestamp": "ISODate" }
