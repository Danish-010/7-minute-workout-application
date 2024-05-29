package com.seven.sevenminuteworkout

import android.app.Dialog
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.seven.sevenminuteworkout.databinding.ActivityExerciseBinding
import com.seven.sevenminuteworkout.databinding.DialogCustomBackConfirmationBinding
import java.util.*
import kotlin.collections.ArrayList

class ExerciseActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var restTimer: CountDownTimer? = null
    private var restProgress = 0
    private var exerciseTimer: CountDownTimer? = null
    private var exerciseProgress = 0
    private var exerciseTimerDuration:Long = 1
    private var exerciseList: ArrayList<ExerciseModel>? = null
    private var currentExercisePosition = -1
    private var binding:ActivityExerciseBinding? = null
    private var tts: TextToSpeech? = null
    private var player: MediaPlayer? = null

    //  Declaring a variable of an adapter class to bind it to recycler view.
    // Declaring an exerciseAdapter object which will be initialized later.
    private var exerciseAdapter: ExerciseStatusAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExerciseBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        setSupportActionBar(binding?.toolbarExercise)
        if (supportActionBar != null){
            supportActionBar?.setDisplayHomeAsUpEnabled(true) // for back button placing on the toolbar
        }
        binding?.toolbarExercise?.setNavigationOnClickListener {
            customDialogForBackButton()// works similar to the back button as the bottom
        }
        tts = TextToSpeech(this, this)
        exerciseList = Constants.defaultExerciseList()
        setupRestView()
        //  Calling the function where we have bound the adapter to recycler view to show the data in the UI.
        // setting up the exercise recycler view
        setupExerciseStatusRecyclerView()
    }
    //Setting up the Get Ready View with 10 seconds of timer
