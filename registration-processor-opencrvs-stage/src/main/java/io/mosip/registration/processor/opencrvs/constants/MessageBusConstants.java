package io.mosip.registration.processor.opencrvs.constants;

import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;

public class MessageBusConstants {

    public static final MessageBusAddress OPENCRVS_STAGE_BUS_IN= new MessageBusAddress("opencrvs-bus-in");
    public static final MessageBusAddress OPENCRVS_STAGE_BUS_OUT = new MessageBusAddress("opencrvs-bus-out");
}
