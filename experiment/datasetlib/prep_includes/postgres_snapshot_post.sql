\echo [INFO] Prepare DB files for postgres (Start)
\timing on
\c :v0;
-- CREATE INDEX N_g___0 ON N_g (_0);
CREATE INDEX N_g___1 ON N_g (_1);
-- CREATE INDEX E_g___0 ON E_g (_0);
CREATE INDEX E_g___1 ON E_g (_1);
CREATE INDEX E_g___2 ON E_g (_2);
CREATE INDEX E_g___3 ON E_g (_3);
\echo [INFO] Create indexes (End)
--\echo [INFO] v1: :'v1'
--\COPY n_g (_0, _1) FROM /home/ubuntu/src/graph-trans/experiment/dataset/targets/SYN-10000-1000/node.csv WITH DELIMITER ',' CSV HEADER;
--\COPY n_g (_0, _1) FROM :'v1' WITH DELIMITER ',' CSV HEADER;
--COPY n_g (_0, _1) FROM :'v1' DELIMITER ',' CSV HEADER;
--COPY e_g (_0, _1, _2, _3) FROM :'v2' DELIMITER ',' CSV HEADER;



