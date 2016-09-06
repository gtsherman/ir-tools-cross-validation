# Cross Validation

Cross validation framework for the ir-tools library.

## Adding to your project

This is not currently published anywhere. To use it, you will have to install it to your local maven repository:
```
git clone https://github.com/gtsherman/cross-validation.git
cd cross-validation
mvn install
```
You can then include it as a dependency in your project by adding the following to your `pom.xml` file:
```
<dependency>
  <groupId>edu.gslis</groupId>
  <artifactId>cross-validation</artifactId>
  <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## Using

See the `RM3CrossValidationDemo.java` file for an example usage of the framework. Each run requires three things: a `Validator` to control the type of cross validation, an `Evaluator` to set the target metric, and a `QueryRunner` to handle parameter sweeps and actual running of the experiment.

In general, the `Validator` requirement is satisfied by the `KFoldValidator` class. Currently, MAP and nDCG `Evaluator` classes exist; other target metrics can be created in the same style.

Runner classes contain experiment code and will be the main entry point into the framework. Create classes that implement `QueryRunner` to define experiment procedure, then pass this class, the chosen `Evaluator`, and relevance judgments to a `Validator` to execute cross validation.
