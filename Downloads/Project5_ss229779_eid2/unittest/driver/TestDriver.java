package unittest.driver;

import unittest.results.TestClassResult;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import unittest.annotations.Ordered;
import unittest.annotations.Test;
import unittest.runners.TestRunner;
import unittest.runners.FilteredTestRunner;
import unittest.runners.OrderedTestRunner;
import unittest.results.TestClassResult;
import unittest.results.TestMethodResult;


public class TestDriver {

    /**
     * Execute the specified test classes, returning the results of running
     * each test class, in the order given.
     */

    public static List<TestClassResult> runTests(String[] testclasses) {
        // TODO: complete this method
        // We will call this method from our JUnit test cases.
        
        List<TestClassResult> results = new ArrayList<>();
        int testsRun = 0;
        int failures = 0;

        for (String name: testclasses) {
            try {

                TestRunner runTest;

                if (name.contains("#")) {
                    String className = name.split("#")[0];
                    List<String> filteredMethods = Arrays.asList(name.split("#")[1].split(","));
                    Class<?> testClass = Class.forName(className);  // use className not name
                    runTest = new FilteredTestRunner(testClass, filteredMethods);
                }
                else {
                    Class<?> testClass = Class.forName(name);
                    if (testClass.isAnnotationPresent(Ordered.class)) {
                        runTest = new OrderedTestRunner(testClass);
                    }
                    else {
                        runTest = new TestRunner(testClass);
                    }
                }
                results.add(runTest.run());

            } catch (ClassNotFoundException E) {}
        }

        for (TestClassResult curClass : results) {
            List<TestMethodResult> associatedMethods = curClass.getTestMethodResults();
            for (TestMethodResult curMethod : associatedMethods) {
                String status;
                if (curMethod.isPass())
                    status = "PASS";
                else
                    status = "FAIL";
                System.out.println(curClass.getTestClassName() + "." + curMethod.getName() + " : " + status);
            }
        }

        System.out.println("==========");

        System.out.println("FAILURES:");

        for (TestClassResult curClass : results) {
            List<TestMethodResult> associatedMethods = curClass.getTestMethodResults();
            for (TestMethodResult curMethod : associatedMethods) {
                testsRun++;
                if (!curMethod.isPass()) {
                    failures++;
                    System.out.println(curClass.getTestClassName() + "." + curMethod.getName() + ":");
                    curMethod.getException().printStackTrace();
                }
            }
        }

        System.out.println("==========");
        System.out.println("Tests run: " + testsRun + ", Failures: " + failures);
        return results;

    }

    public static void main(String[] args) {
        // Use this for your testing.  We will not be calling this method.
        runTests(args);
    }
}