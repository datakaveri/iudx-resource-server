CREATE TABLE IF NOT EXISTS ingestion
(
   _id uuid DEFAULT uuid_generate_v4 () NOT NULL,
   exchange_name varchar NOT NULL,
   resource_id varchar NOT NULL,
   dataset_name varchar NOT NULL,
	dataset_details_json jsonb NOT NULL,
	user_id varchar NOT NULL,
	CONSTRAINT exchange_name_unique UNIQUE (exchange_name)
);