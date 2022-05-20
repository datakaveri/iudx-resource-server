package iudx.resource.server.database.async;

import static iudx.resource.server.database.postgres.Constants.UPDATE_S3_PROGRESS_SQL;
import java.util.Collections;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import iudx.resource.server.database.postgres.PostgresService;

public class AsyncFileScrollProgressListener implements ProgressListener {

  private static final Logger LOGGER = LogManager.getLogger(AsyncFileScrollProgressListener.class);

  private PriorityQueue<Double> progressQueue;
  private PostgresService postgresService;
  private ExecutionCounter executionCounter;
  private final String searchId;
  private ExecutorService executor;

  public AsyncFileScrollProgressListener(String searchId, PostgresService service) {
    progressQueue = new PriorityQueue<Double>(Collections.reverseOrder());
    progressQueue.add(0.1);
    this.searchId = searchId;
    this.postgresService = service;
    this.executionCounter = new ExecutionCounter();
    executor = Executors.newSingleThreadExecutor();
  }

  @Override
  public void updateProgress(double progressCount) {
    progressQueue.add(progressCount);
    if (!executionCounter.isExecuting)
      updateProgress(executionCounter);
    else {
      LOGGER.debug("Queue top: " + progressQueue.peek());
    }
  }


  private void updateProgress(ExecutionCounter executionCounter) {
    if (!executionCounter.isExecuting && !progressQueue.isEmpty() && progressQueue.peek() <= 1.0d) {
      double progress = progressQueue.poll();
      progressQueue.clear();
      LOGGER.debug("queue cleared");
      executionCounter.isExecuting = true;

      executor.execute(() -> {
        updateProgress(progress, executionCounter)
            .onComplete(handler -> {
              try {
                // sleep for 5s to avoid request flooding in PG or pool connection exhaustion.
                Thread.sleep(5000);
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
              executionCounter.isExecuting = false;
            });
      });
    }
  }

  private Future<Void> updateProgress(double progress, ExecutionCounter executionCounter) {
    Promise<Void> promise = Promise.promise();
    StringBuilder query = new StringBuilder(UPDATE_S3_PROGRESS_SQL
        .replace("$1", String.valueOf(progress * 100.0))
        .replace("$2", searchId));
    LOGGER.debug("updating progress : " + query.toString());
    postgresService
        .executeQuery(query.toString(), pgHandler -> {
          LOGGER.debug(pgHandler);
          if (pgHandler.succeeded()) {
            LOGGER.debug("updated success for progress :" + progress);
            LOGGER.debug("execution status : " + executionCounter.isExecuting);
            promise.complete();
          } else {
            LOGGER.error(pgHandler);
            executionCounter.isExecuting = false;
            promise.fail(pgHandler.cause());
          }
        });
    return promise.future();
  }

  @Override
  public void finish() {
    if (executor != null)
      executor.shutdownNow();
  }

  class ExecutionCounter {
    boolean isExecuting;

    ExecutionCounter() {
      this.isExecuting = false;
    }

    synchronized void updateCounter(boolean value) {
      this.isExecuting = value;
    }

  }



}
