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

    CREATE TABLE acm_schema.onboarding_job(
        id VARCHAR(36) PRIMARY KEY,
        app_id VARCHAR(255),
        package_version VARCHAR(255),
        vendor VARCHAR(255),
        type VARCHAR(255),
        file_name VARCHAR(255),
        package_size VARCHAR(255),
        status VARCHAR(50),
        username VARCHAR(255),
        start_timestamp TIMESTAMP DEFAULT NOW(),
        end_timestamp TIMESTAMP
    );

    CREATE TABLE acm_schema.onboarding_event(
        id VARCHAR(36) PRIMARY KEY,
        type VARCHAR(50),
        title VARCHAR(255),
        detail VARCHAR(255),
        timestamp TIMESTAMP DEFAULT NOW(),
        onboarding_job_id VARCHAR(36),
        CONSTRAINT fk_onboarding_job FOREIGN KEY (onboarding_job_id) REFERENCES acm_schema.onboarding_job(id)
    );

COMMIT;
