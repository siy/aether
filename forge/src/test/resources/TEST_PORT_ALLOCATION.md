# Forge Test Port Allocation

Each test class has a dedicated port range to avoid conflicts when running tests in parallel.
Tests use per-method port offsets to avoid TIME_WAIT issues between sequential test methods.

**IMPORTANT**: Tests run sequentially (`@Execution(ExecutionMode.SAME_THREAD)`), so port ranges
between classes can overlap. The per-method offsets ensure no conflicts within a class.

## Port Allocation Table

| Test Class                    | Base Port | Base Mgmt Port | Max Offset | Max Cluster Port | Max Mgmt Port | Nodes |
|-------------------------------|-----------|----------------|------------|------------------|---------------|-------|
| ForgeClusterIntegrationTest   | 5050      | 5150           | 15         | 5067             | 5167          | 3     |
| ClusterFormationTest          | 5060      | 5160           | 20         | 5082             | 5182          | 3     |
| SliceDeploymentTest           | 5070      | 5170           | 30         | 5102             | 5202          | 3     |
| SliceInvocationTest           | 5080      | 5180           | 45         | 5127             | 5227          | 3     |
| NodeFailureTest               | 5090      | 5190           | 60         | 5154             | 5254          | 5     |
| BootstrapTest                 | 5110      | 5210           | 40         | 5152             | 5252          | 3     |
| RollingUpdateTest             | 5120      | 5220           | 60         | 5184             | 5284          | 5     |
| ChaosTest                     | 5130      | 5230           | 50         | 5184             | 5284          | 5     |
| MetricsTest                   | 5140      | 5240           | 0          | 5142             | 5242          | 3 (shared) |
| ManagementApiTest             | 5250      | 5350           | 95         | 5347             | 5447          | 3     |
| ControllerTest                | 5260      | 5360           | 35         | 5297             | 5397          | 3     |
| NetworkPartitionTest          | 5270      | 5370           | 40         | 5312             | 5412          | 3     |
| TtmTest                       | 5280      | 5380           | 35         | 5317             | 5417          | 3     |
| GracefulShutdownTest          | 5290      | 5390           | 30         | 5322             | 5422          | 3     |

## Per-Method Offset Pattern

Tests use `TestInfo` to get unique port offsets per test method:

```java
@BeforeEach
void setUp(TestInfo testInfo) {
    int portOffset = getPortOffset(testInfo);
    cluster = forgeCluster(3, BASE_PORT + portOffset, BASE_MGMT_PORT + portOffset, "prefix");
    // ...
}

private int getPortOffset(TestInfo testInfo) {
    return switch (testInfo.getTestMethod().map(m -> m.getName()).orElse("")) {
        case "testMethod1" -> 0;
        case "testMethod2" -> 5;  // 5-port increment for 5-node clusters
        case "testMethod3" -> 10;
        default -> 15;
    };
}
```

## Notes

- **MetricsTest** uses `@BeforeAll`/`@AfterAll` (shared cluster) so no per-method offset needed
- **Port spacing**: Use 5-port increments (enough for 5-node clusters)
- **Sequential execution**: All tests have `@Execution(ExecutionMode.SAME_THREAD)`

## Adding New Tests

When adding a new test class:
1. Find a base port with sufficient range for (num_tests × 5) + num_nodes
2. Add an entry to this table
3. Implement the `getPortOffset()` pattern
4. Use `@Execution(ExecutionMode.SAME_THREAD)` annotation

## Reserved Ranges

- 5400+ / 5500+: Reserved for future tests
