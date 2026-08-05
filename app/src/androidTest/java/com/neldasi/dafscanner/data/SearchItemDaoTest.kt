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
class SearchItemDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: SearchItemDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.searchItemDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun item(serial: String, scanTimestamp: Long? = null, scanOrder: Int? = null) = SearchItem(
        serialNumber = serial,
        typeCode = "TYPE1",
        decSerial = "1",
        scanTimestamp = scanTimestamp,
        scanOrder = scanOrder,
    )

    @Test
    fun insertAll_thenGetItemBySerial_returnsMatch() = runBlocking {
        dao.insertAll(listOf(item("A"), item("B")))

        assertEquals("A", dao.getItemBySerial("A")?.serialNumber)
        assertNull(dao.getItemBySerial("MISSING"))
    }

    @Test
    fun deleteAll_thenInsertAll_replacesWholeTable() = runBlocking {
        dao.insertAll(listOf(item("A"), item("B")))

        dao.deleteAll()
        dao.insertAll(listOf(item("C")))

        val all = dao.getAllItems().first()
        assertEquals(listOf("C"), all.map { it.serialNumber })
    }

    @Test
    fun update_persistsChanges() = runBlocking {
        dao.insertAll(listOf(item("A")))
        val loaded = dao.getItemBySerial("A")!!

        dao.update(loaded.copy(scanTimestamp = 12345L, scanOrder = 1))

        val updated = dao.getItemBySerial("A")
        assertEquals(12345L, updated?.scanTimestamp)
        assertEquals(1, updated?.scanOrder)
    }

    @Test
    fun getCount_reflectsCurrentRowCount() = runBlocking {
        assertEquals(0, dao.getCount())

        dao.insertAll(listOf(item("A"), item("B"), item("C")))

        assertEquals(3, dao.getCount())
    }

    @Test
    fun getMaxScanOrder_returnsHighestOrder_orNullWhenNoneScanned() = runBlocking {
        assertNull(dao.getMaxScanOrder())

        dao.insertAll(listOf(item("A", scanTimestamp = 1L, scanOrder = 3), item("B", scanTimestamp = 2L, scanOrder = 7)))

        assertEquals(7, dao.getMaxScanOrder())
    }
}
