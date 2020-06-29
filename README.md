# QueryableState CorDapp

This CorDapp replicates a pagination bug while using the vault query API in Corda. Queries that return results below the default pagination limit also produce a pagination error.

## Usage

### Pre-requisites:
See https://docs.corda.net/getting-set-up.html.

### Running the CorDapp Test

Open a terminal and go to the project root directory and type: (to build and run the test)
```
./gradlew clean test --continue
```
