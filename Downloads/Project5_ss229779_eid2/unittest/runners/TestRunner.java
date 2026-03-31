package unittest.runners;

import unittest.listeners.TestListener;
import unittest.results.TestClassResult;
import java.util.ArrayList;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import unittest.annotations.Test;
import unittest.assertions.AssertionException;
import unittest.results.TestMethodResult;
import java.util.Collections;
import java.util.List;


//We used this documentation for reference 
// https://docs.oracle.com/javase/8/docs/api/java/lang/reflect/Method.html#getParameterCount--
public class TestRunner {

    private Class <?> testClass;
    private List<TestListener> listeners = new ArrayList<>();
    public TestRunner(Class testClass) {
        // TODO: complete this constructor
        this.testClass = testClass;
    }


    protected ArrayList<Method> methodSort(ArrayList<Method> methodsToTest) {

        //sorting process
        ArrayList<String> allNames = new ArrayList<>();
        for (Method M : methodsToTest) {
            allNames.add(M.getName());
        }

        //sort the name array
        Collections.sort(allNames);
        ArrayList<Method> temp = new ArrayList<>();

        //build sorted method array
        for (String name: allNames) {
            for (Method M : methodsToTest) {
                if (name.equals(M.getName()))
                    temp.add(M);
            }
        }

        return temp;
    }

    protected ArrayList<Method> getMethods() {
        Method[] methods = testClass.getMethods(); //get all public methods
        ArrayList<Method> methodsToTest = new ArrayList<>();

        for (int i = 0; i < methods.length; i++) {
            if (methods[i].isAnnotationPresent(Test.class)) { //inside annotations folder
                if (methods[i].getReturnType().equals(void.class)) { //ensure no return type
                    if (methods[i].getParameterCount() == 0) { //ensure no parameters
                        methodsToTest.add(methods[i]);
                    }
                }
            }
        }
            return methodsToTest;
    }

    public TestClassResult run() {
        // TODO: complete this method

        ArrayList<Method> methodsToTest = getMethods();
        methodsToTest = methodSort(methodsToTest);
        

        TestClassResult results = new TestClassResult(testClass.getName());

        for (Method M: methodsToTest) {// 1. Notify listeners that the test is starting
            for (TestListener listener : listeners) {
                listener.testStarted(M.getName());
         }

        try {
            Constructor<?> constructor = testClass.getConstructor(); 
            Object testMethodInstance = constructor.newInstance(); 
            M.invoke(testMethodInstance);

            TestMethodResult passResult = new TestMethodResult(M.getName(), true, null);
            results.addTestMethodResult(passResult); 

            // Notify listeners of success
            for (TestListener listener : listeners) {
                listener.testSucceeded(passResult);
            }
        }
        catch (InvocationTargetException E) { 
            TestMethodResult failResult = new TestMethodResult(M.getName(), false, (AssertionException) E.getCause());
            results.addTestMethodResult(failResult); 

            // Notify listeners of failure
            for (TestListener listener : listeners) {
                listener.testFailed(failResult);
            }
        }
        catch (Exception E) {
                // Ignore other exceptions
            }
        }
        return results;
    }


    public void addListener(TestListener listener) {
        if (listener != null) {
            this.listeners.add(listener);
        }
    }
}
