package com.onthi.v_edu.chat.repository;

import com.onthi.v_edu.chat.entity.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderByTimestampAsc(
            Integer senderId1, Integer receiverId1, Integer senderId2, Integer receiverId2);

    @org.springframework.data.mongodb.repository.Query(value = "{ $or: [ { 'senderId': ?0 }, { 'receiverId': ?0 } ] }", fields = "{ 'senderId': 1, 'receiverId': 1 }")
    List<ChatMessage> findRawContacts(Integer userId);

    default List<Integer> findDistinctContactIds(Integer userId) {
        return findRawContacts(userId).stream()
                .map(m -> m.getSenderId().equals(userId) ? m.getReceiverId() : m.getSenderId())
                .distinct()
                .collect(java.util.stream.Collectors.toList());
    }
}
