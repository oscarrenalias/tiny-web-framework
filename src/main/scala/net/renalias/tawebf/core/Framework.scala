package net.renalias.tawebf.core

package object Framework {

  /**
   * Base class that represents a simplified HTTP request, which can be typed based
   * the contents of its body
   */
  case class Request[+A](uri: String, body: A)

  /**
   * Base class that represents an HTTP request; ideally headers (like the content type) should be
   * implemented differently but this is enough for now.
   * The Result class is not typed because everything are strings.
   */
  case class Result(body: String, status: Int = 200, contentType: String = "text/plain")

  /**
   * Base abstract trait that represents actions in an MVC framework. Implemented as a function that takes
   * a request and returns a result
   */
  trait Action[A] extends (Request[A] => Result) {
    def apply(request: Request[A]): Result
  }

  /**
   * Type alias that will be used for testing
   */
  type AnyContent = String

  /**
   * This object contains static methods that help generate Action classes.
   */
  object Action {
    /**
     * Generates an Action class given a request parameter and a function passed as a closure
     */
    def apply[A](code: Request[A] => Result):Action[A] = new Action[A] {
      def apply(request: Request[A]) = code(request)
    }

    /**
     * Generates an Action class when no request parameter is required
     */
    def apply(code: => Result):Action[AnyContent] = Action[AnyContent](_ => code)
  }

  /**
   * A route links a URI to a specific action. Routes are defined as partial functions, whereby
   * a route is defined for a specific URI if the route's URI and the given URI are the same and if so,
   * then the action associated to the URI is returned.
   *
   * As they are defined as partial functions which are only defined for a specifc URI/path, they
   * will be composed into a single partial function that is defined for all the inputs of the
   * chained functions using 'orElse'
   */
  case class Route[A](path: String, action: Action[A]) extends PartialFunction[String, Action[A]] {
    /**
     * If the given URI and the route's URI match, then the function is defined
     */
    def isDefinedAt(uri:String) = path == uri

    /**
     * Returns the given action; please be aware that isDefinedAt must be checked before calling this
     * method to make sure that the route can actually process the URI
     */
    def apply(path:String) = action
  }

  /**
   * This abstract trait represents a wired and configured application, which in the current implementation
   * is only a partial function representing chained routes and the default action that is triggered if
   * no route matches
   *
   * TODO: routes should be of type Route[A] but instead it's a ParticalFunction - check why
   */
  trait FrameworkApplication[A] {
    val routes: PartialFunction[String, Action[A]]

    /**
     * This is a default action that will get executed if none of the configured actions matches
     */
    val defaultAction = Action[AnyContent] { request =>
      Result("No matching route was found for the following request: " + request, 500)
    }
  }

  /**
   * This 'runs' an application, given an Application object as well as a request
   *
   * Internally it 'lifts' the partial function with the routes so that it returns None if no
   * route matches (instead of a match error), so that we can execute the default action as configured
   * within the application instead.
   */
  def runApp[A <: AnyContent](app:FrameworkApplication[A], request: Request[A]) =
    app.routes.lift(request.uri).getOrElse(app.defaultAction)(request)
}
