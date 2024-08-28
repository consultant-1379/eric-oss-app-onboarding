--------------------------------------------------------------------------------------------------------------------------------------------------------------
 -- COPYRIGHT Ericsson 2023
 --
 --
 --
 -- The copyright to the computer program(s) herein is the property of
 --
 -- Ericsson Inc. The programs may be used and/or copied only with written
 --
 -- permission from Ericsson Inc. or in accordance with the terms and
 --
 -- conditions stipulated in the agreement/contract under which the
 --
 -- program(s) have been supplied.
------------------------------------------------------------------------------------------------------------------------------------------------------------/

BEGIN TRANSACTION;

    UPDATE acm_schema.onboarding_job SET start_timestamp=now() WHERE start_timestamp is NULL;

    ALTER TABLE acm_schema.onboarding_job ALTER COLUMN start_timestamp SET NOT NULL;

    UPDATE acm_schema.onboarding_event SET "timestamp"=now() WHERE "timestamp" is NULL;

    ALTER TABLE acm_schema.onboarding_event ALTER COLUMN "timestamp" SET NOT NULL;

COMMIT;
