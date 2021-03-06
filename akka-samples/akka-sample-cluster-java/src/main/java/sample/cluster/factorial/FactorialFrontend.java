package sample.cluster.factorial;

import java.util.concurrent.TimeUnit;
import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.actor.ReceiveTimeout;
import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.routing.FromConfig;

//#frontend
public class FactorialFrontend extends AbstractActor {
  final int upToN;
  final boolean repeat;

  LoggingAdapter log = Logging.getLogger(getContext().system(), this);

  ActorRef backend = getContext().actorOf(FromConfig.getInstance().props(),
      "factorialBackendRouter");

  public FactorialFrontend(int upToN, boolean repeat) {
    this.upToN = upToN;
    this.repeat = repeat;
  }

  @Override
  public void preStart() {
    sendJobs();
    getContext().setReceiveTimeout(Duration.create(10, TimeUnit.SECONDS));
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
      .match(FactorialResult.class, result -> {
        if (result.n == upToN) {
          log.debug("{}! = {}", result.n, result.factorial);
          if (repeat)
            sendJobs();
          else
            getContext().stop(self());
        }
      })
      .match(ReceiveTimeout.class, x -> {
        log.info("Timeout");
        sendJobs();
      })
      .build();
  }

  void sendJobs() {
    log.info("Starting batch of factorials up to [{}]", upToN);
    for (int n = 1; n <= upToN; n++) {
      backend.tell(n, getSelf());
    }
  }

}

//#frontend

