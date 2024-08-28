--------------------------------------------------------------------------------------------------------------------------------------------------------------
 -- COPYRIGHT Ericsson 2021
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

    CREATE TABLE application(
        id BIGSERIAL PRIMARY KEY,
        name VARCHAR(255),
        username VARCHAR(255),
        version VARCHAR(255),
        "size" VARCHAR(255),
        vendor VARCHAR(255),
        onboarded_date TIMESTAMP DEFAULT NOW(),
        type VARCHAR(255),
        descriptor_info VARCHAR(255),
        status SMALLINT
    );

    CREATE TABLE artifact(
        id BIGSERIAL PRIMARY KEY,
        location VARCHAR(255),
        name VARCHAR(255),
        type VARCHAR(10),
        version VARCHAR(255),
        status SMALLINT,
        application_id bigint,
        CONSTRAINT fk_application FOREIGN KEY (application_id) REFERENCES application(id)
    );

COMMIT;
