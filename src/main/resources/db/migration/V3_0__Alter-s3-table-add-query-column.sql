-- Add new value 'SUBMITTED' to 'Query_Progress' enum.
ALTER TYPE Query_Progress ADD VALUE 'SUBMITTED';

-- Add new column 'query' to s3_upload_url table.
ALTER TABLE s3_upload_url ADD COLUMN query JSON NOT NULL DEFAULT '{}'::JSON; 