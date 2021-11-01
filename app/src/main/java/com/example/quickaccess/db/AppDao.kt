package com.example.quickaccess.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.example.quickaccess.data.AppDetails

//@Dao
interface AppDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAppDetails(appDetails : AppDetails)



}