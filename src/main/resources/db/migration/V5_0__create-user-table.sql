-- constants enum
CREATE type status_type as ENUM
(   'PENDING',
    'APPROVED',
    'REVOKED'
);

CREATE TABLE IF NOT EXISTS user_table
(
    _id uuid DEFAULT uuid_generate_v4 () NOT NULL,
    userid uuid NOT NULL,
    role varchar NOT NULL,
    status status_type NOT NULL,
    request_json json not null,
    created_at timestamp without time zone NOT NULL,
    modified_at timestamp without time zone NOT NULL,
    CONSTRAINT id_pk PRIMARY KEY (_id)
);

CREATE TRIGGER update_user_table_created BEFORE INSERT ON user_table FOR EACH ROW EXECUTE PROCEDURE update_created ();
CREATE TRIGGER update_usr_table_modified BEFORE INSERT
OR UPDATE ON
   user_table FOR EACH ROW EXECUTE PROCEDURE update_modified ();

 ---
 -- grants
 ---
 GRANT USAGE ON SCHEMA ${flyway:defaultSchema} TO ${rsUser};

 GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE user_table TO ${rsUser};
