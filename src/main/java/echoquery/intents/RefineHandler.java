package echoquery.intents;

import java.io.IOException;
import java.sql.Connection;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SpeechletResponse;

import echoquery.querier.Querier;
import echoquery.querier.QueryRequest;
import echoquery.querier.schema.ColumnType;
import echoquery.utils.Response;
import echoquery.utils.Serializer;
import echoquery.utils.SessionUtil;
import echoquery.utils.SlotUtil;

public class RefineHandler implements IntentHandler {

  private static final Logger log =
      LoggerFactory.getLogger(RefineHandler.class);
  private final Querier querier;
  private final QueryHandler queryHandler;

  public RefineHandler(Connection conn, QueryHandler aggregationHandler) {
    this.querier = new Querier(conn);
    this.queryHandler = aggregationHandler;
  }

  @Override
  public SpeechletResponse respond(Intent intent, Session session) {
    try {
      return queryHandler.respond(
          updateRequest(intent, (QueryRequest) Serializer.deserialize(
              (String) session.getAttribute(SessionUtil.REQUEST_ATTRIBUTE))),
          session);
    } catch (ClassNotFoundException | IOException e) {
      e.printStackTrace();
      return Response.unexpectedError(session);
    }
  }

  /**
   * Exposed for testing purposes - SpeechletResponse is impossible to inspect.
   */
  public String getResponseInEnglish(Intent intent, Session session) {
    QueryRequest previousRequest;
    try {
      previousRequest = (QueryRequest) Serializer.deserialize(
                (String) session.getAttribute(SessionUtil.REQUEST_ATTRIBUTE));
      return querier.execute(
          updateRequest(intent, previousRequest)).getMessage();
    } catch (ClassNotFoundException | IOException e) {
      e.printStackTrace();
      return "There was an error generating the response in English";
    }
  }

  private QueryRequest updateRequest(
      Intent intent, QueryRequest previousRequest) {
    if (intent.getSlot(SlotUtil.GROUP_BY_COLUMN).getValue() == null) {
      previousRequest
          .addWhereClause(
            previousRequest.getComparisonColumns().size() > 0 ? "and" : null,
            SlotUtil.parseColumnSlot(
            intent.getSlot(SlotUtil.COMPARISON_COLUMN_1).getValue())
                .setType((intent.getSlot(
                    SlotUtil.COLUMN_NUMBER_1).getValue() != null)
                ? ColumnType.NUMBER : ColumnType.STRING),
            intent.getSlot(SlotUtil.COMPARATOR_1).getValue(),
            ObjectUtils.defaultIfNull(
                intent.getSlot(SlotUtil.COLUMN_VALUE_1).getValue(),
                intent.getSlot(SlotUtil.COLUMN_NUMBER_1).getValue()));
    } else {
      previousRequest
          .setGroupByColumn(SlotUtil.parseColumnSlot(
              intent.getSlot(SlotUtil.GROUP_BY_COLUMN).getValue()));
    }
    return previousRequest;
  }
}