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
package fixture.testviews2;

import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import uk.q3c.krail.core.view.LogoutView;
import uk.q3c.krail.core.view.component.AfterViewChangeBusMessage;
import uk.q3c.krail.core.view.component.ViewChangeBusMessage;

public class TestLogoutView implements LogoutView {


    @Override
    public void beforeBuild(ViewChangeBusMessage busMessage) {

    }

    @Override
    public void buildView(ViewChangeBusMessage busMessage) {
    }

    @Override
    public Component getRootComponent() {
        return new Label("not used");
    }

    @Override
    public String viewName() {

        return getClass().getSimpleName();
    }

    @Override
    public void init() {
    }

    @Override
    public void afterBuild(AfterViewChangeBusMessage busMessage) {

    }


}
