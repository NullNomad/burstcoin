package brs.http;

import brs.Burst;
import brs.BurstException;
import brs.Transaction;
import brs.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class BroadcastTransaction extends APIServlet.APIRequestHandler {

  private static final Logger logger = Logger.getLogger(BroadcastTransaction.class.getSimpleName());

  static final BroadcastTransaction instance = new BroadcastTransaction();

  private BroadcastTransaction() {
    super(new APITag[] {APITag.TRANSACTIONS}, "transactionBytes", "transactionJSON");
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    String transactionBytes = Convert.emptyToNull(req.getParameter("transactionBytes"));
    String transactionJSON = Convert.emptyToNull(req.getParameter("transactionJSON"));
    Transaction transaction = ParameterParser.parseTransaction(transactionBytes, transactionJSON);
    JSONObject response = new JSONObject();
    try {
      transaction.validate();
      Burst.getTransactionProcessor().broadcast(transaction);
      response.put("transaction", transaction.getStringId());
      response.put("fullHash", transaction.getFullHash());
    } catch (BurstException.ValidationException|RuntimeException e) {
      logger.log(Level.INFO,e.getMessage(), e);
      response.put("errorCode", 4);
      response.put("errorDescription", "Incorrect transaction: " + e.toString());
      response.put("error", e.getMessage());
    }
    return response;

  }

  @Override
  boolean requirePost() {
    return true;
  }

}
