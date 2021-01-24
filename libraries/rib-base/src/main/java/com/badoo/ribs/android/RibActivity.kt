package com.badoo.ribs.android

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.badoo.ribs.android.activitystarter.ActivityStarter
import com.badoo.ribs.android.dialog.DialogLauncher
import com.badoo.ribs.android.integrationpoint.ActivityIntegrationPoint
import com.badoo.ribs.core.Rib
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable

/**
 * Helper class for root [Rib] integration.
 *
 * Also offers base functionality to satisfy dependencies of Android-related functionality
 * down the tree:
 * - [DialogLauncher]
 * - [ActivityStarter]
 *
 * Feel free to not extend this and use your own integration point - in this case,
 * don't forget to take a look here what methods needs to be forwarded to the root Node.
 */
abstract class RibActivity : AppCompatActivity() {

    lateinit var integrationPoint: ActivityIntegrationPoint
        protected set

    abstract val rootViewGroup: ViewGroup

    abstract fun createRib(savedInstanceState: Bundle?): Rib

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        integrationPoint = ActivityIntegrationPoint(
            activity = this,
            savedInstanceState = savedInstanceState,
            rootViewGroup = rootViewGroup
        )

        val root = createRib(savedInstanceState)
        integrationPoint.attach(root)

        if (intent?.action == Intent.ACTION_VIEW) {
            handleDeepLink(intent)
        }
    }

    private val disposables = CompositeDisposable()

    fun handleDeepLink(intent: Intent) {
        workflowFactory.invoke(intent)?.let {
            disposables.add(it.subscribe())
        }
    }

    open val workflowFactory: (Intent) -> Observable<*>? = {
        null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        integrationPoint.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        integrationPoint.onLowMemory()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        integrationPoint.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) =
        integrationPoint.onRequestPermissionsResult(requestCode, permissions, grantResults)

    override fun onBackPressed() {
        if (!integrationPoint.handleBackPress()) {
            super.onBackPressed()
        }
    }

}
