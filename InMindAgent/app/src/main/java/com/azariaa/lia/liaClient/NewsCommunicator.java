//package com.inMind.inMindAgent;
//
//import android.os.Handler;
//import android.os.Message;
//import android.util.Log;
//
//import com.yahoo.inmind.comm.generic.control.MessageBroker;
//import com.yahoo.inmind.comm.generic.model.MBRequest;
//import com.yahoo.inmind.commons.control.Constants;
//import com.yahoo.inmind.services.news.model.vo.NewsArticleVector;
//
//
///**
// * Created by Amos Azaria on 03-Feb-15.
// */
//public class NewsCommunicator
//{
//    private static Class className = NewsCommunicator.class;
//
//    public static void dealWithMessage(String args, Handler talkHandler)
//    {
//        //MessageBroker messageBroker = MessageBroker.getExistingInstance( className );
//        if (messageBroker == null)
//        {
//            Log.e("NewsCommunicator", "Error messageBroker==null");
//            return;
//        }
//        Log.d("Middleware", "Contacting News");
//
//        try
//        {
//            if (args.equalsIgnoreCase("launch"))
//            {
//                MessageBroker.set(MBRequest.build(Constants.SET_NEWS_LIST_SIZE, 40));
//                MBRequest request = MBRequest.build(Constants.MSG_LAUNCH_BASE_NEWS_ACTIVITY);
//                messageBroker.send( className, request);//TODO: fix!
//
//            }
//            else if (args.equalsIgnoreCase("next"))
//            {
//                MBRequest request = MBRequest.build(Constants.MSG_SHOW_NEXT_NEWS_ARTICLE);
//                messageBroker.send(className, request);
//            }
//            else if (args.equalsIgnoreCase("previous"))
//            {
//                    MBRequest request = MBRequest.build(Constants.MSG_SHOW_PREVIOUS_NEWS_ARTICLE);
//                    messageBroker.send(className, request);
//            }
//            else if (args.equalsIgnoreCase("expand"))
//            {
//                MBRequest request = MBRequest.build(Constants.MSG_EXPAND_NEWS_ARTICLE);
//                messageBroker.send(className, request);
//            }
//            else if (args.equalsIgnoreCase("read"))
//            {
//                MBRequest request = MBRequest.build(Constants.MSG_GET_ARTICLE_POSITION);
//                int position = (Integer) messageBroker.get(null, request);
//
//                String title = NewsArticleVector.getInstance().get( position ).getTitle();
//                String summary = NewsArticleVector.getInstance().get( position ).getSummary();
//                Message msgTalk = new Message();
//                msgTalk.arg1 = 0; //do not toast
//                msgTalk.obj = title + "." + summary;
//                talkHandler.sendMessage(msgTalk);
//            }
//        }
//        catch (Exception ex)
//        {
//            Log.e("Middleware", "Excpetion creating News Activity: " + ex.getMessage());
//        }
//
//    }
//}
