package com.example.foodmapvn.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.foodmapvn.FoodMapApplication

/** Lấy AppContainer trong khối initializer { } của ViewModel factory. */
fun CreationExtras.appContainer(): AppContainer =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as FoodMapApplication).container
