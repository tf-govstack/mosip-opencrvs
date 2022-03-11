package io.mosip.opencrvs.util;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.logger.logback.factory.Logfactory;

public class LogUtil{
  public static Logger getLogger(Class<?> clazz) {
      return Logfactory.getSlf4jLogger(clazz);
  }
}
