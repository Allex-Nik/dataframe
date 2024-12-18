# Kotlin DataFrame Library Fork: improved toString method

Modified version of the Kotlin DataFrame library. The focus of the modifications is on improving the `toString()` method for better representetion of hierarchical and nested data, commonly found in JSON files.

### What's new
1. Improved `toString()` for nested data

The default method truncates nested JSON structures, leading to loss of clarity in hierarchical data. The new implementstion preserves hierarchical structure by expanding `ColumnGroup` data into multi-level headers with clear alignment.

2. Added a new method `toStringWithParams()` that wraps the internal `renderToString()` method, allowing to pass arguments to the `renderToString()`.

3. Added a method `showDFInSwing()` to display the dataframe in a Swing window (as was suggested in the task).

4. Configured neat borders and alignment for the new implementation.

## Setup and Build

1. Clone the repository
```
https://github.com/Allex-Nik/dataframe.git
```
2. Enable the Kotlin Notebook plugin

3. Open the notebook `assignment.ipynb`
```
examples/notebooks/assignment.ipynb
```
4. Build the project

For this project, 17.0.11 Oracle OpenJDK was used.
```
./gradlew clean build -x test
```

## Getting started

Follow the `assignment.ipynb` notebook showcasing the new implementation with examples and comparisons with the original library.
The repository contains the examples that are necessary to show how the new implementation works, the notebook loads them.

## Examples

1. Input JSON:
```
{
  "region": "North America",
  "countries": {
    "USA": {
      "states": {
        "California": {"population": 39538223, "capital": "Sacramento"},
        "Texas": {"population": 29145505, "capital": "Austin"}
      }
    },
    "Canada": {
      "provinces": {
        "Ontario": {"population": 14734014, "capital": "Toronto"},
        "Quebec": {"population": 8574571, "capital": "Quebec City"}
      }
    }
  }
}
```

Output when using `toString()` in the original implementation:
```
          region                                countries
 0 North America { USA:{ states:{ California:{ populat...
```
    
Output when using `toString()` in the new implementation:
```
          region  countries                                                                            
                        USA                                     Canada                                 
                     states                                  provinces                                 
                 California                 Texas              Ontario               Quebec            
                 population    capital population   capital population   capital population     capital
 0 North America   39538223 Sacramento   29145505    Austin   14734014   Toronto    8574571 Quebec City
```

Output when using `toStringWithParams(borders = true)` in the new implementation:
```
⌌--+--------------+-----------+-----------+-----------+----------+-----------+----------+-----------+------------⌍
 |  |        region|  countries|           |           |          |           |          |           |            |
 |  |              |        USA|           |           |          |     Canada|          |           |            |
 |  |              |     states|           |           |          |  provinces|          |           |            |
 |  |              | California|           |      Texas|          |    Ontario|          |     Quebec|            |
 |  |              | population|    capital| population|   capital| population|   capital| population|     capital|
 |--|--------------|-----------|-----------|-----------|----------|-----------|----------|-----------|------------|
 | 0| North America|   39538223| Sacramento|   29145505|    Austin|   14734014|   Toronto|    8574571| Quebec City|
⌎--+--------------+-----------+-----------+-----------+----------+-----------+----------+-----------+------------⌏
```

2. Input JSON:
```
{
  "university": {
    "name": "Example University",
    "location": {
      "city": "Boston",
      "state": "MA"
    },
    "departments": [
      {
        "name": "Computer Science",
        "courses": [
          {"name": "Algorithms", "credits": 3},
          {"name": "Data Structures", "credits": 4}
        ]
      },
      {
        "name": "Mathematics",
        "courses": [
          {"name": "Calculus", "credits": 4},
          {"name": "Linear Algebra", "credits": 3}
        ]
      }
    ]
  }
}
```

Output when using `toString()` in the original implementation:
```
                                 university
 0 { name:Example University, location:{...
```

Output when using `toString()` in the new implementation:
```
           university                                  
                 name   location            departments
                            city      state            
 0 Example University     Boston         MA     [2 x 2]
```

Output when using `toStringWithParams(borders = true, alignLeft = true, columnTypes = true)` in the new implementation:
```
⌌-----+-------------------+------------+-------------+--------------------------------------------------------------⌍
 |:Int |university         |            |             |                                                              |
 |     |name:String        |location    |             |departments:[name:String, courses:[name:String, credits:Int]] |
 |     |                   |city:String |state:String |                                                              |
 |-----|-------------------|------------|-------------|--------------------------------------------------------------|
 |0    |Example University |Boston      |MA           |[2 x 2]                                                       |
⌎-----+-------------------+------------+-------------+--------------------------------------------------------------⌏
```
