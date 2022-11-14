---
-- rs audit table
---

CREATE TABLE IF NOT EXISTS auditing_rs
(
   id varchar NOT NULL,
   api varchar NOT NULL,
   userid varchar NOT NULL,
   epochtime numeric NOT NULL,
   resourceid varchar NOT NULL,
   isotime varchar NOT NULL,
   providerid varchar NOT NULL,
   size numeric NOT NULL
);

ALTER TABLE auditing_rs OWNER TO ${flyway:user};

---
-- cat audit table
---

CREATE TABLE IF NOT EXISTS auditing_cat
(
   id varchar NOT NULL,
   userrole varchar NOT NULL,
   userid varchar NOT NULL,
   iid varchar NOT NULL,
   api varchar NOT NULL,
   method varchar NOT NULL,
   time numeric NOT NULL,
   iudxid varchar NOT NULL,
   CONSTRAINT auditing_cat_pk PRIMARY KEY (id)
);
ALTER TABLE auditing_cat OWNER TO ${flyway:user};

---
-- aaa audit table
---

CREATE TABLE IF NOT EXISTS auditing_aaa
(
   id varchar NOT NULL,
   body varchar NOT NULL,
   userid varchar NOT NULL,
   endpoint varchar NOT NULL,
   method varchar NOT NULL,
   time numeric NOT NULL,
   CONSTRAINT auditing_aaa_pk PRIMARY KEY (id)
);
ALTER TABLE auditing_aaa OWNER TO ${flyway:user};

 ---
 -- grants
 ---

 GRANT USAGE ON SCHEMA ${flyway:defaultSchema} TO ${rsUser};

 GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE auditing_rs TO ${rsUser};
 GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE auditing_cat TO ${rsUser};
 GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE auditing_aaa TO ${rsUser};