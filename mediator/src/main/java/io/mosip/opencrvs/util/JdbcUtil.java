package io.mosip.opencrvs.util;

import java.sql.Timestamp;

import org.springframework.dao.DataAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;

import io.mosip.opencrvs.constant.LoggingConstants;

@Component
public class JdbcUtil{
  private static final Logger LOGGER = LogUtil.getLogger(JdbcUtil.class);

  @Value("${mosip.opencrvs.db.datasource.birth.transaction.table}")
  private String birthTableName;

  @Value("${mosip.opencrvs.db.datasource.cr.by}")
  private String crBy;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private Environment env;

  public boolean ifTxnExists(String txnId) {
    try{
      String str = jdbcTemplate.queryForObject("SELECT txn_id FROM "+birthTableName+" WHERE txn_id=\'"+txnId+"\';", String.class);
      if(str != null)if(!str.isEmpty()) return true;
    } catch(DataAccessException dae){
      LOGGER.error(LoggingConstants.SESSION,LoggingConstants.ID,"jdbc txn_id - "+txnId,"Error getting status from table "+dae);
    }
    return false;
  }

  public void createBirthTransaction(String txnId) {
    Timestamp crDtimes = Timestamp.valueOf(DateUtils.getUTCCurrentDateTime());

    try{
      jdbcTemplate.update("INSERT into "+birthTableName+" (txn_id, cr_by, cr_dtimes) VALUES (\'"+txnId+"\',\'"+crBy+"\',\'"+crDtimes+"\');");
    } catch(DataAccessException dae){
      LOGGER.error(LoggingConstants.SESSION,LoggingConstants.ID,"jdbc txn_id - "+txnId,"Error inserting data into table "+dae);
    }
  }

  public void updateBirthStatus(String txnId, String status) {
    Timestamp updDtimes = Timestamp.valueOf(DateUtils.getUTCCurrentDateTime());

    try{
      jdbcTemplate.update("UPDATE "+birthTableName+" SET status=\'"+status+"\', upd_by=\'"+crBy+"\', upd_dtimes=\'"+updDtimes+"\' WHERE txn_id=\'"+txnId+"\';");
    } catch(DataAccessException dae){
      LOGGER.error(LoggingConstants.SESSION,LoggingConstants.ID,"jdbc txn_id - "+txnId,"Error updating status in table "+dae);
    }
  }

  public void updateBirthStatusOfRid(String rid, String status) {
    Timestamp updDtimes = Timestamp.valueOf(DateUtils.getUTCCurrentDateTime());

    try{
      jdbcTemplate.update("UPDATE "+birthTableName+" SET status=\'"+status+"\', upd_by=\'"+crBy+"\', upd_dtimes=\'"+updDtimes+"\' WHERE rid=\'"+rid+"\';");
    } catch(DataAccessException dae){
      LOGGER.error(LoggingConstants.SESSION,LoggingConstants.ID,"jdbc rid - "+rid,"Error updating status in table "+dae);
    }
  }

  public void updateBirthRidAndStatus(String txnId, String rid, String status){
    Timestamp updDtimes = Timestamp.valueOf(DateUtils.getUTCCurrentDateTime());

    try{
      jdbcTemplate.update("UPDATE "+birthTableName+" SET rid=\'"+rid+"\', status=\'"+status+"\', upd_by=\'"+crBy+"\', upd_dtimes=\'"+updDtimes+"\' WHERE txn_id=\'"+txnId+"\';");
    } catch(DataAccessException dae){
      LOGGER.error(LoggingConstants.SESSION,LoggingConstants.ID,"jdbc txn_id - "+txnId,"Error updating rid and status in table "+dae);
    }
  }

  public String getBirthStatus(String txnId){
    try{
      return jdbcTemplate.queryForObject("SELECT status FROM "+birthTableName+" WHERE txn_id=\'"+txnId+"\';", String.class);
    } catch(DataAccessException dae){
      LOGGER.error(LoggingConstants.SESSION,LoggingConstants.ID,"jdbc txn_id - "+txnId,"Error getting status from table "+dae);
      return null;
    }
  }

  public String getBirthRid(String txnId) throws DataAccessException{
    try{
      return jdbcTemplate.queryForObject("SELECT rid FROM "+birthTableName+" WHERE txn_id=\'"+txnId+"\';", String.class);
    } catch(DataAccessException dae){
      LOGGER.error(LoggingConstants.SESSION,LoggingConstants.ID,"jdbc txn_id - "+txnId,"Error getting rid from table "+dae);
      return null;
    }
  }
}
