# DAG Docker Sim Java

This workspace is now a Java refactor of the original `D:\dag_docker_sim` prototype.

## What is included

- Java domain model for transactions and lifecycle actions
- Deterministic bootstrap data for `cloud`, `fusion1`, and `fusion2` / `fusion3`
- Core DAG ledger logic:
  - genesis bootstrap
  - register transactions
  - business transactions
  - MCMC-style parent selection
  - ancestor weight accumulation
  - confirm / soft delete / hard delete lifecycle
- In-memory service orchestration for:
  - `CloudStation`
  - `FusionTerminal`
  - `DeviceSimulator`
- A self-check suite that ports the key Python-side behaviors into Java assertions

## Project structure

- `src/App.java`: runnable demo entry
- `src/com/dagdockersim/crypto`: hashing and secp256k1 signing helpers
- `src/com/dagdockersim/model`: transaction model classes
- `src/com/dagdockersim/ledger`: DAG ledger core
- `src/com/dagdockersim/bootstrap`: preload environment and seed data
- `src/com/dagdockersim/service`: cloud / fusion / device orchestration
- `src/com/dagdockersim/demo/LedgerSelfCheck.java`: behavior self-check

## Build

```powershell
javac -d bin (Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName })
```

## Run demo

```powershell
java -cp bin App
```

## Run self-check

```powershell
java -cp bin App --self-check
```

## Notes

- This refactor focuses on the ledger core and service interaction semantics first.
- The original Python HTTP APIs, Docker deployment, plotting, and experiment scripts are not ported yet.
- The Java version is dependency-free and compiles with the current VS Code simple Java project layout.
