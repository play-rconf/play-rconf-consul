/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 The Play Remote Configuration Authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.playrconf.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import io.playrconf.sdk.AbstractProvider;
import io.playrconf.sdk.FileCfgObject;
import io.playrconf.sdk.KeyValueCfgObject;
import io.playrconf.sdk.exception.ProviderException;
import io.playrconf.sdk.exception.RemoteConfException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * Retrieves configuration from HashiCorp Consul.
 *
 * @author Thibault Meyer
 * @since 18.04.01
 */
public class ConsulProvider extends AbstractProvider {

    /**
     * Contains the provider version.
     */
    private static String providerVersion;

    @Override
    public String getName() {
        return "HashiCorp Consul";
    }

    @Override
    public String getVersion() {
        if (ConsulProvider.providerVersion == null) {
            synchronized (ConsulProvider.class) {
                final Properties properties = new Properties();
                final InputStream is = ConsulProvider.class.getClassLoader()
                    .getResourceAsStream("playrconf-consul.properties");
                try {
                    properties.load(is);
                    ConsulProvider.providerVersion = properties.getProperty("playrconf.consul.version", "unknown");
                    properties.clear();
                    is.close();
                } catch (final IOException ignore) {
                }
            }
        }
        return ConsulProvider.providerVersion;
    }

    @Override
    public String getConfigurationObjectName() {
        return "consul";
    }

    @Override
    public void loadData(final Config config,
                         final Consumer<KeyValueCfgObject> kvObjConsumer,
                         final Consumer<FileCfgObject> fileObjConsumer) throws ConfigException, RemoteConfException {
        final String consulAuthToken = config.hasPath("auth-token") ? config.getString("auth-token") : null;
        String consulEndpoint = config.getString("endpoint");
        String consulPrefix = config.getString("prefix");

        // Quick check of vital configuration keys
        if (consulEndpoint == null) {
            throw new ConfigException.BadValue(config.origin(), "endpoint", "Could not be null");
        } else if (!consulEndpoint.startsWith("http")) {
            throw new ConfigException.BadValue(config.origin(), "endpoint", "Must start with http:// or https://");
        } else if (consulPrefix == null) {
            throw new ConfigException.BadValue(config.origin(), "prefix", "Could not be null");
        }

        // Normalize configuration values
        if (!consulEndpoint.endsWith("/")) {
            consulEndpoint += "/";
        }
        if (consulPrefix.endsWith("/")) {
            consulPrefix = consulPrefix.substring(0, consulPrefix.length() - 1);
        }
        if (consulPrefix.startsWith("/")) {
            consulPrefix = consulPrefix.substring(1, consulPrefix.length());
        }

        // Get data from Consul
        InputStream is = null;
        try {
            final URL consulUrl = new URL(
                String.format(
                    "%sv1/kv/%s/?recurse&token=%s",
                    consulEndpoint,
                    consulPrefix,
                    consulAuthToken
                )
            );
            final HttpURLConnection conn = (HttpURLConnection) consulUrl.openConnection();
            conn.setConnectTimeout(1500);
            if (conn.getResponseCode() / 100 == 2) {
                is = conn.getInputStream();
                final ObjectMapper mapper = new ObjectMapper();
                final JsonNode jsonDocument = mapper.readTree(is);
                final Base64.Decoder decoder = Base64.getDecoder();
                for (final JsonNode entry : jsonDocument) {
                    if (entry.hasNonNull("Value")) {
                        final String cfgKey = entry.get("Key")
                            .asText()
                            .replace(consulPrefix.isEmpty() ? "" : consulPrefix + "/", "")
                            .replace("/", ".");
                        final String cfgValue = new String(
                            decoder.decode(
                                entry.get("Value").asText()
                            )
                        );

                        // Check if current configuration object is a file
                        if (isFile(cfgValue)) {
                            fileObjConsumer.accept(
                                new FileCfgObject(cfgKey, cfgValue)
                            );
                        } else {

                            // Standard configuration value
                            kvObjConsumer.accept(
                                new KeyValueCfgObject(cfgKey, cfgValue)
                            );
                        }
                    }
                }
            } else {
                throw new ProviderException("Return non 200 status: " + conn.getResponseCode());
            }
        } catch (final MalformedURLException ex) {
            throw new ConfigException.BadValue("endpoint", ex.getMessage());
        } catch (final IOException ex) {
            throw new ProviderException("Can't connect to the provider", ex);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (final IOException ignore) {
                }
            }
        }
    }
}
