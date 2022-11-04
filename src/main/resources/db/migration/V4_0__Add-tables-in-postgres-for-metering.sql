---
-- rsaudit table
---

CREATE TABLE IF NOT EXISTS rsaudit
(
   id varchar NOT NULL,
   api varchar NOT NULL,
   userid varchar NOT NULL,
   epochtime integer NOT NULL,
   resourceid varchar NOT NULL,
   isotime varchar NOT NULL,
   providerid varchar NOT NULL,
   size integer NOT NULL
);

ALTER TABLE rsaudit OWNER TO ${flyway:user};

 ---
 -- grants
 ---

 GRANT USAGE ON SCHEMA ${flyway:defaultSchema} TO ${rsUser};

 GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE rsaudit TO ${rsUser};