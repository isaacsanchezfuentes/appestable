package com.example.appestable.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PersonaViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PersonaViewModel::class.java)) {
            return PersonaViewModel(app) as T
        }
        throw IllegalArgumentException("ViewModel desconocido")
    }
}
