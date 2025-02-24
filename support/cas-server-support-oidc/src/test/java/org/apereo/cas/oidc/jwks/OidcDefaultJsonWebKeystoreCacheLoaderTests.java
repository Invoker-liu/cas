package org.apereo.cas.oidc.jwks;

import org.apereo.cas.oidc.AbstractOidcTests;
import org.apereo.cas.oidc.jwks.generator.OidcJsonWebKeystoreGeneratorService;
import org.apereo.cas.util.ResourceUtils;

import lombok.val;
import org.apache.commons.io.FileUtils;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.PublicJsonWebKey;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.TestPropertySource;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * This is {@link OidcDefaultJsonWebKeystoreCacheLoaderTests}.
 *
 * @author Misagh Moayyed
 * @since 5.3.0
 */
@Tag("OIDC")
@TestPropertySource(properties = {
    "cas.authn.oidc.jwks.core.jwks-type=ec",
    "cas.authn.oidc.jwks.core.jwks-key-size=384",
    "cas.authn.oidc.jwks.file-system.jwks-file=file:${#systemProperties['java.io.tmpdir']}/keystore.jwks"
})
public class OidcDefaultJsonWebKeystoreCacheLoaderTests extends AbstractOidcTests {
    @BeforeAll
    public static void setup() throws Exception {
        val file = new File(FileUtils.getTempDirectory(), "keystore.jwks");
        if (file.exists()) {
            FileUtils.delete(file);
        }
    }

    @Test
    public void verifyNoWebKeys() {
        val jwks = new JsonWebKeySet();
        val loader = mock(OidcDefaultJsonWebKeystoreCacheLoader.class);
        when(loader.buildJsonWebKeySet()).thenReturn(Optional.of(jwks));
        when(loader.load(anyString())).thenCallRealMethod();
        assertTrue(loader.load(UUID.randomUUID().toString()).isEmpty());

        jwks.getJsonWebKeys().add(mock(JsonWebKey.class));
        when(loader.getJsonSigningWebKeyFromJwks(any())).thenReturn(null);
        assertTrue(loader.load(UUID.randomUUID().toString()).isEmpty());
    }

    @Test
    public void verifyBadKeyCount() throws Exception {
        val jwks = new JsonWebKeySet();
        val jsonWebKey = mock(PublicJsonWebKey.class);
        jwks.getJsonWebKeys().add(jsonWebKey);

        val loader = mock(OidcDefaultJsonWebKeystoreCacheLoader.class);
        when(loader.buildJsonWebKeySet()).thenCallRealMethod();
        when(loader.load(anyString())).thenCallRealMethod();
        when(loader.generateJwksResource()).thenReturn(new ByteArrayResource("jwks".getBytes(StandardCharsets.UTF_8)));
        when(loader.buildJsonWebKeySet(any(Resource.class))).thenReturn(jwks);
        assertTrue(loader.load(UUID.randomUUID().toString()).isEmpty());

        when(jsonWebKey.getAlgorithm()).thenReturn("some-alg");
        when(loader.getJsonSigningWebKeyFromJwks(any())).thenReturn(jsonWebKey);
        assertTrue(loader.load(UUID.randomUUID().toString()).isEmpty());
    }

    @Test
    public void verifyOperation() {
        val publicJsonWebKey1 = oidcDefaultJsonWebKeystoreCache.get("https://sso.example.org/cas/oidc");
        assertNotNull(publicJsonWebKey1);
        assertTrue(publicJsonWebKey1.isPresent());

        val publicJsonWebKey2 = oidcDefaultJsonWebKeystoreCache.get("https://sso.example.org/cas/oidc");
        assertNotNull(publicJsonWebKey2);
        assertTrue(publicJsonWebKey2.isPresent());
    }

    @Test
    public void verifyNullResource() throws Exception {
        val gen = mock(OidcJsonWebKeystoreGeneratorService.class);
        when(gen.generate()).thenReturn(null);
        val loader = new OidcDefaultJsonWebKeystoreCacheLoader(gen);
        assertTrue(loader.load("https://cas.example.org").isEmpty());
    }

    @Test
    public void verifyEmptyFile() throws Exception {
        val gen = mock(OidcJsonWebKeystoreGeneratorService.class);
        when(gen.generate()).thenReturn(ResourceUtils.EMPTY_RESOURCE);
        val loader = new OidcDefaultJsonWebKeystoreCacheLoader(gen);
        assertTrue(loader.load("https://cas.example.org").isEmpty());

        val file = File.createTempFile("keys", ".json");
        FileUtils.writeStringToFile(file, new JsonWebKeySet(List.of()).toJson(), StandardCharsets.UTF_8);
        when(gen.generate()).thenReturn(new FileSystemResource(file));
        assertTrue(loader.load("https://cas.example.org").isEmpty());
    }

    @Test
    public void verifyBadKeys() throws Exception {
        val gen = mock(OidcJsonWebKeystoreGeneratorService.class);
        val keys = "{ \"keys\": [ {\"kty\":\"EC\","
                   + "\"x\":\"sPlKwAgSxxOE\",\"y\":\"6AyisnUKM"
                   + "9H8\",\"crv\":\"P-256\"} ]}";
        when(gen.generate()).thenReturn(new ByteArrayResource(keys.getBytes(StandardCharsets.UTF_8)));
        val loader = new OidcDefaultJsonWebKeystoreCacheLoader(gen);
        assertTrue(loader.load("https://cas.example.org").isEmpty());
    }
}
