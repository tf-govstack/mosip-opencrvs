CREATE DATABASE mosip_opencrvs
	ENCODING = 'UTF8'
	LC_COLLATE = 'en_US.UTF-8'
	LC_CTYPE = 'en_US.UTF-8'
	TABLESPACE = pg_default
	OWNER = postgres
	TEMPLATE  = template0;

COMMENT ON DATABASE mosip_opencrvs IS 'Metadata related to Opencrvs birth and death transactions is stored here';

\c mosip_opencrvs postgres

DROP SCHEMA IF EXISTS opencrvs CASCADE;
CREATE SCHEMA opencrvs;
ALTER SCHEMA opencrvs OWNER TO postgres;
ALTER DATABASE mosip_opencrvs SET search_path TO opencrvs,pg_catalog,public;
