package com.opty.socket.service;

import com.opty.socket.model.ChatMessageDocument;
import com.opty.socket.model.Message;
import com.opty.socket.model.MessageType;
import com.opty.socket.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Serviço responsável por persistir mensagens de chat no MongoDB.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageStorageService {

    private final ChatMessageRepository chatMessageRepository;

    /**
     * Salva uma mensagem no banco de dados.
     * Só salva mensagens do tipo MESSAGE.
     *
     * @param message O 'record' da mensagem recebida.
     */
    public void saveMessage(Message message) {
        // Nós só queremos salvar mensagens de chat reais, não pings ou 'connect'
        if (message.type() != MessageType.MESSAGE) {
            return;
        }

        try {
            // Converte o record 'Message' para o 'ChatMessageDocument'
            ChatMessageDocument document = new ChatMessageDocument(message);
            
            // Salva no MongoDB
            chatMessageRepository.save(document);
            
            log.debug("Mensagem salva no DB: sessionId={}", message.sessionId());

        } catch (Exception e) {
            log.error("Falha ao salvar mensagem no MongoDB: sessionId={}, error={}",
                    message.sessionId(), e.getMessage(), e);
            // Mesmo se falhar ao salvar, não queremos quebrar o chat em tempo real
        }
    }
}