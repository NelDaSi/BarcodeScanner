package com.neldasi.dafscanner.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScanDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: ScanDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.scanDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndGetByCode_returnsInsertedPart() = runBlocking {
        val part = ScannedPart(fullCode = "ABC123", timestamp = 1000L)
        dao.insertPart(part)

        assertEquals(part, dao.getPartByCode("ABC123"))
    }

    @Test
    fun insertPart_withSameFullCode_replacesExisting() = runBlocking {
        dao.insertPart(ScannedPart(fullCode = "ABC123", timestamp = 1000L, note = "first"))
        dao.insertPart(ScannedPart(fullCode = "ABC123", timestamp = 2000L, note = "second"))

        val loaded = dao.getPartByCode("ABC123")

        assertEquals("second", loaded?.note)
        assertEquals(2000L, loaded?.timestamp)
    }

    @Test
    fun deletePart_removesOnlyThatPart() = runBlocking {
        val kept = ScannedPart(fullCode = "KEEP", timestamp = 1L)
        val removed = ScannedPart(fullCode = "REMOVE", timestamp = 2L)
        dao.insertPart(kept)
        dao.insertPart(removed)

        dao.deletePart(removed)

        assertNull(dao.getPartByCode("REMOVE"))
        assertEquals(kept, dao.getPartByCode("KEEP"))
    }

    @Test
    fun deleteParts_removesAllMatchingCodes() = runBlocking {
        dao.insertPart(ScannedPart(fullCode = "A", timestamp = 1L))
        dao.insertPart(ScannedPart(fullCode = "B", timestamp = 2L))
        dao.insertPart(ScannedPart(fullCode = "C", timestamp = 3L))

        dao.deleteParts(listOf("A", "B"))

        assertNull(dao.getPartByCode("A"))
        assertNull(dao.getPartByCode("B"))
        assertEquals("C", dao.getPartByCode("C")?.fullCode)
    }

    @Test
    fun getAllParts_ordersByTimestampDescending() = runBlocking {
        dao.insertPart(ScannedPart(fullCode = "OLD", timestamp = 1L))
        dao.insertPart(ScannedPart(fullCode = "NEW", timestamp = 2L))

        val all = dao.getAllParts().first()

        assertEquals(listOf("NEW", "OLD"), all.map { it.fullCode })
    }

    @Test
    fun deleteAll_clearsTable() = runBlocking {
        dao.insertPart(ScannedPart(fullCode = "A", timestamp = 1L))
        dao.insertPart(ScannedPart(fullCode = "B", timestamp = 2L))

        dao.deleteAll()

        assertEquals(emptyList<ScannedPart>(), dao.getAllParts().first())
    }
}
