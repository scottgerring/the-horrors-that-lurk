#include <jni.h>
#include <stdio.h>
#include <string.h>
#include <time.h>
#include "com_horror_TagLibrary.h"

/* Helper function to add key-value pairs to a Java map */
void addToMap(JNIEnv *env, jobject map, const char *key, const char *value) {
    jclass mapClass = (*env)->GetObjectClass(env, map);
    jmethodID putMethod = (*env)->GetMethodID(env, mapClass, "put", 
                         "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

    jstring jKey = (*env)->NewStringUTF(env, key);
    jstring jValue = (*env)->NewStringUTF(env, value);

    (*env)->CallObjectMethod(env, map, putMethod, jKey, jValue);

    (*env)->DeleteLocalRef(env, jKey);
    (*env)->DeleteLocalRef(env, jValue);
}

/* Helper function to get the current date as yyyy-mm-dd */
void getCurrentDate(char *buffer, size_t bufferSize) {
    time_t now = time(NULL);
    struct tm *tm_info = localtime(&now);

    // Format the date as yyyy-mm-dd
    strftime(buffer, bufferSize, "%Y-%m-%d", tm_info);
}

/* JNI function implementation */
JNIEXPORT jobject JNICALL Java_TagLibrary_getTags(JNIEnv *env, jobject obj) {
    // Create a new HashMap
    jclass hashMapClass = (*env)->FindClass(env, "java/util/HashMap");
    jmethodID init = (*env)->GetMethodID(env, hashMapClass, "<init>", "()V");
    jobject hashMap = (*env)->NewObject(env, hashMapClass, init);

    // Add predefined entries to the HashMap
    addToMap(env, hashMap, "company", "Datadog");
    addToMap(env, hashMap, "department", "Engineering");
    addToMap(env, hashMap, "location", "New York");

    // Get the current date
    char currentDate[11]; // Buffer for "yyyy-mm-dd\0"
    getCurrentDate(currentDate, sizeof(currentDate));

    // Add the current date as a tag
    addToMap(env, hashMap, "current_date", currentDate);

    return hashMap;
}
