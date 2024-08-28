#! /bin/bash

#
# COPYRIGHT Ericsson 2023
#
#
#
# The copyright to the computer program(s) herein is the property of
#
# Ericsson Inc. The programs may be used and/or copied only with written
#
# permission from Ericsson Inc. or in accordance with the terms and
#
# conditions stipulated in the agreement/contract under which the
#
# program(s) have been supplied.
#

function command_wrapper {
    log_message=''
    timestamp=`date +%G-%m-%eT%T.%3N`
    output_log=$($@ 2>&1 )
    log_message+="{\"timestamp\":\"$timestamp\","
    log_message+="\"version\":\"1.0.0\","
    log_message+="\"message\":\"$output_log\","
    log_message+="\"logger\":\"bash_logger\","
    log_message+="\"thread\":\"init app onboarding db command: $@\","
    log_message+="\"path\":\"/init-db.sh\","
    log_message+="\"service_id\":\"eric-oss-app-onboarding\","
    log_message+="\"severity\":\"info\"}"

    echo $log_message
}

function init_sql {
    until
     pg_isready; do
       command_wrapper echo "Database instance $PGHOST is not ready. Waiting ..."
       sleep 3
    done

    #===DB-SQL-SCRIPT======
    cat << EOF | psql
SELECT 'CREATE DATABASE "$APPONBOARDINGPGDATABASE"'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$APPONBOARDINGPGDATABASE')\gexec

DO \$\$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE  rolname = '$PGUSER') THEN

      CREATE ROLE "$PGUSER" LOGIN;
      GRANT ALL PRIVILEGES ON DATABASE "$APPONBOARDINGPGDATABASE" TO "$PGUSER";
   END IF;
END
\$\$;
EOF

    cat << EOF | psql -d $APPONBOARDINGPGDATABASE;

CREATE SCHEMA IF NOT EXISTS acm_schema;
DO \$\$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE  rolname = '$ONBOARDING_JOBS_DB_USER') THEN

      CREATE ROLE "$ONBOARDING_JOBS_DB_USER" LOGIN;
   END IF;
END
\$\$;

GRANT ALL PRIVILEGES ON SCHEMA public TO $ONBOARDING_JOBS_DB_USER WITH GRANT OPTION;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO $ONBOARDING_JOBS_DB_USER WITH GRANT OPTION;
GRANT ALL PRIVILEGES ON SCHEMA acm_schema TO $ONBOARDING_JOBS_DB_USER WITH GRANT OPTION;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA acm_schema TO $ONBOARDING_JOBS_DB_USER WITH GRANT OPTION;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public to $ONBOARDING_JOBS_DB_USER WITH GRANT OPTION;
ALTER ROLE $ONBOARDING_JOBS_DB_USER IN DATABASE $APPONBOARDINGPGDATABASE SET search_path to public, acm_schema;
ALTER ROLE $ONBOARDING_JOBS_DB_USER WITH CREATEROLE;
DO \$\$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE  rolname = 'service_user') THEN

      CREATE ROLE "service_user";
   END IF;
END
\$\$;
GRANT SELECT ON ALL TABLES IN SCHEMA pg_catalog TO service_user;

ALTER TABLE IF EXISTS acm_schema.onboarding_job OWNER TO $ONBOARDING_JOBS_DB_USER;
ALTER TABLE IF EXISTS acm_schema.onboarding_event OWNER TO $ONBOARDING_JOBS_DB_USER;

ALTER TABLE IF EXISTS application OWNER TO $ONBOARDING_JOBS_DB_USER;
ALTER TABLE IF EXISTS application_event OWNER TO $ONBOARDING_JOBS_DB_USER;
ALTER TABLE IF EXISTS artifact OWNER TO $ONBOARDING_JOBS_DB_USER;
ALTER TABLE IF EXISTS artifact_event OWNER TO $ONBOARDING_JOBS_DB_USER;
ALTER TABLE IF EXISTS permission OWNER TO $ONBOARDING_JOBS_DB_USER;
ALTER TABLE IF EXISTS role OWNER TO $ONBOARDING_JOBS_DB_USER;
ALTER TABLE IF EXISTS flyway_schema_history OWNER TO $ONBOARDING_JOBS_DB_USER;

EOF
    #===

   if [ $? -eq 0 ];
     then
       command_wrapper echo "Create DB sql-script loaded into the $PGDATABASE DB";
     else
       command_wrapper echo "PG login failed";
       exit 1;
   fi
}

init_sql

command_wrapper echo "The Init container completed preparation"
