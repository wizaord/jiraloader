package sopra.grenoble.jiraLoader.jira.dao.project.impl;

import com.atlassian.jira.rest.client.domain.*;
import com.atlassian.jira.rest.client.domain.input.ComplexIssueInputFieldValue;
import com.atlassian.jira.rest.client.domain.input.FieldInput;
import com.atlassian.jira.rest.client.domain.input.IssueInputBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sopra.grenoble.jiraLoader.exceptions.*;
import sopra.grenoble.jiraLoader.jira.dao.metadatas.JiraFieldLoader;
import sopra.grenoble.jiraLoader.jira.dao.metadatas.JiraIssuesTypeLoader;
import sopra.grenoble.jiraLoader.jira.dao.project.IIssueEpicService;
import sopra.grenoble.jiraLoader.jira.dao.project.IIssueService;
import sopra.grenoble.jiraLoader.jira.dao.project.IVersionService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Service
public class IssueStoryAndSubTaskService extends IssueAbstractGenericService implements IIssueService {

	public static final Logger LOG = LoggerFactory.getLogger(IssueStoryAndSubTaskService.class);

	@Autowired private IIssueEpicService epicSrv;
	@Autowired private IVersionService versionSrv;
	@Autowired private JiraFieldLoader fieldLoader;

	@Override
	public BasicIssue createStory(String projectName, String epicName, String versionName, String clientReference, String resume, String description, String priority, String componentName, String versionCorrected) throws JiraGeneralException {
		//call generic builder
		IssueInputBuilder iib = createGenericIssue(projectName,  JiraIssuesTypeLoader.JIRA_STORY_ISSUE_TYPE_NAME, resume, description, priority, componentName);
		
		//add epicLink
		if (epicName != null) {
			//get epic based on the name
			BasicIssue epicI = epicSrv.getByName(epicName, projectName);
			if (epicI == null) {
				throw new JiraEpicNotFound();
			}
			addEpicLink(iib, epicI.getKey());
		}

		//add clientReference
		if (clientReference != null) {
			addClientReference(iib, clientReference);
		}
		//add version
		if (versionName != null) {
			addAffectedVersion(iib, projectName, versionName);
		}
		// add correctedVersion
		if (versionCorrected != null) {
			addFixVersion(iib, projectName, versionCorrected);
		}
				
		//insert in JIRA
		BasicIssue bi = super.createIssueInJIRA(iib.build());
		return bi;
	}

	@Override
	public BasicIssue createSubTask(String projectName, String parentKey, String issueTypeName, String resume, String description, String priority, String estimation, String componentName) throws JiraGeneralException {
		//get the parentIssue
		Issue parentIssue = getByKey(parentKey, projectName);
		
		//call generic builder
		IssueInputBuilder iib = createGenericIssue(projectName, issueTypeName, resume, description, priority, componentName);

		// on ajoute un parent
		if (parentKey == null) {
			LOG.error("Parent key cannot be null");
			throw new IssueNotFoundException();
		}
		HashMap<String, Object> parentHashMap = new HashMap<>();
		parentHashMap.put("key", parentIssue.getKey());
		iib.setFieldValue("parent", new ComplexIssueInputFieldValue(parentHashMap));
		
		
		//add estimation
		if (estimation != null) {
			addEstimation(iib, estimation);
		}
		//add in the same epic
		//Not allowed in our screen JIRA. Subtask is automatically in the same epic as the parent

		//add same versions as the parent
		List<Version> parentVersions = (List<Version>) parentIssue.getAffectedVersions();
		if (parentVersions != null && parentVersions.size() != 0) {
			iib.setAffectedVersions(parentVersions);
		}
		
		//insert in JIRA
		BasicIssue bi = super.createIssueInJIRA(iib.build());

		return bi;
	}
	
	
	/**
	 * Add a reference to an epic in issue.
	 * @param iib
	 * @param epicName
	 * @throws JiraGeneralException 
	 * @throws IssueNotFoundException 
	 */
	private void addEpicLink(IssueInputBuilder iib, String epicKey) throws JiraGeneralException {
		//get field id by name from loader
		Field fieldEpic = fieldLoader.getElement(JiraFieldLoader.EPIC_LINK_FIELD_NAME);
		//add Epic field : Epic Name
		FieldInput fi = new FieldInput(fieldEpic.getId(), epicKey);
		iib.setFieldInput(fi);
	}
	
