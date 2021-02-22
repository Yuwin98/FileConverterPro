package com.yuwin.fileconverterpro

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.ads.MobileAds
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

   private lateinit var navController: NavController
   private lateinit var bottomNavigationView: BottomNavigationView



   private val onNavigationViewSelectedItemListener = BottomNavigationView.OnNavigationItemSelectedListener { menuItem ->

       when(menuItem.itemId){
            R.id.home -> {
                findNavController(R.id.navHostFragment).navigate(R.id.home)
                return@OnNavigationItemSelectedListener true
            }
           R.id.settings -> {
               findNavController(R.id.navHostFragment).navigate(R.id.settings)
               return@OnNavigationItemSelectedListener true
           }
           R.id.info -> {
               findNavController(R.id.navHostFragment).navigate(R.id.info)
               return@OnNavigationItemSelectedListener true
           }

           R.id.chooseImage -> {
               chooseImages()
               return@OnNavigationItemSelectedListener true
           }
       }
       false
   }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        navController = findNavController(R.id.navHostFragment)

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.home,
                R.id.settings,
                R.id.info,
                R.id.convert,
                R.id.convertedFilesFragment,
                R.id.convertProgressFragment
            )
        )

        bottomNavigationView.setupWithNavController(navController)
        bottomNavigationView.setOnNavigationItemSelectedListener(onNavigationViewSelectedItemListener)
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun chooseImages() {
        val intent = Intent()
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Images"), 200)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uriList = mutableListOf<Uri>()
        if(data != null && requestCode == 200 && resultCode == Activity.RESULT_OK) {
            if(data.clipData != null){
                val count = data.clipData!!.itemCount
                for(i in 0 until count) {
                    val imageUri = data.clipData!!.getItemAt(i).uri
                    uriList.add(imageUri)
                }
            }else {
                val imageUri = data.data!!
                uriList.add(imageUri)
            }

            val newUriList = UriList(uriList)

            var action = FileListFragmentDirections.actionHomeToConvert(newUriList)

            when(navController.currentDestination?.id) {
                R.id.convert -> {
                    action = ConvertFragmentDirections.actionConvertSelf(newUriList)
                }
                R.id.settings -> {
                    action = SettingsFragmentDirections.actionSettingsToConvert(newUriList)
                }
                R.id.info -> {
                    action = InfoFragmentDirections.actionInfoToConvert(newUriList)
                }
                R.id.convertProgressFragment -> {
                    action = ConvertProgressFragmentDirections.actionConvertProgressFragmentToConvert(newUriList)
                }
                R.id.convertedFilesFragment -> {
                    action = ConvertedFilesFragmentDirections.actionConvertedFilesFragmentToConvert(newUriList)
                }
            }
            findNavController(R.id.navHostFragment).navigate(action)
        }else if(requestCode == 200) {
            findNavController(R.id.navHostFragment).navigate(R.id.home)
        }
    }
}