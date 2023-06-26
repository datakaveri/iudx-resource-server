CREATE type events_type AS ENUM
(
    'SUBS_CREATED',
    'SUBS_DELETED',
    'SUBS_APPEND',
    'SUBS_UPDATED'
);


CREATE TABLE subscription_auditing (
    subscription_id character varying NOT NULL,
    event_type events_type NOT NULL,
    subscription_type sub_type NOT NULL,
    user_id uuid NOT NULL,
    resource_id character varying,
    created_at timestamp without time zone NOT NULL,
    modified_at timestamp without time zone NOT NULL,
);
ALTER TABLE ONLY subscription_auditing
    ADD CONSTRAINT subscription_auditing_pkey PRIMARY KEY (subscription_id,user_id);

ALTER TABLE subscription_auditing OWNER TO iudx_rs_user;


CREATE TRIGGER update_sa_created BEFORE INSERT ON subscription_auditing FOR EACH ROW EXECUTE PROCEDURE update_created ();
CREATE TRIGGER update_sa_modified BEFORE INSERT
OR UPDATE ON
   subscription_auditing FOR EACH ROW EXECUTE PROCEDURE update_modified ();
