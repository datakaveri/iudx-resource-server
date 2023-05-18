-- constants enum
CREATE type status_type as ENUM
(   'PENDING',
    'APPROVED',
    'REVOKED'
);

CREATE TABLE IF NOT EXISTS dx_user
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

CREATE TRIGGER update_dx_user_created BEFORE INSERT ON dx_user FOR EACH ROW EXECUTE PROCEDURE update_created ();
CREATE TRIGGER update_dx_usr_modified BEFORE INSERT
OR UPDATE ON
   dx_user FOR EACH ROW EXECUTE PROCEDURE update_modified ();

 ---
 -- grants
 ---
 GRANT USAGE ON SCHEMA ${flyway:defaultSchema} TO ${rsUser};

 GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE dx_user TO ${rsUser};
