-- random uuid extension for primary key.
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Token invalidation table
-- TODO: decide max length for varchar
CREATE TABLE revoked_tokens (
    _id uuid DEFAULT uuid_generate_v4() NOT NULL,
    client_id varchar NOT NULL,
    rs_url varchar NOT NULL,
    token varchar NOT NULL,
    expiry timestamp with time zone NOT NULL
);


-- pk constraints
ALTER TABLE ONLY revoked_tokens ADD CONSTRAINT revoke_tokens_pk PRIMARY KEY (_id);