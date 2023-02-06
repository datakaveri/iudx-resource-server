ALTER TABLE auditing_rs ADD COLUMN time timestamp without time zone
UPDATE auditing_rs set time = split_part(isotime,'[',1)::timestamp with time zone at time zone 'UTC'
