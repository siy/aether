# MessageRouter Dependency Analysis - Aether Project

## Overview
Analysis of MessageRouter usage in the Aether distributed runtime environment to prepare for the MessageRouter API rework (Epic #1).

## Current Usage Analysis

### 1. Direct Dependencies Found

**File**: `slice/src/main/java/org/pragmatica/aether/cluster/NodeDeploymentManager.java`

**Usage Pattern**:
```java
// Line 8: Import
import org.pragmatica.message.MessageRouter;

// Line 33: Parameter in factory method
static NodeDeploymentManager nodeSliceManager(NodeId self, MessageRouter router, SliceStore sliceStore)

// Lines 54-56: Route registration during initialization
router.addRoute(ValuePut.class, deploymentManager::onValuePut);
router.addRoute(ValueRemove.class, deploymentManager::onValueRemove);
router.addRoute(QuorumStateNotification.class, deploymentManager::onQuorumStateChange);
```

### 2. Usage Pattern Classification

**Pattern Type**: **Configuration-time Route Registration**
- Router passed as constructor dependency
- Routes added during object initialization
- No runtime route modifications
- Clear message-to-handler mapping

**Message Types Handled**:
1. `ValuePut.class` → `deploymentManager::onValuePut`
2. `ValueRemove.class` → `deploymentManager::onValueRemove` 
3. `QuorumStateNotification.class` → `deploymentManager::onQuorumStateChange`

## Impact Assessment

### High Impact
- ✅ **NodeDeploymentManager initialization** - Direct dependency on `MessageRouter.addRoute()`

### Medium Impact
- ⚠️ **Integration with pragmatica-lite** - Depends on MessageRouter from common module
- ⚠️ **Testing approach** - May need different router instance for tests

### Low Impact
- ✅ **Runtime behavior** - No dynamic route changes, only initialization-time setup
- ✅ **Message routing logic** - Uses router as abstraction, implementation details hidden

## Migration Plan

### Phase 1: Preparation (Can start now)
1. **Update aether dependencies** to use new pragmatica-lite version when available
2. **Review NodeDeploymentManager tests** to identify testing patterns

### Phase 2: API Migration (After Core Framework completion)

**For Production Usage** (NodeDeploymentManager):
```java
// OLD (current):
router.addRoute(ValuePut.class, deploymentManager::onValuePut);
router.addRoute(ValueRemove.class, deploymentManager::onValueRemove);
router.addRoute(QuorumStateNotification.class, deploymentManager::onQuorumStateChange);

// NEW (immutable hierarchical builder):
var router = ImmutableMessageRouter
    .from(ValuePut.class).route(deploymentManager::onValuePut)
    .from(ValueRemove.class).route(deploymentManager::onValueRemove)
    .from(QuorumStateNotification.class).route(deploymentManager::onQuorumStateChange)
    .build();

// Factory method signature change:
static NodeDeploymentManager nodeSliceManager(NodeId self, SliceStore sliceStore) {
    // Router created internally instead of passed in
}
```

**For Test Usage** (if applicable):
```java
// NEW (mutable builder for tests):
var testRouter = MutableMessageRouter.builder()
    .addRoute(ValuePut.class, mockHandler)
    .addRoute(ValueRemove.class, mockHandler)
    .build();
```

### Phase 3: Coordination Requirements

**Dependencies**:
- ✅ **Core Framework** completes MessageRouter implementation (Issues #5-#9)
- ✅ **pragmatica-lite** releases new version with updated MessageRouter

**Coordination Points**:
1. **API Design Review** - Ensure hierarchical builder meets aether needs
2. **Testing Strategy** - Align test patterns between projects
3. **Version Coordination** - Update aether dependencies when pragmatica-lite releases RC

## Risk Assessment

### Low Risk ✅
- **Single usage location** - Only one file affected
- **Clear pattern** - Configuration-time setup, no runtime changes
- **Good abstraction** - Code uses interface, not implementation details

### Mitigation Strategies
- **Early testing** - Test migration approach with Core Framework team
- **Gradual rollout** - Can migrate incrementally
- **Backward compatibility** - Core Framework maintaining migration period

## Recommendations

### Immediate Actions
1. **Coordinate with Core Framework** team on hierarchical builder API
2. **Review test patterns** in NodeDeploymentManager
3. **Prepare migration branch** when Core Framework APIs are ready

### Future Considerations
1. **Consider dependency injection** - Could reduce coupling by injecting pre-configured router
2. **Evaluate message patterns** - Opportunity to review aether messaging architecture
3. **Performance testing** - Verify no performance regression with new API

## Summary

**Impact**: ✅ **LOW TO MEDIUM**
- Only 1 file directly affected
- Clear migration path identified
- No complex usage patterns

**Readiness**: ✅ **HIGH**
- Well-understood usage pattern
- Clear migration strategy
- Good coordination with Core Framework team

**Next Steps**: Ready to coordinate implementation timing with Core Framework team.

---
*Analysis by: Distributed Systems Team*
*Date: 2025-08-09*
*Related to: Epic #1 - MessageRouter API Rework*