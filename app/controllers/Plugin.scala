package controllers

import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import play.api._
import play.api.mvc._

// FIXME: Move GamePlugin and its related classes, traits and objects to beyond package.
private trait GamePlugin {
  // FIXME: Pass request parameters along with path and
  // make it possible to return the result asynchronously.
  def handle(path: String): String
}

// FIXME: Handle script errors.
private object GamePlugin {
  def apply(): GamePlugin = {
    // FIXME: Don't hardcode plugin source code here.
    val source = "function handle(path) { return 'Hello ' + path; }"
    val cx: Context = Context.enter()
    try {
      val scope: Scriptable = cx.initStandardObjects()
      cx.evaluateString(scope, source, "source", 1, null)
      // FIXME: Don't hardcode the name of handler function.
      // FIXME: handler might be Scriptable.NOT_FOUND if there is no function named "handle".
      // Also, it might not be an instance of Function.
      val handler = scope.get("handle", scope).asInstanceOf[Function]
      new GamePluginImpl(scope, handler)
    } finally {
      Context.exit()
    }
  }

  private class GamePluginImpl(scope: Scriptable, handler: Function) extends GamePlugin {
    def handle(path: String): String = {
      val cx: Context = Context.enter()
      try {
        val args: Array[AnyRef] = Array(path)
        val result = handler.call(cx, scope, scope, args)
        result.toString
      } finally {
        Context.exit()
      }
    }
  }
}

object Plugin extends Controller {
  private val gamePlugin: GamePlugin = GamePlugin()

  def route(path: String) : Action[AnyContent] = Action { request =>
    val result = gamePlugin.handle(request.path)
    Ok(result)
  }
}

