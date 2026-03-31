package unittest.gui;

import unittest.annotations.Ordered;
import unittest.annotations.Test;
import unittest.listeners.GUITestListener;
import unittest.results.TestClassResult;
import unittest.results.TestMethodResult;
import unittest.runners.OrderedTestRunner;
import unittest.runners.TestRunner;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class TestController {

    private TestGUI view; // reference to the GUI to update it

    public TestController(TestGUI view) {
        this.view = view;
    }

    // finds all test classes under the given path
    public List<String> findTestClasses(String basePath) {
        List<String> found = new ArrayList<>();
        File rootDir = new File(basePath);

        if (!rootDir.exists() || !rootDir.isDirectory()) {
            return found; // empty list
        }

        searchDirectory(rootDir, rootDir.getAbsolutePath(), found);
        return found;
    }

    private void searchDirectory(File dir, String baseDirPath, List<String> found) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                searchDirectory(file, baseDirPath, found);
            } else if (file.getName().endsWith(".class")) {
                String relativePath = file.getAbsolutePath()
                        .substring(baseDirPath.length() + 1);
                String className = relativePath
                        .replace(File.separatorChar, '.')
                        .replace(".class", "");

                try {
                    Class<?> testClass = Class.forName(className);
                    if (hasTestMethods(testClass)) {
                        found.add(className);
                    }
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                }
            }
        }
    }

    private boolean hasTestMethods(Class<?> testClass) {
        for (Method m : testClass.getMethods()) {
            if (m.isAnnotationPresent(Test.class)) {
                return true;
            }
        }
        return false;
    }

    // runs the selected test classes and reports to the GUI via listener
    public void runTests(List<String> classesToRun) {
        new Thread(() -> {
            int totalRun = 0;
            int totalFailures = 0;
            List<TestClassResult> allResults = new ArrayList<>();

            GUITestListener listener = new GUITestListener(view);

            for (String className : classesToRun) {
                try {
                    Class<?> testClass = Class.forName(className);

                    listener.setCurrentClass(className);

                    TestRunner runner;
                    if (testClass.isAnnotationPresent(Ordered.class)) {
                        runner = new OrderedTestRunner(testClass);
                    } else {
                        runner = new TestRunner(testClass);
                    }

                    runner.addListener(listener);
                    TestClassResult result = runner.run();
                    allResults.add(result);

                } catch (ClassNotFoundException e) {
                    view.addMessage("Could not load class: " + className);
                }
            }

            for (TestClassResult classResult : allResults) {
                for (TestMethodResult methodResult : classResult.getTestMethodResults()) {
                    totalRun++;
                    if (!methodResult.isPass()) totalFailures++;
                }
            }

            final int fRun = totalRun;
            final int fFail = totalFailures;

            view.showSummary(allResults, fRun, fFail);

        }).start();
    }
}