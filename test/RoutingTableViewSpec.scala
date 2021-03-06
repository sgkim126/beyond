import beyond.route.RoutingTableView
import beyond.route.RoutingTableView.HandleHere
import beyond.route.RoutingTableView.HandleIn
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.libs.json.JsArray
import play.api.libs.json.Json

@RunWith(classOf[JUnitRunner])
class RoutingTableViewSpec extends Specification {
  "RoutingTableView" should {
    "(previous server hash, current server hash] will be handled by current server" in {
      val addressOfServer1 = "127.0.0.1:9001"
      val addressOfServer2 = "127.0.0.1:9002"
      val addressOfServer3 = "127.0.0.1:9003"
      val addressOfServer4 = "127.0.0.1:9004"
      val routingTableData = JsArray(Seq(
        Json.obj("address" -> addressOfServer1, "hash" -> 100),
        Json.obj("address" -> addressOfServer2, "hash" -> 200),
        Json.obj("address" -> addressOfServer3, "hash" -> 300),
        Json.obj("address" -> addressOfServer4, "hash" -> 400)))

      val routingTableOfServer = new RoutingTableView(addressOfServer2, routingTableData)

      routingTableOfServer.queryServerToHandle(120) must equalTo(HandleHere)
      routingTableOfServer.queryServerToHandle(180) must equalTo(HandleHere)
    }

    "If hash is not in my range, redirect it to other server" in {
      val addressOfServer1 = "127.0.0.1:9001"
      val addressOfServer2 = "127.0.0.1:9002"
      val addressOfServer3 = "127.0.0.1:9003"
      val addressOfServer4 = "127.0.0.1:9004"
      val routingTableData = JsArray(Seq(
        Json.obj("address" -> addressOfServer1, "hash" -> 100),
        Json.obj("address" -> addressOfServer2, "hash" -> 200),
        Json.obj("address" -> addressOfServer3, "hash" -> 300),
        Json.obj("address" -> addressOfServer4, "hash" -> 400)))

      val routingTableOfServer = new RoutingTableView(addressOfServer2, routingTableData)

      routingTableOfServer.queryServerToHandle(100) must equalTo(HandleIn(addressOfServer1))
      routingTableOfServer.queryServerToHandle(201) must equalTo(HandleIn(addressOfServer3))
      routingTableOfServer.queryServerToHandle(205) must equalTo(HandleIn(addressOfServer3))
    }

    "If hash is less than minimum hash in routing table, first server will handle it." in {
      val addressOfServer1 = "127.0.0.1:9001"
      val addressOfServer2 = "127.0.0.1:9002"
      val addressOfServer3 = "127.0.0.1:9003"
      val addressOfServer4 = "127.0.0.1:9004"
      val routingTableData = JsArray(Seq(
        Json.obj("address" -> addressOfServer1, "hash" -> 100),
        Json.obj("address" -> addressOfServer2, "hash" -> 200),
        Json.obj("address" -> addressOfServer3, "hash" -> 300),
        Json.obj("address" -> addressOfServer4, "hash" -> 400)))

      val routingTableOfServer = new RoutingTableView(addressOfServer2, routingTableData)

      routingTableOfServer.queryServerToHandle(99) must equalTo(HandleIn(addressOfServer1))
    }

    "If hash is more than maximum hash in routing table, first server will handle it." in {
      val addressOfServer1 = "127.0.0.1:9001"
      val addressOfServer2 = "127.0.0.1:9002"
      val addressOfServer3 = "127.0.0.1:9003"
      val addressOfServer4 = "127.0.0.1:9004"
      val routingTableData = JsArray(Seq(
        Json.obj("address" -> addressOfServer1, "hash" -> 100),
        Json.obj("address" -> addressOfServer2, "hash" -> 200),
        Json.obj("address" -> addressOfServer3, "hash" -> 300),
        Json.obj("address" -> addressOfServer4, "hash" -> 400)))

      val routingTableOfServer = new RoutingTableView(addressOfServer2, routingTableData)

      routingTableOfServer.queryServerToHandle(401) must equalTo(HandleIn(addressOfServer1))
    }

    "If RoutingTable is empty, always return HandleHere" in {
      val routingTableOfServer = new RoutingTableView("some.address", new JsArray)

      routingTableOfServer.queryServerToHandle(20) must equalTo(HandleHere)
      routingTableOfServer.queryServerToHandle(100) must equalTo(HandleHere)
      routingTableOfServer.queryServerToHandle(120) must equalTo(HandleHere)
      routingTableOfServer.queryServerToHandle(401) must equalTo(HandleHere)
    }
  }
}
