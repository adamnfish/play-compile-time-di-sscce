import play.api._
import play.api.ApplicationLoader.Context
import play.filters.HttpFiltersComponents
import router.Routes

class MyApplicationLoader extends ApplicationLoader {
  def load(context: Context) = {
    new MyComponents(context).application
  }
}

class MyComponents(context: Context)
  extends BuiltInComponentsFromContext(context)
    with HttpFiltersComponents
    with controllers.AssetsComponents {
  lazy val applicationController = new controllers.Application(controllerComponents)

  lazy val router: Routes = new Routes(httpErrorHandler, applicationController, assets)
}
