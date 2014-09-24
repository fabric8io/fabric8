/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.fabric8.maven.util.decrypt;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sonatype.plexus.components.cipher.PBECipher;
import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

/**
 * Inspired by DefaultPlexusCipher from Sonatype
 *
 * @author <a href="kpiwko@redhat.com">Karel Piwko</a>
 * @author Oleg Gusakov
 */
public class MavenPlexusCipher implements PlexusCipher {

    private static final Pattern ENCRYPTED_STRING_PATTERN = Pattern.compile(".*?[^\\\\]?\\{(.*?[^\\\\])\\}.*");

    private final PBECipher cipher;

    public MavenPlexusCipher() throws IllegalStateException {
        try {
            cipher = new PBECipher();
        } catch (PlexusCipherException e) {
            throw new IllegalStateException("Unable to instantiate Cipher to decrypt Maven passwords");
        }
    }

    @Override
    public String encrypt(final String str, final String passPhrase) throws PlexusCipherException {
        if (str == null || str.length() < 1) {
            return str;
        }

        return cipher.encrypt64(str, passPhrase);
    }

    @Override
    public String encryptAndDecorate(final String str, final String passPhrase) throws PlexusCipherException {
        return decorate(encrypt(str, passPhrase));
    }

    @Override
    public String decrypt(final String str, final String passPhrase) throws PlexusCipherException {
        if (str == null || str.length() < 1) {
            return str;
        }

        return cipher.decrypt64(str, passPhrase);
    }

    @Override
    public String decryptDecorated(final String str, final String passPhrase) throws PlexusCipherException {
        if (str == null || str.length() < 1) {
            return str;
        }

        if (isEncryptedString(str)) {
            return decrypt(unDecorate(str), passPhrase);
        }

        return decrypt(str, passPhrase);
    }

    @Override
    public boolean isEncryptedString(final String str) {
        if (str == null || str.length() < 1) {
            return false;
        }

        Matcher matcher = ENCRYPTED_STRING_PATTERN.matcher(str);

        return matcher.matches() || matcher.find();
    }

    @Override
    public String unDecorate(final String str) throws PlexusCipherException {
        Matcher matcher = ENCRYPTED_STRING_PATTERN.matcher(str);

        if (matcher.matches() || matcher.find()) {
            return matcher.group(1);
        }
        else {
            throw new IllegalStateException("Unable to undecorate decrypted string " + str);
        }
    }

    @Override
    public String decorate(final String str) {
        return ENCRYPTED_STRING_DECORATION_START + (str == null ? "" : str) + ENCRYPTED_STRING_DECORATION_STOP;
    }

}
