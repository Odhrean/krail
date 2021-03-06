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

package uk.q3c.krail.core.services;

import uk.q3c.krail.core.eventbus.BusMessage;

/**
 * Created by David Sowerby on 10/03/15.
 */
public class ServiceBusMessage implements BusMessage {
    private final Service service;
    private final Service.Status fromStatus;
    private final Service.Status toStatus;

    public ServiceBusMessage(Service service, Service.Status fromStatus, Service.Status toStatus) {
        this.service = service;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
    }

    public Service getService() {
        return service;
    }

    public Service.Status getFromStatus() {
        return fromStatus;
    }

    public Service.Status getToStatus() {
        return toStatus;
    }
}
