/**
 * 
 */
package com.example;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.jcabi.github.Coordinates;
import com.jcabi.github.Github;
import com.jcabi.github.Issue;
import com.jcabi.github.Repo;
import com.jcabi.github.RtGithub;

/**
 * @author Shikhar
 *
 */
@Path("/issue-dashboard")
public class PublicGitDiagnose {
	


	/*
	 * This method will give you total number of open issues. Parameter URL
	 * Return Total number of issues:String type
	 */

	Map<String, Integer> openedIssues = new HashMap<String, Integer>();
	Map<String, Integer> actualNoOfIssues = new HashMap<String, Integer>();
	int issuesBasedon24Hours = 0;
	int issuesBasedon24HoursAnd7Days = 0;
	int issuesBasedOn7Days = 0;
	int otherCount = 0;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String getCountIssues() throws IOException {

		StringBuilder responseBuilder = new StringBuilder();
		String url = "https://github.com/ntop/ntopng"; // git repository url
		int countIssues = 0; // Number of open issues
		String simpleCoordinator = getSimpleCoordinator(url); // git repository
																// name

		Github github = new RtGithub("ushikhar", "7uf%mpdp6");
		Coordinates coords = new Coordinates.Simple(simpleCoordinator);
		Repo repo = github.repos().get(coords);

		// this loop will count the number of open issues
		for (Issue issues : repo.issues().iterate(Collections.emptyMap())) {
			countIssues++;
		}

		System.out.println("The count is " + countIssues);

		getIssuesOnCondition(repo);

		for (Map.Entry<String, Integer> entry : actualNoOfIssues.entrySet()) {
			System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
			responseBuilder.append("Response ").append("Key = " + entry.getKey() + ", Value = " + entry.getValue());
		}
		return responseBuilder.append("Total Issues are "+countIssues).toString();

	}

	/**
	 * This method will extract the issues based on the condition. Conditions:
	 * a. total number of open issues b. number of open issues that were opened
	 * in the last 24 hours c. number of open issues that were opened more than
	 * 24 hours ago but less than 7 days ago d. Number of open issues that were
	 * opened more than 7 days ago
	 * 
	 * @param repository
	 */

	private void getIssuesOnCondition(Repo repository) {

		// Used this ArrayList to store dates of all opened issues.
		List<Date> issueDateList = new ArrayList<Date>();
		for (Issue issues : repository.issues().iterate(Collections.emptyMap())) {

			// Used a supplementary "smart" decorator to get other properties
			// from an issue.
			Issue.Smart smart = new Issue.Smart(issues);
			Date d = null;
			try {
				d = smart.createdAt();
				issueDateList.add(d);
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		try {
			getTimeDifference(issueDateList);
		} catch (ParseException e) {
			e.printStackTrace();
		}

	}

	/**
	 * @param dateDiff
	 * 
	 *            This method will use map and get the number of days, hours,
	 *            minutes and seconds of an issue and search the days, minutes,
	 *            hours and seconds. Based on the conditions(as key) it will put
	 *            the number of issues(as value) into the map.
	 */
	private void doMappingOnCondition(Map<String, Long> timeMap) {

		if ((timeMap.get("Hours") != 0 || timeMap.get("Minutes") != 0 || timeMap.get("Seconds") != 0)
				&& (timeMap.get("Days") == 0)) {
			issuesBasedon24Hours++;
			actualNoOfIssues.put("24HoursAgo", issuesBasedon24Hours);
		}
		if (timeMap.get("Days") >= 1 && timeMap.get("Days") < 7) {
			issuesBasedon24HoursAnd7Days++;
			actualNoOfIssues.put("7DaysAgo", issuesBasedon24HoursAnd7Days);
		}
		if (timeMap.get("Days") > 7) {
			issuesBasedOn7Days++;
			actualNoOfIssues.put("MoreThan7Days", issuesBasedOn7Days);
		}

	}

	/**
	 * This method will use HashMap to store the number of days, hours, minutes
	 * and seconds of an issue from now. While loop will be used to get the
	 * issue date and here we will calculate the time difference.
	 * 
	 * @param ArrayList
	 *            issueCreatedDateList- contains list of opened issues dates.
	 * 
	 * @throws ParseException
	 */
	private void getTimeDifference(List<Date> issueCreatedDateList) throws ParseException {
		Map<String, Long> timeMap = new HashMap<String, Long>();
		System.out.println("in the getTimeDifference");
		Iterator<Date> iterator = issueCreatedDateList.iterator();
		Date d = null;
		while (iterator.hasNext()) {
			d = iterator.next();
			SimpleDateFormat sdfIssue = new SimpleDateFormat("EE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
			String issueDate = sdfIssue.format(d);
			Date date = sdfIssue.parse(issueDate);

			SimpleDateFormat print = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			issueDate = print.format(date);

			SimpleDateFormat sdfCurrent = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			Date currentDate = new Date();
			String currentDateString = sdfCurrent.format(currentDate);

			SimpleDateFormat dateDifferenceFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

			Date oldIssueDate = null;
			Date presentDate = null;

			oldIssueDate = dateDifferenceFormat.parse(issueDate);
			presentDate = dateDifferenceFormat.parse(currentDateString);

			long diffInMilliSeconds = presentDate.getTime() - oldIssueDate.getTime();

			long seconds = diffInMilliSeconds / 1000 % 60;
			long minutes = diffInMilliSeconds / (60 * 1000) % 60;
			long hours = diffInMilliSeconds / (60 * 60 * 1000) % 24;
			long days = diffInMilliSeconds / (24 * 60 * 60 * 1000);

			timeMap.put("Days", days);
			timeMap.put("Hours", hours);
			timeMap.put("Minutes", minutes);
			timeMap.put("Seconds", seconds);
			doMappingOnCondition(timeMap);
		}

	}

	/*
	 * This method will extract the git repository from public URL. The
	 * combination of username and repository will be denoted by coordinator
	 * here.
	 * 
	 */
	public String getSimpleCoordinator(String url) {

		String pattern = "https://github.com/";
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(url);
		System.out.println(m.find());
		String toDelete = m.group(0);
		String coordinator = url.replace(toDelete, "");
		System.out.println("The Coordinator Simple is " + coordinator);
		return coordinator;
	}



}
