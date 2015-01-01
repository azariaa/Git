// ASRMicrosoftCpp.cpp : Defines the entry point for the console application.
//

#include "stdafx.h"
#include "ASRwrapper.h"
#include "InMind_Server_MicrosoftASR.h"

using namespace std;


//int _tmain(int argc, _TCHAR* argv[])

//std::wstring Java_To_WStr(JNIEnv *env, jstring string)
//{
//	std::wstring value;
//
//	const jchar *raw = env->GetStringChars(string, 0);
//	jsize len = env->GetStringLength(string);
//	const jchar *temp = raw;
//
//	value.assign(raw, raw + len);
//
//	env->ReleaseStringChars(string, raw);
//
//	return value;
//}

//JNIEXPORT jstring JNICALL Java_com_company_MicrosoftASR_fromFile(JNIEnv *env, jobject obj, jstring sPathToFile)
JNIEXPORT jstring JNICALL Java_InMind_Server_MicrosoftASR_fromByteArr(JNIEnv *env, jclass, jbyteArray jbyteJArr)
//int main()
{
	jboolean isCopy;
	jbyte* jbytePtr = env->GetByteArrayElements(jbyteJArr, &isCopy);
	jsize jarrSize = env->GetArrayLength(jbyteJArr);

	char* arrRec = (char*)jbytePtr;
	long arrSize = (long)jarrSize;
	std::string sretRes = "";
	if (arrRec != NULL && arrSize > 0)
	{
		std::wstring speechRes = CASRwrapper::DecodeFromCharArr(arrRec, arrSize); // (*env)->GetStringUTFChars(env, string, 0); ////Java_To_WStr(env, sPathToFile);

		std::string sspeachRes(speechRes.begin(), speechRes.end()); //converting from wstring to string
		sretRes = sspeachRes;

		cout << sspeachRes << endl;

		env->ReleaseByteArrayElements(jbyteJArr, jbytePtr, JNI_ABORT);
	}
	return env->NewStringUTF(sretRes.c_str());
}

