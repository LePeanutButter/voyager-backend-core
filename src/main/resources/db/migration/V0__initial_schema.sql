-- Baseline schema aligned with JPA entities (PostgreSQL).
-- user_interest_tags: User @ElementCollection "interests"
-- user_interests: UserInterest entity (matching / discovery)

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    first_name      VARCHAR(50)  NOT NULL,
    last_name       VARCHAR(50)  NOT NULL,
    phone_number    VARCHAR(20),
    role            VARCHAR(32)  NOT NULL DEFAULT 'TRAVELER',
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    account_non_expired       BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_locked        BOOLEAN NOT NULL DEFAULT TRUE,
    credentials_non_expired   BOOLEAN NOT NULL DEFAULT TRUE,
    profile_image_url TEXT,
    bio             VARCHAR(500),
    is_google_user  BOOLEAN      NOT NULL DEFAULT FALSE,
    date_of_birth   TIMESTAMP,
    status          VARCHAR(32)  DEFAULT 'ACTIVE',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP
);

CREATE INDEX idx_user_email ON users (email);
CREATE INDEX idx_user_username ON users (username);

CREATE TABLE user_interest_tags (
    user_id   BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    interest  VARCHAR(255) NOT NULL,
    PRIMARY KEY (user_id, interest)
);

CREATE TABLE user_interests (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    interest   VARCHAR(255) NOT NULL
);

CREATE INDEX idx_user_interest_user ON user_interests (user_id);
CREATE INDEX idx_user_interest_value ON user_interests (interest);

CREATE TABLE connections (
    id            BIGSERIAL PRIMARY KEY,
    requester_id  BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    recipient_id  BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status        VARCHAR(32)  NOT NULL,
    message       VARCHAR(500),
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP
);

CREATE TABLE travel_plans (
    id                    BIGSERIAL PRIMARY KEY,
    user_id               BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title                 VARCHAR(255) NOT NULL,
    description           TEXT,
    status                VARCHAR(32)  NOT NULL DEFAULT 'DRAFT',
    travel_type           VARCHAR(32)  NOT NULL DEFAULT 'LEISURE',
    start_date            TIMESTAMP    NOT NULL,
    end_date              TIMESTAMP    NOT NULL,
    estimated_budget      NUMERIC(19, 2),
    actual_cost           NUMERIC(19, 2),
    number_of_travelers   INTEGER      DEFAULT 1,
    origin_location       VARCHAR(255),
    destination_location  VARCHAR(255) NOT NULL,
    is_public             BOOLEAN      DEFAULT FALSE,
    share_token           VARCHAR(255),
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP
);

CREATE INDEX idx_travel_plan_user ON travel_plans (user_id);
CREATE INDEX idx_travel_plan_status ON travel_plans (status);
CREATE INDEX idx_travel_plan_start_date ON travel_plans (start_date);

CREATE TABLE travel_plan_activities (
    id                 BIGSERIAL PRIMARY KEY,
    travel_plan_id     BIGINT       NOT NULL REFERENCES travel_plans (id) ON DELETE CASCADE,
    name               VARCHAR(255) NOT NULL,
    description        TEXT,
    type               VARCHAR(32)  NOT NULL DEFAULT 'SIGHTSEEING',
    start_time         TIMESTAMP    NOT NULL,
    end_time           TIMESTAMP    NOT NULL,
    location           VARCHAR(255),
    estimated_cost     NUMERIC(19, 2),
    actual_cost        NUMERIC(19, 2),
    booking_reference  VARCHAR(255),
    is_confirmed       BOOLEAN      DEFAULT FALSE,
    notes              VARCHAR(255),
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP
);

CREATE INDEX idx_activity_travel_plan ON travel_plan_activities (travel_plan_id);
CREATE INDEX idx_activity_start_time ON travel_plan_activities (start_time);

CREATE TABLE reservations (
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    travel_plan_id       BIGINT       REFERENCES travel_plans (id) ON DELETE SET NULL,
    name                 VARCHAR(255) NOT NULL,
    description          TEXT,
    type                 VARCHAR(32)  NOT NULL DEFAULT 'HOTEL',
    status               VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    confirmation_number  VARCHAR(255),
    start_date           TIMESTAMP    NOT NULL,
    end_date             TIMESTAMP    NOT NULL,
    location             VARCHAR(255),
    service_provider     VARCHAR(255),
    total_cost           NUMERIC(19, 2),
    deposit_paid         NUMERIC(19, 2),
    is_paid              BOOLEAN      DEFAULT FALSE,
    payment_method       VARCHAR(255),
    cancellation_policy  VARCHAR(255),
    special_requests     VARCHAR(255),
    contact_info         VARCHAR(255),
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP
);

CREATE INDEX idx_reservation_user ON reservations (user_id);
CREATE INDEX idx_reservation_travel_plan ON reservations (travel_plan_id);
CREATE INDEX idx_reservation_status ON reservations (status);
CREATE INDEX idx_reservation_type ON reservations (type);

CREATE TABLE messages (
    id             BIGSERIAL PRIMARY KEY,
    connection_id  BIGINT       NOT NULL REFERENCES connections (id) ON DELETE CASCADE,
    sender_id      BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    recipient_id   BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    content        VARCHAR(1000) NOT NULL,
    status         VARCHAR(32)  NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP
);

CREATE TABLE user_connections (
    id             BIGSERIAL PRIMARY KEY,
    requester_id   BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    recipient_id   BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status         VARCHAR(20)  NOT NULL,
    CONSTRAINT uk_user_connection_pair UNIQUE (requester_id, recipient_id)
);

CREATE INDEX idx_user_connection_requester ON user_connections (requester_id);
CREATE INDEX idx_user_connection_recipient ON user_connections (recipient_id);
CREATE INDEX idx_user_connection_status ON user_connections (status);

CREATE TABLE travel_plan_participants (
    id              BIGSERIAL PRIMARY KEY,
    travel_plan_id  BIGINT       NOT NULL REFERENCES travel_plans (id) ON DELETE CASCADE,
    user_id         BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_tpp_travel_plan_user UNIQUE (travel_plan_id, user_id)
);

CREATE INDEX idx_tpp_travel_plan ON travel_plan_participants (travel_plan_id);
CREATE INDEX idx_tpp_user ON travel_plan_participants (user_id);

CREATE TABLE activities (
    id               BIGSERIAL PRIMARY KEY,
    owner_user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title            VARCHAR(120) NOT NULL,
    trip_context_id  BIGINT       NOT NULL,
    scheduled_at     TIMESTAMP,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP
);

CREATE INDEX idx_activity_owner ON activities (owner_user_id);
CREATE INDEX idx_activity_trip_context ON activities (trip_context_id);

CREATE TABLE shared_activities (
    id               BIGSERIAL PRIMARY KEY,
    activity_id      BIGINT       NOT NULL REFERENCES travel_plan_activities (id) ON DELETE CASCADE,
    sender_id        BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    receiver_id      BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status           VARCHAR(20)  NOT NULL,
    is_shared_plan   BOOLEAN      NOT NULL DEFAULT FALSE,
    version          BIGINT       NOT NULL DEFAULT 0,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP
);

CREATE INDEX idx_shared_activity_receiver_status ON shared_activities (receiver_id, status);
CREATE INDEX idx_shared_activity_activity ON shared_activities (activity_id);

CREATE TABLE shared_space_access (
    id                 BIGSERIAL PRIMARY KEY,
    connection_id      BIGINT       NOT NULL REFERENCES connections (id) ON DELETE CASCADE,
    user_id            BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    space_type         VARCHAR(255) NOT NULL,
    access_granted_at  TIMESTAMP
);
