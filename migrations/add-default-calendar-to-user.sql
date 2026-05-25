--liquibase formatted sql

--changeset vadimgupal:3
ALTER TABLE users
ADD COLUMN default_calendar_id VARCHAR(512);
--rollback ALTER TABLE users DROP COLUMN default_calendar_id;