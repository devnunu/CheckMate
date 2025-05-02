package co.kr.checkmate.di

import androidx.room.Room
import co.kr.checkmate.data.local.database.CheckMateDatabase
import co.kr.checkmate.data.repository.TaskRepositoryImpl
import co.kr.checkmate.domain.repository.TaskRepository
import co.kr.checkmate.domain.usecase.AddMemoUseCase
import co.kr.checkmate.domain.usecase.AddTodoUseCase
import co.kr.checkmate.domain.usecase.DeleteTaskUseCase
import co.kr.checkmate.domain.usecase.GetTasksUseCase
import co.kr.checkmate.domain.usecase.ToggleTodoUseCase
import co.kr.checkmate.domain.usecase.UpdateMemoUseCase
import co.kr.checkmate.domain.usecase.UpdateTodoUseCase
import co.kr.checkmate.presentation.calendar.CalendarViewModel
import co.kr.checkmate.presentation.home.HomeViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Database
    single {
        Room.databaseBuilder(
            androidContext(),
            CheckMateDatabase::class.java,
            CheckMateDatabase.DATABASE_NAME
        ).build()
    }
    single { get<CheckMateDatabase>().todoDao() }
    single { get<CheckMateDatabase>().memoDao() }

    // Repositories
    single<TaskRepository> { TaskRepositoryImpl(get(), get()) }

    // UseCases
    factory { GetTasksUseCase(get()) }
    factory { AddTodoUseCase(get()) }
    factory { AddMemoUseCase(get()) }
    factory { ToggleTodoUseCase(get()) }
    factory { DeleteTaskUseCase(get()) }
    factory { UpdateTodoUseCase(get()) }
    factory { UpdateMemoUseCase(get()) }

    // ViewModels
    viewModel { HomeViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { CalendarViewModel() }
}