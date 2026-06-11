CREATE TABLE `SPRING_AI_CHAT_MEMORY` (
                                         `conversation_id` varchar(36) NOT NULL,
                                         `content` longtext NOT NULL,
                                         `type` varchar(20) NOT NULL,
                                         `timestamp` timestamp NOT NULL,
                                         KEY `idx_conversation_id_timestamp` (`conversation_id`,`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;