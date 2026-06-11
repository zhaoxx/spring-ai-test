CREATE TABLE IF NOT EXISTS `spring_ai_chat_memory` (
                                         `id` bigint NOT NULL AUTO_INCREMENT,
                                         `conversation_id` varchar(36) NOT NULL,
                                         `content` longtext NOT NULL,
                                         `type` varchar(20) NOT NULL,
                                         `timestamp` timestamp NOT NULL,
                                         PRIMARY KEY (`id`),
                                         KEY `idx_conversation_id_timestamp` (`conversation_id`,`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;