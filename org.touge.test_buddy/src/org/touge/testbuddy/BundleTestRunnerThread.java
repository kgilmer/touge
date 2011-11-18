package org.touge.testbuddy;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.Test;
import junit.framework.TestFailure;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

/**
 * @author kgilmer
 *
 */
public class BundleTestRunnerThread extends Thread {
	private final BundleContext context;

	private final File outputDir;
	private boolean errorOccurred = false;
	private final long shutdownTimeout;
	private final LogService log;
	private final long startupTimeout;

	private final boolean verboseOutput;

	private final boolean keepRunning;

	/**
	 * @param context
	 * @param outputDir
	 * @param startupTimeout
	 * @param shutdownTimeout
	 * @param log
	 * @param verboseOutput
	 * @param keepRunning
	 */
	public BundleTestRunnerThread(BundleContext context, File outputDir, long startupTimeout, long shutdownTimeout, LogService log, boolean verboseOutput, boolean keepRunning) {
		this.context = context;
		this.outputDir = outputDir;
		this.startupTimeout = startupTimeout;
		this.shutdownTimeout = shutdownTimeout;
		this.log = log;
		this.verboseOutput = verboseOutput;
		this.keepRunning = keepRunning;
	}

	@Override
	public void run() {
		try {
			log.log(LogService.LOG_INFO, "Waiting " + startupTimeout + " millis for OSGi instance to settle...");
			Thread.sleep(startupTimeout);

			if (verboseOutput) {
				printSystemProperties();
			}

			if (!OSGiUtil.onServices(context, TestSuite.class.getName(), null, new OSGiUtil.ServiceVisitor<TestSuite>() {
			
				@Override
				public void apply(ServiceReference sr, TestSuite service) {
					
					try {
						runTest(service);
					} catch (Exception e) {
						log.log(LogService.LOG_INFO, "An error occurred while running a test.", e);
					}
				}

			})) {
				System.out.println("No " + TestSuite.class.getName() + " tests were found in the service registry.");
			}

			Thread.sleep(shutdownTimeout);
		} catch (InterruptedException e) {
			return;
		}

		if (!keepRunning) {
			// Shutdown all the bundles
			OSGiUtil.onBundles(context, new OSGiUtil.BundleVisitor() {

				@Override
				public void apply(Bundle bundle) {
					if (bundle.getBundleId() != 0) {
						try {
							bundle.stop();
						} catch (Exception e) {
							// Ignore errors
						}
					}
				}
			});

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}

			// Test execution complete, now forcibly shutdown the JVM.
			if (errorOccurred)
				System.exit(1);
			else
				System.exit(0);
		}
	}

	/**
	 * Print system properties to log.
	 */
	private void printSystemProperties() {
		log.log(LogService.LOG_INFO, "System properties:");
		for (Map.Entry<Object, Object> entry : System.getProperties().entrySet())
			log.log(LogService.LOG_INFO, "Property: " + entry.getKey().toString() + "  Value: " + entry.getValue());
	}

	protected void runTest(TestSuite tc) throws IOException {
		XmlNode out = new XmlNode("testsuite");
		writeProperties(out);
		ByteArrayOutputStream outbuf = new ByteArrayOutputStream();
		TestRunner tr = new TestRunner(new PrintStream(outbuf));
		log.log(LogService.LOG_INFO, "Running Test Suite: " + tc.getName());

		long time = System.currentTimeMillis();
		TestResult result = tr.doRun(tc);
		time = (System.currentTimeMillis() - time) / 1000;

		Enumeration<Test> te = tc.tests();
		while (te.hasMoreElements()) {
			Test test = te.nextElement();

			XmlNode testNode = new XmlNode(out, "testcase").addAttribute("classname", tc.getName()).addAttribute("name", test.toString()).addAttribute("time", "" + time);

			for (Enumeration<TestFailure> fenum = result.failures(); fenum.hasMoreElements();) {
				TestFailure f = fenum.nextElement();
				testNode.addChild(new XmlNode("failure", f.trace()).addAttribute("type", f.exceptionMessage()));
			}

			for (Enumeration<TestFailure> fenum = result.errors(); fenum.hasMoreElements();) {
				TestFailure f = fenum.nextElement();
				testNode.addChild(new XmlNode("error", f.trace()).addAttribute("type", f.exceptionMessage()));
			}
		}

		out.addAttribute("errors", "" + result.errorCount());
		out.addAttribute("failures", "" + result.failureCount());

		out.addAttribute("hostname", getHostName());
		out.addAttribute("name", tc.getName());
		out.addAttribute("tests", "" + tc.testCount());
		out.addAttribute("time", "" + time);
		out.addAttribute("timestamp", getDateStamp());

		out.addChild(new XmlNode("system-out", outbuf.toString()));

		File outFile = new File(outputDir, "TEST-" + tc.getName() + ".xml");
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outFile));
		bos.write(out.toString().getBytes());
		bos.flush();
		bos.close();

		log.log(LogService.LOG_INFO, "Test Suite Complete: " + tc.getName());
		log.log(LogService.LOG_INFO, "Results ~ Errors: " + result.errorCount() + " Failures: " + result.failureCount());

		if (result.errorCount() > 0 || result.failureCount() > 0)
			errorOccurred = true;
	}

	private String getDateStamp() {
		Calendar cal = Calendar.getInstance();
		// 2011-05-11T06:20:03
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		return sdf.format(cal.getTime());
	}

	/**
	 * @return
	 */
	private String getHostName() {
		return System.getProperty("os.name") + 
			System.getProperty("os.version");
	}

	private void writeProperties(XmlNode out) {
		for (Object key : System.getProperties().keySet())
			out.addChild(new XmlNode("property").addAttribute("name", key.toString()).addAttribute("value", System.getProperty(key.toString())));
	}
	
	public static void main(String[] args) {
		for (Entry<Object, Object> e : System.getProperties().entrySet()) 
			System.out.println(e.getKey().toString() + " : " + e.getValue().toString());
	}
}
