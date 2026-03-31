package unittest.runners;

import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Method;

public class FilteredTestRunner extends TestRunner {

    List<String> testMethods;

    public FilteredTestRunner(Class testClass, List<String> testMethods) {
        super(testClass);
        // TODO: complete this constructor
        this.testMethods = testMethods;
    }

    @Override
    protected ArrayList<Method> methodSort(ArrayList<Method> methodsToTest) { //as per lab doc --> "Run the test methods in the order in which they are specified."
    return methodsToTest; //override to avoid alpha sorting
    }

     protected ArrayList<Method> getMethods() {
        ArrayList<Method> methods = super.getMethods();
        ArrayList<Method> methodsToTest = new ArrayList<>();

        for (String S: testMethods) {
            for (Method M: methods) {
                if (M.getName().equals(S)) {
                    methodsToTest.add(M);
                    break;
                }
            }
        }
        return methodsToTest;
     }

    // TODO: Finish implementing this class
}
