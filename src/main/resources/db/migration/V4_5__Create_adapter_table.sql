----Adapter Table Query
CREATE TABLE IF NOT EXISTS adaptors_details
(
   _id uuid DEFAULT uuid_generate_v4 () NOT NULL,
   exchange_name varchar NOT NULL,
   resource_id varchar NOT NULL,
   dataset_name varchar NOT NULL,
	dataset_details_json jsonb NOT NULL,
	user_id varchar NOT NULL,
    created_at timestamp without time zone NOT NULL,
    modified_at timestamp without time zone NOT NULL,
	CONSTRAINT exchange_name_unique UNIQUE (exchange_name),
	CONSTRAINT candidate_key PRIMARY KEY (_id,exchange_name)
);

CREATE TRIGGER update_ad_created BEFORE INSERT ON adaptors_details FOR EACH ROW EXECUTE PROCEDURE update_created ();
CREATE TRIGGER update_ad_modified BEFORE INSERT
OR UPDATE ON
   adaptors_details FOR EACH ROW EXECUTE PROCEDURE update_modified ();