package com.meilisearch.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.meilisearch.integration.classes.AbstractIT;
import com.meilisearch.integration.classes.TestData;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.model.Task;
import com.meilisearch.sdk.model.TaskInfo;
import com.meilisearch.sdk.model.TasksQuery;
import com.meilisearch.sdk.model.TasksResults;
import com.meilisearch.sdk.utils.Movie;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
public class TasksTest extends AbstractIT {

    private TestData<Movie> testData;

    @BeforeEach
    public void initialize() {
        this.setUp();
        if (testData == null) testData = this.getTestData(MOVIES_INDEX, Movie.class);
    }

    @AfterAll
    static void cleanMeilisearch() {
        cleanup();
    }

    /** Test Get Task */
    @Test
    public void testClientGetTask() throws Exception {
        String indexUid = "GetClientTask";
        TaskInfo response = client.createIndex(indexUid);
        client.waitForTask(response.getTaskUid());

        Task task = client.getTask(response.getTaskUid());

        assertTrue(task instanceof Task);
        assertNotNull(task.getStatus());
        assertNotEquals("", task.getStatus());
        assertNotNull(task.getStartedAt());
        assertNotNull(task.getEnqueuedAt());
        assertNotNull(task.getFinishedAt());
        assertTrue(task.getUid() >= 0);
        assertNotNull(task.getDetails());
        assertNull(task.getDetails().getPrimaryKey());
    }

    /** Test Get Tasks */
    @Test
    public void testClientGetTasks() throws Exception {
        TasksResults result = client.getTasks();
        Task[] tasks = result.getResults();

        for (Task task : tasks) {
            client.waitForTask(task.getUid());

            assertNotNull(task.getStatus());
            assertNotEquals("", task.getStatus());
            assertTrue(task.getUid() >= 0);
            assertNotNull(task.getDetails());
            if (task.getType().equals("indexDeletion")) {
                assertNotNull(task.getDetails().getDeletedDocuments());
            }
        }
    }

    /** Test Get Tasks with limit */
    @Test
    public void testClientGetTasksLimit() throws Exception {
        int limit = 2;
        TasksQuery query = new TasksQuery().setLimit(limit);
        TasksResults result = client.getTasks(query);

        assertEquals(limit, result.getLimit());
        assertNotNull(result.getFrom());
        assertNotNull(result.getNext());
        assertNotNull(result.getResults().length);
    }

    /** Test Get Tasks with limit and from */
    @Test
    public void testClientGetTasksLimitAndFrom() throws Exception {
        int limit = 2;
        int from = 2;
        TasksQuery query = new TasksQuery().setLimit(limit).setFrom(from);
        TasksResults result = client.getTasks(query);

        assertEquals(limit, result.getLimit());
        assertEquals(from, result.getFrom());
        assertNotNull(result.getFrom());
        assertNotNull(result.getNext());
        assertNotNull(result.getResults().length);
    }

    /** Test Get Tasks with all query parameters */
    @Test
    public void testClientGetTasksAllQueryParameters() throws Exception {
        int limit = 2;
        int from = 2;
        TasksQuery query =
                new TasksQuery()
                        .setLimit(limit)
                        .setFrom(from)
                        .setStatus(new String[] {"enqueued", "succeeded"})
                        .setType(new String[] {"indexDeletion"});
        TasksResults result = client.getTasks(query);
        Task[] tasks = result.getResults();

        assertEquals(limit, result.getLimit());
        assertNotNull(result.getFrom());
        assertNotNull(result.getNext());
    }

    /** Test waitForTask */
    @Test
    public void testWaitForTask() throws Exception {
        String indexUid = "WaitForTask";
        TaskInfo response = client.createIndex(indexUid);
        client.waitForTask(response.getTaskUid());

        Task task = client.getTask(response.getTaskUid());

        assertTrue(task.getUid() >= 0);
        assertNotNull(task.getEnqueuedAt());
        assertNotNull(task.getStartedAt());
        assertNotNull(task.getFinishedAt());
        assertEquals("succeeded", task.getStatus());
        assertNotNull(task.getDetails());
        assertNull(task.getDetails().getPrimaryKey());

        client.deleteIndex(task.getIndexUid());
    }

    /** Test waitForTask timeoutInMs */
    @Test
    public void testWaitForTaskTimoutInMs() throws Exception {
        String indexUid = "WaitForTaskTimoutInMs";
        Index index = client.index(indexUid);

        TaskInfo task = index.addDocuments(this.testData.getRaw());
        index.waitForTask(task.getTaskUid());

        assertThrows(Exception.class, () -> index.waitForTask(task.getTaskUid(), 0, 50));
    }
}
