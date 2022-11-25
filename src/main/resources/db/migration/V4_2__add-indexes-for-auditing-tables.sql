-- Add  indexes for auditing_rs.
CREATE INDEX rs_userid_index ON auditing_rs USING HASH (userid);
CREATE INDEX rs_providerid_index ON auditing_rs USING HASH (providerid);
CREATE INDEX rs_resourceid_index on auditing_rs (resourceid,epochtime);

-- Add  indexes for auditing_cat.
CREATE INDEX cat_userid_index ON auditing_cat (userid);
CREATE INDEX cat_api_index ON auditing_cat (api);
CREATE INDEX cat_time_index ON auditing_cat (time);
CREATE INDEX cat_iudxid_index ON auditing_cat (iudxid);

-- Add  indexes for auditing_aaa.
CREATE INDEX auth_time_index ON auditing_aaa (time);
CREATE INDEX auth_endpoint_index ON auditing_aaa (endpoint);
CREATE INDEX auth_userid_Index ON auditing_aaa (userid);

