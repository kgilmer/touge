package org.touge.testbuddy;

import java.io.File;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.log.LogService;

/**
 * A test runner for OSGi-contexted JUnit tests.
 * 
 * To expose tests to this runner, register the tests as such:
 * 
 * <code>BundleContext.registerService(TestSuite.class.getName(), new TestSuite(<Test Case Name>.class), null); </code>
 * 
 * In runtime, two console commands become available: tlist and trun
 * 
 * <code>tlist</code> is used to list available test cases that the tester can see.
 * <code>trun</code> is used to execute all or a specific test case.
 * 
 * @author kgilmer
 *
 */
public class Activator implements BundleActivator {
	private static final String JUNIT_REPORT_DIR = "testrunner.report.dir";
	private static final String SHUTDOWN_DELAY_MILLIS = "testrunner.shutdown.delay";
	private static final String STARTUP_DELAY_MILLIS = "testrunner.startup.delay";
	private static final String VERBOSE_OUTPUT = "testrunner.output.verbose";
	private static final String KEEP_RUNNING = "testrunner.keeprunning";
	private static final int DEFAULT_SHUTDOWN_TIMEOUT = 10000;
	private static final long SETTLE_MILLIS = 5000;

	@Override
	public void start(final BundleContext context) throws Exception {
		LogService log = LogServiceUtil.getLogService(context);
		File outputDir = context.getDataFile("temp").getParentFile();
		
		if (context.getProperty(JUNIT_REPORT_DIR) != null) {
			outputDir = new File(context.getProperty(JUNIT_REPORT_DIR));
			
			if (outputDir.isFile())
				throw new BundleException("Unable to start tester, " + JUNIT_REPORT_DIR + " is set to an existing file, needs to be a directory.");
			
			if (!outputDir.exists())
				if (!outputDir.mkdirs())
					throw new BundleException("Unable to start tester, unable to create directory " + JUNIT_REPORT_DIR);
		}
		log.log(LogService.LOG_INFO, Activator.class.getName() + "  Test report output directory: " + outputDir);
		
		long startupTimeout = OSGiUtil.getProperty(context, STARTUP_DELAY_MILLIS, SETTLE_MILLIS);
		long shutdownTimeout = OSGiUtil.getProperty(context, SHUTDOWN_DELAY_MILLIS, DEFAULT_SHUTDOWN_TIMEOUT);		
		boolean verbose = OSGiUtil.getProperty(context, VERBOSE_OUTPUT, true);
		boolean keepRunning = OSGiUtil.getProperty(context, KEEP_RUNNING, false);
		
		BundleTestRunnerThread thread = new BundleTestRunnerThread(context, outputDir, startupTimeout, shutdownTimeout, log, verbose, keepRunning);
		thread.start();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		
	}
}