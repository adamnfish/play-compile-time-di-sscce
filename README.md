Compile-time DI SSCCE
=====================

A [SSCCE](http://www.sscce.org/) that demonstrates that following the documented steps for setting up compile-time DI for a Scala Play application does not work.

## Context

This application retraces the steps of someone setting up a new project using the Play framework template, and then following the instructions to switch it to use compile-time dependency injection.

### [29e375c](https://github.com/adamnfish/play-compile-time-di-sscce/commit/29e375c618cfa52820880d3e887ec0260513abf4)

After the initial commit, we check in the project that was generated by `sbt new playframework/play-scala-seed.g8`.

At this point the application uses standard runtime dependency injection, and works perfectly.

### [43912f7](https://github.com/adamnfish/play-compile-time-di-sscce/commit/43912f7347146766e406add5ea73d36cb75b5e21)

Next, we change the default program to better match the documented approach to using compile-time DI.
This is a simple change, which renames `HomeController` to `Application`.

**Note:** I decided not to also add the sub-routes at `bar.Routes`, to better focus on the problem.
This means we'll have to slightly tweak the next step, instead of being able to copy/paste it out the documentation.

### [0a0f06d](https://github.com/adamnfish/play-compile-time-di-sscce/commit/0a0f06d99d0535c9fae21371d645c5ff036341e5)

This is the key change. We take the definition of the app loader and app components classes that is demonstrated in the
[providing a router](https://www.playframework.com/documentation/3.0.x/ScalaCompileTimeDependencyInjection#Providing-a-router)
section of the compile-time DI documentation.

As mentioned in the note above, this isn't exactly copy-pasted because we've omitted the nested `bar` route for clarity.
In all other respects this is exactly as documented.

The source for the documented code block is [here in Play's source code](https://github.com/playframework/playframework/blob/b1f5470a2d1f0fc9a45db8de4d381b5dc1b1a01b/documentation/manual/working/scalaGuide/main/dependencyinjection/code/CompileTimeDependencyInjection.scala#L105-L126).

The application at this state demonstrates the problem.

## Reproducing the issue

You can compile this project to see the problem, but here is the compile error:

```
[compile-time-di-sscce] $ compile
[info] compiling 2 Scala sources to .../compile-time-di-sscce/target/scala-2.13/classes ...
[error] .../compile-time-di-sscce/app/AppLoader.scala:15:22: type AssetsComponents is not a member of package play.api.controllers
[error]     with controllers.AssetsComponents {
[error]                      ^
[error] .../compile-time-di-sscce/app/AppLoader.scala:16:52: type Application is not a member of package play.api.controllers
[error]   lazy val applicationController = new controllers.Application(controllerComponents)
[error]                                                    ^
[error] .../compile-time-di-sscce/app/AppLoader.scala:18:81: not found: value assets
[error]   lazy val router: Routes = new Routes(httpErrorHandler, applicationController, assets)
[error]                                                                                 ^
[error] three errors found
[error] (Compile / compileIncremental) Compilation failed
[error] Total time: 0 s, completed 21 Aug 2024, 11:33:26
```

## The fix

The fix is simple once you find it, but it is not easy to discover the right approach.
Here is the PR that demonstrates the fix, and a duplicate of the PR's description.

### [Pull Request that fixes the issue (#1)](https://github.com/adamnfish/play-compile-time-di-sscce/pull/1)

Removing the wildcard import and following the import errors resolves the issue.

It isn't obvious that this is the solution. Following the compile errors present in the documented approach leads to a deep and confusing rabbit hole of incompatible imports.

The main area of trouble is the controllers package, which has at least three of versions.
The application controller is in the controllers package in this application, the AssetsComponents mixin comes from another, and an incompatible AssetsComponents is available under play.controllers.
The last of these is how IntelliJ tries to fix the problem, which leads to another rabbit hole of discovering why there are incompatible members in the class (see below - this error is a DevX disaster even for an experienced Scala / Play user).

```
[error] override def configuration: play.api.Configuration (defined in trait ContextBasedBuiltInComponents)
[error]   with <defaultmethod> def configuration(): play.api.Configuration (defined in trait ConfigurationComponents);
[error]  other members with override errors are: environment, applicationLifecycle, httpErrorHandler, fileMimeTypes
[error] class MyComponents(context: Context)
```
