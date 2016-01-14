package sopra.grenoble.jiraLoader.unittests.jira.dao.project.impl;

import static org.junit.Assert.*;

import java.net.URISyntaxException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.atlassian.jira.rest.client.domain.BasicIssue;
import com.atlassian.jira.rest.client.domain.Version;

import sopra.grenoble.jiraLoader.ApplicationConfiguration;
import sopra.grenoble.jiraLoader.exceptions.IssueNotFoundException;
import sopra.grenoble.jiraLoader.exceptions.JiraEpicNotFound;
import sopra.grenoble.jiraLoader.exceptions.JiraGeneralException;
import sopra.grenoble.jiraLoader.exceptions.VersionNotFoundException;
import sopra.grenoble.jiraLoader.jira.connection.IJiraRestClientV2;
import sopra.grenoble.jiraLoader.jira.dao.metadatas.JiraFieldLoader;
import sopra.grenoble.jiraLoader.jira.dao.metadatas.JiraIssuesTypeLoader;
import sopra.grenoble.jiraLoader.jira.dao.metadatas.JiraPriorityLoader;
import sopra.grenoble.jiraLoader.jira.dao.project.IIssueEpicService;
import sopra.grenoble.jiraLoader.jira.dao.project.IIssueService;
import sopra.grenoble.jiraLoader.jira.dao.project.IVersionService;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(ApplicationConfiguration.class)
public class IssueServiceTest {

	@Autowired
	private IIssueService issueSrv;
	
	@Autowired 
	private IIssueEpicService epicSrv;
	
	@Value("${default.project.name}")
	private String projectTestName;
	
	@Value("${test.component.name}")
	private String componentName;
	
	@Value("${test.OPAL_ODYSEE.version.name}")
	private String versionName;
	
	@Autowired private JiraIssuesTypeLoader issueTypeLoader;
	@Autowired private JiraFieldLoader fieldLoader;
	@Autowired private IVersionService vService;
	@Autowired private JiraPriorityLoader priorityLoader;
	@Autowired private IJiraRestClientV2 jiraConnection;
	private static boolean elementLoaded = false;
	private static Version v;
	private static BasicIssue epicIssue;
	
	@Before
	public void initJira() throws JiraGeneralException, URISyntaxException {
		if (!elementLoaded) {
			jiraConnection.openConnection();
			issueTypeLoader.loadElements();
			fieldLoader.loadElements();
			priorityLoader.loadElements();
			v = vService.getVersion(projectTestName, versionName);
			assertNotNull(v);
			elementLoaded = true;
		}
		//creating epic
		epicIssue = epicSrv.createEpic(projectTestName, "EpicTestSummary", componentName);
	}
	
	@After
	public void cleanJira() throws VersionNotFoundException, JiraGeneralException {
		epicSrv.removeIssue(epicIssue.getKey(), true);
	}
		
	
	@Test
	public void createStoryAndDeleteIssue() throws JiraGeneralException {
		BasicIssue bi = issueSrv.createStory(projectTestName, null, null, "resume", "description", "urgent", componentName);
		assertNotNull(bi);
		issueSrv.removeIssue(bi.getKey(), true);
	}
	
	@Test
	public void createStoryAndSubTaskAndDeleteAll() throws JiraGeneralException {
		BasicIssue bi = issueSrv.createStory(projectTestName, null, null, "resume", "description", "urgent", componentName);
		assertNotNull(bi);
		//Create sub task
		try {
			BasicIssue subTask = issueSrv.createSubTask(projectTestName, bi.getKey(), "Sous-tâche", "sous tach resume", "sous tach desc", "urgent", "1d", componentName);
			assertNotNull(subTask);
		} finally {
			issueSrv.removeIssue(bi.getKey(), true);
		}
	}
	
