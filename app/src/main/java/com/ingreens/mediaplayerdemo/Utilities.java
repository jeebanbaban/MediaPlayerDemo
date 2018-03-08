package com.ingreens.mediaplayerdemo;

/**
 * Created by root on 8/3/18.
 */

public class Utilities {
    public Utilities() {
    }

    public String milliSecondsToTimer(long milliseconds) {
        String finalTimerString = "";
        String secondsString = "";
        int hours = (int)(milliseconds / 3600000L);
        int minutes = (int)(milliseconds % 3600000L) / '\uea60';
        int seconds = (int)(milliseconds % 3600000L % 60000L / 1000L);
        if(hours > 0) {
            finalTimerString = hours + ":";
        }

        if(seconds < 10) {
            secondsString = "0" + seconds;
        } else {
            secondsString = "" + seconds;
        }

        finalTimerString = finalTimerString + minutes + ":" + secondsString;
        return finalTimerString;
    }

    public int getProgressPercentage(long currentDuration, long totalDuration) {
        Double percentage = Double.valueOf(0.0D);
        long currentSeconds = (long)((int)(currentDuration / 1000L));
        long totalSeconds = (long)((int)(totalDuration / 1000L));
        percentage = Double.valueOf((double)currentSeconds / (double)totalSeconds * 100.0D);
        return percentage.intValue();
    }

    public int progressToTimer(int progress, int totalDuration) {
        int currentDuration = 0;
        totalDuration = (int) (totalDuration / 1000);
        currentDuration = (int) ((((double)progress) / 100) * totalDuration);

        // return current duration in milliseconds
        return currentDuration * 1000;
    }
}
