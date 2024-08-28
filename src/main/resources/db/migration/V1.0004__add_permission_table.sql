--------------------------------------------------------------------------------------------------------------------------------------------------------------
 -- COPYRIGHT Ericsson 2022
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

    CREATE TABLE permission(
        id BIGSERIAL PRIMARY KEY,
        resource VARCHAR(255),
        scope VARCHAR(255),
        application_id bigint,
        CONSTRAINT fk_application FOREIGN KEY (application_id) REFERENCES application(id)
    );

COMMIT;