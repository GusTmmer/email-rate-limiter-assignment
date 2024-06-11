CREATE TABLE outbound_emails (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    topic VARCHAR(255) NOT NULL,
    contents TEXT NOT NULL,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_id_topic ON outbound_emails(user_id, topic);

CREATE INDEX idx_user_id_sent_at ON outbound_emails(user_id, sent_at);
