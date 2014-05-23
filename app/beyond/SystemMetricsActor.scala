package beyond

import akka.actor.Actor
import akka.actor.ActorLogging
import com.sun.tools.attach.VirtualMachine
import java.io.Closeable
import java.io.File
import java.lang.management.ManagementFactory
import java.lang.management.OperatingSystemMXBean
import java.lang.management.RuntimeMXBean
import javax.management.MBeanServerConnection
import javax.management.remote.JMXConnector
import javax.management.remote.JMXConnectorFactory
import javax.management.remote.JMXServiceURL
import scala.collection.mutable

object SystemMetricsActor {
  sealed trait SystemMetricsRequest
  case object SystemLoadAverageRequest extends SystemMetricsRequest

  sealed trait SystemMetricsReply
  case class SystemLoadAverageReply(loadAverage: Double) extends SystemMetricsReply
}

class SystemMetricsActor extends Actor with ActorLogging {
  import SystemMetricsActor._

  private val jmxResources: mutable.Stack[Closeable] = mutable.Stack()

  override def preStart() {
    def getProcessID: String = {
      val bean: RuntimeMXBean = ManagementFactory.getRuntimeMXBean
      // Get the name representing the running Java virtual machine.
      // It returns something like 6460@AURORA. Where the value
      // before the @ symbol is the PID.
      val jvmName = bean.getName
      // Extract the PID by splitting the string returned by the
      // bean.getName() method.
      jvmName.split("@")(0)
    }

    try {
      val vm = VirtualMachine.attach(getProcessID)
      val ConnectorAddress = "com.sun.management.jmxremote.localConnectorAddress"
      val urlString = Option(vm.getAgentProperties.getProperty(ConnectorAddress)).getOrElse {
        val agent = vm.getSystemProperties.getProperty("java.home") +
          File.separator + "lib" + File.separator + "management-agent.jar"
        vm.loadAgent(agent)

        // agent is started, get the connector address again
        vm.getAgentProperties.getProperty(ConnectorAddress)
      }

      val url = new JMXServiceURL(urlString)
      val jmxc: JMXConnector = JMXConnectorFactory.connect(url)
      jmxResources.push(jmxc)
      val mbsc: MBeanServerConnection = jmxc.getMBeanServerConnection()

      val bean: OperatingSystemMXBean = ManagementFactory.newPlatformMXBeanProxy(
        mbsc, ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME, classOf[OperatingSystemMXBean])
      context.become(receiveWithOperatingSystemMXBean(bean))

    } catch {
      case ex: Throwable =>
        closeAllJMXResources()
        throw ex
    }

    log.info("SystemMetricsActor started")
  }

  override def postStop() {
    closeAllJMXResources()

    log.info("SystemMetricsActor stopped")
  }

  private def closeAllJMXResources() {
    jmxResources.foreach(_.close())
  }

  override def receive: Receive = {
    // Does nothing when this actor is not yet initialized.
    case _ =>
  }

  private def receiveWithOperatingSystemMXBean(bean: OperatingSystemMXBean): Receive = {
    case SystemLoadAverageRequest =>
      sender ! SystemLoadAverageReply(bean.getSystemLoadAverage)
  }
}
