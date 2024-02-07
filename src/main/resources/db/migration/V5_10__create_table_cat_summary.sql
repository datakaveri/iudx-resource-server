---
-- cat summary table
---

CREATE TABLE IF NOT EXISTS cat_summary
(
   description varchar NOT NULL,
   count varchar NOT NULL,
   size varchar NOT NULL,
   CONSTRAINT cat_summary_pk PRIMARY KEY (description)
);

ALTER TABLE cat_summary OWNER TO ${flyway:user};

GRANT USAGE ON SCHEMA ${flyway:defaultSchema} TO ${rsUser};
GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE cat_summary TO ${rsUser};