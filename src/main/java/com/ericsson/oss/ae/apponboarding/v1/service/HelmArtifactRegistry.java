/*******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/

package com.ericsson.oss.ae.apponboarding.v1.service;

import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.API;
import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.CHARTS;
import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.FORWARD_SLASH;
import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.UNDERSCORE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.ericsson.oss.ae.apponboarding.common.consts.utils.StringUtils;
import com.ericsson.oss.ae.apponboarding.v1.dao.ArtifactRepository;
import com.ericsson.oss.ae.apponboarding.v1.model.entity.Artifact;
import com.ericsson.oss.ae.apponboarding.v1.model.exception.AppOnboardingException;
import com.ericsson.oss.ae.apponboarding.v1.model.exception.AppOnboardingServerException;
import com.ericsson.oss.ae.apponboarding.v1.model.exception.NoSuchElementFoundException;
import jakarta.annotation.PostConstruct;

@Component
public class HelmArtifactRegistry implements ArtifactRegistry {

    private static final Logger logger = LoggerFactory.getLogger(HelmArtifactRegistry.class);

    @Value("${HELM_REG_URL}") private String helmUrl;
    @Value("${HELM_REG_USER}") private String username;
    @Value("${HELM_REG_PASSWORD}") private String password;
    @Value("${TMP_DIR}") private String tempDir;

    @Autowired
    @Qualifier("helmRegistry")
    private RestTemplate restTemplate;
    @Autowired private ArtifactRepository artifactRepository;

    @PostConstruct
    private void init() {
        restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(username, password));
    }

    @Override
    public String download(final Long id) throws IOException {
        final Artifact artifact = artifactRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementFoundException(1500, StringUtils.appendWithSpace("Artifact not found, ID:", id)));
        final String fileName = artifact.getName() + "-" + artifact.getVersion() + ".tgz";
        final String path = StringUtils.append(helmUrl, FORWARD_SLASH, artifact.getApplication().getName(), UNDERSCORE,
            artifact.getApplication().getVersion(), CHARTS, fileName);
        logger.debug("download() Downloading from helm registry, {}", path);
        final ResponseEntity<byte[]> downloaded = restTemplate.exchange(path, HttpMethod.GET, null, byte[].class);
        final UUID uuid = UUID.randomUUID();
        final Path tempPath = Paths.get(tempDir, StringUtils.append(uuid.toString(), "-", fileName));
        Files.write(tempPath, downloaded.getBody());
        return tempPath.toAbsolutePath().toString();
    }

    @Override
    @Retryable(retryFor = {AppOnboardingServerException.class}, maxAttemptsExpression = "${artifactRegistryRetry.maxAttempts}", backoff = @Backoff(delayExpression = "${artifactRegistryRetry.delay}"))
    public void delete(Artifact artifact) {
        final String path = StringUtils.append(helmUrl, API, artifact.getApplication().getName(), UNDERSCORE, artifact.getApplication().getVersion(),
            CHARTS, artifact.getName(), FORWARD_SLASH, artifact.getVersion());
        try {
            logger.info("delete() helm chart from helm registry, {}", path);
            restTemplate.delete(path);
            logger.info("delete() helm chart deleted from helm registry");
        } catch (HttpClientErrorException ce) {
            logger.error("delete() Client Exception occurred while attempting to delete Helm Chart", ce);
            handleClientExceptions(ce);
        } catch (HttpServerErrorException se) {
            logger.error("delete() Server Exception occurred while attempting to delete Helm Chart", se);
            throw new AppOnboardingServerException(se.getLocalizedMessage(), se.getStatusCode());
        }
    }

    private void handleClientExceptions(HttpClientErrorException clientException) {
        if (clientException.getStatusCode() instanceof HttpStatus) {
            final HttpStatus status = (HttpStatus) clientException.getStatusCode();
            switch (status) {
                case NOT_FOUND:
                    throw new AppOnboardingException("Helm chart not found in helm registry", HttpStatus.NOT_FOUND);
                case UNAUTHORIZED:
                    throw new AppOnboardingException("Unauthorized to communicate with helm registry", HttpStatus.UNAUTHORIZED);
                case BAD_REQUEST:
                    throw new AppOnboardingException("Unable to perform request due to invalid helm chart information", HttpStatus.BAD_REQUEST);
                case FORBIDDEN:
                    throw new AppOnboardingException("Forbidden to communicate with helm registry, credentials don't have the required permissions",
                        HttpStatus.FORBIDDEN);
                case TOO_MANY_REQUESTS:
                    throw new AppOnboardingException("Unable to communicate with helm registry, as too many requests have been made", HttpStatus.TOO_MANY_REQUESTS);
                case METHOD_NOT_ALLOWED:
                    throw new AppOnboardingException("Unable to communicate with helm registry, as this operation has been disabled", HttpStatus.METHOD_NOT_ALLOWED);
                default:
                    throw new AppOnboardingException(clientException.getLocalizedMessage(), clientException.getStatusCode());
            }
        } else {
            throw new AppOnboardingException(clientException.getLocalizedMessage(), clientException.getStatusCode());
        }
    }
}
