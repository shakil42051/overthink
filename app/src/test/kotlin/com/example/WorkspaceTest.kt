package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WorkspaceTest {
    @Test
    fun exactArxivIdentifiersAreRecognized() {
        assertEquals("1706.03762", arxivIdentifier("https://arxiv.org/pdf/1706.03762.pdf"))
        assertEquals("1706.03762v7", arxivIdentifier("arXiv:1706.03762v7"))
        assertNull(arxivIdentifier("transformer attention"))
    }

    @Test
    fun taskAndBackupRoundTripPreservesStructure() {
        val paper = ResearchItem("p1", "A paper", year = 2024)
        val project =
            Project(
                title = "Thesis",
                tasks =
                    listOf(
                        ProjectTask(title = "Read", status = TaskStatus.DONE),
                        ProjectTask(title = "Write"),
                    ),
                paperIds = listOf(paper.id),
            )
        val workspace =
            Workspace(
                listOf(paper),
                listOf(project),
                listOf(Note(itemId = paper.id, text = "An observation", page = 3)),
            )
        val restored = Workspace.from(JSONObject(workspace.json().toString()))
        assertEquals(workspace, restored)
        assertEquals(0.5f, restored.projects.first().progress)
        assertEquals(0f, Project(title = "New").progress)
    }

    @Test
    fun databaseScopesAndReopeningPreserveIsolation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scope = "test:${System.nanoTime()}"
        val store = WorkspaceStore(context)
        store.switch(scope)
        val item = ResearchItem("paper", "Saved source")
        store.save(item)
        store.note(Note(itemId = item.id, text = "Private", page = 2))
        store.project(Project(id = "project", title = "Project", paperIds = listOf(item.id)))
        store.switch("another:$scope")
        assertTrue(store.workspace.value.items.isEmpty())
        assertTrue(store.workspace.value.notes.isEmpty())
        store.switch(scope)
        assertEquals(1, store.workspace.value.notes.size)
        store.save(item.copy(collection = "Thesis"))
        assertEquals(1, store.workspace.value.notes.size)
        store.remove(item.id)
        assertTrue(store.workspace.value.notes.isEmpty())
        assertTrue(store.workspace.value.projects.first().paperIds.isEmpty())
        store.close()
        val reopened = WorkspaceStore(context)
        reopened.switch(scope)
        assertTrue(reopened.workspace.value.items.isEmpty())
        reopened.close()
    }

    @Test
    fun bibtexUsesRealTypesAndEscapesMetadata() {
        val citation = ResearchItem("repo", "A & B_1", kind = ItemKind.CODE).bibtex()
        assertTrue(citation.startsWith("@misc{"))
        assertTrue(citation.contains("A \\& B\\_1"))
        assertFalse(citation.contains("year={"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownBackupVersionIsRejected() {
        Workspace.from(JSONObject("{\"schema\":999}"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun danglingBackupNotesAreRejected() {
        Workspace.from(Workspace(notes = listOf(Note(itemId = "missing", text = "Orphan"))).json())
    }

    @Test
    fun legacyTaskFormatsRetainTheirStatuses() {
        assertEquals(
            listOf(TaskStatus.DONE, TaskStatus.TODO),
            parseLegacyTasks("[\"Read|true\",\"Write|false\"]").map { it.status },
        )
        assertEquals(
            listOf(TaskStatus.IN_PROGRESS, TaskStatus.DONE),
            parseLegacyTasks("Read::IN_PROGRESS||Write::DONE").map { it.status },
        )
        assertEquals("Read", parseLegacyTasks("[\"Read|true\"]").single().title)
    }
}
