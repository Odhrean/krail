/*
 * Copyright (c) 2014 David Sowerby
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the License.
 */

package uk.co.q3c.v7.base.view;

import com.mycila.testing.junit.MycilaJunitRunner;
import com.mycila.testing.plugin.guice.GuiceContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.co.q3c.v7.base.navigate.NavigationState;
import uk.co.q3c.v7.base.shiro.LoginExceptionHandler;
import uk.co.q3c.v7.base.shiro.SubjectProvider;
import uk.co.q3c.v7.base.user.status.UserStatus;
import uk.co.q3c.v7.i18n.Translate;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MycilaJunitRunner.class)
@GuiceContext({})
public class DefaultLoginViewTest {

    DefaultLoginView view;

    @Mock
    Translate translate;

    @Mock
    LoginExceptionHandler loginExceptionHandler;

    @Mock
    UserStatus userStatus;

    @Mock
    private SubjectProvider subjectProvider;


    @Before
    public void setup() {
        view = new DefaultLoginView(loginExceptionHandler, subjectProvider, translate, userStatus);
    }

    @Test
    public void buildView() {
        //given
        V7ViewChangeEvent event = new V7ViewChangeEvent(new NavigationState(), new NavigationState());
        //when

        view.buildView(event);
        //then
        assertThat(view.getRootComponent()).isNotNull();
    }
}