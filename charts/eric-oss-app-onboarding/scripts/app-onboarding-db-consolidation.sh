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
    log_message+="\"thread\":\"db consolidation db data transfer command: $@\","
    log_message+="\"path\":\"/scripts/app-onboarding-db-consolidation.sh\","
    log_message+="\"service_id\":\"eric-oss-app-onboarding\","
    log_message+="\"severity\":\"info\"}"
    echo $log_message
}

function db_consolidation {
    pg_dump -h $APPONBOARDINGPGHOST -U $PGUSER $APPONBOARDINGPGDATABASE | psql -h $PGHOST -U $PGUSER $APPONBOARDINGPGDATABASE
}
#===FOR UPGRADE TRANSITION PHASE FROM TLS OFF TO TLS ON===
pg_isready >/dev/null
if [ $? -eq 2 ]; then
  unset PGSSLMODE PGSSLCERT PGSSLKEY PGSSLROOTCERT;
fi
#==========================================================
if psql -lqt | cut -d \| -f 1 | grep -qw $APPONBOARDINGPGDATABASE; then
    if psql -h $APPONBOARDINGPGHOST -U $PGUSER -p $PGPORT -lqt | cut -d \| -f 1 | grep -qw $APPONBOARDINGPGDATABASE; then
        db_consolidation
        command_wrapper echo "The app-onboarding db data was successfully copied to the app-mgr db"
    else
        command_wrapper echo "app-onboarding internal db does not exist, no db consolidation required";
    fi
else
    command_wrapper echo "The app-onboarding db does not exist in the app-mgr db"
fi
