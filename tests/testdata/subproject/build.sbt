lazy val backend = project.in(file("backend"))
lazy val backendWE = project.in(file("backendWE"))

lazy val root = project.in(file(".")).aggregate(backend).aggregate(backendWE)


