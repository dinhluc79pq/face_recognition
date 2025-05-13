package com.facerecognition.Server;

import org.opencv.core.Core;

public class CheckOpenCV {
    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.out.println("✅ OpenCV loaded: " + Core.VERSION);
    }
}
