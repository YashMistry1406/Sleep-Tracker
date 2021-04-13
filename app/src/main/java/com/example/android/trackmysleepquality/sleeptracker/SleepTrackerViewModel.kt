/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import android.provider.SyncStateContract.Helpers.insert
import android.provider.SyncStateContract.Helpers.update
import androidx.lifecycle.*


import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import com.example.android.trackmysleepquality.sleepquality.SleepQualityFragment
import kotlinx.coroutines.launch

/**
 * ViewModel for SleepTrackerFragment.
 */
class SleepTrackerViewModel(
        val database: SleepDatabaseDao,
        application: Application) : AndroidViewModel(application) {





    //adding snackbar
    private var _showSnackbarEvent = MutableLiveData<Boolean>()

    val showSnackBarEvent: LiveData<Boolean>
        get() = _showSnackbarEvent

    fun doneShowingSnackbar() {
        _showSnackbarEvent.value = false
    }



    private val _navigateToSleepQuality = MutableLiveData<SleepNight>()

    val navigateToSleepQuality: LiveData<SleepNight>
        get() = _navigateToSleepQuality


    fun doneNavigating() {
        _navigateToSleepQuality.value = null
    }




    private var tonight=MutableLiveData<SleepNight?>()
    private val nights=database.getAllNights()

    private fun initializeTonight() {
        viewModelScope.launch {
            tonight.value = getTonightFromDatabase()
        }
    }


    val nightsString = Transformations.map(nights) { nights ->
        formatNights(nights, application.resources)
    }

    init {

        initializeTonight()

    }
    private suspend fun getTonightFromDatabase(): SleepNight? {
        var night = database.getTonight()
        if (night?.endTimeMilli != night?.startTimeMilli) {
            night = null
        }
        return night
    }



    private suspend fun clear(){
        database.clear()
    }


    private suspend fun update(night: SleepNight){
        database.update(night)
    }

    private suspend fun insert(night: SleepNight){
        database.insert(night)
    }





    fun onStartTracking(){
        viewModelScope.launch {

            val newNight=SleepNight()
            insert(newNight)
            tonight.value=getTonightFromDatabase()


        }
    }
    fun onStopTracking(){
        viewModelScope.launch {
            val oldNight = tonight.value ?: return@launch

            // Update the night in the database to add the end time.
            oldNight.endTimeMilli = System.currentTimeMillis()

            update(oldNight)

            _navigateToSleepQuality.value = oldNight
        }
    }

    fun onClear() {
        viewModelScope.launch {
            // Clear the database table.
               clear()

            // And clear tonight since it's no longer in the database
            tonight.value = null

            _showSnackbarEvent.value = true
        }
    }



    val startButtonVisible = Transformations.map(tonight) {
        null == it
    }
    val stopButtonVisible = Transformations.map(tonight) {
        null != it
    }
    val clearButtonVisible = Transformations.map(nights) {
        it?.isNotEmpty()
    }


}

/*
this class is same as view model
it takes the application context as parameter as makes it available as a property
a view model needs access to the data base
which is to through the interface defined in the DAO
ans then we pass it to the super class as well
Next we need a factory to instantiate the view model and provide it with data source

<<<<--------------------------------------------COROUTINES --------------------------------------------->>>>>
            here we are going to use kotlin coroutines that are a way to handle background task
            it is done to reduce the work on the main thread that is the UI thread
            ans pass it to other threads
            using kotlin coroutines we are able to do the task effectively without any delay to the ui

            To manage all our corountines we need a job it is the task that the coroutines do and return to the work when done
            We also define the scope, which determines on which thread the corountines will run on and it also needs to know about the job

AT THE END WE HAVE ADDED DATA BINDING


 */