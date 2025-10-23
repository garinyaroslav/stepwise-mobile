package com.github.stepwise

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.github.stepwise.databinding.ActivityTeacherBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class TeacherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTeacherBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeacherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navViewTeacher

        val navController = findNavController(R.id.nav_host_fragment_teacher)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.teacher_home_fragment,
                R.id.teacher_projects_fragment,
                R.id.teacher_profile_fragment
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }
}