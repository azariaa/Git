#pragma once
#define GID_DICTATION   0           // Dictation grammar has grammar ID 0
class CASRwrapper
{

	//Initialize the Recognizer
	CComPtr<ISpRecognizer> cpRecoEngine;

	//The ISpRecoContext interface enables applications to create 
	//different functional views or contexts of the SR engine
	CComPtr<ISpRecoContext>     m_cpRecoCtxt;

	//The ISpRecoGrammar interface enables applications to manage 
	//the words and phrases that the SR engine will recognize.
	CComPtr<ISpRecoGrammar>		m_cpDictationGrammar;


private:
	//ISpVoice* m_pVoice;
public:
	CASRwrapper();
	HRESULT InitSpeech(std::wstring sPathToFile = L"");
	~CASRwrapper();

	HRESULT Listen();
	HRESULT CASRwrapper::StopListenning();
	void CASRwrapper::GetText(std::wstring& speechRes, float* pconfidence = NULL, int requestedAlternates = 0, std::wstring alternates[] = NULL, float alternatesConfidence[] = NULL);
	HANDLE GetNotifyHandle();

	static void __stdcall SpRecCallback(WPARAM wParam, LPARAM lParam);
};

