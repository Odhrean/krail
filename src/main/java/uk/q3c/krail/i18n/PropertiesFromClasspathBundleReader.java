/*
 * Copyright (c) 2015. David Sowerby
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package uk.q3c.krail.i18n;

import com.google.inject.Inject;
import com.vaadin.data.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.q3c.krail.core.user.opt.Option;
import uk.q3c.krail.core.user.opt.OptionKey;

import javax.annotation.Nonnull;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * This default options for this reader assume that the properties files are on the same class path as the key - for
 * example, with default settings, with a key class of com.example.i18n.LabelKey will expect to find the properties
 * file
 * at com.example.i18n.Labels.properties
 * <p>
 * For a Gradle project that means placing the properties file in the directory src/main/resources/com/example/i18n
 * <p>
 * The default options can be changed by setting the options:
 * <p>
 * {@code getOption().set(false, OptionProperty.USE_KEY_PATH, source);
 * <p>
 * getOption().set("path.to.properties.files",UserProperty.USE_KEY_PATH,source);
 * }
 * Created by David Sowerby on 25/11/14.
 */
public class PropertiesFromClasspathBundleReader extends NativeBundleReaderBase implements BundleReader {
    private static Logger log = LoggerFactory.getLogger(PropertiesFromClasspathBundleReader.class);
    private Properties properties;

    @Inject
    protected PropertiesFromClasspathBundleReader(Option option, PropertiesFromClasspathBundleControl control) {
        super(option, control);
    }


    @Override
    public OptionKey getOptionKeyUseKeyPath() {
        return optionKeyUseKeyPath.qualifiedWith(this.getClass()
                                                     .getSimpleName(), "class");
    }

    @Override
    protected OptionKey getOptionKeyPath() {
        return optionKeyPath.qualifiedWith(this.getClass()
                                               .getSimpleName(), "class");
    }

    private String toResourceName0(String bundleName, String suffix) {
        return bundleName.contains("://") ? null : this.toResourceName(bundleName, suffix);
    }

    public final String toResourceName(String bundleName, String suffix) {
        StringBuilder sb = new StringBuilder(bundleName.length() + 1 + suffix.length());
        sb.append(bundleName.replace('.', '/'))
          .append('.')
          .append(suffix);
        return sb.toString();
    }


    /**
     * Called for sub-classes to write the {@code stubValue} to persistence.  This implementation cannot write back to its source, so will just ignore
     * this call
     *
     * @param cacheKey the key to identify the value
     * @param stubValue the value to write
     *
     */
    @Override
    public void writeStubValue(@Nonnull PatternCacheKey cacheKey, @Nonnull String stubValue) {
        log.debug("This implementation does not support writing values back, call to '{}' ignored.", "writeStubValue");
    }

    /**
     * Only supports String values
     *
     * @param bundle
     * @param key
     *
     * @return
     */
    @Override
    protected String getValue(ResourceBundle bundle, Enum<?> key) {
        PropertyResourceBundle propBundle = (PropertyResourceBundle) bundle;
        Object v = propBundle.handleGetObject(key.name());
        if (v instanceof String) {
            return (String) v;
        } else {
            return null;
        }

    }

    @Override
    public void optionValueChanged(Property.ValueChangeEvent event) {
        //do nothing options only used on demand
    }
}

