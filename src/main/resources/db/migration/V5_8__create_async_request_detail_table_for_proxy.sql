---
-- async_request_detail
---
CREATE TABLE IF NOT EXISTS async_request_detail
(
   _id uuid DEFAULT uuid_generate_v4 () NOT NULL,
   search_id uuid NOT NULL,
   consumer_id uuid NOT NULL,
   resource_id uuid NOT NULL,
   request_query json NOT NULL,
   created_at timestamp without time zone NOT NULL,
   CONSTRAINT async_id_pk PRIMARY KEY (_id)
);

GRANT USAGE ON SCHEMA ${flyway:defaultSchema} TO ${rsUser};

--
-- Trigger
--
CREATE TRIGGER async_request_detail_created BEFORE INSERT
  ON async_request_detail FOR EACH ROW EXECUTE PROCEDURE update_created ();

---
-- index
---
  CREATE INDEX async_request_search_id_index ON async_request_detail USING HASH (search_id);
---
-- grants
---
 GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE async_request_detail TO ${rsUser};