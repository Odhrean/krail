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

package uk.q3c.krail.core.ui;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.vaadin.data.util.converter.ConverterFactory;
import com.vaadin.server.ErrorHandler;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.VerticalLayout;
import uk.q3c.krail.core.navigate.Navigator;
import uk.q3c.krail.core.push.Broadcaster;
import uk.q3c.krail.core.push.PushMessageRouter;
import uk.q3c.krail.core.user.notify.VaadinNotification;
import uk.q3c.krail.core.user.opt.Option;
import uk.q3c.krail.core.user.opt.OptionContext;
import uk.q3c.krail.core.user.opt.OptionDescriptor;
import uk.q3c.krail.core.user.opt.OptionKey;
import uk.q3c.krail.core.view.component.*;
import uk.q3c.krail.i18n.*;

import javax.annotation.Nonnull;

/**
 * A common layout for a business-type application. This is a good place to start even if you replace it eventually.
 *
 * @author David Sowerby
 */
public class DefaultApplicationUI extends ScopedUI implements OptionContext {

    public static final OptionKey optionBreadcrumbVisible = new OptionKey(DefaultApplicationUI.class, LabelKey.Breadcrumb_is_Visible);
    public static final OptionKey optionNavTreeVisible = new OptionKey(DefaultApplicationUI.class, LabelKey.Navigation_Tree_is_Visible);
    public static final OptionKey optionMenuVisible = new OptionKey(DefaultApplicationUI.class, LabelKey.Navigation_Menu_is_Visible);
    public static final OptionKey optionMessageBarVisible = new OptionKey(DefaultApplicationUI.class, LabelKey.Message_bar_is_Visible);
    public static final OptionKey optionSubPagePanelVisible = new OptionKey(DefaultApplicationUI.class, LabelKey.SubPage_Panel_is_Visible);

    private final UserNavigationTree navTree;
    private final Breadcrumb breadcrumb;
    private final UserStatusPanel userStatus;
    private final UserNavigationMenu menu;
    private final SubPagePanel subpage;
    private final MessageBar messageBar;
    private final ApplicationLogo logo;
    private final ApplicationHeader header;
    private final LocaleSelector localeSelector;
    private VerticalLayout baseLayout;
    private Option option;
    // this appears not to be used but does receive bus messages
    private VaadinNotification vaadinNotification;

    @Inject
    protected DefaultApplicationUI(Navigator navigator, ErrorHandler errorHandler, ConverterFactory converterFactory, ApplicationLogo logo, ApplicationHeader
            header, UserStatusPanel userStatusPanel, UserNavigationMenu menu, UserNavigationTree navTree, Breadcrumb breadcrumb, SubPagePanel subpage,
                                   MessageBar messageBar, Broadcaster broadcaster, PushMessageRouter pushMessageRouter, ApplicationTitle applicationTitle, Translate translate, CurrentLocale currentLocale, I18NProcessor translator, LocaleSelector localeSelector, VaadinNotification vaadinNotification, Option option) {
        super(navigator, errorHandler, converterFactory, broadcaster, pushMessageRouter, applicationTitle, translate, currentLocale, translator);
        this.navTree = navTree;
        this.breadcrumb = breadcrumb;
        this.userStatus = userStatusPanel;
        this.menu = menu;
        this.subpage = subpage;
        this.messageBar = messageBar;
        this.logo = logo;
        this.header = header;
        this.localeSelector = localeSelector;
        this.vaadinNotification = vaadinNotification;
        this.option = option;
    }

    @Override
    protected AbstractOrderedLayout screenLayout() {
        if (baseLayout == null) {

            setSizes();

            baseLayout = new VerticalLayout();
            baseLayout.setSizeFull();

            HorizontalLayout row0 = new HorizontalLayout(header, localeSelector.getComponent(), userStatus);
            row0.setWidth("100%");
            baseLayout.addComponent(row0);
            baseLayout.addComponent(menu);
            HorizontalSplitPanel row2 = new HorizontalSplitPanel();
            row2.setWidth("100%");
            row2.setSplitPosition(200, Unit.PIXELS);

            row2.setFirstComponent(navTree);

            VerticalLayout mainArea = new VerticalLayout(breadcrumb, getViewDisplayPanel(), subpage);
            mainArea.setSizeFull();
            row2.setSecondComponent(mainArea);
            baseLayout.addComponent(row2);
            baseLayout.addComponent(messageBar);
            mainArea.setExpandRatio(getViewDisplayPanel(), 1f);

            row0.setExpandRatio(header, 1f);
            baseLayout.setExpandRatio(row2, 1f);

        }
        processOptions();
        if (navTree.isVisible()) {
            navTree.build();
        }
        if (menu.isVisible()) {
            menu.build();
        }

        return baseLayout;
    }

    protected void processOptions() {
        breadcrumb.setVisible(option.get(true, optionBreadcrumbVisible));
        menu.setVisible(option.get(true, optionMenuVisible));
        navTree.setVisible(option.get(true, optionNavTreeVisible));
        messageBar.setVisible(option.get(true, optionMessageBarVisible));
        subpage.setVisible(option.get(true, optionSubPagePanelVisible));
    }


    private void setSizes() {
        logo.setWidth("100px");
        logo.setHeight("100px");

        header.setHeight("100%");
        userStatus.setSizeUndefined();

        navTree.setSizeFull();
        breadcrumb.setSizeUndefined();

        menu.setSizeUndefined();
        menu.setWidth("100%");

        subpage.setSizeUndefined();

        messageBar.setSizeUndefined();
        messageBar.setWidth("100%");

    }

    public MessageBar getMessageBar() {
        return messageBar;
    }

    public VerticalLayout getBaseLayout() {
        return baseLayout;
    }

    public UserNavigationTree getNavTree() {
        return navTree;
    }

    public Breadcrumb getBreadcrumb() {
        return breadcrumb;
    }

    public UserStatusPanel getUserStatus() {
        return userStatus;
    }

    public UserNavigationMenu getMenu() {
        return menu;
    }

    public SubPagePanel getSubpage() {
        return subpage;
    }

    public ApplicationLogo getLogo() {
        return logo;
    }

    public ApplicationHeader getHeader() {
        return header;
    }

    /**
     * Returns the {@link Option} instance being used by this context
     *
     * @return the {@link Option} instance being used by this context
     */
    @Nonnull
    @Override
    public Option getOption() {
        return option;
    }

    @Override
    public ImmutableSet<OptionDescriptor> optionDescriptors() {

        return ImmutableSet.of(OptionDescriptor.descriptor(optionBreadcrumbVisible, DescriptionKey.Breadcrumb_is_Visible), OptionDescriptor.descriptor
                (optionMessageBarVisible, DescriptionKey.MessageBar_is_Visible), OptionDescriptor.descriptor(optionNavTreeVisible, DescriptionKey
                .Navigation_Tree_is_Visible), OptionDescriptor.descriptor(optionMenuVisible, DescriptionKey.Navigation_Menu_is_Visible));
    }

}