# Forge Test Port Allocation

Each test class has a dedicated port range to avoid conflicts when running tests in parallel.
Each range provides 10 ports (5 cluster + 5 management) to support up to 5 nodes per test.

## Port Allocation Table

| Test Class                    | Base Port | Base Mgmt Port | Cluster Ports | Mgmt Ports  |
|-------------------------------|-----------|----------------|---------------|-------------|
| ForgeClusterIntegrationTest   | 5050      | 5150           | 5050-5054     | 5150-5154   |
| ClusterFormationTest          | 5060      | 5160           | 5060-5064     | 5160-5164   |
| SliceDeploymentTest           | 5070      | 5170           | 5070-5074     | 5170-5174   |
| SliceInvocationTest           | 5080      | 5180           | 5080-5084     | 5180-5184   |
| NodeFailureTest               | 5090      | 5190           | 5090-5094     | 5190-5194   |
| GracefulShutdownTest          | 5100      | 5200           | 5100-5104     | 5200-5204   |
| BootstrapTest                 | 5110      | 5210           | 5110-5114     | 5210-5214   |
| RollingUpdateTest             | 5120      | 5220           | 5120-5124     | 5220-5224   |
| ChaosTest                     | 5130      | 5230           | 5130-5134     | 5230-5234   |
| MetricsTest                   | 5140      | 5240           | 5140-5144     | 5240-5244   |
| ManagementApiTest             | 5250      | 5350           | 5250-5254     | 5350-5354   |
| ControllerTest                | 5260      | 5360           | 5260-5264     | 5360-5364   |
| NetworkPartitionTest          | 5270      | 5370           | 5270-5274     | 5370-5374   |
| TtmTest                       | 5280      | 5380           | 5280-5284     | 5380-5384   |

## Usage

In each test class, initialize ForgeCluster with the assigned ports:

```java
// Example for ClusterFormationTest
private static final int BASE_PORT = 5060;
private static final int BASE_MGMT_PORT = 5160;

@BeforeEach
void setUp() {
    cluster = ForgeCluster.forgeCluster(3, BASE_PORT, BASE_MGMT_PORT);
}
```

## Adding New Tests

When adding a new test class:
1. Find the next available port range (increment by 10 from the last used)
2. Add an entry to this table
3. Use the allocated ports in the test class

## Reserved Ranges

- 5050-5059 / 5150-5159: ForgeClusterIntegrationTest (default when not specified)
- 5290+ / 5390+: Reserved for future tests
