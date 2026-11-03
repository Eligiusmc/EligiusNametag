package com.makrozai.eligiusnametag;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * First-install default language must be "en" — the same fallback
 * YamlConfigAdapter uses when the config key is missing entirely.
 */
public class DefaultLanguageTest {

    private static final Pattern LANGUAGE_LINE = Pattern.compile("(?m)^language:\\s*\"en\"\\s*$");

    @Test
    public void testPackagedConfigDefaultsToEnglish() throws Exception {
        InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml");
        assertNotNull(in, "packaged config.yml must be on the test classpath");

        String yaml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(LANGUAGE_LINE.matcher(yaml).find(),
                "packaged config.yml must default to language \"en\"");
    }
}
