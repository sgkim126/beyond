package beyond

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import beyond.config.BeyondConfiguration
import beyond.route.RoutingTableUpdater
import beyond.route.RoutingTableWatcher
import com.typesafe.scalalogging.slf4j.{ StrictLogging => Logging }
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.CuratorFrameworkFactory

private object CuratorFrameworkFactoryWithDefaultPolicy extends Logging {
  def apply(serversToConnect: String): CuratorFramework = {
    logger.info("Create a connection to {} .", serversToConnect)
    CuratorFrameworkFactory.newClient(serversToConnect, BeyondConfiguration.curatorConnectionPolicy)
  }
}

object CuratorSupervisor {
  val Name: String = "curatorSupervisor"
}

class CuratorSupervisor extends Actor with ActorLogging {
  private val curatorFramework = {
    // FIXME: Make connection string configurable.
    val framework = CuratorFrameworkFactoryWithDefaultPolicy("localhost:2181")
    framework.start()
    framework
  }

  context.actorOf(Props(classOf[LeaderSelectorActor], curatorFramework), LeaderSelectorActor.Name)
  context.actorOf(Props(classOf[WorkerRegistrationActor], curatorFramework), WorkerRegistrationActor.Name)

  context.actorOf(Props(classOf[RoutingTableUpdater], curatorFramework), RoutingTableUpdater.Name)
  context.actorOf(Props(classOf[RoutingTableWatcher], curatorFramework), RoutingTableWatcher.Name)

  override def postStop() {
    curatorFramework.close()
  }

  override def receive: Receive = Actor.emptyBehavior
}

