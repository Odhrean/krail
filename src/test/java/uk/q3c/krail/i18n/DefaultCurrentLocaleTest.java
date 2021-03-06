/*
 * Copyright (C) 2013 David Sowerby
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.q3c.krail.i18n;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.mycila.testing.junit.MycilaJunitRunner;
import com.mycila.testing.plugin.guice.GuiceContext;
import com.mycila.testing.plugin.guice.ModuleProvider;
import com.vaadin.server.WebBrowser;
import net.engio.mbassy.bus.common.PubSubSupport;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.Listener;
import org.apache.shiro.subject.Subject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.q3c.krail.core.eventbus.BusMessage;
import uk.q3c.krail.core.eventbus.EventBusModule;
import uk.q3c.krail.core.eventbus.SessionBus;
import uk.q3c.krail.core.guice.vsscope.VaadinSessionScopeModule;
import uk.q3c.krail.core.shiro.SubjectProvider;
import uk.q3c.krail.core.ui.BrowserProvider;
import uk.q3c.krail.core.user.opt.Option;
import uk.q3c.krail.core.user.opt.OptionKey;
import uk.q3c.krail.core.user.status.UserStatusBusMessage;
import uk.q3c.krail.core.user.status.UserStatusChangeSource;
import uk.q3c.krail.testutil.TestUIScopeModule;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MycilaJunitRunner.class)
@GuiceContext({EventBusModule.class, TestUIScopeModule.class, VaadinSessionScopeModule.class})
@Listener
public class DefaultCurrentLocaleTest {

    boolean listenerFired = false;

    DefaultCurrentLocale currentLocale;

    @Mock
    Annotation annotation;
    @Mock
    Subject subject;

    @Mock
    SubjectProvider subjectProvider;

    @Mock
    UserStatusChangeSource source;

    @Inject
    @SessionBus
    PubSubSupport<BusMessage> eventBus;

    @Mock
    Option option;

    Locale defaultLocale;
    @Mock
    private WebBrowser browser;
    @Mock
    private BrowserProvider browserProvider;
    private Set<Locale> supportedLocales;


    @Before
    public void setup() {
        Locale.setDefault(Locale.UK);


        when(browserProvider.get()).thenReturn(browser);
        when(subjectProvider.get()).thenReturn(subject);
        listenerFired = false;
        supportedLocales = new HashSet<>();
        supportedLocales.add(Locale.UK);
        supportedLocales.add(Locale.GERMANY);
        supportedLocales.add(Locale.FRANCE);
        defaultLocale = Locale.UK;


    }

    @Test
    public void initialise_user_not_logged_in() {
        //given
        when(browser.getLocale()).thenReturn(Locale.GERMANY);
        when(subject.isAuthenticated()).thenReturn(false);
        //when
        currentLocale = createCurrentLocale();
        currentLocale.readFromEnvironment();
        //then
        assertThat(currentLocale.getLocale()).isEqualTo(Locale.GERMANY);
    }

    private DefaultCurrentLocale createCurrentLocale() {
        return new DefaultCurrentLocale(browserProvider, supportedLocales, defaultLocale, eventBus, subjectProvider, option);
    }

    @Test
    public void initialise_user_already_logged_in() {
        //given
        when(browser.getLocale()).thenReturn(Locale.GERMANY);
        when(subject.isAuthenticated()).thenReturn(true);



        //when
        currentLocale = createCurrentLocale();
        setOption(Locale.UK);

        //then
        assertThat(currentLocale.getLocale()).isEqualTo(Locale.UK);
    }

    private void setOption(Locale userPref) {
        OptionKey<Locale> optionPreferredLocale = new OptionKey<>(currentLocale.getLocale(), DefaultCurrentLocale.class, LabelKey.Preferred_Locale,
                DescriptionKey.Preferred_Locale);
        when(option.get(optionPreferredLocale)).thenReturn(userPref);
    }

    @Test
    public void initialise_browser_locale_not_supported_user_not_logged_in() {

        //given
        when(browser.getLocale()).thenReturn(Locale.CHINA);
        when(subject.isAuthenticated()).thenReturn(false);

        //when
        currentLocale = createCurrentLocale();
        //then
        assertThat(currentLocale.getLocale()).isEqualTo(defaultLocale);
    }

    @Test
    public void initialise_browser_locale_not_supported_user_logged_in() {
        //        given
        when(browser.getLocale()).thenReturn(Locale.CHINA);
        when(subject.isAuthenticated()).thenReturn(true);


        //when
        currentLocale = createCurrentLocale();
        setOption(Locale.UK);
        //then
        assertThat(currentLocale.getLocale()).isEqualTo(Locale.UK);
    }

    @Test
    public void initialise_user_option_invalid_browser_invalid() {
        //        given
        when(browser.getLocale()).thenReturn(Locale.CHINA);
        when(subject.isAuthenticated()).thenReturn(true);


        //when
        currentLocale = createCurrentLocale();
        setOption(Locale.CHINA);
        //then
        assertThat(currentLocale.getLocale()).isEqualTo(defaultLocale);
    }

    @Test
    public void user_logs_out() {
        //given
        when(browser.getLocale()).thenReturn(Locale.GERMANY);
        when(subject.isAuthenticated()).thenReturn(true);


        //when
        currentLocale = createCurrentLocale();
        setOption(Locale.FRANCE);
        currentLocale.readFromEnvironment();

        //then
        assertThat(currentLocale.getLocale()).isEqualTo(Locale.FRANCE);

        //given
        listenerFired = false;
        eventBus.subscribe(this);
        when(subject.isAuthenticated()).thenReturn(false);
        when(source.identity()).thenReturn("LogoutView");
        //when user logs out
        currentLocale.userStatusChange(new UserStatusBusMessage(source, false));

        //then nothing should happen
        assertThat(currentLocale.getLocale()).isEqualTo(Locale.FRANCE);
        assertThat(listenerFired).isFalse();

    }

    @Test(expected = UnsupportedLocaleException.class)
    public void set_Locale_not_valid() {
        //given
        when(browser.getLocale()).thenReturn(Locale.GERMANY);
        when(subject.isAuthenticated()).thenReturn(false);
        currentLocale = createCurrentLocale();
        //when
        currentLocale.setLocale(Locale.CHINA);
        //then

    }

    @Test
    public void setLocaleValid() {

        // given
        when(browser.getLocale()).thenReturn(Locale.GERMANY);
        when(subject.isAuthenticated()).thenReturn(false);
        currentLocale = createCurrentLocale();
        currentLocale.readFromEnvironment();
        eventBus.subscribe(this);
        listenerFired = false;
        // when
        currentLocale.setLocale(Locale.UK);
        // then
        assertThat(listenerFired).isTrue();

    }

    @Test
    public void setLocaleNoFire() {
        // given
        currentLocale = createCurrentLocale();
        eventBus.subscribe(this);
        listenerFired = false;
        // when
        currentLocale.setLocale(Locale.FRANCE, false);
        // then
        assertThat(listenerFired).isFalse();
        //        assertThat(Locale.getDefault()).isEqualTo(Locale.FRANCE);
    }

    @Test
    public void setLocaleFire() {
        // given
        currentLocale = createCurrentLocale();
        eventBus.subscribe(this);
        listenerFired = false;
        currentLocale.setLocale(Locale.UK);
        // when
        currentLocale.setLocale(Locale.FRANCE, true);
        // then
        assertThat(listenerFired).isTrue();
        //        assertThat(Locale.getDefault()).isEqualTo(Locale.FRANCE);
    }

    @Test
    public void changeButNoChange() {

        // given
        currentLocale = createCurrentLocale();
        eventBus.subscribe(this);
        currentLocale.setLocale(Locale.GERMANY);
        listenerFired = false;
        // when
        currentLocale.setLocale(Locale.GERMANY);
        // then
        assertThat(listenerFired).isFalse();

    }

    @Test(expected = UnsupportedLocaleException.class)
    public void invalid_setup_default_locale_not_in_supported_locales() {
        //given
        defaultLocale = Locale.CANADA;
        //when
        currentLocale = createCurrentLocale();
        //then
    }

    @Handler
    public void localeChanged(LocaleChangeBusMessage toLocale) {
        listenerFired = true;
    }

    @ModuleProvider
    protected AbstractModule moduleProvider() {
        return new AbstractModule() {

            @Override
            protected void configure() {
                bind(Locale.class).annotatedWith(DefaultLocale.class)
                                  .toInstance(Locale.UK);
            }

        };
    }

}
