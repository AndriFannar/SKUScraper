# SKUScraper
A simple Java program that reads links to filter pages from a CSV file, scrapes SKUs from the corresponding web pages, and updates a master CSV file with the new data while avoiding duplicates.

## Features
- Reads filter definitions from `FilterSites.csv` (Group, Filter, URL)
- Scrapes SKUs using JSoup
- Updates `Filters.csv` by adding new SKUs into a date-labeled column
- Automatically merges old and new SKUs without duplicates

## Requirements
- Java 17
- Maven (to build and package)
- Input files:
  - `FilterSites.csv` -> contains filter groups and URLs
  - `Filters.csv` -> contains previously recorded SKUs

## Directory Structure
- Source: [`src/main/java/is/hagi/andri/`](src/main/java/is/hagi/andri/)
- Entry point: `SKUScraper.java`

### Build
To build the project, run the following command in the root directory of the project:
```bash
mvn compile
```

### Run (via Maven)
To run the project, run the following command in the root directory of the project:
```bash
mvn exec:java
```
Ensure `FilterSites.csv` and `Filters.csv` are in the project root directory.

### Package (Fat JAR)
To create a self-contained executable JAR:
```bash
mvn package
```
This will create a fat JAR (which includes all dependencies) in the [`target/`](target/) directory.

You can also use the provided script 
```bash
./createjar.cmd
```

### Run JAR
To run the generated JAR:
```bash
java -jar target/SKUScraper-<version>-shaded.jar
```
Or use the included script:
```bash
./runjar.cmd
```
Ensure `FilterSites.csv` and `Filters.csv` are in the same directory as the JAR when running it.

### License
This project is licensed under the MIT License - see the [LICENSE](LICENSE), and [SPDX](https://spdx.org/licenses/MIT.html)

