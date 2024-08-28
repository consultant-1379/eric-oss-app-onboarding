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

import com.ericsson.oss.ae.apponboarding.common.consts.Consts;
import com.ericsson.oss.ae.apponboarding.v1.model.entity.Artifact;
import com.ericsson.oss.ae.apponboarding.v1.model.exception.AppOnboardingException;
import com.ericsson.oss.ae.apponboarding.v1.model.exception.AppOnboardingServerException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;


@Component
public class ImageArtifactRegistry implements ArtifactRegistry {

    private static final String DOCKER_CONTENT_DIGEST = "docker-content-digest";
    @Value("${deployment.containerReg.URL}") private String path;
    @Value("${deployment.containerReg.user}") private String username;
    @Value("${deployment.containerReg.password}") private String password;

    @Autowired
    @Qualifier("imageRegistry")
    private RestTemplate restTemplate;

    private final Logger logger = LoggerFactory.getLogger(ImageArtifactRegistry.class);

    @PostConstruct
    private void init() {
        restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(username, password));
    }

    @Override
    public String download(Long id) throws IOException {
        throw new AppOnboardingException("Images can not be downloaded");
    }

    @Override
    @Retryable(retryFor = {AppOnboardingServerException.class}, maxAttemptsExpression = "${artifactRegistryRetry.maxAttempts}", backoff = @Backoff(delayExpression = "${artifactRegistryRetry.delay}"))
    public void delete(Artifact artifact) {
        String manifest = getManifest(artifact);
        logger.info("delete() manifest for image: {} retrieved from container registry", artifact.getLocation());
        logger.info("delete() image artifact from container registry, {}", path);
        deleteImage(artifact, manifest);
        logger.info("delete() image artifact: {} deleted from container registry", artifact.getLocation());
    }

    private String getManifest(final Artifact artifact) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/vnd.docker.distribution.manifest.v2+json");
            HttpEntity entity = new HttpEntity(headers);
            String imgLocation = path + (artifact.getLocation().startsWith(Consts.FORWARD_SLASH) ? artifact.getLocation() :
                (Consts.FORWARD_SLASH + artifact.getLocation()));
            final ResponseEntity<String> responseEntity = restTemplate.exchange(imgLocation, HttpMethod.GET, entity, String.class);
            return getHeaderValueForDockerContentDigestValue(responseEntity);
        } catch (final HttpClientErrorException httpClientError) {
            logger.error("getManifest() Http Client exception occurred when attempting to get image manifest", httpClientError);
            if (httpClientError.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                throw new AppOnboardingException("Unable to get manifest, as manifest was not found", HttpStatus.NOT_FOUND);
            }
            handleContainerRegistry4xxExceptions(httpClientError);
        } catch (final HttpServerErrorException httpServerError) {
            logger.error("getManifest() Http Server exception occurred when attempting to get image manifest", httpServerError);
            throw new AppOnboardingServerException(httpServerError.getLocalizedMessage(), httpServerError.getStatusCode());
        }
        return "";
    }

    /**
     * Retrieves the key for the Docker-Content-Digest in the headers of the supplied {@code ResponseEntity}. To handle the scenario of service mesh
     * being enabled or not, this method will retrieve the supplied key irrespective of the usage of camel-case or lowercase keys.
     *
     * @param responseEntity
     *     the response entity whose header values will be checked for the {@code DOCKER_CONTENT_DIGEST} key
     * @return the value provided for the {@code DOCKER_CONTENT_DIGEST} key in the headers of this entity
     */
    private String getHeaderValueForDockerContentDigestValue(final ResponseEntity<String> responseEntity) {
        Map<String, String> caseInsensitiveMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitiveMap.putAll(responseEntity.getHeaders().toSingleValueMap());
        if (!caseInsensitiveMap.containsKey(DOCKER_CONTENT_DIGEST)) {
            logger.error("getHeaderValueForDockerContentDigestValue() For getManifest function headers are: {}", responseEntity.getHeaders());
            throw new AppOnboardingException("Unable to get header value for DOCKER_CONTENT_DIGEST", HttpStatus.NOT_ACCEPTABLE);
        }
        return caseInsensitiveMap.get(DOCKER_CONTENT_DIGEST);
    }

    private void deleteImage(Artifact artifact, String manifestDigest) {
        try {
            restTemplate.exchange(path + getSubString(artifact.getLocation()) + manifestDigest, HttpMethod.DELETE, null, String.class);
        } catch (HttpClientErrorException e) {
            logger.error("deleteImage() Client exception occurred when attempting to delete image", e);
            if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                throw new AppOnboardingException("Unable to delete image, as image was not found", HttpStatus.NOT_FOUND);
            }
            handleContainerRegistry4xxExceptions(e);
        } catch (HttpServerErrorException e) {
            logger.error("deleteImage() Server exception occurred when attempting to get delete image", e);
            throw new AppOnboardingServerException(e.getLocalizedMessage(), e.getStatusCode());
        }
    }

    private String getSubString(String mainString) {
        String manifests = "manifests/";
        String result = mainString.split(manifests)[0];
        return result + manifests;
    }

    private void handleContainerRegistry4xxExceptions(HttpClientErrorException clientException) {
        if (clientException.getStatusCode() instanceof HttpStatus) {
            final HttpStatus status = (HttpStatus) clientException.getStatusCode();
            switch (status) {
                case UNAUTHORIZED:
                    throw new AppOnboardingException("Unauthorized to communicate with container registry", HttpStatus.UNAUTHORIZED);
                case BAD_REQUEST:
                    throw new AppOnboardingException("Unable to perform request due to invalid image information", HttpStatus.BAD_REQUEST);
                case FORBIDDEN:
                    throw new AppOnboardingException(
                        "Forbidden to communicate with container registry, credentials don't have the required permissions", HttpStatus.FORBIDDEN);
                case TOO_MANY_REQUESTS:
                    throw new AppOnboardingException("Unable to communicate with container registry, as too many requests have been made", HttpStatus.TOO_MANY_REQUESTS);
                case METHOD_NOT_ALLOWED:
                    throw new AppOnboardingException("Unable to communicate with container registry, as this operation has been disabled", HttpStatus.METHOD_NOT_ALLOWED);
                default:
                    throw new AppOnboardingException(clientException.getLocalizedMessage(), clientException.getStatusCode());
            }
        } else {
            throw new AppOnboardingException(clientException.getLocalizedMessage(), clientException.getStatusCode());
        }
    }
}