	@Test
	public void createStoryAndSubTaskWithoutPriority() throws JiraGeneralException {
		BasicIssue bi = issueSrv.createStory(projectTestName, null, null, "resume", "description", null, componentName);
		assertNotNull(bi);
		//Create sub task
		try {
			BasicIssue subTask = issueSrv.createSubTask(projectTestName, bi.getKey(), "Sous-tâche", "sous tach resume", "sous tach desc", null, "1d", componentName);
			assertNotNull(subTask);
		} finally {
			issueSrv.removeIssue(bi.getKey(), true);
		}
	}
	
	@Test
	public void createStoryAndSubTaskWithoutDescriptionAndDeleteAll() throws JiraGeneralException {
		BasicIssue bi = issueSrv.createStory(projectTestName, null, null, "resume", null, "urgent", componentName);
		assertNotNull(bi);
		//Create sub task
		try {
			BasicIssue subTask = issueSrv.createSubTask(projectTestName, bi.getKey(), "Sous-tâche", "sous tach resume", null, "urgent", "1d", componentName);
			assertNotNull(subTask);
		} finally {
			issueSrv.removeIssue(bi.getKey(), true);
		}
	}
	
	@Test(expected=IssueNotFoundException.class)
	public void createSubTaskWithoutParent() throws JiraGeneralException {
		issueSrv.createSubTask(projectTestName, null, "Sous-tâche", "sous tach resume", "sous tach desc", "urgent", "1d", componentName);
	}
	
	@Test
	public void createStoryWithEpic() throws JiraGeneralException {
		BasicIssue bi = issueSrv.createStory(projectTestName, "EpicTestSummary", null, "resume", "description", "urgent", componentName);
		assertNotNull(bi);
		issueSrv.removeIssue(bi.getKey(), true);
	}
	
	@Test
	public void createStoryWithEpicWithoutDescription() throws JiraGeneralException {
		BasicIssue bi = issueSrv.createStory(projectTestName, "EpicTestSummary", null, "resume", null, "urgent", componentName);
		assertNotNull(bi);
		issueSrv.removeIssue(bi.getKey(), true);
	}
	
	@Test
	public void createStoryWithoutPriority() throws JiraGeneralException {
		BasicIssue bi = issueSrv.createStory(projectTestName, "EpicTestSummary", null, "resume", "description", null, componentName);
		assertNotNull(bi);
		issueSrv.removeIssue(bi.getKey(), true);
	}
	
	@Test
	public void createStoryWithVersion() throws JiraGeneralException {
		BasicIssue bi = issueSrv.createStory(projectTestName, null, v.getName(), "resume", "description", "urgent", componentName);
		assertNotNull(bi);
		issueSrv.removeIssue(bi.getKey(), true);
	}
	
	/**
	 * @throws JiraGeneralException
	 */
	@Test(expected=VersionNotFoundException.class)
	public void createStoryWithBadVersion() throws JiraGeneralException {
		issueSrv.createStory(projectTestName, null, "BADVERSION", "resume", "description", "urgent", componentName);
	}
	
	@Test
	public void createStoryWithSpecialChar() throws JiraGeneralException {
		BasicIssue bi = issueSrv.createStory(projectTestName, null, v.getName(), "resume\nOK", "description\nOK", "urgent", componentName);
		assertNotNull(bi);
		issueSrv.removeIssue(bi.getKey(), true);
	}
	
	@Test(expected=JiraEpicNotFound.class)
	public void createStoryWithBadEpics() throws JiraGeneralException {
		issueSrv.createStory(projectTestName, "LOLOLOLO", v.getName(), "resume", "description\nOK", "urgent", componentName);
	}
	
	@Test
	public void createStoryWithFullOptionAndSubTasksWithFullOption() throws JiraGeneralException {
		BasicIssue bi = issueSrv.createStory(projectTestName, "EpicTestSummary", v.getName(), "resume", "description", "urgent", componentName);
		assertNotNull(bi);
		//Create sub task
		try {
			BasicIssue subTask = issueSrv.createSubTask(projectTestName, bi.getKey(), "Sous-tâche", "sous tach resume", "sous tach desc", "urgent", "1d", componentName);
			assertNotNull(subTask);
		} finally {
			issueSrv.removeIssue(bi.getKey(), true);
		}
	}
}
