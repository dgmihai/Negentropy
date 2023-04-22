//package com.trajan.negentropy.server.util;
//
//import java.time.Duration;
//import java.util.Timer;
//import java.util.TimerTask;
//
//public class ElapsedTimeUpdater {
//    private Duration elapsedTime;
//    private Timer timer;
//    private TimerTask updateElapsedTimeTask;
//
//    public ElapsedTimeUpdater(Duration elapsedTime, Duration updateInterval, Duration durationToRun) {
//        this.elapsedTime = elapsedTime;
//        this.timer = new Timer();
//        this.updateElapsedTimeTask = new UpdateElapsedTimeTask();
//        // Schedule the timer to update the elapsed time every specified interval
//        // and stop after the specified duration
//        this.timer.schedule(updateElapsedTimeTask, 0, updateInterval.toMillis());
//        this.timer.schedule(new StopTimerTask(), durationToRun.toMillis());
//    }
//
//    public Duration getElapsedTime() {
//        return elapsedTime;
//    }
//
//    private class UpdateElapsedTimeTask extends TimerTask {
//        @Override
//        public void run() {
//            // Update the elapsed time by adding the update interval
//            elapsedTime = elapsedTime.plus(updateInterval);
//        }
//    }
//
//    private class StopTimerTask extends TimerTask {
//        @Override
//        public void run() {
//            // Cancel the updateElapsedTimeTask to stop updating the elapsed time
//            updateElapsedTimeTask.cancel();
//            // Cancel the timer to stop all scheduled tasks
//            timer.cancel();
//        }
//    }
//}