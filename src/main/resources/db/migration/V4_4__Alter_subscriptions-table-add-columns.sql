ALTER TABLE subscriptions
ADD COLUMN dataset_name varchar,
ADD COLUMN dataset_json JSON,
ADD COLUMN user_id uuid;