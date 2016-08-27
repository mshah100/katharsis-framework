package io.katharsis.client;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import io.katharsis.client.mock.models.Project;
import io.katharsis.client.mock.models.Task;
import io.katharsis.queryParams.QueryParams;

public class ClientTest extends AbstractClientTest {

	@Test
	public void testFindEmpty() {
		List<Task> tasks = taskRepo.findAll(new QueryParams());
		Assert.assertTrue(tasks.isEmpty());
	}

	@Test
	public void testFindNull() {
		try {
			taskRepo.findOne(1L, new QueryParams());
			Assert.fail();
		} catch (ClientException e) {
			Assert.assertEquals("Not Found", e.getMessage());
		}
	}

	@Test
	public void testSaveAndFind() {
		Task task = new Task();
		task.setId(1L);
		task.setName("test");
		taskRepo.save(task);

		// check retrievable with findAll
		List<Task> tasks = taskRepo.findAll(new QueryParams());
		Assert.assertEquals(1, tasks.size());
		Task savedTask = tasks.get(0);
		Assert.assertEquals(task.getId(), savedTask.getId());
		Assert.assertEquals(task.getName(), savedTask.getName());

		// check retrievable with findAll(ids)
		tasks = taskRepo.findAll(Arrays.asList(1L), new QueryParams());
		Assert.assertEquals(1, tasks.size());
		savedTask = tasks.get(0);
		Assert.assertEquals(task.getId(), savedTask.getId());
		Assert.assertEquals(task.getName(), savedTask.getName());

		// check retrievable with findOne
		savedTask = taskRepo.findOne(1L, new QueryParams());
		Assert.assertEquals(task.getId(), savedTask.getId());
		Assert.assertEquals(task.getName(), savedTask.getName());
	}

	@Test
	public void testGeneratedId() {
		Task task = new Task();
		task.setId(null);
		task.setName("test");
		Task savedTask = taskRepo.save(task);
		Assert.assertNotNull(savedTask.getId());
	}

	@Test
	public void testDelete() {
		Task task = new Task();
		task.setId(1L);
		task.setName("test");
		taskRepo.save(task);

		taskRepo.delete(1L);

		List<Task> tasks = taskRepo.findAll(new QueryParams());
		Assert.assertEquals(0, tasks.size());
	}

	@Test
	@Ignore // not supported by spec
	public void testSaveOneRelation() {
		Task task = new Task();
		task.setId(2L);
		task.setName("test");

		Project project = new Project();
		project.setId(1L);
		project.setName("project");
		project.getTasks().add(task);

		Map<String, Set<String>> params = new HashMap<String, Set<String>>();
		addParams(params, "include[projects]", "tasks");
		QueryParams queryParams = queryParamsBuilder.buildQueryParams(params);

		projectRepo.save(project, queryParams);

		Task savedTask = taskRepo.findOne(2L, queryParams);
		Assert.assertEquals(task.getId(), savedTask.getId());
		Assert.assertEquals(task.getName(), savedTask.getName());
		Assert.assertNotNull(savedTask.getProject());
		Assert.assertEquals(1L, savedTask.getProject().getId().longValue());
	}

	@Test
	public void testSetRelation() {
		Project project = new Project();
		project.setId(1L);
		project.setName("project");
		projectRepo.save(project);

		Task task = new Task();
		task.setId(2L);
		task.setName("test");
		taskRepo.save(task);

		relRepo.setRelation(task, project.getId(), "project");

		Project relProject = relRepo.findOneTarget(task.getId(), "project", new QueryParams());
		Assert.assertNotNull(relProject);
		Assert.assertEquals(project.getId(), relProject.getId());
	}

	@Test
	public void testAddSetRemoveRelations() {
		Project project0 = new Project();
		project0.setId(1L);
		project0.setName("project0");
		projectRepo.save(project0);

		Project project1 = new Project();
		project1.setId(2L);
		project1.setName("project1");
		projectRepo.save(project1);

		Task task = new Task();
		task.setId(3L);
		task.setName("test");
		taskRepo.save(task);

		relRepo.addRelations(task, Arrays.asList(project0.getId(), project1.getId()), "projects");
		List<Project> relProjects = relRepo.findManyTargets(task.getId(), "projects", new QueryParams());
		Assert.assertEquals(2, relProjects.size());

		relRepo.setRelations(task, Arrays.asList(project1.getId()), "projects");
		relProjects = relRepo.findManyTargets(task.getId(), "projects", new QueryParams());
		Assert.assertEquals(1, relProjects.size());
		Assert.assertEquals(project1.getId(), relProjects.get(0).getId());

		// FIXME HTTP DELETE method with payload not supported? at least in
		// Jersey
		// relRepo.removeRelations(task, Arrays.asList(project1.getId()),
		// "projects");
		// relProjects = relRepo.findManyTargets(task.getId(), "projects", new
		// QueryParams());
		// Assert.assertEquals(0, relProjects.size());
	}

	@Test
	public void testValidationException() {

	}

	private void addParams(Map<String, Set<String>> params, String key, String value) {
		params.put(key, new HashSet<String>(Arrays.asList(value)));
	}
}