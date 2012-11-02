package sample

import net.renalias.tawebf.core.Framework._
import net.renalias.tawebf.core.Framework.Result

object Application {
  object Controller {
    def index = Action {
      Result("This is the index page")
    }

    def login = Action {
      Result("This is the login page")
    }

    def test = Action[AnyContent] { request =>
      Result("Request body was: " + request.body)
    }
  }

  lazy val app = new FrameworkApplication[AnyContent] {
    val routes = {
      Route("/", Controller.index) orElse
      Route("/login", Controller.login) orElse
      Route("/test", Controller.test)
    }
  }
}
