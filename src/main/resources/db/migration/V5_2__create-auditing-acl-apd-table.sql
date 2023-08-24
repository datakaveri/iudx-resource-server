---
-- creating auditing_acl_apd;
---
CREATE TABLE IF NOT EXISTS auditing_acl_apd
(
   id varchar NOT NULL,
   userid uuid NOT NULL,
   endpoint uuid NOT NULL,
   method varchar NOT NULL,
   body JSON NOT NULL,
   size numeric NOT NULL,
   time timestamp without time zone,
   created_at timestamp without time zone NOT NULL,
   modified_at timestamp without time zone NOT NULL,
   CONSTRAINT auditing_apd_pk PRIMARY KEY (id)
);

ALTER TABLE auditing_acl_apd OWNER TO ${flyway:user};


 CREATE TRIGGER update_acl_apd_created BEFORE INSERT
  ON auditing_acl_apd FOR EACH ROW EXECUTE PROCEDURE update_created ();

  CREATE TRIGGER update_acl_apd_modified BEFORE INSERT
  OR UPDATE ON
     auditing_acl_apd FOR EACH ROW EXECUTE PROCEDURE update_modified ();
---
-- index
 ---
     CREATE INDEX acl_apd_userid_index ON auditing_acl_apd USING HASH (userid);

---
-- grants
---

  GRANT USAGE ON SCHEMA ${flyway:defaultSchema} TO ${rsUser};
  GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE auditing_acl_apd TO ${rsUser};

