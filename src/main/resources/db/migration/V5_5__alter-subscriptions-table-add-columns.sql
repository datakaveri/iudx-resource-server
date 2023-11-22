ALTER TABLE subscriptions ADD COLUMN resource_group uuid;
ALTER TABLE subscriptions ADD COLUMN delegator_id uuid;
ALTER TABLE subscriptions ADD COLUMN item_type item;
ALTER TABLE subscriptions ADD COLUMN provider_id uuid;