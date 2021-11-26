-- random uuid extension for primary key.
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";


-- Token invalidation table
-- TODO: decide max length for varchar
CREATE TABLE IF NOT EXISTS revoked_tokens
(
   _id uuid DEFAULT uuid_generate_v4 () NOT NULL,
   client_id varchar NOT NULL,
   rs_url varchar NOT NULL,
   token varchar NOT NULL,
   expiry timestamp with time zone NOT NULL,
   created_at timestamp without time zone NOT NULL,
   CONSTRAINT revoke_tokens_pk PRIMARY KEY (_id)
);


-- Unique attribute table
CREATE TABLE IF NOT EXISTS unique_attributes
(
   _id uuid DEFAULT uuid_generate_v4 () NOT NULL,
   resource_id varchar NOT NULL,
   unique_attribute varchar NOT NULL,
   created_at timestamp without time zone NOT NULL,
   modified_at timestamp without time zone NOT NULL,
   CONSTRAINT unique_attrib_pk PRIMARY KEY (_id)
);


-- Functions for audit[new,update] on table/column 
-- modified_at column function
CREATE
OR REPLACE
   FUNCTION update_modified () RETURNS TRIGGER AS $$
BEGIN NEW.modified_at = now ();
RETURN NEW;
END;
$$ language 'plpgsql';


-- created_at column function
CREATE
OR REPLACE
   FUNCTION update_created () RETURNS TRIGGER AS $$
BEGIN NEW.created_at = now ();
RETURN NEW;
END;
$$ language 'plpgsql';


-- Triggers for audit
-- unique attribute table
CREATE TRIGGER update_ua_modified BEFORE INSERT OR UPDATE ON unique_attributes FOR EACH ROW EXECUTE PROCEDURE update_modified ();
CREATE TRIGGER update_ua_created BEFORE INSERT ON unique_attributes FOR EACH ROW EXECUTE PROCEDURE update_created ();

-- revoked_tokens table
CREATE TRIGGER update_rt_created BEFORE INSERT ON revoked_tokens FOR EACH ROW EXECUTE PROCEDURE update_created ();