	/**
	 * Add estimation
	 * @param iib
	 * @param estimation
	 */
	private void addEstimation(IssueInputBuilder iib, String estimation) {
		HashMap<String, Object> estimationHashMap = new HashMap<>();
		estimationHashMap.put("originalEstimate", estimation);
		iib.setFieldValue("timetracking", new ComplexIssueInputFieldValue(estimationHashMap));
	}

	/**
	 * Add client reference in iib
	 *
	 * @param iib
	 * @param clientReference
	 */
	private void addClientReference(IssueInputBuilder iib, String clientReference) {
		// get field id by name from loader
		Field fieldClientReference = fieldLoader.getElement(JiraFieldLoader.REFERENCE_CLIENT_FIELD_NAME);
		// add field : Reference client
		FieldInput fi = new FieldInput(fieldClientReference.getId(), clientReference);
		iib.setFieldInput(fi);
	}

	/**
	 * Set a specific version in iib
	 * @param iib
	 * @param projectName
	 * @param versionName
	 * @throws VersionNotFoundException
	 */
	private void addAffectedVersion(IssueInputBuilder iib, String projectName, String versionName) throws VersionNotFoundException {
		//get version from name
		Version v = versionSrv.getVersion(projectName, versionName);
		List<Version> versions = new ArrayList<>();
		versions.add(v);
		iib.setAffectedVersions(versions);
	}

	private void addFixVersion(IssueInputBuilder iib, String projectName, String versionName) throws VersionNotFoundException {
		// get version from name
		Version v = versionSrv.getVersion(projectName, versionName);
		List<Version> versions = new ArrayList<>();
		versions.add(v);
		iib.setFixVersions(versions);
	}
	
	/**
	 * Search on JIRA if a issue's name is starting by the name passed in parameter
	 * @param issueName
	 * @param epicName
	 * @param projectName
	 * @return
	 */
	public Optional<BasicIssue> getByStartingName(String issueName, String projectName) {
		String jpqlFormat = String.format("issuetype = " + JiraIssuesTypeLoader.JIRA_STORY_ISSUE_TYPE_NAME + " AND summary ~ '%s'", issueName);
		SearchResult sr = jiraConnection.getSearchClient().searchJql(jpqlFormat, pm);
		for (BasicIssue epicIssue : sr.getIssues()) {
			//JIRA can return issue with another char before or after the summary. Thus we need to check the name
			try {
				Issue is = getByKey(epicIssue.getKey(), projectName);
				LOG.debug("Found possible match issue with name " + is.getSummary()); 
				if (is.getSummary().startsWith(issueName)) {
					return Optional.of(is);
				}
			} catch (IssueNotFoundException | JiraIssueTypeException e) {
			}
		}
		return Optional.empty();
	}

	
	@Override
	public void updateIssue(String issueKey, String projectName, String priority) throws IssueNotFoundException, JiraGeneralException {

		Issue issue = getByKey(issueKey, projectName);
		List<FieldInput> lstFields = new ArrayList<>();

		issue.getPriority().getId();

		//set priority (optional)
		if ((priority != null) && (priority.compareTo(issue.getPriority().getName())!= 0 )) {
			Priority p = priorityLoader.getElement(priority);
			if (p == null) {
				LOG.error("Priority with name <" + p + "> does not exist");
				throw new JiraPriorityNotFoundException();
			}

			lstFields.add(new FieldInput("priority", ComplexIssueInputFieldValue.with("id", ""+p.getId())));
		}
		// check status of the Story & update her if !Completed
		if (lstFields.size() != 0 && !(new Long(10007).equals(issue.getStatus().getId()))) {
			this.updateIssueInJira(issue, lstFields);
		} else if (new Long(10007).equals(issue.getStatus().getId())) {
			LOG.info("Story was completed, nothing was updated");
		}
	}
	
}
