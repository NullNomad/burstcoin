package brs.http;

import brs.Account;
import brs.Attachment;
import brs.BurstException;
import brs.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.JSONResponses.INCORRECT_PERIOD;
import static brs.http.JSONResponses.MISSING_PERIOD;

public final class LeaseBalance extends CreateTransaction {

  static final LeaseBalance instance = new LeaseBalance();

  private LeaseBalance() {
    super(new APITag[] {APITag.FORGING}, "period", "recipient");
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    String periodString = Convert.emptyToNull(req.getParameter("period"));
    if (periodString == null) {
      return MISSING_PERIOD;
    }
    short period;
    try {
      period = Short.parseShort(periodString);
      if (period < 1440) {
        return INCORRECT_PERIOD;
      }
    } catch (NumberFormatException e) {
      return INCORRECT_PERIOD;
    }

    Account account = ParameterParser.getSenderAccount(req);
    long recipient = ParameterParser.getRecipientId(req);
    Account recipientAccount = Account.getAccount(recipient);
    if (recipientAccount == null || recipientAccount.getPublicKey() == null) {
      JSONObject response = new JSONObject();
      response.put("errorCode", 8);
      response.put("errorDescription", "recipient account does not have public key");
      return response;
    }
    Attachment attachment = new Attachment.AccountControlEffectiveBalanceLeasing(period);
    return createTransaction(req, account, recipient, 0, attachment);

  }

}
