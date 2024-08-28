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

    ALTER TABLE acm_schema.onboarding_event DROP CONSTRAINT fk_onboarding_job;

    ALTER TABLE acm_schema.onboarding_job ALTER COLUMN id TYPE uuid USING id::uuid::uuid;

    ALTER TABLE acm_schema.onboarding_event ALTER COLUMN onboarding_job_id TYPE uuid USING onboarding_job_id::uuid::uuid;

    ALTER TABLE acm_schema.onboarding_event ALTER COLUMN id TYPE uuid USING id::uuid::uuid;

    ALTER TABLE acm_schema.onboarding_event ADD CONSTRAINT fk_onboarding_job FOREIGN KEY (onboarding_job_id) REFERENCES acm_schema.onboarding_job(id);

COMMIT;
