--liquibase formatted sql

--changeset vadimgupal:4
ALTER TABLE users
ADD COLUMN time_zone VARCHAR(128);
--rollback ALTER TABLE users DROP COLUMN time_zone;