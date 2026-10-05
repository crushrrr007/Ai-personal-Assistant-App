package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.ResponseParser
import com.example.data.remote.InterpretResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Assistant", appName)
    }

    @Test
    fun `parse proposed task modification`() {
        val json = """
            {
              "intent": "modify_task",
              "proposed_task_mod": {
                "task_id": 42,
                "action": "update",
                "new_title": "Updated Math Assignment",
                "new_due_at": "2026-10-06T19:00:00+00:00",
                "new_is_deadline": true
              },
              "reply": "I'll reschedule Math Assignment to 7 PM."
            }
        """.trimIndent()

        val result = ResponseParser.parse(json)
        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        assertEquals(InterpretResult.INTENT_MODIFY_TASK, parsed.intent)
        assertNotNull(parsed.proposedTaskMod)
        assertEquals(42L, parsed.proposedTaskMod?.taskId)
        assertEquals("update", parsed.proposedTaskMod?.action)
        assertEquals("Updated Math Assignment", parsed.proposedTaskMod?.newTitle)
        assertTrue(parsed.proposedTaskMod?.newIsDeadline == true)
    }

    @Test
    fun `parse proposed note modification`() {
        val json = """
            {
              "intent": "modify_note",
              "proposed_note_mod": {
                "note_id": 7,
                "action": "append",
                "new_body": "- Almond milk\n- Coffee beans"
              },
              "reply": "I'll add almond milk and coffee beans to your Grocery Note."
            }
        """.trimIndent()

        val result = ResponseParser.parse(json)
        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        assertEquals(InterpretResult.INTENT_MODIFY_NOTE, parsed.intent)
        assertNotNull(parsed.proposedNoteMod)
        assertEquals(7L, parsed.proposedNoteMod?.noteId)
        assertEquals("append", parsed.proposedNoteMod?.action)
    }

    @Test
    fun `parse answer notes intent`() {
        val json = """
            {
              "intent": "answer_notes",
              "reply": "According to your Physics Note, the key equation is E=mc^2 and you need to review Chapter 3."
            }
        """.trimIndent()

        val result = ResponseParser.parse(json)
        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        assertEquals(InterpretResult.INTENT_ANSWER_NOTES, parsed.intent)
        assertTrue(parsed.reply.contains("Physics Note"))
    }
}
