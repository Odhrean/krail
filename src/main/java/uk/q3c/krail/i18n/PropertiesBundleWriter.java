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
import org.apache.commons.io.FileUtils;
import uk.q3c.krail.core.user.opt.Option;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Created by David Sowerby on 26/11/14.
 */
public class PropertiesBundleWriter<E extends Enum<E>> extends BundleWriterBase<E> {


    @Inject
    protected PropertiesBundleWriter(Option option) {
        super(option);
    }


    /**
     * @throws IOException
     */
    @Override
    public void write(Locale locale, Optional<String> bundleName) throws IOException {
        Properties properties = new Properties();
        EnumMap<E, String> entryMap = getBundle().getMap();

        //copy to SortedMap so that output is sorted by key
        SortedMap<String, String> sortedMap = new TreeMap<>();
        entryMap.forEach((k, v) -> {
            sortedMap.put(k.name(), v);
        });

        for (Map.Entry<String, String> entry : sortedMap.entrySet()) {
            properties.put(entry.getKey(), entry.getValue());
        }

        String bundleNameWithLocale = bundleNameWithLocale(locale, bundleName);
        File targetDir = getOptionWritePath();
        if (!targetDir.exists()) {
            FileUtils.forceMkdir(targetDir);
        }
        FileOutputStream fos = new FileOutputStream(new File(targetDir, bundleNameWithLocale + ".properties"));

        properties.store(fos, "created by PropertiesBundleWriter");
    }

    @Override
    public void optionValueChanged(Property.ValueChangeEvent event) {
        //do nothing options only used on demand
    }
}

