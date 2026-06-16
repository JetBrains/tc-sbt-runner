# Test Framework
Tests in this module use TestNG.

TeamCity's `BaseTestCase` test fixture is TestNG-first: its setup, teardown, suite initialization, and after-test hook behavior are wired through TestNG annotations and interfaces. 
Keeping this module on TestNG preserves that lifecycle without per-test bridging code.

Do not add JUnit tests to this module. 
The previous mixed setup used a hand-maintained `testng.xml` suite, and CI executed only the TestNG tests listed there; JUnit-annotated tests were skipped. 
Maven now uses Surefire's TestNG provider and standard `*Test` discovery, so new TestNG test classes under `src/test/java` are picked up automatically.
