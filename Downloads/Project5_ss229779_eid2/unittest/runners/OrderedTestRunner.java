package unittest.runners;

import java.util.ArrayList;
import java.lang.reflect.Method;
import unittest.annotations.Order;

public class OrderedTestRunner extends TestRunner {

    public OrderedTestRunner(Class testClass) {
        super(testClass);
        // TODO: complete this constructor
    }

    @Override 
    protected ArrayList<Method> methodSort(ArrayList<Method> methodsToTest) {
        
        ArrayList<Method> orderedMethods = new ArrayList<>();
        ArrayList<Method> unorderedMethods = new ArrayList<>(methodsToTest);

        while (!unorderedMethods.isEmpty()) {
            Method priority = unorderedMethods.get(0);
            int priorityRank;

            if (priority.isAnnotationPresent(Order.class))
                priorityRank = priority.getAnnotation(Order.class).value();
            else
                priorityRank = Integer.MAX_VALUE;

    
            for (int i = 0; i < unorderedMethods.size(); i++) {
                int curRank;
                Method curMethod = unorderedMethods.get(i);

                if (curMethod.isAnnotationPresent(Order.class))
                    curRank = curMethod.getAnnotation(Order.class).value();
                else
                    curRank = Integer.MAX_VALUE;


                if (curRank < priorityRank) {
                    priority = curMethod;
                    priorityRank = curRank;
                }
                    
                else if (curRank == priorityRank) { //default to alphabetical
                    if (curMethod.getName().compareTo(priority.getName()) < 0) {
                        priority = curMethod;
                    }
                }

            }
            orderedMethods.add(priority);
            unorderedMethods.remove(priority);
        }
        return orderedMethods;
    }


    // TODO: Finish implementing this class
}
