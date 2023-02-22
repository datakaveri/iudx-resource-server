ALTER TABLE subscriptions
ALTER COLUMN dataset_name TYPE varchar,
ALTER COLUMN dataset_json TYPE  json,
ALTER COLUMN user_id TYPE uuid;
