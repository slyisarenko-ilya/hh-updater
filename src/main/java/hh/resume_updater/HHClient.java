package hh.resume_updater;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HHClient {
	
	/**
	 * @param args
	 * @throws Exception 
	 */

	public static void main(String[] args) throws Exception {
		 ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		 ResumeUpdater resumeUpdater = new ResumeUpdater();
		 System.out.println("Update resume scheduler started");
		// scheduler.scheduleAtFixedRate(resumeUpdater, 0, 5, TimeUnit.HOURS);
	}
}