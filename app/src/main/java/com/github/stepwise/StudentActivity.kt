package com.github.stepwise

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.github.stepwise.databinding.ActivityStudentBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class StudentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navViewStudent

        val navController = findNavController(R.id.nav_host_fragment_student)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.student_home_fragment,
                R.id.student_projects_fragment,
                R.id.student_profile_fragment
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }
}