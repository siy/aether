# Pragmatica Aether

> **This project has moved to [pragmaticalabs/pragmatica](https://github.com/pragmaticalabs/pragmatica)**
>
> The `aether/` module in the monorepo contains the full Aether distributed runtime.
>
> This repository is archived for historical reference.

## New Location

- **Repository:** https://github.com/pragmaticalabs/pragmatica
- **Module:** `aether/`
- **Website:** https://pragmaticalabs.io/aether
- **Documentation:** https://github.com/pragmaticalabs/pragmatica/tree/main/aether/docs

## What is Aether?

Distributed Java runtime with predictive autoscaling. The third option between monolith and microservices.

- **Predictive autoscaling** - ML-based scaling that anticipates load
- **Zero-downtime updates** - Two-stage deploy/route model
- **Chaos-tested** - 80 E2E tests, survives rolling restarts with ~100% success rate
- **10K req/s** - Stable throughput on single machine

## Why the Move?

Aether is now part of the Pragmatica monorepo alongside:
- `core/` - Pragmatica Lite (Result, Option, Promise)
- `jbct/` - JBCT CLI and Maven plugin
- `integrations/` - Framework integrations

This enables coordinated releases between the runtime, tooling, and core library.

## Demo

Watch Aether survive rolling restarts while handling requests:
https://www.awesomescreenshot.com/video/49014059?key=7c38f783c05577f9df19318ded22b966

## License

Business Source License 1.1 -> Apache 2.0 after 4 years.

