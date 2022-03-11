-- object: opencrvs.birth_transactions | type: TABLE --
-- DROP TABLE IF EXISTS opencrvs.birth_transactions CASCADE;
CREATE TABLE opencrvs.birth_transactions(
	txn_id character varying(64) NOT NULL,
	rid character varying(64),
	status character varying(2048),
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	is_deleted boolean DEFAULT FALSE,
	del_dtimes timestamp,
	CONSTRAINT pk_birth_txn_id PRIMARY KEY (txn_id)

);
-- ddl-end --
COMMENT ON TABLE opencrvs.birth_transactions IS 'Opencrvs Birth Transactions : To track status,rid of each birth record received from opencrvs';
-- ddl-end --
COMMENT ON COLUMN opencrvs.birth_transactions.txn_id IS 'Transaction Id: Unique id sent from opencrvs for this transaction';
-- ddl-end --
COMMENT ON COLUMN opencrvs.birth_transactions.rid IS 'Registration Id: generated registration id';
-- ddl-end --
COMMENT ON COLUMN opencrvs.birth_transactions.status IS 'Status: status of this transaction.';
-- ddl-end --