//     Function is used to set the timer for REST.
    private fun setupRestView() {
        try {
            val soundURI = Uri.parse("android.resource://com.seven.sevenminuteworkout/" + R.raw.press_start)
            player = MediaPlayer.create(applicationContext, soundURI)
            player?.isLooping = false // Sets the player to be looping or non-looping.
            player?.start() // Starts Playback.
        } catch (e: Exception) {
            e.printStackTrace()
        }
        binding?.flRestView?.visibility = View.VISIBLE
        binding?.tvTitle?.visibility = View.VISIBLE
        binding?.upcomingLabel?.visibility = View.VISIBLE
        binding?.tvUpcomingExerciseName?.visibility = View.VISIBLE
        binding?.tvExerciseName?.visibility = View.INVISIBLE
        binding?.flExerciseView?.visibility = View.INVISIBLE
        binding?.ivImage?.visibility = View.INVISIBLE
        if (restTimer != null) {
            restTimer?.cancel()
            restProgress = 0
        }
        binding?.tvUpcomingExerciseName?.text = exerciseList!![currentExercisePosition + 1].getName()
        setRestProgressBar()
    }
    // Setting up the 10 seconds timer for rest view and updating it continuously.
    //START
    /**
     * Function is used to set the progress of timer using the progress
     */
    private fun setRestProgressBar() {

        binding?.progressBar?.progress = restProgress // Sets the current progress to the specified value.
        restTimer = object : CountDownTimer(1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                restProgress++
                binding?.progressBar?.progress = 10 - restProgress
                binding?.tvTimer?.text = (10 - restProgress).toString()
            }
            override fun onFinish(){
                currentExercisePosition++
                // When we are getting an updated position of exercise set that item in the list as selected and notify the adapter class.)
                exerciseList!![currentExercisePosition].setIsSelected(true) // Current Item is selected
                exerciseAdapter?.notifyDataSetChanged() // Notified the current item to adapter class to reflect it into UI.
           setupExerciseView()
            }
        }.start()
    }
    // Setting up the Exercise View with a 30 seconds timer
    //Function is used to set the progress of the timer using the progress for Exercise View.
    private fun setupExerciseView() {
        binding?.flRestView?.visibility = View.INVISIBLE
        binding?.tvTitle?.visibility = View.INVISIBLE
        binding?.tvUpcomingExerciseName?.visibility = View.INVISIBLE
        binding?.upcomingLabel?.visibility = View.INVISIBLE
        binding?.tvExerciseName?.visibility = View.VISIBLE
        binding?.flExerciseView?.visibility = View.VISIBLE
        binding?.ivImage?.visibility = View.VISIBLE

        if (exerciseTimer != null) {
            exerciseTimer?.cancel()
            exerciseProgress = 0
        }
        // Get the current exercise name from the list and pass it to the speak out method.
        speakOut(exerciseList!![currentExercisePosition].getName())
        // Setting up the current exercise name and imageview to the UI element.
         //Here current exercise name and image is set to exercise view.
        binding?.ivImage?.setImageResource(exerciseList!![currentExercisePosition].getImage())
        binding?.tvExerciseName?.text = exerciseList!![currentExercisePosition].getName()
        setExerciseProgressBar()

    }
     // Function to set the progress of the timer using the progress for Exercise View for 30 Seconds
    private fun setExerciseProgressBar() {
        binding?.progressBarExercise?.progress = exerciseProgress
        exerciseTimer = object : CountDownTimer(exerciseTimerDuration * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                exerciseProgress++
                binding?.progressBarExercise?.progress = exerciseTimerDuration.toInt() - exerciseProgress
               binding?.tvTimerExercise?.text = (exerciseTimerDuration.toInt() - exerciseProgress).toString()
            }
            override fun onFinish() {
                if (currentExercisePosition < exerciseList?.size!! - 1) {
                    exerciseList!![currentExercisePosition].setIsSelected(false) // exercise is completed so selection is set to false
                    exerciseList!![currentExercisePosition].setIsCompleted(true) // updating in the list that this exercise is completed
                    exerciseAdapter?.notifyDataSetChanged()
                    setupRestView()
                } else {
                     finish()
                  val intent = Intent(this@ExerciseActivity,FinishActivity::class.java)
                    startActivity(intent)
                }
            }
        }.start()
    }
    public override fun onDestroy() {
        if (restTimer != null) {
            restTimer?.cancel()
            restProgress = 0
        }
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
        if(player != null){
            player!!.stop()
        }
        super.onDestroy()
        binding = null
    }
//     This the TextToSpeech override function
//      Called to signal the completion of the TextToSpeech engine initialization.
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "The Language specified is not supported!")
            }
        } else {
            Log.e("TTS", "Initialization Failed!")
        }
    }
    // Making a function to speak the text.
    private fun speakOut(text: String) {
        tts!!.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
    }
    //Function to set up the recycler view to UI and asining the Layout Manager and Adapter Class is attached to it.
    // Binding adapter class to recycler view and setting the recycler view layout manager and passing a list to the adapter.
    private fun setupExerciseStatusRecyclerView() {
        // Defining a layout manager for the recycle view
        // Here i used a LinearLayout Manager with horizontal scroll.
       binding?.rvExerciseStatus?.layoutManager=LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        // As the adapter expects the exercises list and context so initialize it passing it.
        exerciseAdapter = ExerciseStatusAdapter(exerciseList!!)
        // Adapter class is attached to recycler view
        binding?.rvExerciseStatus?.adapter = exerciseAdapter
    }
    //function for custom confirmation dialog box
    private fun customDialogForBackButton() {
        val customDialog = Dialog(this)
         val dialogBinding = DialogCustomBackConfirmationBinding.inflate(layoutInflater)
        customDialog.setContentView(dialogBinding.root)
        /*to ensure that the user clicks one of the button and that the dialog is
        not dismissed when surrounding parts of the screen is clicked*/
        customDialog.setCanceledOnTouchOutside(false)
        dialogBinding.tvYes.setOnClickListener {
            this@ExerciseActivity.finish()
            customDialog.dismiss()
        }
        dialogBinding.tvNo.setOnClickListener {
            customDialog.dismiss()
        }
        customDialog.show()
    }
}