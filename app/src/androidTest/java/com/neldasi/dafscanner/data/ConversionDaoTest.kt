package com.neldasi.dafscanner.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConversionDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: ConversionDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.conversionDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertRecord_thenGetAllRecords_ordersByTimestampDescending() = runBlocking {
        dao.insertRecord(ConversionRecord(hex = "1A", dec = "26", timestamp = 1L))
        dao.insertRecord(ConversionRecord(hex = "2B", dec = "43", timestamp = 2L))

        val all = dao.getAllRecords().first()

        assertEquals(listOf("2B", "1A"), all.map { it.hex })
    }

    @Test
    fun deleteRecord_removesOnlyThatRecord() = runBlocking {
        dao.insertRecord(ConversionRecord(hex = "KEEP", dec = "0", timestamp = 1L))
        dao.insertRecord(ConversionRecord(hex = "REMOVE", dec = "0", timestamp = 2L))
        val toRemove = dao.getAllRecords().first().first { it.hex == "REMOVE" }

        dao.deleteRecord(toRemove)

        val remaining = dao.getAllRecords().first()
        assertEquals(listOf("KEEP"), remaining.map { it.hex })
    }

    @Test
    fun deleteAll_clearsTable() = runBlocking {
        dao.insertRecord(ConversionRecord(hex = "A", dec = "0", timestamp = 1L))

        dao.deleteAll()

        assertEquals(emptyList<ConversionRecord>(), dao.getAllRecords().first())
    }
}
