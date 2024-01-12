---
-- audit consent table
---
CREATE TYPE consent_type AS ENUM
(
   'DATA_REQUESTED',
   'DATA_SENT',
   'DATA_DENIED'
);

CREATE TABLE IF NOT EXISTS auditing_consent
(
   _id uuid NOT NULL,
   item_id uuid NOT NULL,
   aip_id uuid NOT NULL,
   aiu_id uuid NOT NULL,
   dp_id varchar NOT NULL,
   artifact uuid NOT NULL,
   event consent_type NOT NULL,
   item_type item NOT NULL,
   created_at timestamp without time zone NOT NULL,
   log bytea NOT NULL
);

GRANT USAGE ON SCHEMA ${flyway:defaultSchema} TO ${rsUser};

 GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE auditing_consent TO ${rsUser};