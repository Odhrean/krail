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
import com.mycila.testing.junit.MycilaJunitRunner;
import com.mycila.testing.plugin.guice.GuiceContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.q3c.krail.core.eventbus.EventBusModule;
import uk.q3c.krail.core.guice.uiscope.UIScopeModule;
import uk.q3c.krail.core.guice.vsscope.VaadinSessionScopeModule;
import uk.q3c.krail.core.user.opt.Option;
import uk.q3c.krail.testutil.TestI18NModule;
import uk.q3c.krail.testutil.TestOptionModule;

import java.util.Locale;
import java.util.ResourceBundle;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MycilaJunitRunner.class)
@GuiceContext({TestI18NModule.class, TestOptionModule.class, EventBusModule.class, UIScopeModule.class, VaadinSessionScopeModule.class})
public class DefaultPatternSourceTest {


    @Inject
    Option option;

    DefaultPatternSource source;


    @Inject
    PatternCacheLoader patternCacheLoader;


    @Before
    public void setUp() throws Exception {
        //essential to stop pollution from one test to another
        ResourceBundle.clearCache();
        source = new DefaultPatternSource(patternCacheLoader);
    }

    /**
     * PatternSource is not required to check for a supportedLocale
     */
    @Test
    public void retrievePattern() {
        //given

        //when
        String value = source.retrievePattern(TestLabelKey.No, Locale.UK);
        //then
        assertThat(value).isEqualTo("No");

        //when supported locale
        value = source.retrievePattern(TestLabelKey.No, Locale.GERMANY);
        //then
        assertThat(value).isEqualTo("Nein");

        //when not a supported locale, it defaults to standard Java behaviour and uses default translation
        value = source.retrievePattern(TestLabelKey.No, Locale.CHINA);
        //then
        assertThat(value).isEqualTo("No");

        //when not a supported locale, but there is not even a default translation
        value = source.retrievePattern(TestLabelKey.ViewA, Locale.CHINA);
        //then
        assertThat(value).isEqualTo("ViewA");

        //when supported locale but no value for key
        value = source.retrievePattern(TestLabelKey.ViewA, Locale.UK);
        //then
        assertThat(value).isEqualTo("ViewA");
    }

    @Test
    public void clearCache() {
        //given
        String value = source.retrievePattern(TestLabelKey.No, Locale.UK);
        //when

        //then
        assertThat(source.getCache()
                         .size()).isEqualTo(1);

        //when
        source.clearCache();

        //then
        assertThat(source.getCache()
                         .size()).isEqualTo(0);
    }

    @Test
    public void clearCache_Source() {
        //given
        String value = source.retrievePattern(TestLabelKey.No, Locale.UK);
        value = source.retrievePattern(TestLabelKey.No, Locale.GERMANY);
        value = source.retrievePattern(TestLabelKey.Yes, Locale.ITALIAN);
        value = source.retrievePattern(TestLabelKey.Blank, new Locale(""));
        //when
        //Map map=source.getCache().asMap();
        //then
        assertThat(source.getCache()
                         .size()).isEqualTo(4);

        //when
        source.clearCache("class");

        //then
        assertThat(source.getCache()
                         .size()).isEqualTo(1);
    }
}


