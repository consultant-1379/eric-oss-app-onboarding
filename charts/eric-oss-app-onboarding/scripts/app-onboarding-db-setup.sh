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

#! /bin/bash

function command_wrapper {
    log_message=''
    timestamp=`date +%G-%m-%eT%T.%3N`
    output_log=$($@ 2>&1 )
    log_message+="{\"timestamp\":\"$timestamp\","
    log_message+="\"version\":\"1.0.0\","
    log_message+="\"message\":\"$output_log\","
    log_message+="\"logger\":\"bash_logger\","
    log_message+="\"thread\":\"init_image_entrypoint_execution command: $@\","
    log_message+="\"path\":\"/app-onboarding-db-setup.sh\","
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
CREATE DATABASE $APPONBOARDINGPGDATABASE;
CREATE ROLE service_user;
EOF
    #===
   if [ $? -eq 0 ];
     then
       command_wrapper echo "App-mgr db configured for app-onboarding db data";
     else
       command_wrapper echo "Error configuring app-mgr db for app-onboarding db data";
       exit 1;
   fi
}
#===FOR UPGRADE TRANSITION PHASE FROM TLS OFF TO TLS ON===
pg_isready >/dev/null
if [ $? -eq 2 ]; then
  unset PGSSLMODE PGSSLCERT PGSSLKEY PGSSLROOTCERT;
fi
#==========================================================
if psql -lqt | cut -d \| -f 1 | grep -qw $APPONBOARDINGPGDATABASE; then
    command_wrapper echo "App-onboarding db exists in app-mgr db, no action required";
else
    init_sql
    command_wrapper echo "The app-mgr db is ready for app-onboarding db data"
fi