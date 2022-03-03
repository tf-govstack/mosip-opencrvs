package io.mosip.opencrvs;

import java.io.Serializable;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

import org.json.JSONArray;

import lombok.Data;

@Data
public class SyncServiceRequestDto implements Serializable {
  private static final long serialVersionUID = 7914304502765754692L;
  String id;
  String version;
  String requesttime;
  List<SyncDto> request;
}
