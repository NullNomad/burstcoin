package brs.db.sql;

import brs.Subscription;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.VersionedEntityTable;
import brs.db.store.SubscriptionStore;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jooq.DSLContext;
import org.jooq.Condition;

import static brs.schema.Tables.SUBSCRIPTION;

public class SqlSubscriptionStore implements SubscriptionStore {

  private final BurstKey.LongKeyFactory<Subscription> subscriptionDbKeyFactory = new DbKey.LongKeyFactory<Subscription>("id") {
      @Override
      public BurstKey newKey(Subscription subscription) {
        return subscription.dbKey;
      }
    };

  private final VersionedEntityTable<Subscription> subscriptionTable =
      new VersionedEntitySqlTable<Subscription>("subscription", brs.schema.Tables.SUBSCRIPTION, subscriptionDbKeyFactory) {
        @Override
        protected Subscription load(DSLContext ctx, ResultSet rs) throws SQLException {
          return new SqlSubscription(rs);
        }

        @Override
        protected void save(DSLContext ctx, Subscription subscription) throws SQLException {
          saveSubscription(ctx, subscription);
        }

        @Override
        protected String defaultSort() {
          return " ORDER BY time_next ASC, id ASC ";
        }
      };

  private static Condition getByParticipantClause(final long id) {
    return SUBSCRIPTION.SENDER_ID.eq(id).or(SUBSCRIPTION.RECIPIENT_ID.eq(id));
  }

  private static Condition getUpdateOnBlockClause(final int timestamp) {
    return SUBSCRIPTION.TIME_NEXT.le(timestamp);
  }

  @Override
  public BurstKey.LongKeyFactory<Subscription> getSubscriptionDbKeyFactory() {
    return subscriptionDbKeyFactory;
  }

  @Override
  public VersionedEntityTable<Subscription> getSubscriptionTable() {
    return subscriptionTable;
  }

  @Override
  public BurstIterator<Subscription> getSubscriptionsByParticipant(Long accountId) {
    return subscriptionTable.getManyBy(getByParticipantClause(accountId), 0, -1);
  }

  @Override
  public BurstIterator<Subscription> getIdSubscriptions(Long accountId) {
    return subscriptionTable.getManyBy(SUBSCRIPTION.SENDER_ID.eq(accountId), 0, -1);
  }

  @Override
  public BurstIterator<Subscription> getSubscriptionsToId(Long accountId) {
    return subscriptionTable.getManyBy(SUBSCRIPTION.RECIPIENT_ID.eq(accountId), 0, -1);
  }

  @Override
  public BurstIterator<Subscription> getUpdateSubscriptions(int timestamp) {
    return subscriptionTable.getManyBy(getUpdateOnBlockClause(timestamp), 0, -1);
  }

  protected void saveSubscription(DSLContext ctx, Subscription subscription) throws SQLException {
    ctx.mergeInto(
      SUBSCRIPTION,
      SUBSCRIPTION.ID, SUBSCRIPTION.SENDER_ID, SUBSCRIPTION.RECIPIENT_ID, SUBSCRIPTION.AMOUNT,
      SUBSCRIPTION.FREQUENCY, SUBSCRIPTION.TIME_NEXT, SUBSCRIPTION.HEIGHT, SUBSCRIPTION.LATEST
    )
    .key(
      SUBSCRIPTION.ID, SUBSCRIPTION.SENDER_ID, SUBSCRIPTION.RECIPIENT_ID, SUBSCRIPTION.AMOUNT,
      SUBSCRIPTION.FREQUENCY, SUBSCRIPTION.TIME_NEXT, SUBSCRIPTION.HEIGHT, SUBSCRIPTION.LATEST
    )
    .values(
      subscription.id, subscription.senderId, subscription.recipientId, subscription.amountNQT, subscription.frequency,
      subscription.getTimeNext(), brs.Burst.getBlockchain().getHeight(), true
    )
    .execute();
  }

  private class SqlSubscription extends Subscription {
    public SqlSubscription(ResultSet rs) throws SQLException {
      super(
            rs.getLong("sender_id"),
            rs.getLong("recipient_id"),
            rs.getLong("id"),
            rs.getLong("amount"),
            rs.getInt("frequency"),
            rs.getInt("time_next"),
            subscriptionDbKeyFactory.newKey(rs.getLong("id"))

            );
    }


  }
}
