---
-- Creating audit table for Data MarketPlace server
---


CREATE TABLE IF NOT EXISTS auditing_dmp
(
   _id uuid DEFAULT uuid_generate_v4 () NOT NULL PRIMARY KEY,
   user_id uuid NOT NULL,
   api varchar NOT NULL,
   method varchar NOT NULL,
   info JSON NOT NULL,
   time timestamp without time zone NOT NULL,
   created_at timestamp without time zone NOT NULL,
   modified_at timestamp without time zone NOT NULL
);

GRANT USAGE ON SCHEMA ${flyway:defaultSchema} TO ${rsUser};

--
-- Triggers
--
CREATE TRIGGER update_auditing_dmp_created BEFORE INSERT ON auditing_dmp FOR EACH ROW EXECUTE PROCEDURE update_created();
CREATE TRIGGER update_auditing_dmp_modified BEFORE INSERT OR UPDATE ON auditing_dmp FOR EACH ROW EXECUTE procedure update_modified();

--
-- Index
--
CREATE INDEX auditing_dmp_user_id_index ON auditing_dmp USING HASH (user_id);

--
-- Grants
--

GRANT USAGE ON SCHEMA ${flyway:defaultSchema} TO ${rsUser};
GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE auditing_dmp TO ${rsUser};