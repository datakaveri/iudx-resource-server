CREATE TYPE item AS ENUM
(
   'RESOURCE',
   'RESOURCE_GROUP'
);

ALTER TABLE auditing_rs ADD COLUMN resource_group varchar;
ALTER TABLE auditing_rs ADD COLUMN item_type item;