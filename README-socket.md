Opty - Servidor de Mensageria (WebSocket)
Este repositório contém o servidor de mensageria em tempo real para a aplicação Opty, desenvolvido em Java com Spring Boot e Spring WebSocket.

Funcionalidades
Estabelecimento de conexão WebSocket segura.
Validação de usuários via token JWT na conexão.
Troca de mensagens em formato JSON entre cliente e servidor.
Lógica para respostas automáticas e broadcast de mensagens (se necessário no futuro).

Tecnologias
Framework: Spring Boot
Comunicação: Spring WebSocket\
Build: Maven

Executando Localmente
Pré-requisitos
Java (JDK 17 ou superior)
Maven

Passos
Clone o repositório:

git clone [https://github.com/seu-usuario/opty-socket-server.git](https://github.com/seu-usuario/opty-socket-server.git)
cd opty-socket-server

Configure as variáveis de ambiente:

No arquivo src/main/resources/application.properties, configure a chave secreta para validar o JWT (deve ser a mesma do backend):

jwt.secret=uma_chave_secreta_muito_longa

Compile e execute o projeto:

mvn spring-boot:run

O servidor WebSocket estará escutando na porta 8080.
