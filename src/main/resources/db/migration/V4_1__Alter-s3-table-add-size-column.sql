-- Add new column 'size' to s3_upload_url table.
ALTER TABLE s3_upload_url ADD COLUMN size numeric DEFAULT 0 NOT NULL;