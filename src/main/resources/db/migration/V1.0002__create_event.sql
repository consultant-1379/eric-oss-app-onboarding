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

    CREATE TABLE application_event(
        id BIGSERIAL PRIMARY KEY,
        text VARCHAR(255),
        type VARCHAR(255),
        date TIMESTAMP DEFAULT NOW(),
        application_id bigint,
        CONSTRAINT fk_application FOREIGN KEY (application_id) REFERENCES application(id)

    );

    CREATE TABLE artifact_event(
        id BIGSERIAL PRIMARY KEY,
        text VARCHAR(255),
        type VARCHAR(255),
        date TIMESTAMP DEFAULT NOW(),
        artifact_id bigint,
        CONSTRAINT fk_artifact FOREIGN KEY (artifact_id) REFERENCES artifact(id)

    );


COMMIT;
