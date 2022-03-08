-- random uuid extension for primary key.
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

ALTER SCHEMA ${flyway:defaultSchema} OWNER TO ${flyway:user};

-- constants enum
-- subscription types
CREATE TYPE sub_type AS ENUM
(
   'STREAMING',
   'CALLBACK'
);

CREATE type Query_Progress as ENUM
(
   'IN_PROGRESS',
   'ERROR',
   'COMPLETE'
)


---
-- Token invalidation table
---
CREATE TABLE IF NOT EXISTS revoked_tokens
(
   _id uuid NOT NULL,
   expiry timestamp with time zone NOT NULL,
   created_at timestamp without time zone NOT NULL,
   modified_at timestamp without time zone NOT NULL,
   CONSTRAINT revoke_tokens_pk PRIMARY KEY (_id)
);

ALTER TABLE revoked_tokens OWNER TO ${flyway:user};

---
-- Unique attribute table
---
CREATE TABLE IF NOT EXISTS unique_attributes
(
   _id uuid DEFAULT uuid_generate_v4 () NOT NULL,
   resource_id varchar NOT NULL,
   unique_attribute varchar NOT NULL,
   created_at timestamp without time zone NOT NULL,
   modified_at timestamp without time zone NOT NULL,
   CONSTRAINT unique_attrib_pk PRIMARY KEY (_id),
   CONSTRAINT resource_id_unique UNIQUE (resource_id)
);

ALTER TABLE unique_attributes OWNER TO ${flyway:user};

---
-- subscription table
---
CREATE TABLE IF NOT EXISTS subscriptions
(
   _id varchar NOT NULL,
   -- subscription id
   _type sub_type NOT NULL,
   queue_name varchar NOT NULL,
   entity varchar NOT NULL,
   expiry timestamp without time zone NOT NULL,
   created_at timestamp without time zone NOT NULL,
   modified_at timestamp without time zone NOT NULL,
   CONSTRAINT sub_pk PRIMARY KEY
   (
      queue_name,
      entity
   )
);

---
-- s3 URL table
---
CREATE TABLE IF NOT EXISTS s3_upload_url
(
   _id uuid NOT NULL,
   search_id uuid NOT NULL,
   request_id TEXT NOT NULL,
   status Query_Progress NOT NULL,
   s3_url varchar,
   expiry timestamp without time zone,
   user_id varchar,
   object_id varchar,
   created_at timestamp without time zone NOT NULL,
   modified_at timestamp without time zone NOT NULL,
   CONSTRAINT upload_url_pk PRIMARY KEY (_id)
);

ALTER TABLE subscriptions OWNER TO ${flyway:user};

---
-- databroker table
---
CREATE TABLE IF NOT EXISTS databroker
(
   username character varying (255) NOT NULL,
   password character varying (50) NOT NULL,
   created_at timestamp without time zone NOT NULL,
   modified_at timestamp without time zone NOT NULL,
   CONSTRAINT databroker_pkey PRIMARY KEY (username),
   CONSTRAINT databroker_username_key UNIQUE (username)
);

ALTER TABLE databroker OWNER TO ${flyway:user};

---
--gis table
---
CREATE TABLE IF NOT EXISTS gis
(
   iudx_resource_id character varying NOT NULL,
   url varchar NOT NULL,
   isOpen BOOLEAN NOT NULL,
   port integer NOT NULL,
   created_at timestamp without time zone NOT NULL,
   modified_at timestamp without time zone NOT NULL,
   username varchar,
   password varchar,
   tokenurl character varying,
   CONSTRAINT gis_pk PRIMARY KEY (iudx_resource_id)
)

ALTER TABLE gis OWNER TO ${flyway:user};

---
-- Functions for audit[cerated,updated] on table/column
---

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

---
-- Triggers
---

-- unique attribute table
CREATE TRIGGER update_ua_created BEFORE INSERT ON unique_attributes FOR EACH ROW EXECUTE PROCEDURE update_created ();
CREATE TRIGGER update_ua_modified BEFORE INSERT
OR UPDATE ON
   unique_attributes FOR EACH ROW EXECUTE PROCEDURE update_modified ();
   

-- revoked_tokens table
CREATE TRIGGER update_rt_created BEFORE INSERT ON revoked_tokens FOR EACH ROW EXECUTE PROCEDURE update_created ();
CREATE TRIGGER update_rt_modified BEFORE INSERT
OR UPDATE ON
   revoked_tokens FOR EACH ROW EXECUTE PROCEDURE update_modified ();


--subscription table
CREATE TRIGGER update_sub_created BEFORE INSERT ON subscriptions FOR EACH ROW EXECUTE PROCEDURE update_created ();
CREATE TRIGGER update_sub_modified BEFORE INSERT
OR UPDATE ON
   subscriptions FOR EACH ROW EXECUTE PROCEDURE update_modified ();

-- s3_upload_url
CREATE TRIGGER update_s3_url_created BEFORE INSERT ON s3_upload_url FOR EACH ROW EXECUTE PROCEDURE update_created ();
CREATE TRIGGER update_s3_url_modified BEFORE INSERT
OR UPDATE ON
   s3_upload_url FOR EACH ROW EXECUTE PROCEDURE update_modified ();


--databroker table
CREATE TRIGGER update_user_created BEFORE INSERT ON databroker FOR EACH ROW EXECUTE PROCEDURE update_created ();
CREATE TRIGGER update_user_modified BEFORE INSERT
OR UPDATE ON
   databroker FOR EACH ROW EXECUTE PROCEDURE update_modified ();
   
   
-- gis table
CREATE TRIGGER update_gis_created BEFORE INSERT ON gis FOR EACH ROW EXECUTE PROCEDURE update_created ();
CREATE TRIGGER update_gis_modified BEFORE INSERT OR UPDATE ON gis FOR EACH ROW EXECUTE PROCEDURE update_modified ();
   
   
 ---  
 -- grants
 ---
 
 GRANT USAGE ON SCHEMA ${flyway:defaultSchema} TO ${rsUser};
 
 GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE revoked_tokens TO ${rsUser};
 GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE unique_attributes TO ${rsUser};
 GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE subscriptions TO ${rsUser};
 GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE databroker TO ${rsUser};
 GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE gis TO ${rsUser};
 
