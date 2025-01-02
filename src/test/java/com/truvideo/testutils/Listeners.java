package com.truvideo.testutils;

import com.microsoft.playwright.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;

import java.lang.reflect.Method;

import static com.truvideo.factory.PlaywrightFactory.getBrowserContext;
import static com.truvideo.factory.PlaywrightFactory.getPage;
import static com.truvideo.testutils.JiraTestCaseUtils.attachJiraTestId;


public class Listeners extends TestUtils implements ITestListener {
    public Logger logger = LogManager.getLogger(this.getClass().getName());
    private static final ThreadLocal<Page> threadLocalPage = new ThreadLocal<>();
    private static final ThreadLocal<ExtentTest> threadLocalTest = new ThreadLocal<>();
    private static final ThreadLocal<String> threadLocalTracePath = new ThreadLocal<>();

    ExtentReports extent = TestUtils.getReporterObject();

    @Override
    public void onTestStart(ITestResult result) {
        getBrowserContext().tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true));
        Method method = result.getMethod().getConstructorOrMethod().getMethod();
        String methodName = result.getMethod().getMethodName();
        ExtentTest test = extent.createTest(methodName);
        threadLocalTest.set(test);
        logger.info("Test Execution started for test case : {}", methodName);
        String tracePath = "./Reports/traces/" + methodName + ".zip";
        threadLocalTracePath.set(tracePath);
        attachJiraTestId(method, test);
        Page page = getPage();
        threadLocalPage.set(page);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        handleTestCompletion(result, true);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        handleTestCompletion(result, false);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTest test = threadLocalTest.get();
        Page page = threadLocalPage.get();
        String methodName = result.getMethod().getMethodName();
        attachScreenshotToReport(test, methodName, page);
		test.skip(result.getThrowable());
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {

    }

    @Override
    public void onStart(ITestContext context) {

    }

    @Override
    public void onFinish(ITestContext context) {
        extent.flush();
    }

    // Handle test completion for success or failure
    private void handleTestCompletion(ITestResult result, boolean isSuccess) {
        String methodName = result.getMethod().getMethodName();
        try {
            Page page = threadLocalPage.get();
            ExtentTest test = threadLocalTest.get();
            if (page != null && test != null) {
                attachScreenshotToReport(test, methodName, page);
                attachTrace(test, methodName);
                //attachVideo(test,methodName);
                if (!isSuccess) {
                    test.fail(result.getThrowable());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            stopTracing();
            threadLocalPage.remove();
            threadLocalTest.remove();
            threadLocalTracePath.remove();
        }
    }

    private void stopTracing() {
        try {
            if (getBrowserContext().tracing() != null) {
                getBrowserContext().tracing().stop();
            }
        } catch (Exception e) {
            System.err.println("Error stopping tracing: " + e.getMessage());
        }
    }


}
