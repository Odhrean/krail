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
package uk.q3c.krail.core.navigate.sitemap;

import com.google.inject.Inject;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.q3c.krail.core.guice.vsscope.VaadinSessionScoped;
import uk.q3c.krail.core.shiro.SubjectProvider;
import uk.q3c.krail.core.user.status.UserStatusBusMessage;
import uk.q3c.util.SourceTreeWrapper_BasicForest;
import uk.q3c.util.TargetTreeWrapper_BasicForest;
import uk.q3c.util.TreeCopy;

@VaadinSessionScoped
@Listener
public class UserSitemapBuilder {
    private static Logger log = LoggerFactory.getLogger(UserSitemapBuilder.class);
    private final TreeCopy<MasterSitemapNode, UserSitemapNode> treeCopy;
    private final UserSitemap userSitemap;
    private SubjectProvider subjectProvider;

    @Inject
    protected UserSitemapBuilder(MasterSitemap masterSitemap, UserSitemap userSitemap, UserSitemapNodeModifier nodeModifier, UserSitemapCopyExtension copyExtension, SubjectProvider subjectProvider) {

        this.userSitemap = userSitemap;
        this.subjectProvider = subjectProvider;
        TargetTreeWrapper_BasicForest<MasterSitemapNode, UserSitemapNode> target = new TargetTreeWrapper_BasicForest<>(userSitemap.getForest());
        target.setNodeModifier(nodeModifier);
        SourceTreeWrapper_BasicForest<MasterSitemapNode> source = new SourceTreeWrapper_BasicForest<>(masterSitemap.getForest());
        treeCopy = new TreeCopy<>(source, target);
        treeCopy.setExtension(copyExtension);

    }

    public UserSitemap getUserSitemap() {
        return userSitemap;
    }


    @Handler
    public synchronized void userStatusChanged(UserStatusBusMessage busMessage) {
        log.debug("UserStatusBusMessage received");
        log.debug("user status is now authenticated = '{}', rebuild the userSitemap", busMessage.isAuthenticated());
        userSitemap.clear();
        build();

    }

    public synchronized void build() {
        log.debug("building or rebuilding the map, user status is {}", subjectProvider.get()
                                                                                      .isAuthenticated());
        if (!userSitemap.isLoaded()) {
            treeCopy.copy();
            userSitemap.setLoaded(true);
        }
    }


}
